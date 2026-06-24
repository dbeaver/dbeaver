/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

/**
 * GPT preference constants
 */
public class AIConstants {

    // Misc

    public static final String AI_COMPLETION_EXECUTE_IMMEDIATELY = "ai.completion.executeImmediately";
    public static final String AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT = "ai.completion.includeSourceTextInQuery";

    public static final String AI_COMPLETION_MAX_CHOICES = "ai.completion.maxChoices";
    public static final String AI_RESPONSE_LANGUAGE = "ai.language";

    // Metadata send default properties

    public static final String AI_SEND_FOREIGN_KEYS = "ai.useForeignKeys";
    public static final String AI_SEND_CONSTRAINTS = "ai.useConstraints";
    public static final String AI_SEND_INDEXES = "ai.useIndexes";
    public static final String AI_SEND_TYPE_INFO = "ai.completion.sendType";
    public static final String AI_SEND_DESCRIPTION = "ai.completion.description";

    // Confirmations

    public static final String AI_CONFIRM_SQL = "ai.confirmation.sql";
    public static final String AI_CONFIRM_DML = "ai.confirmation.dml";
    public static final String AI_CONFIRM_DDL = "ai.confirmation.ddl";
    public static final String AI_CONFIRM_OTHER = "ai.confirmation.other";
    public static final String AI_JOIN_RULE = "ai.joinRule";
    public static final int DEFAULT_CONTEXT_WINDOW_SIZE = 16384;

    // Engine settings

    public static final String AI_MODEL_PROPERTY = "model";
    public static final String AI_CONTEXT_SIZE_PROPERTY = "contextWindowSize";
    public static final String AI_TEMPERATURE_PROPERTY = "temperature";

    public static final String AI_MODEL_PLUGIN_ID = "org.jkiss.dbeaver.model.ai";

    // Toolbox
    public static final String INTERNAL_TOOLBOX_ID = "db";


    public static final String AI_TOOLBOX_INTERNAL = "db";
    public static final String AI_TOOL_GET_TABLE_DETAILS = AIFunctionDescriptor.getFullFunctionId(AI_TOOLBOX_INTERNAL, "getTableDetails");
    public static final String AI_PROMPT_FEATURE_FULL_DDL = "fullDDL";
    public static final String AI_USE_STREAM_MODE = "ai.streamMode";
    // AI Chat
    public static final String AI_CHAT_SHOW_MESSAGE_TIME = "ai.chat.show.message.time";
    public static final String AI_CHAT_SHOW_TIME_SPENT = "ai.chat.show.time.spent";
    public static final String AI_CHAT_SHOW_TOKENS_SPENT = "ai.chat.show.tokens.spent";
    public static final String AI_CHAT_SHOW_TOTAL_TOKENS_SPENT = "ai.chat.show.total.tokens.spent";
    public static final String USER_QUOTA_PROPERTY = "ai.userQuota";
    public static final String LOG_STATS_PROPERTY = "ai.logStats";
}

