/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.ai.completion.DAICompletionMessage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// All these ideally should be a part of a given AI engine
public class AITextUtils {
    private static final Log log = Log.getLog(AITextUtils.class);

    private AITextUtils() {
        // prevents instantiation
    }

    @NotNull
    public static String convertToSQL(
        @NotNull DAICompletionMessage prompt,
        @NotNull MessageChunk[] response,
        @Nullable DBPDataSource dataSource
    ) {
        final StringBuilder builder = new StringBuilder();

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT)) {
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
        if (text.startsWith("SELECT") && text.endsWith(";")) {
            // Likely a SQL query
            return new MessageChunk[]{new MessageChunk.Code(text, "sql")};
        }

        final List<MessageChunk> chunks = new ArrayList<>();
        final StringBuilder buffer = new StringBuilder();
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
    public static List<DBSEntity> loadCustomEntities(
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
    private static List<DBSEntity> loadCheckedEntitiesById(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPProject project,
        @NotNull Set<String> ids
    ) throws DBException {
        final List<DBSEntity> output = new ArrayList<>();

        for (String id : ids) {
            if (DBUtils.findObjectById(monitor, project, id) instanceof DBSEntity entity) {
                output.add(entity);
            }
            monitor.worked(1);
        }

        return output;
    }
}
