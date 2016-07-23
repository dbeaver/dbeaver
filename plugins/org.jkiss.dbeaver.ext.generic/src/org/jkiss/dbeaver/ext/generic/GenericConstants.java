/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ext.generic;

/**
 * Generic provider constants
 */
public class GenericConstants {

    public static final String PARAM_META_MODEL = "meta-model";
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
    public static final String PARAM_EMBEDDED = "embedded";
    public static final String PARAM_DDL_DROP_COLUMN_SHORT = "ddl-drop-column-short";
    public static final String PARAM_LEGACY_DIALECT = "legacy-sql-dialect";
    public static final String PARAM_QUOTE_RESERVED_WORDS = "quote-reserved-words";

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
