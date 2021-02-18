/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.IDocument;

/**
 * Partitions and other
 */
public class SQLParserPartitions {

    public final static String SQL_PARTITIONING = "___sql_partitioning";
    public final static String CONTENT_TYPE_SQL_COMMENT = "sql_comment";
    public final static String CONTENT_TYPE_SQL_MULTILINE_COMMENT = "sql_multiline_comment";
    public final static String CONTENT_TYPE_SQL_STRING = "sql_character";
    public final static String CONTENT_TYPE_SQL_QUOTED = "sql_quoted";
    public final static String[] SQL_CONTENT_TYPES = new String[]{
        IDocument.DEFAULT_CONTENT_TYPE,
        CONTENT_TYPE_SQL_COMMENT,
        CONTENT_TYPE_SQL_MULTILINE_COMMENT,
        CONTENT_TYPE_SQL_STRING,
        CONTENT_TYPE_SQL_QUOTED,
    };
}