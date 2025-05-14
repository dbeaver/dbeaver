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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class PromptBuilder {
    private final List<String> goals = new ArrayList<>();
    private final List<String> instructions = new ArrayList<>();
    private final List<String> examples = new ArrayList<>();
    private final List<String> contexts = new ArrayList<>();
    @Nullable
    private String databaseSnapshot;
    private final List<String> outputFormats = new ArrayList<>();

    private PromptBuilder() {
    }

    @NotNull
    public static PromptBuilder createForDataSource(@Nullable DBPDataSource dataSource, @NotNull IAIFormatter formatter) {
        return createForDataSource0(dataSource, formatter);
    }

    @NotNull
    public static PromptBuilder createForDataSource0(
        @Nullable DBPDataSource dataSource,
        @NotNull IAIFormatter formatter
    ) {
        PromptBuilder promptBuilder = new PromptBuilder();

        promptBuilder.addInstructions(createInstructionList(dataSource));
        promptBuilder.addInstructions(formatter.getExtraInstructions().toArray(new String[0]));

        promptBuilder.addContexts(describeContext(dataSource));

        return promptBuilder;
    }

    public PromptBuilder addGoals(@NotNull String... goals) {
        this.goals.addAll(Arrays.asList(goals));
        return this;
    }

    public PromptBuilder addExamples(@NotNull String... examples) {
        this.examples.addAll(Arrays.asList(examples));
        return this;
    }

    public PromptBuilder addInstructions(@NotNull String... instructions) {
        this.instructions.addAll(Arrays.asList(instructions));
        return this;
    }

    public PromptBuilder addContexts(@NotNull String... contexts) {
        this.contexts.addAll(Arrays.asList(contexts));
        return this;
    }

    public PromptBuilder addDatabaseSnapshot(@NotNull String databaseSnapshot) {
        this.databaseSnapshot = databaseSnapshot.trim();
        return this;
    }

    public PromptBuilder addOutputFormats(@NotNull String... outputFormats) {
        this.outputFormats.addAll(Arrays.asList(outputFormats));
        return this;
    }

    public String build() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Goals:\n");
        goals.forEach(goal -> prompt.append("- ").append(goal).append("\n"));

        prompt.append("\nInstructions:\n");
        instructions.forEach(instruction -> prompt.append("- ").append(instruction).append("\n"));

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

    private static String[] createInstructionList(@Nullable DBPDataSource dataSource) {
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(dataSource);
        return Stream.of(
                "You are the DBeaver AI assistant.",
                "Act as a database architect and SQL expert.",
                "Rely only on the schema information provided below.",
                "Stick strictly to " + dialect.getDialectName() + " syntax.",
                "Do not invent columns, tables, or data that aren’t explicitly defined.",
                identifiersQuoteRule(dialect),
                stringsQuoteRule(dialect),
                "Use the same language as the user."
            )
            .filter(Objects::nonNull)
            .toArray(String[]::new);
    }

    private static String[] describeContext(@Nullable DBPDataSource dataSource) {
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(dataSource);
        List<String> lines = new ArrayList<>();
        lines.add("Current date and time: " + ZonedDateTime.now());
        lines.add("Current SQL dialect: " + dialect.getDialectName());
        DBPDriver driver = dataSource == null ? null : dataSource.getContainer().getDriver();
        if (driver != null) {
            lines.add("Current Java driver: " + driver.getFullName());
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
