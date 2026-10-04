package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CompleteInterviewSessionCommand;
import pe.upc.simutalk.dtos.CreateInterviewSessionCommand;
import pe.upc.simutalk.dtos.CreateQuestionCommand;
import pe.upc.simutalk.dtos.GetInterviewSessionByApplicationIdQuery;
import pe.upc.simutalk.dtos.GetQuestionsByJobPostingIdQuery;
import pe.upc.simutalk.dtos.RecordAnswerCommand;
import pe.upc.simutalk.dtos.StartInterviewSessionCommand;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.enums.QuestionOrigin;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.InterviewSessionCommandService;
import pe.upc.simutalk.services.InterviewSessionQueryService;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.QuestionCommandService;
import pe.upc.simutalk.services.QuestionQueryService;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interviews part of the demo data ({@code app.seed-demo-data=true}), two steps of the ordered
 * demo sequence described in recruitment's seeder:
 * <ul>
 *   <li>300 ({@link #writeScript}): while the demo job posting is in DRAFT and has no questions,
 *       writes a 6-question script over its COMPETENCY criteria, one of them allowing a follow-up.</li>
 *   <li>500 ({@link #runInterviews}): once it is PUBLISHED and none of its applications has a session,
 *       rosa.quispe and jorge.huaman get a COMPLETED session with the 6 answers (jorge's includes a
 *       follow-up) and carmen.ramos and diego.salazar an IN_PROGRESS one with 3 and 5 answers.
 *       The other applications stay RECEIVED.</li>
 * </ul>
 * The demo job posting is found through the shared facades; everything goes through the command
 * services, so the same rules as the API apply. Idempotent: each step checks first whether its data
 * already exists, and a session is only created for an application that is still RECEIVED. A failure
 * is logged as WARN and never stops the application from starting.
 */
@Slf4j
@Service
public class InterviewDemoDataSeeder {

    private static final String COMPANY_USERNAME = "consultora.andina";
    private static final int DEMO_SESSION_VALIDITY_DAYS = 14;

    /** Candidates whose interview ends COMPLETED, in the order of {@link #ASSESSED_ANSWERS}. */
    private static final List<String> COMPLETED_INTERVIEWS = List.of("rosa.quispe", "jorge.huaman");

    /** Candidates whose interview stays IN_PROGRESS, in the order of {@link #INTERVIEWING_ANSWERS}. */
    private static final List<String> IN_PROGRESS_INTERVIEWS = List.of("carmen.ramos", "diego.salazar");

    private final boolean enabled;
    private final QuestionCommandService questionCommandService;
    private final QuestionQueryService questionQueryService;
    private final InterviewSessionCommandService interviewSessionCommandService;
    private final InterviewSessionQueryService interviewSessionQueryService;
    private final IamContextFacade iamContextFacade;
    private final ProfilesContextFacade profilesContextFacade;
    private final RecruitmentContextFacade recruitmentContextFacade;
    private final TransactionTemplate transactionTemplate;

    public InterviewDemoDataSeeder(@Value("${app.seed-demo-data:false}") boolean enabled,
                                    QuestionCommandService questionCommandService,
                                    QuestionQueryService questionQueryService,
                                    InterviewSessionCommandService interviewSessionCommandService,
                                    InterviewSessionQueryService interviewSessionQueryService,
                                    IamContextFacade iamContextFacade,
                                    ProfilesContextFacade profilesContextFacade,
                                    RecruitmentContextFacade recruitmentContextFacade,
                                    TransactionTemplate transactionTemplate) {
        this.enabled = enabled;
        this.questionCommandService = questionCommandService;
        this.questionQueryService = questionQueryService;
        this.interviewSessionCommandService = interviewSessionCommandService;
        this.interviewSessionQueryService = interviewSessionQueryService;
        this.iamContextFacade = iamContextFacade;
        this.profilesContextFacade = profilesContextFacade;
        this.recruitmentContextFacade = recruitmentContextFacade;
        this.transactionTemplate = transactionTemplate;
    }

    private record DemoQuestion(int criterionIndex, String statement, int maxDurationSeconds, boolean allowsFollowUp) {
    }

    /** criterionIndex: 0 = analytical thinking, 1 = communication (COMPETENCY criteria in id order). */
    private static final List<DemoQuestion> SCRIPT = List.of(
            new DemoQuestion(0, "Un cliente reporta que sus ventas mensuales cayeron 15 % respecto al mes anterior. "
                    + "¿Cómo usarías los datos disponibles para encontrar la causa?", 240, true),
            new DemoQuestion(0, "Recibes una tabla de pedidos con clientes duplicados, montos vacíos y fechas en formatos "
                    + "distintos. ¿Qué pasos seguirías para limpiarla antes de analizarla?", 180, false),
            new DemoQuestion(0, "Explica la diferencia entre un LEFT JOIN y un INNER JOIN y cuándo usarías cada uno "
                    + "al cruzar la tabla de clientes con la de pedidos.", 150, false),
            new DemoQuestion(0, "¿Cómo detectarías y qué harías con valores atípicos en los montos de facturación?",
                    150, false),
            new DemoQuestion(1, "¿Cómo le presentarías un hallazgo negativo a un gerente comercial que no tiene "
                    + "formación técnica?", 180, false),
            new DemoQuestion(1, "Un cliente cuestiona las cifras de tu reporte en una reunión. ¿Cómo le explicas de dónde "
                    + "salen los números?", 180, false));

    private record DemoAnswer(String transcript, int durationSeconds) {
    }

    /** Transcripts per demo interview, one per script question, in script order. */
    private static final List<List<DemoAnswer>> ASSESSED_ANSWERS = List.of(
            List.of(
                    new DemoAnswer("Primero confirmaría que la caída es real y no un problema de carga: comparo el total del "
                            + "reporte con la fuente y reviso si faltan días. Luego la descompongo por región, canal y "
                            + "categoría para ver dónde se concentra, y separo volumen de ticket promedio: no es lo mismo "
                            + "vender menos unidades que vender más barato. Con eso armo dos o tres hipótesis y las valido "
                            + "con el área comercial.", 205),
                    new DemoAnswer("Empezaría perfilando la tabla: cuántas filas, cuántos nulos por columna y cuántos "
                            + "clientes aparecen más de una vez con el mismo documento. Para los duplicados me quedaría con "
                            + "el registro más reciente usando ROW_NUMBER particionado por documento. Las fechas las "
                            + "normalizaría a formato ISO y los montos vacíos no los llenaría con cero: los marcaría y "
                            + "preguntaría si son pedidos anulados.", 170),
                    new DemoAnswer("El INNER JOIN solo devuelve clientes que tienen pedidos; el LEFT JOIN devuelve todos los "
                            + "clientes y deja nulos donde no hay pedidos. Si quiero saber cuántos clientes no compraron "
                            + "este mes uso LEFT JOIN y filtro donde el id del pedido es nulo. Para sumar ventas por "
                            + "cliente, el INNER JOIN es suficiente.", 120),
                    new DemoAnswer("Calcularía el rango intercuartílico y marcaría lo que queda fuera de 1,5 veces ese rango, "
                            + "pero no lo borraría de inmediato. Revisaría algunos casos con el equipo de facturación: a "
                            + "veces es una venta corporativa legítima y a veces un error de digitación con ceros de más. "
                            + "En el reporte mostraría la mediana además del promedio.", 140),
                    new DemoAnswer("Empezaría por la conclusión en una frase, sin tecnicismos: por ejemplo, que la zona norte "
                            + "explica casi toda la caída. Luego mostraría un solo gráfico que lo respalde y cerraría con "
                            + "lo que se puede hacer. Si pregunta por el método, tengo el detalle en un anexo, pero no "
                            + "abro con eso.", 150),
                    new DemoAnswer("Le mostraría la trazabilidad: de qué sistema sale cada cifra, qué filtros apliqué y en qué "
                            + "fecha se extrajo. Si la diferencia es con su propio número, busco juntos el origen, que "
                            + "suele ser un filtro distinto, como incluir o no las devoluciones. Me comprometo a enviarle "
                            + "la conciliación por escrito.", 160)),
            List.of(
                    new DemoAnswer("Lo primero es comparar contra el mismo mes del año anterior, porque una caída mensual "
                            + "puede ser estacional. Si se confirma, filtraría por producto y por tienda para ver si hay "
                            + "un grupo que arrastra el resultado, y cruzaría con cambios de precio o quiebres de stock "
                            + "de ese mes.", 190),
                    new DemoAnswer("Unificaría las fechas con una función de conversión y dejaría en una tabla aparte las que "
                            + "no se puedan convertir, para revisarlas. Los duplicados los identificaría por documento y "
                            + "correo, porque el nombre suele venir escrito de formas distintas. Antes de borrar nada "
                            + "guardaría una copia de la tabla original.", 160),
                    new DemoAnswer("Con INNER JOIN pierdo a los clientes sin pedidos, así que para un reporte de cobertura "
                            + "de cartera usaría LEFT JOIN desde clientes. Hay que tener cuidado al contar después: se "
                            + "cuenta el id del pedido, no las filas, porque el LEFT JOIN genera una fila aunque no haya "
                            + "pedido.", 115),
                    new DemoAnswer("Graficaría la distribución de montos por mes para ver si los atípicos se concentran en una "
                            + "fecha o en un vendedor. Si son errores, los corrijo en la fuente con quien corresponda; si "
                            + "son reales, los analizo por separado para que no distorsionen el promedio del resto.", 125),
                    new DemoAnswer("Traduciría el hallazgo a dinero y a una decisión: cuánto se dejó de vender y qué se podría "
                            + "hacer la próxima semana. Evito porcentajes sobre porcentajes y uso comparaciones que el "
                            + "gerente conozca, como el mismo mes del año pasado.", 135),
                    new DemoAnswer("Primero escucharía cuál es la cifra que no le cuadra. Después le mostraría el cálculo paso "
                            + "a paso, con el mismo filtro que él usa, y si encuentro un error lo reconozco en la reunión "
                            + "y envío el reporte corregido ese mismo día.", 140)));

    /** Follow-up to the first question, recorded in the second ASSESSED interview. */
    private static final DemoAnswer FOLLOW_UP = new DemoAnswer("Si la caída se concentra en una sola tienda, revisaría "
            + "primero eventos operativos de ese mes: si cerró algunos días, si cambió el horario o si hubo quiebre de stock "
            + "en los productos que más vende. Lo validaría con el jefe de tienda antes de presentarlo como causa.", 95);

    private static final List<List<DemoAnswer>> INTERVIEWING_ANSWERS = List.of(
            List.of(
                    new DemoAnswer("Revisaría si la caída es igual en todos los productos o solo en algunos. Haría una tabla "
                            + "dinámica por categoría y mes para comparar, y hablaría con ventas para saber si hubo "
                            + "cambios de precio o promociones que terminaron.", 150),
                    new DemoAnswer("Usaría Power Query para quitar duplicados y dar formato de fecha a la columna. Los montos "
                            + "vacíos los revisaría uno por uno si son pocos; si son muchos, preguntaría al área de "
                            + "facturación antes de decidir qué hacer con ellos.", 130),
                    new DemoAnswer("El LEFT JOIN trae todos los registros de la tabla de la izquierda aunque no tengan pareja "
                            + "en la otra, y el INNER JOIN solo los que coinciden en ambas. Para listar clientes con y sin "
                            + "pedidos usaría el LEFT JOIN.", 95)),
            List.of(
                    new DemoAnswer("Separaría la caída en cantidad de pedidos y valor promedio por pedido. Si bajaron los "
                            + "pedidos, miro canales y campañas; si bajó el valor, miro descuentos y mezcla de productos. "
                            + "Así sé a qué área llevarle la pregunta.", 160),
                    new DemoAnswer("Con SQL haría un SELECT DISTINCT sobre las columnas clave para ver cuántos duplicados hay "
                            + "y luego los eliminaría conservando el de menor id. Para las fechas usaría TO_DATE con cada "
                            + "formato detectado y dejaría registro de las filas que modifiqué.", 145),
                    new DemoAnswer("Si hago INNER JOIN entre clientes y pedidos, un cliente que nunca compró desaparece del "
                            + "resultado. Con LEFT JOIN aparece con valores nulos, que es lo que necesito si quiero medir "
                            + "clientes inactivos.", 90),
                    new DemoAnswer("Primero confirmaría si el valor es posible: una factura de diez veces el promedio puede ser "
                            + "un cliente corporativo. Si es un error, lo excluyo del análisis y lo reporto; si es real, "
                            + "lo dejo pero lo señalo en el gráfico.", 110),
                    new DemoAnswer("Le diría directamente qué pasó y por qué, con un ejemplo concreto de una tienda o un "
                            + "producto. Prefiero un gráfico de barras simple antes que una tabla llena de números, y "
                            + "terminaría con una recomendación.", 120)));

    @EventListener(ApplicationReadyEvent.class)
    @Order(300)
    public void writeScript(ApplicationReadyEvent event) {
        // Demo data must never keep the application from starting.
        try {
            seedScript();
        } catch (RuntimeException ex) {
            log.warn("Interviews demo data: the interview script could not be written; the application starts without it", ex);
        }
    }

    private void seedScript() {
        var jobPostingId = demoJobPostingId();
        if (jobPostingId == 0L || !recruitmentContextFacade.isJobPostingDraft(jobPostingId)) {
            return;
        }
        if (!questionQueryService.handle(new GetQuestionsByJobPostingIdQuery(jobPostingId)).isEmpty()) {
            log.info("Interviews demo data skipped: the demo job posting already has an interview script");
            return;
        }
        var criterionIds = recruitmentContextFacade.fetchCompetencyCriterionIds(jobPostingId);
        transactionTemplate.executeWithoutResult(status -> {
            for (var demo : SCRIPT) {
                questionCommandService.handle(new CreateQuestionCommand(jobPostingId, criterionIds.get(demo.criterionIndex()),
                        demo.statement(), demo.maxDurationSeconds(), QuestionOrigin.MANUAL, demo.allowsFollowUp()));
            }
        });
        log.info("Interviews demo data: {} questions written for job posting {}", SCRIPT.size(), jobPostingId);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(500)
    public void runInterviews(ApplicationReadyEvent event) {
        // Demo data must never keep the application from starting.
        try {
            seedInterviews();
        } catch (RuntimeException ex) {
            log.warn("Interviews demo data: the demo interview sessions could not be seeded; the application starts without them", ex);
        }
    }

    private void seedInterviews() {
        var jobPostingId = demoJobPostingId();
        if (jobPostingId == 0L || !recruitmentContextFacade.isJobPostingPublished(jobPostingId)) {
            return;
        }
        var applicationIds = recruitmentContextFacade.fetchApplicationIds(jobPostingId);
        if (applicationIds.isEmpty()) {
            return;
        }
        if (applicationIds.stream().anyMatch(applicationId ->
                interviewSessionQueryService.handle(new GetInterviewSessionByApplicationIdQuery(applicationId)).isPresent())) {
            log.info("Interviews demo data skipped: the demo job posting already has interview sessions");
            return;
        }
        var script = questionQueryService.handle(new GetQuestionsByJobPostingIdQuery(jobPostingId));
        var completed = new AtomicInteger();
        var inProgress = new AtomicInteger();
        transactionTemplate.executeWithoutResult(status -> {
            for (var index = 0; index < COMPLETED_INTERVIEWS.size(); index++) {
                var applicationId = applicationIdOf(COMPLETED_INTERVIEWS.get(index), applicationIds);
                if (!isReceived(applicationId, COMPLETED_INTERVIEWS.get(index))) {
                    continue;
                }
                var sessionId = startSession(applicationId);
                answer(sessionId, script, ASSESSED_ANSWERS.get(index), index == 1);
                interviewSessionCommandService.handle(new CompleteInterviewSessionCommand(sessionId));
                completed.incrementAndGet();
            }
            for (var index = 0; index < IN_PROGRESS_INTERVIEWS.size(); index++) {
                var applicationId = applicationIdOf(IN_PROGRESS_INTERVIEWS.get(index), applicationIds);
                if (isReceived(applicationId, IN_PROGRESS_INTERVIEWS.get(index))) {
                    answer(startSession(applicationId), script, INTERVIEWING_ANSWERS.get(index), false);
                    inProgress.incrementAndGet();
                }
            }
        });
        log.info("Interviews demo data: {} completed and {} in-progress sessions", completed, inProgress);
    }

    /** The demo company's first job posting (the one recruitment's seeder created), or {@code 0L}. */
    private Long demoJobPostingId() {
        if (!enabled) {
            return 0L;
        }
        var companyId = profilesContextFacade.fetchCompanyIdByUserId(userIdOf(COMPANY_USERNAME));
        var jobPostingIds = companyId == 0L ? List.<Long>of() : recruitmentContextFacade.fetchJobPostingIdsByCompanyId(companyId);
        return jobPostingIds.isEmpty() ? 0L : jobPostingIds.getFirst();
    }

    /** A session can only be created for a RECEIVED application; anything else is left as it is. */
    private boolean isReceived(Long applicationId, String username) {
        if (applicationId == 0L) {
            return false;
        }
        var status = recruitmentContextFacade.fetchApplicationStatus(applicationId);
        if (!"RECEIVED".equals(status)) {
            log.info("Interviews demo data: {}'s application {} is {}, not RECEIVED; no session created for it",
                    username, applicationId, status);
            return false;
        }
        return true;
    }

    private Long applicationIdOf(String username, List<Long> applicationIds) {
        var candidateId = profilesContextFacade.fetchCandidateIdByUserId(userIdOf(username));
        return applicationIds.stream()
                .filter(applicationId -> candidateId != 0L
                        && candidateId.equals(recruitmentContextFacade.fetchCandidateIdByApplicationId(applicationId)))
                .findFirst()
                .orElse(0L);
    }

    private Long userIdOf(String username) {
        var userId = iamContextFacade.fetchUserIdByUsername(username);
        return userId == null ? 0L : userId;
    }

    private Long startSession(Long applicationId) {
        var session = interviewSessionCommandService.handle(
                new CreateInterviewSessionCommand(applicationId, LocalDate.now().plusDays(DEMO_SESSION_VALIDITY_DAYS)));
        interviewSessionCommandService.handle(new StartInterviewSessionCommand(session.getId()));
        return session.getId();
    }

    private void answer(Long sessionId, List<Question> script, List<DemoAnswer> answers, boolean withFollowUp) {
        for (var index = 0; index < answers.size(); index++) {
            var question = script.get(index);
            var demo = answers.get(index);
            var recorded = interviewSessionCommandService.handle(new RecordAnswerCommand(sessionId, question.getId(),
                    demo.transcript(), null, demo.durationSeconds(), false, null));
            if (withFollowUp && question.isAllowsFollowUp()) {
                interviewSessionCommandService.handle(new RecordAnswerCommand(sessionId, question.getId(),
                        FOLLOW_UP.transcript(), null, FOLLOW_UP.durationSeconds(), true, recorded.getId()));
            }
        }
    }
}
