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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.impl.MessageChunk;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// All these ideally should be a part of a given AI engine
public class AITextUtils {
    private static final Log log = Log.getLog(AITextUtils.class);
    public static final String SQL_LANGUAGE_ID = "sql";

    private AITextUtils() {
        // prevents instantiation
    }

    // Matches ```<optional language>\n ... \n```
    private static final Pattern CODE_BLOCK_PATTERN =
        Pattern.compile("```[\\w+]*\\R([\\s\\S]*?)\\R```");

    /**
     * Extracts the contents of the first Markdown code block in the input.
     * If the code ends with a semicolon, it’s removed.
     *
     * @param markdown the full Markdown string
     * @return the inner code without trailing semicolon, or an empty string if none found
     */
    public static String extractCode(String markdown) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(markdown);
        if (matcher.find()) {
            String code = matcher.group(1).trim();
            // Remove trailing semicolon if present
            if (code.endsWith(";")) {
                code = code.substring(0, code.length() - 1);
            }
            return code;
        }

        return markdown;
    }


    @NotNull
    public static String convertToSQL(
        @NotNull AIMessage prompt,
        @NotNull MessageChunk[] response,
        @Nullable DBPDataSource dataSource
    ) {
        final StringBuilder builder = new StringBuilder();

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AIConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT)) {
            builder.append(SQLUtils.generateCommentLine(dataSource, prompt.getContent()));
        }

        for (MessageChunk chunk : response) {
            if (chunk instanceof MessageChunk.Code code) {
                builder.append(code.text()).append(System.lineSeparator());
            } else if (chunk instanceof MessageChunk.Text text) {
                builder.append(SQLUtils.generateCommentLine(dataSource, text.text()));
            }
        }

        return builder.toString().trim();
    }

    @NotNull
    public static MessageChunk[] splitIntoChunks(@NotNull String text) {
        return splitIntoChunks(BasicSQLDialect.INSTANCE, text);
    }

    @NotNull
    public static MessageChunk[] splitIntoChunks(@NotNull SQLDialect dialect, @NotNull String text) {
        final List<MessageChunk> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String codeBlockTag = null;

        for (String line : text.lines().toArray(String[]::new)) {
            if (line.startsWith("```")) {
                // Add pending chunk
                if (!buffer.isEmpty()) {
                    if (codeBlockTag != null) {
                        chunks.add(new MessageChunk.Code(buffer.toString(), codeBlockTag));
                    } else {
                        chunks.add(new MessageChunk.Text(buffer.toString()));
                    }

                    buffer.setLength(0);
                }

                if (codeBlockTag != null) {
                    codeBlockTag = null;
                } else {
                    codeBlockTag = line.substring(3);
                }

                continue;
            } else if (codeBlockTag == null && !SQLUtils.isCommentLine(dialect, line)) {
                String firstKeyword = SQLUtils.getFirstKeyword(dialect, line);
                if (firstKeyword != null && ArrayUtils.contains(SQLConstants.QUERY_KEYWORDS, firstKeyword)) {
                    codeBlockTag = SQL_LANGUAGE_ID;
                }
            }

            if (!buffer.isEmpty()) {
                buffer.append('\n');
            }

            buffer.append(line);
        }

        // Add last chunk
        if (!buffer.isEmpty()) {
            if (codeBlockTag != null) {
                chunks.add(new MessageChunk.Code(buffer.toString(), codeBlockTag));
            } else {
                chunks.add(new MessageChunk.Text(buffer.toString()));
            }
        }

        return chunks.toArray(MessageChunk[]::new);
    }

    @NotNull
    public static List<DBSObject> loadCustomEntities(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull Set<String> ids
    ) {
        monitor.beginTask("Load custom entities", ids.size());
        try {
            return loadCheckedEntitiesById(monitor, dataSource.getContainer().getProject(), ids);
        } catch (Exception e) {
            log.error(e);
            return List.of();
        } finally {
            monitor.done();
        }
    }

    @NotNull
    private static List<DBSObject> loadCheckedEntitiesById(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPProject project,
        @NotNull Set<String> ids
    ) throws DBException {
        final List<DBSObject> output = new ArrayList<>();

        for (String id : ids) {
            output.add(DBUtils.findObjectById(monitor, project, id));
            monitor.worked(1);
        }

        return output;
    }

    public static MessageChunk[] processAndSplitCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIDatabaseContext context,
        @NotNull AISqlFormatter sqlFormatter,
        @NotNull String text
    ) {
        String processedCompletion = sqlFormatter.formatGeneratedQuery(
            monitor,
            context.getExecutionContext().getDataSource(),
            text
        );

        return splitIntoChunks(
            SQLUtils.getDialectFromDataSource(context.getExecutionContext().getDataSource()),
            processedCompletion
        );
    }

    @NotNull
    public static String extractGeneratedSqlQuery(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIDatabaseContext dbContext,
        @NotNull AIMessage userMessage,
        @NotNull String result
    ) throws DBException {
        AISqlFormatter sqlFormatter = AIAssistantRegistry.getInstance().getDescriptor().createSqlFormatter();
        MessageChunk[] messageChunks = processAndSplitCompletion(
            monitor,
            dbContext,
            sqlFormatter,
            result
        );

        return convertToSQL(
            userMessage,
            messageChunks,
            dbContext.getExecutionContext().getDataSource()
        );
    }
}
