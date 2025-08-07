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

/**
 * GPT preference constants
 */
public class AIConstants {
    public static final String AI_LOG_QUERY = "gpt.log.query";

    public static final int MAX_RESPONSE_TOKENS = 2000;

    public static final String AI_DISABLED = "ai.completion.disabled";
    public static final String AI_COMPLETION_EXECUTE_IMMEDIATELY = "ai.completion.executeImmediately";
    public static final String AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT = "ai.completion.includeSourceTextInQuery";
    public static final String AI_SEND_TYPE_INFO = "ai.completion.sendType";
    public static final String AI_SEND_DESCRIPTION = "ai.completion.description";
    public static final String AI_COMPLETION_MAX_CHOICES = "ai.completion.maxChoices";
    public static final String AI_RESPONSE_LANGUAGE = "ai.language";

    public static final String AI_MODEL_PLUGIN_ID = "org.jkiss.dbeaver.model.ai";

    public static final String AI_CONFIRM_SQL = "ai.confirmation.sql";
    public static final String AI_CONFIRM_DML = "ai.confirmation.dml";
    public static final String AI_CONFIRM_DDL = "ai.confirmation.ddl";
}

