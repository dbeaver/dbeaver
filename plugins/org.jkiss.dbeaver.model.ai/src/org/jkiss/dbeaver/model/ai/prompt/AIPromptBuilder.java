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
package org.jkiss.dbeaver.model.ai.prompt;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
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
    @Nullable
    private String databaseSnapshot;
    private final List<String> outputFormats = new ArrayList<>();
    private String useLanguage;
    private boolean showSummary;
    private boolean useSqlGenerateInstructions = true;

    private AIPromptBuilder() {
    }

    @NotNull
    public static AIPromptBuilder createEmpty() {
        return new AIPromptBuilder();
    }

    @NotNull
    public static AIPromptBuilder createForDataSource(@Nullable DBSLogicalDataSource dataSource, @NotNull AIPromptFormatter formatter) {
        AIPromptBuilder promptBuilder = new AIPromptBuilder();

        return fullForDataSource(promptBuilder, dataSource, formatter);
    }

    @NotNull
    public static AIPromptBuilder fullForDataSource(
        @NotNull AIPromptBuilder promptBuilder,
        @Nullable DBSLogicalDataSource dataSource,
        @NotNull AIPromptFormatter formatter
    ) {
        if (promptBuilder.isUseSqlGenerateInstructions()) {
            promptBuilder.addInstructions(promptBuilder.createInstructionList(dataSource));
            promptBuilder.addInstructions(formatter.getExtraInstructions().toArray(new String[0]));
        }

        promptBuilder.addContexts(describeContext(dataSource));

        return promptBuilder;
    }

    public AIPromptBuilder showSummary(boolean show) {
        this.showSummary = show;
        return this;
    }

    public AIPromptBuilder useLanguage(String language) {
        this.useLanguage = language;
        return this;
    }

    public boolean isUseSqlGenerateInstructions() {
        return useSqlGenerateInstructions;
    }

    public AIPromptBuilder useSqlGenerateInstructions(boolean use) {
        this.useSqlGenerateInstructions = use;
        return this;
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

    public AIPromptBuilder addDatabaseSnapshot(@NotNull String databaseSnapshot) {
        this.databaseSnapshot = databaseSnapshot.trim();
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

        if (databaseSnapshot != null && !databaseSnapshot.isBlank()) {
            prompt.append("\nDatabase Snapshot:\n");
            prompt.append(databaseSnapshot).append("\n");
        }

        prompt.append("\nOutput Format:\n");
        outputFormats.forEach(outputFormat -> prompt.append("- ").append(outputFormat).append("\n"));

        return prompt.toString();
    }

    private String[] createInstructionList(@Nullable DBSLogicalDataSource dataSource) {
        SQLDialect dialect = dataSource == null ? BasicSQLDialect.INSTANCE :
            SQLUtils.getDialectFromDataSource(dataSource.getDataSourceContainer().getDataSource());
        List<String> instructions = new ArrayList<>();
        instructions.add("You are the DBeaver AI assistant.");
        instructions.add("Act as a database architect and SQL expert.");
        instructions.add("Rely only on the schema information provided below.");
        instructions.add("Stick strictly to " + dialect.getDialectName() + " syntax.");
        instructions.add("Do not invent columns, tables, or data that aren’t explicitly defined.");
        String quoteRule = identifiersQuoteRule(dialect);
        if (quoteRule != null) {
            instructions.add(quoteRule);
        }
        String stringsQuoteRule = stringsQuoteRule(dialect);
        if (stringsQuoteRule != null) {
            instructions.add(stringsQuoteRule);
        }
        if (useLanguage != null) {
            instructions.add("Use language '" + useLanguage + "'.");
        } else {
            instructions.add("Use the same language as the user.");
        }
        if (showSummary) {
            instructions.add("Write a very short one-sentence summary of this conversation (for chat caption) in the end of response in xml tag <summary>.");
        }
        return instructions.toArray(new String[0]);
    }

    private static String[] describeContext(@Nullable DBSLogicalDataSource dataSource) {
        SQLDialect dialect = dataSource == null ? BasicSQLDialect.INSTANCE :
            SQLUtils.getDialectFromDataSource(dataSource.getDataSourceContainer().getDataSource());
        List<String> lines = new ArrayList<>();
        lines.add("Current date and time: " + DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()));
        lines.add("Current SQL dialect: " + dialect.getDialectName());
        if (dataSource != null) {
            DBPDataSource ds = dataSource.getDataSourceContainer().getDataSource();
            DBPDataSourceInfo dsInfo = ds == null ? null : ds.getInfo();

            String currentSchema = dataSource.getCurrentSchema();
            if (!CommonUtils.isEmpty(currentSchema)) {
                lines.add("Current " + (dsInfo == null ? "schema" : dsInfo.getSchemaTerm()) + ": " + currentSchema);
            }

            if (dataSource.getDataSourceContainer() instanceof DataSourceDescriptor) {
                DBPDriver driver = dataSource.getDataSourceContainer().getDriver();
                if (ds instanceof JDBCDataSource) {
                    lines.add("Current JDBC driver: " + dsInfo.getDriverName() + " (" + dsInfo.getDriverVersion() + ")");
                } else {
                    lines.add("Current Java driver: " + driver.getFullName() + ")");
                }
            }
        }
        return lines.toArray(String[]::new);
    }

    @Nullable
    private static String identifiersQuoteRule(SQLDialect dialect) {
        String[][] identifierQuoteStrings = dialect.getIdentifierQuoteStrings();
        if (identifierQuoteStrings == null || identifierQuoteStrings.length == 0) {
            return null;
        }

        return "Use (" + identifierQuoteStrings[0][0] + identifierQuoteStrings[0][1] + ") to quote identifiers if needed.";
    }

    private static String stringsQuoteRule(SQLDialect dialect) {
        String[][] stringQuoteStrings = dialect.getStringQuoteStrings();
        if (stringQuoteStrings.length == 0) {
            return null;
        }

        return "Use (" + stringQuoteStrings[0][0] + stringQuoteStrings[0][1] + ") to quote strings.";
    }
}
