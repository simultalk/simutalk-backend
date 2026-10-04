package pe.upc.simutalk.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts every identifier to snake_case and pluralizes table names, so the entity
 * {@code JobPosting} maps to the table {@code job_postings}.
 * <p>
 * Hibernate also passes explicit {@code @Table} names through this strategy, so names
 * that are already plural ({@code job_postings}, {@code evaluation_criteria},
 * {@code user_roles}) are left untouched.
 */
public class SnakeCasePluralizedNamingStrategy implements PhysicalNamingStrategy {

    private static final Map<String, String> IRREGULAR_PLURALS = Map.of(
            "criterion", "criteria",
            "datum", "data",
            "person", "people",
            "child", "children");

    private static final Set<String> ALREADY_PLURAL = Set.copyOf(IRREGULAR_PLURALS.values());

    @Override
    public Identifier toPhysicalCatalogName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return toSnakeCase(identifier);
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return toSnakeCase(identifier);
    }

    @Override
    public Identifier toPhysicalTableName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        if (identifier == null) {
            return null;
        }
        return Identifier.toIdentifier(pluralize(snakeCase(identifier.getText())), identifier.isQuoted());
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return toSnakeCase(identifier);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return toSnakeCase(identifier);
    }

    private static Identifier toSnakeCase(Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        return Identifier.toIdentifier(snakeCase(identifier.getText()), identifier.isQuoted());
    }

    static String snakeCase(String name) {
        return name
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace('.', '_')
                .toLowerCase(Locale.ROOT);
    }

    static String pluralize(String snakeCaseName) {
        var separator = snakeCaseName.lastIndexOf('_');
        var prefix = snakeCaseName.substring(0, separator + 1);
        var lastWord = snakeCaseName.substring(separator + 1);

        if (lastWord.isEmpty() || lastWord.endsWith("s") || ALREADY_PLURAL.contains(lastWord)) {
            return snakeCaseName;
        }
        if (IRREGULAR_PLURALS.containsKey(lastWord)) {
            return prefix + IRREGULAR_PLURALS.get(lastWord);
        }
        if (lastWord.matches(".*[^aeiou]y")) {
            return prefix + lastWord.substring(0, lastWord.length() - 1) + "ies";
        }
        if (lastWord.matches(".*(x|z|ch|sh)")) {
            return prefix + lastWord + "es";
        }
        return prefix + lastWord + "s";
    }
}
