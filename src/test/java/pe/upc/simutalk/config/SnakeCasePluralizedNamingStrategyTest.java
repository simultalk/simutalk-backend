package pe.upc.simutalk.config;

import org.hibernate.boot.model.naming.Identifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SnakeCasePluralizedNamingStrategyTest {

    private final SnakeCasePluralizedNamingStrategy strategy =
            new SnakeCasePluralizedNamingStrategy();

    @ParameterizedTest
    @CsvSource({
            "JobPosting, job_postings",
            "User, users",
            "Role, roles",
            "Company, companies",
            "Box, boxes",
            "EvaluationCriterion, evaluation_criteria",
            "job_postings, job_postings",
            "evaluation_criteria, evaluation_criteria",
            "user_roles, user_roles",
            "users, users"
    })
    void convertsTableNamesToPluralSnakeCase(String logical, String expected) {
        var physical = strategy.toPhysicalTableName(Identifier.toIdentifier(logical), null);
        assertThat(physical.getText()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "createdAt, created_at",
            "anonymizedScreening, anonymized_screening",
            "jobPosting_id, job_posting_id",
            "company_id, company_id",
            "id, id"
    })
    void convertsColumnNamesToSnakeCaseWithoutPluralizing(String logical, String expected) {
        var physical = strategy.toPhysicalColumnName(Identifier.toIdentifier(logical), null);
        assertThat(physical.getText()).isEqualTo(expected);
    }
}
