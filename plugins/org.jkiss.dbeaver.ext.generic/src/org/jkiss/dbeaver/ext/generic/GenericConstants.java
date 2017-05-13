/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.generic;

/**
 * Generic provider constants
 */
public class GenericConstants {

    public static final String PARAM_META_MODEL = "meta-model";
    public static final String PARAM_QUERY_PING = "ping-sql";
    public static final String PARAM_QUERY_GET_ACTIVE_DB = "query-get-active-db";
    public static final String PARAM_QUERY_SET_ACTIVE_DB = "query-set-active-db";
    public static final String PARAM_ACTIVE_ENTITY_TYPE = "active-entity-type";
    public static final String PARAM_SUPPORTS_REFERENCES = "supports-references";
    public static final String PARAM_SUPPORTS_INDEXES = "supports-indexes";
    public static final String PARAM_SUPPORTS_STORED_CODE = "supports-stored-code";
    public static final String PARAM_SUPPORTS_SUBQUERIES = "supports-subqueries";
    public static final String PARAM_SUPPORTS_SELECT_COUNT = "supports-select-count";
    public static final String PARAM_SUPPORTS_LIMITS = "supports-limits";
    public static final String PARAM_SUPPORTS_SCROLL = "supports-scroll";
    public static final String PARAM_SUPPORTS_STRUCT_CACHE = "supports-struct-cache";
    public static final String PARAM_SUPPORTS_MULTIPLE_RESULTS = "supports-multiple-results";
    public static final String PARAM_OMIT_TYPE_CACHE = "omit-type-cache";
    public static final String PARAM_OMIT_CATALOG = "omit-catalog";
    public static final String PARAM_ALL_OBJECTS_PATTERN = "all-objects-pattern";
    public static final String PARAM_SCRIPT_DELIMITER = "script-delimiter";
    public static final String PARAM_SCRIPT_DELIMITER_REDEFINER = "script-delimiter-redefiner";
    public static final String PARAM_SQL_DELIMITER_AFTER_QUERY = "script-delimiter-after-query";
    public static final String PARAM_SQL_DELIMITER_AFTER_BLOCK = "script-delimiter-after-block";
    public static final String PARAM_EMBEDDED = "embedded";
    public static final String PARAM_DDL_DROP_COLUMN_SHORT = "ddl-drop-column-short";
    public static final String PARAM_LEGACY_DIALECT = "legacy-sql-dialect";
    public static final String PARAM_QUOTE_RESERVED_WORDS = "quote-reserved-words";
    public static final String PARAM_DUAL_TABLE = "dual-table";

    public static final String PARAM_NATIVE_FORMAT_TIMESTAMP = "native-format-timestamp";
    public static final String PARAM_NATIVE_FORMAT_TIME = "native-format-time";
    public static final String PARAM_NATIVE_FORMAT_DATE = "native-format-date";

    public static final String ENTITY_TYPE_CATALOG = "catalog";
    public static final String ENTITY_TYPE_SCHEMA = "schema";

    // URL parameter for DB shutdown. Added to support Derby DB shutdown process
    public static final String PARAM_CREATE_URL_PARAM = "create-url-param";
    public static final String PARAM_SHUTDOWN_URL_PARAM = "shutdown-url-param";
    public static final String TYPE_MODIFIER_IDENTITY = " IDENTITY";

    public static final String TERM_CATALOG = "catalog";
    public static final String TERM_SCHEMA = "schema";
    public static final String TERM_PROCEDURE = "procedure";

    public static final String OBJECT_CATALOG = "catalog";
    public static final String OBJECT_SCHEMA = "schema";
    public static final String OBJECT_TABLE_TYPE = "table-type";
    public static final String OBJECT_TABLE = "table";
    public static final String OBJECT_TABLE_COLUMN = "table-column";
    public static final String OBJECT_FOREIGN_KEY = "foreign-key";
    public static final String OBJECT_PRIMARY_KEY = "primary-key";
    public static final String OBJECT_INDEX = "index";
    public static final String OBJECT_PROCEDURE = "procedure";
    public static final String OBJECT_PROCEDURE_COLUMN = "procedure-column";

    public static final String META_MODEL_STANDARD = "standard";
}
