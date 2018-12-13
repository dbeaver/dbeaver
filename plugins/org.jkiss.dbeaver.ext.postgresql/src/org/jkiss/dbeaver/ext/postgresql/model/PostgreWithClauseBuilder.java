package org.jkiss.dbeaver.ext.postgresql.model;

public class PostgreWithClauseBuilder {

    public static String generateWithClause(PostgreTableRegular table) {
        StringBuilder withClauseBuilder = new StringBuilder();

        if (table.getDataSource().getServerType().supportsOids() && table.isHasOids()) {
            withClauseBuilder.append("\nWITH (\n\tOIDS=").append(table.isHasOids() ? "TRUE" : "FALSE");
            withClauseBuilder.append("\n)");
        }

        return withClauseBuilder.toString();
    }
}
