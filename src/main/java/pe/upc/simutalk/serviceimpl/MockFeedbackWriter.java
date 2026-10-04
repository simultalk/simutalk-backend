package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.AnswerScoringService.CriterionFeedback;

import pe.upc.simutalk.enums.CriterionKind;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic candidate feedback, used in mock mode and as the fallback in live mode. It only
 * summarizes the numbers it receives: the criterion that sustained the score most and the one where
 * most weighted points were lost. It knows nothing about the ranking or other candidates.
 */
final class MockFeedbackWriter {

    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private MockFeedbackWriter() {
    }

    static String write(BigDecimal weightedScore, List<CriterionFeedback> criteria) {
        var text = new StringBuilder("Tu puntaje ponderado fue %s de 10.".formatted(format(weightedScore)));
        if (criteria == null || criteria.isEmpty()) {
            return text.toString();
        }
        var strongest = criteria.stream()
                .max(Comparator.comparing(CriterionFeedback::score).thenComparingInt(CriterionFeedback::weightApplied))
                .orElseThrow();
        var weakest = criteria.stream()
                .max(Comparator.comparing(MockFeedbackWriter::lostPoints).thenComparingInt(CriterionFeedback::weightApplied))
                .orElseThrow();
        text.append(" Lo que más sostuvo tu puntaje fue «%s» (%s de 10)".formatted(strongest.criterionName(),
                format(strongest.score())));
        text.append(strongest.criterionKind() == CriterionKind.CERTIFICATION
                ? ", gracias a tus certificaciones verificadas."
                : ", donde tus respuestas mostraron evidencia concreta de lo que pide el criterio.");
        if (weakest.equals(strongest) || lostPoints(weakest).signum() == 0) {
            text.append(" No perdiste puntos relevantes en ningún criterio.");
            return text.toString();
        }
        text.append(" Donde más puntos se perdieron fue «%s» (%s de 10, peso %d %%)".formatted(weakest.criterionName(),
                format(weakest.score()), weakest.weightApplied()));
        text.append(weakest.criterionKind() == CriterionKind.CERTIFICATION
                ? ": una certificación verificada y vigente relacionada con el puesto lo mejoraría."
                : ": respuestas con ejemplos concretos de ese criterio lo mejorarían.");
        text.append(" Cada punto que mejores ahí suma %s a tu puntaje ponderado.".formatted(
                format(BigDecimal.valueOf(weakest.weightApplied()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP))));
        return text.toString();
    }

    /** Weighted points below the maximum: (10 − score) × weight / 100. */
    private static BigDecimal lostPoints(CriterionFeedback criterion) {
        return TEN.subtract(criterion.score()).multiply(BigDecimal.valueOf(criterion.weightApplied()));
    }

    private static String format(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
