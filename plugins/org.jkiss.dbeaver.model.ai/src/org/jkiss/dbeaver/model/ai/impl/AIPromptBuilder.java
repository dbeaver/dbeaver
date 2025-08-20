/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AIPromptBuilder {
    private final List<String> goals = new ArrayList<>();
    private final List<String> instructions = new ArrayList<>();
    private final List<String> examples = new ArrayList<>();
    private final List<String> contexts = new ArrayList<>();
    private final List<String> outputFormats = new ArrayList<>();

    private AIPromptBuilder() {
    }

    @NotNull
    public static AIPromptBuilder create() {
        return new AIPromptBuilder();
    }

    public AIPromptBuilder addGoals(@NotNull String... goals) {
        this.goals.addAll(Arrays.asList(goals));
        return this;
    }

    public AIPromptBuilder addExamples(@NotNull String... examples) {
        this.examples.addAll(Arrays.asList(examples));
        return this;
    }

    public AIPromptBuilder addInstructions(@NotNull String... instructions) {
        this.instructions.addAll(Arrays.asList(instructions));
        return this;
    }

    public AIPromptBuilder addContexts(@NotNull String... contexts) {
        this.contexts.addAll(Arrays.asList(contexts));
        return this;
    }

    public AIPromptBuilder addOutputFormats(@NotNull String... outputFormats) {
        this.outputFormats.addAll(Arrays.asList(outputFormats));
        return this;
    }

    public String build() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Goals:\n");
        goals.forEach(goal -> prompt.append("- ").append(goal).append("\n"));

        if (!instructions.isEmpty()) {
            prompt.append("\nInstructions:\n");
            instructions.forEach(instruction -> prompt.append("- ").append(instruction).append("\n"));
        }

        if (!examples.isEmpty()) {
            prompt.append("\nExamples:\n");
            examples.forEach(example -> prompt.append("- ").append(example).append("\n"));
        }

        prompt.append("\nContext:\n");
        contexts.forEach(context -> prompt.append("- ").append(context).append("\n"));

        if (!outputFormats.isEmpty()) {
            prompt.append("\nOutput Format:\n");
            outputFormats.forEach(outputFormat -> prompt.append("- ").append(outputFormat).append("\n"));
        }

        return prompt.toString();
    }

    public static String[] describeContext(@Nullable DBSLogicalDataSource dataSource) {
        SQLDialect dialect = dataSource == null ? BasicSQLDialect.INSTANCE :
            SQLUtils.getDialectFromDataSource(dataSource.getDataSourceContainer().getDataSource());
        List<String> lines = new ArrayList<>();

        if (dataSource != null) {
            DBPDataSource ds = dataSource.getDataSourceContainer().getDataSource();
            DBPDataSourceInfo dsInfo = ds == null ? null : ds.getInfo();

            if (dataSource.getDataSourceContainer() instanceof DataSourceDescriptor) {
                lines.add("DBeaver connection name: " + dataSource.getDataSourceContainer().getName());
                DBPDriver driver = dataSource.getDataSourceContainer().getDriver();
                if (ds instanceof JDBCDataSource) {
                    lines.add("JDBC driver: " + dsInfo.getDriverName() + " (" + dsInfo.getDriverVersion() + ")");
                } else {
                    lines.add("Java driver: " + driver.getFullName() + ")");
                }
            }

            String currentSchema = dataSource.getCurrentSchema();
            if (!CommonUtils.isEmpty(currentSchema)) {
                lines.add("Current " + (dsInfo == null ? "Schema" : dsInfo.getSchemaTerm()) + ": " + currentSchema);
            }
            String currentCatalog = dataSource.getCurrentCatalog();
            if (!CommonUtils.isEmpty(currentCatalog)) {
                lines.add("Current " + (dsInfo == null ? "Catalog" : dsInfo.getCatalogTerm()) + ": " + currentCatalog);
            }
        }
        lines.add("SQL dialect: " + dialect.getDialectName());
        lines.add("Current date and time: " + DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()));
        return lines.toArray(String[]::new);
    }

    public static String[] createInstructionList(@Nullable DBSLogicalDataSource dataSource) {
        SQLDialect dialect = dataSource == null ? BasicSQLDialect.INSTANCE :
            SQLUtils.getDialectFromDataSource(dataSource.getDataSourceContainer().getDataSource());
        List<String> instructions = new ArrayList<>();
        instructions.add("You are the DBeaver AI assistant.");
        instructions.add("Act as a database architect and SQL expert.");
        instructions.add("Rely only on the schema information provided below.");
        instructions.add("Stick strictly to " + dialect.getDialectName() + " syntax.");
        instructions.add("Do not invent columns, tables, or data that aren’t explicitly defined.");

        String useLanguage = DBWorkbench.getPlatform().getPreferenceStore().getString(AIConstants.AI_RESPONSE_LANGUAGE);
        if (!CommonUtils.isEmpty(useLanguage)) {
            instructions.add("Use " + useLanguage + " language in your responses.");
        } else {
            instructions.add("Use the same language as the user.");
        }

        String quoteRule = identifiersQuoteRule(dialect);
        if (quoteRule != null) {
            instructions.add(quoteRule);
        }
        String stringsQuoteRule = stringsQuoteRule(dialect);
        if (stringsQuoteRule != null) {
            instructions.add(stringsQuoteRule);
        }

        return instructions.toArray(new String[0]);
    }

    @Nullable
    private static String identifiersQuoteRule(SQLDialect dialect) {
        String[][] identifierQuoteStrings = dialect.getIdentifierQuoteStrings();
        if (identifierQuoteStrings == null || identifierQuoteStrings.length == 0) {
            return null;
        }

        return "Use " + identifierQuoteStrings[0][0] + identifierQuoteStrings[0][1] + " to quote identifiers if needed.";
    }

    private static String stringsQuoteRule(SQLDialect dialect) {
        String[][] stringQuoteStrings = dialect.getStringQuoteStrings();
        if (stringQuoteStrings.length == 0) {
            return null;
        }

        return "Use " + stringQuoteStrings[0][0] + stringQuoteStrings[0][1] + " to quote strings.";
    }
}
