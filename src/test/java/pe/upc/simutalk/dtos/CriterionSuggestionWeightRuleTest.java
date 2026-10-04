package pe.upc.simutalk.dtos;

import pe.upc.simutalk.entities.JobPosting;
import pe.upc.simutalk.entities.Weight;
import pe.upc.simutalk.enums.CriterionOrigin;
import pe.upc.simutalk.enums.CriterionType;
import pe.upc.simutalk.mappers.CriterionSuggestionResourceFromValueAssembler;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * US-04 rule: the system never assigns a weight on its own; the recruiter always sets it.
 */
class CriterionSuggestionWeightRuleTest {

    @Test
    void aSuggestionHasNoWeightAnywhereInTheDomainOrTheApi() {
        assertThat(componentNames(CriterionSuggestion.class)).noneMatch(name -> name.toLowerCase(Locale.ROOT).contains("weight"));
        assertThat(componentNames(CriterionSuggestionResource.class)).noneMatch(name -> name.toLowerCase(Locale.ROOT).contains("weight"));
    }

    @Test
    void theSuggestionsResponseBodyHasNoWeight() {
        var resource = CriterionSuggestionResourceFromValueAssembler.toResourceFromValue(
                new CriterionSuggestion("Pensamiento analítico", "Usa datos", "datos"));

        var json = new ObjectMapper().writeValueAsString(resource);

        assertThat(json).doesNotContainIgnoringCase("weight").contains("\"origin\":\"AI_SUGGESTED\"");
    }

    @Test
    void acceptingASuggestionRequiresTheWeightTheRecruiterChooses() {
        var jobPosting = new JobPosting(new CreateJobPostingCommand("Analista", "Datos", 1L, LocalDate.now().plusDays(30), false));

        assertThatThrownBy(() -> new Weight(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jobPosting.addCriterion("Pensamiento analítico", "Usa datos", null,
                CriterionType.COMPETENCY, null, false, CriterionOrigin.AI_SUGGESTED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jobPosting.getCriterionNames()).isEmpty();

        var accepted = jobPosting.addCriterion("Pensamiento analítico", "Usa datos", new Weight(35),
                CriterionType.COMPETENCY, null, false, CriterionOrigin.AI_SUGGESTED);

        assertThat(accepted.getWeight().value()).isEqualTo(35);
        assertThat(accepted.getOrigin()).isEqualTo(CriterionOrigin.AI_SUGGESTED);
    }

    private static java.util.List<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}
