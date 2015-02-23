/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

    public static final String ENTITY_TYPE_CATALOG = "catalog";
    public static final String ENTITY_TYPE_SCHEMA = "schema";

    // URL parameter for DB shutdown. Added to support Derby DB shutdown process
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
