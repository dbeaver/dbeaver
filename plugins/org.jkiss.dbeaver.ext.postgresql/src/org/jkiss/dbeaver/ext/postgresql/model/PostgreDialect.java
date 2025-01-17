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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.internal.PostgreSQLMessages;
import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreBinaryFormatter;
import org.jkiss.dbeaver.ext.postgresql.sql.PostgreEscapeStringRule;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDollarQuoteRule;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;

/**
 * PostgreSQL dialect
 */
public class PostgreDialect extends JDBCSQLDialect implements TPRuleProvider, SQLDataTypeConverter,
    SQLDialectDDLExtension, SQLDialectSchemaController {
    public static final String[] POSTGRE_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "SHOW", "SET"
        }
    );

    private static final String[][] PG_STRING_QUOTES = {
        {"'", "'"}
    };

    // In PgSQL there are no blocks. DO $$ ... $$ queries are processed as strings
    public static final String[][] BLOCK_BOUND_KEYWORDS = {
//        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
//        {"LOOP", "END LOOP"}
    };

    private static final String[] EXEC_KEYWORDS = {
        "CALL"
    };

    //Function without arguments/parameters #8710
    private static final GlobalVariableInfo[] GLOBAL_VARIABLES = {
        new GlobalVariableInfo("current_date", PostgreSQLMessages.global_variable_current_date_description, DBPDataKind.DATETIME),
        new GlobalVariableInfo("current_time", PostgreSQLMessages.global_variable_current_time_description, DBPDataKind.DATETIME),
        new GlobalVariableInfo("current_timestamp", PostgreSQLMessages.global_variable_current_timestamp_description, DBPDataKind.DATETIME),
        new GlobalVariableInfo("localtime", PostgreSQLMessages.global_variable_localtime_description, DBPDataKind.DATETIME),
        new GlobalVariableInfo("localtimestamp", PostgreSQLMessages.global_variable_localtimestamp_description, DBPDataKind.DATETIME),
        new GlobalVariableInfo("current_role", PostgreSQLMessages.global_variable_user_description, DBPDataKind.STRING),
        new GlobalVariableInfo("current_user", PostgreSQLMessages.global_variable_user_description, DBPDataKind.STRING),
        new GlobalVariableInfo("current_catalog ", PostgreSQLMessages.global_variable_current_catalog_description, DBPDataKind.STRING),
        new GlobalVariableInfo("current_schema", PostgreSQLMessages.global_variable_current_schema_description, DBPDataKind.STRING),
        new GlobalVariableInfo("session_user", PostgreSQLMessages.global_variable_session_user_description, DBPDataKind.STRING),
        new GlobalVariableInfo("system_user", PostgreSQLMessages.global_variable_system_user_description, DBPDataKind.STRING),
        new GlobalVariableInfo("user", PostgreSQLMessages.global_variable_user_description, DBPDataKind.STRING)
    };

    public static final String AUTO_INCREMENT_KEYWORD = "AUTO_INCREMENT";

    //region KeyWords

    public static String[] POSTGRE_EXTRA_KEYWORDS = new String[]{
        "ABSENT",
        "ACCORDING",
        "ADA",
        "ADMIN",
//            "ARRAY_AGG",
//            "ARRAY_MAX_CARDINALITY",
        "BASE64",
        "BEGIN_FRAME",
        "BEGIN_PARTITION",
        "BERNOULLI",
        "BIT_LENGTH",
        "BLOCKED",
        "BOM",
        //"BREADTH",
//            "CATALOG_NAME",
//            "CHARACTER_SET_CATALOG",
//            "CHARACTER_SET_NAME",
//            "CHARACTER_SET_SCHEMA",
//            "CLASS_ORIGIN",
        //"COBOL",
//            "COLLATION_CATALOG",
//            "COLLATION_NAME",
//            "COLLATION_SCHEMA",
//            "COLUMN_NAME",
//            "COMMAND_FUNCTION",
//            "COMMAND_FUNCTION_CODE",
//            "CONDITION_NUMBER",
//            "CONNECTION_NAME",
//            "CONSTRAINT_CATALOG",
//            "CONSTRAINT_NAME",
//            "CONSTRAINT_SCHEMA",
        "CONTROL",
//            "CURRENT_ROW",
//            "DATALINK",
//            "DATETIME_INTERVAL_CODE",
//            "DATETIME_INTERVAL_PRECISION",
        //"DB",
        "DLNEWCOPY",
        "DLPREVIOUSCOPY",
        "DLURLCOMPLETE",
        "DLURLCOMPLETEONLY",
        "DLURLCOMPLETEWRITE",
        "DLURLPATH",
        "DLURLPATHONLY",
        "DLURLPATHWRITE",
        "DLURLSCHEME",
        "DLURLSERVER",
        "DLVALUE",
        "DYNAMIC_FUNCTION",
        "DYNAMIC_FUNCTION_CODE",
        "EMPTY",
        "END_FRAME",
        "END_PARTITION",
        "ENFORCED",
        "EXIT",
        "EXPRESSION",
        //"FILE",
        "FIRST_VALUE",
        //"FLAG",
        //"FORTRAN",
        "FRAME_ROW",
        "FS",
        "GROUPS",
        //"HEX",
        //"ID",
        "IGNORE",
        "IMMEDIATELY",
        "INCLUDE",
        "INDENT",
        "INTEGRITY",
        "KEY_MEMBER",
        "LAG",
        "LAST_VALUE",
        "LEAD",
        "LIBRARY",
        "LIKE_REGEX",
        //"LINK",
//            "MAX_CARDINALITY",
//            "MESSAGE_LENGTH",
//            "MESSAGE_OCTET_LENGTH",
//            "MESSAGE_TEXT",
        //"MODULE",
        //"NAME",
        //"NAMES",
        "NAMESPACE",
        //"NFC",
        //"NFD",
        //"NFKC",
        //"NFKD",
        "NIL",
        "NTH_VALUE",
        "NTILE",
        "NULLABLE",
        "OCCURRENCES_REGEX",
//            "PARAMETER_MODE",
//            "PARAMETER_NAME",
//            "PARAMETER_ORDINAL_POSITION",
//            "PARAMETER_SPECIFIC_CATALOG",
//            "PARAMETER_SPECIFIC_NAME",
//            "PARAMETER_SPECIFIC_SCHEMA",
        //"PASCAL",
        "PASSTHROUGH",
        "PERCENT",
        "PERIOD",
        "PERMISSION",
        //"PLI",
        //"PORTION",
        "POSITION_REGEX",
        "PRECEDES",
        "PROCEDURES",
        //"PUBLIC",
        "RECOVERY",
        "REQUIRING",
        "RESPECT",
        "RESTORE",
        "RULE",
//            "RETURNED_CARDINALITY",
//            "RETURNED_LENGTH",
//            "RETURNED_OCTET_LENGTH",
//            "RETURNED_SQLSTATE",
//            "ROUTINES",
//            "ROUTINE_CATALOG",
//            "ROUTINE_NAME",
//            "ROUTINE_SCHEMA",
        //"ROW_COUNT",
        //"SCHEMA_NAME",
        //"SCOPE_CATALOG",
        //"SCOPE_NAME",
        //"SCOPE_SCHEMA",
        //"SELECTIVE",
        //"SERVER_NAME",
        "SIMPLE",
        //"SPECIFIC_NAME",
        "SQLCODE",
        "SQLERROR",
        //"STATE",
        //"SUBCLASS_ORIGIN",
        //"SUBSTRING_REGEX",
        "SUCCEEDS",
        //"SYSTEM_TIME",
        //"TABLE_NAME",
        "TOKEN",
        //"TOP_LEVEL_COUNT",
        //"TRANSACTIONS_COMMITTED",
        //"TRANSACTIONS_ROLLED_BACK",
        //"TRANSACTION_ACTIVE",
        //"TRANSLATE_REGEX",
        //"TRIGGER_CATALOG",
        //"TRIGGER_NAME",
        //"TRIGGER_SCHEMA",
        //"TRIM_ARRAY",
        "UNLINK",
        "UNTYPED",
        //"URI",
        //"USER_DEFINED_TYPE_CATALOG",
        //"USER_DEFINED_TYPE_CODE",
        //"USER_DEFINED_TYPE_NAME",
        //"USER_DEFINED_TYPE_SCHEMA",        
        //"VALUE",
        //"VALUE_OF",
        "VERSIONING",
        "XMLAGG",
        "XMLBINARY",
        "XMLCAST",
        "XMLCOMMENT",
        "XMLDECLARATION",
        "XMLDOCUMENT",
        "XMLITERATE",
        "XMLQUERY",
        "XMLSCHEMA",
        "XMLTEXT",
        "XMLVALIDATE",
        "SQLERRM",
        "WHILE"
    };
    
    public static String[] POSTGRE_EXTRA_TYPES = new String[]{
        "UUID",
    };

    public static String[] POSTGRE_ONE_CHAR_KEYWORDS = new String[]{
        "C",
        "G",
        "K",
        "M",
        "T",
        "P"
    };
    //endregion

    //region FUNCTIONS KW

    public static String[] POSTGRE_FUNCTIONS_AGGREGATE = new String[]{
        "array_agg",
        "bit_and",
        "bit_or",
        "bool_and",
        "bool_or",
        "every",
        "json_agg",
        "jsonb_agg",
        "json_object_agg",
        "jsonb_object_agg",
        "mode",
        "string_agg",
        "xmlagg",
        "corr",
        "covar_pop",
        "covar_samp",
        "stddev",
        "stddev_pop",
        "stddev_samp",
        "variance",
        "var_pop",
        "var_samp"
    };

    public static String[] POSTGRE_FUNCTIONS_WINDOW = new String[]{
        "row_number",
        "rank",
        "dense_rank",
        "percent_rank",
        "cume_dist",
        "ntile",
        "lag",
        "lead",
        "first_value",
        "last_value",
        "nth_value"
    };


    public static String[] POSTGRE_FUNCTIONS_MATH = new String[]{
        "abs",
        "acos",
        "acosd",
        "asin",
        "asind",
        "atan",
        "atan2",
        "atan2d",
        "atand",
        "cbrt",
        "ceil",
        "ceiling",
        "cos",
        "cosd",
        "cosh",
        "cot",
        "cotd",
        "div",
        "exp",
        "floor",
        "gcd",
        "lcm",
        "ln",
        "log",
        "log10",
        "mod",
        "pi",
        "power",
        "random",
        "round",
        "scale",
        "setseed",
        "sin",
        "sind",
        "sinh",
        "sqrt",
        "tan",
        "tand",
        "trunc",
        "width_bucket"
    };
    public static String[] POSTGRE_FUNCTIONS_STRING = new String[]{
        "bit_length",
        "btrim",
        "chr",
        "concat_ws",
        "convert",
        "convert_from",
        "convert_to",
        "decode",
        "encode",
        "initcap",
        "left",
        "length",
        "lpad",
        "md5",
        "overlay",
        "parse_ident",
        "pg_client_encoding",
        "pg_backend_pid",
        "pg_database_size",
        "pg_sleep",
        "pg_terminate_backend",
        "position",
        "quote_ident",
        "quote_literal",
        "quote_nullable",
        "regexp_count",
        "regexp_instr",
        "regexp_like",
        "regexp_match",
        "regexp_matches",
        "regexp_replace",
        "regexp_split_to_array",
        "regexp_substr",
        "regexp_split_to_table",
        "replace",
        "reverse",
        "right",
        "rpad",
        "split_part",
        "strpos",
        "substring",
        "to_ascii",
        "to_hex",
        "translate",
        "treat",
        "unaccent"
    };

    public static String[] POSTGRE_FUNCTIONS_DATETIME = new String[]{
        "age",
        "clock_timestamp",
        "date_part",
        "date_trunc",
        "isfinite",
        "justify_days",
        "justify_hours",
        "justify_interval",
        "localtime",
        "localtimestamp",
        "make_date",
        "make_interval",
        "make_time",
        "make_timestamp",
        "make_timestamptz",
        "statement_timestamp",
        "timeofday",
        "to_timestamp",
        "transaction_timestamp"
    };

    public static String[] POSTGRE_FUNCTIONS_GEOMETRY = new String[]{
        "area",
        "center",
        "diagonal",
        "diameter",
        "height",
        "isclosed",
        "isopen",
        "npoints",
        "pclose",
        "popen",
        "radius",
        "slope",
        "width",
        "box",
        "bound_box",
        "circle",
        "line",
        "lseg",
        "path",
        "point",
        "polygon"
    };

    public static String[] POSTGRE_FUNCTIONS_NETWROK = new String[]{
        "abbrev",
        "broadcast",
        "family",
        "host",
        "hostmask",
        "masklen",
        "netmask",
        "network",
        "set_masklen",
        "text",
        "inet_same_family",
        "inet_merge",
        "macaddr8_set7bit"
    };

    public static String[] POSTGRE_FUNCTIONS_LO = new String[]{
        "lo_from_bytea",
        "lo_put",
        "lo_get",
        "lo_creat",
        "lo_create",
        "lo_unlink",
        "lo_import",
        "lo_export",
        "loread",
        "lowrite",
        "grouping",
        "cast"
    };

    public static String[] POSTGRE_FUNCTIONS_ADMIN = new String[]{
        "current_setting",
        "set_config",
        "brin_summarize_new_values",
        "brin_summarize_range",
        "brin_desummarize_range",
        "gin_clean_pending_list",
        "pg_cancel_backend",
        "pg_log_backend_memory_contexts",
        "pg_reload_conf",
        "pg_rotate_logfile",
        "pg_create_restore_point",
        "pg_current_wal_flush_lsn",
        "pg_current_wal_insert_lsn",
        "pg_current_wal_lsn",
        "pg_backup_start",
        "pg_backup_stop",
        "pg_switch_wal",
        "pg_walfile_name",
        "pg_walfile_name_offset",
        "pg_split_walfile_name",
        "pg_wal_lsn_diff",
        "pg_is_in_recovery",
        "pg_last_wal_receive_lsn",
        "pg_last_wal_replay_lsn",
        "pg_last_xact_replay_timestamp",
        "pg_get_wal_resource_managers",
        "pg_is_wal_replay_paused",
        "pg_get_wal_replay_pause_state",
        "pg_promote",
        "pg_wal_replay_pause",
        "pg_wal_replay_resume",
        "pg_export_snapshot",
        "pg_log_standby_snapshot",
        "pg_create_physical_replication_slot",
        "pg_drop_replication_slot",
        "pg_create_logical_replication_slot",
        "pg_copy_physical_replication_slot",
        "pg_copy_logical_replication_slot",
        "pg_logical_slot_get_changes",
        "pg_logical_slot_peek_changes",
        "pg_logical_slot_get_binary_changes",
        "pg_logical_slot_peek_binary_changes",
        "pg_replication_slot_advance",
        "pg_replication_origin_create",
        "pg_replication_origin_drop",
        "pg_replication_origin_oid",
        "pg_replication_origin_session_setup",
        "pg_replication_origin_session_reset",
        "pg_replication_origin_session_is_setup",
        "pg_replication_origin_session_progress",
        "pg_replication_origin_xact_setup",
        "pg_replication_origin_xact_reset",
        "pg_replication_origin_advance",
        "pg_replication_origin_progress",
        "pg_logical_emit_message",
        "pg_column_size",
        "pg_column_compression",
        "pg_indexes_size",
        "pg_relation_size",
        "pg_size_bytes",
        "pg_size_pretty",
        "pg_table_size",
        "pg_tablespace_size",
        "pg_total_relation_size",
        "pg_relation_filenode",
        "pg_relation_filepath",
        "pg_filenode_relation",
        "pg_collation_actual_version",
        "pg_database_collation_actual_version",
        "pg_import_system_collations",
        "pg_partition_tree",
        "pg_partition_ancestors",
        "pg_partition_root",
        "pg_ls_dir",
        "pg_ls_logdir",
        "pg_ls_waldir",
        "pg_ls_logicalmapdir",
        "pg_ls_logicalsnapdir",
        "pg_ls_replslotdir",
        "pg_ls_archive_statusdir",
        "pg_ls_tmpdir",
        "pg_read_file",
        "pg_read_binary_file",
        "pg_stat_file",
        "pg_advisory_lock",
        "pg_advisory_lock_shared",
        "pg_advisory_unlock",
        "pg_advisory_unlock_all",
        "pg_advisory_unlock_shared",
        "pg_advisory_xact_lock",
        "pg_advisory_xact_lock_shared",
        "pg_try_advisory_lock",
        "pg_try_advisory_lock_shared",
        "pg_try_advisory_xact_lock",
        "pg_try_advisory_xact_lock_shared"
    };

    public static String[] POSTGRE_FUNCTIONS_RANGE = new String[]{
        "isempty",
        "lower_inc",
        "upper_inc",
        "lower_inf",
        "upper_inf",
        "range_merge"
    };

    public static String[] POSTGRE_FUNCTIONS_TEXT_SEARCH = new String[]{
        "array_to_tsvector",
        "get_current_ts_config",
        "numnode",
        "plainto_tsquery",
        "phraseto_tsquery",
        "websearch_to_tsquery",
        "querytree",
        "setweight",
        "strip",
        "to_tsquery",
        "to_tsvector",
        "json_to_tsvector",
        "jsonb_to_tsvector",
        "ts_delete",
        "ts_filter",
        "ts_headline",
        "ts_rank",
        "ts_rank_cd",
        "ts_rewrite",
        "tsquery_phrase",
        "tsvector_to_array",
        "tsvector_update_trigger",
        "tsvector_update_trigger_column"
    };

    public static String[] POSTGRE_FUNCTIONS_XML = new String[]{
        "xmlcomment",
        "xmlconcat",
        "xmlelement",
        "xmlforest",
        "xmlpi",
        "xmlroot",
        "xmlexists",
        "xml_is_well_formed",
        "xml_is_well_formed_document",
        "xml_is_well_formed_content",
        "xpath",
        "xpath_exists",
        "xmltable",
        "xmlnamespaces",
        "table_to_xml",
        "table_to_xmlschema",
        "table_to_xml_and_xmlschema",
        "query_to_xml",
        "query_to_xmlschema",
        "query_to_xml_and_xmlschema",
        "cursor_to_xml",
        "cursor_to_xmlschema",
        "schema_to_xml",
        "schema_to_xmlschema",
        "schema_to_xml_and_xmlschema",
        "database_to_xml",
        "database_to_xmlschema",
        "database_to_xml_and_xmlschema",
        "xmlattributes"
    };

    public static String[] POSTGRE_FUNCTIONS_JSON = new String[]{
        "to_json",
        "to_jsonb",
        "array_to_json",
        "row_to_json",
        "json_build_array",
        "jsonb_build_array",
        "json_build_object",
        "jsonb_build_object",
        "json_object",
        "jsonb_object",
        "json_array_length",
        "jsonb_array_length",
        "json_each",
        "jsonb_each",
        "json_each_text",
        "jsonb_each_text",
        "json_extract_path",
        "jsonb_extract_path",
        "json_object_keys",
        "jsonb_object_keys",
        "json_populate_record",
        "jsonb_populate_record",
        "json_populate_recordset",
        "jsonb_populate_recordset",
        "json_array_elements",
        "jsonb_array_elements",
        "json_array_elements_text",
        "jsonb_array_elements_text",
        "json_typeof",
        "jsonb_typeof",
        "json_to_record",
        "jsonb_to_record",
        "json_to_recordset",
        "jsonb_to_recordset",
        "json_strip_nulls",
        "jsonb_strip_nulls",
        "jsonb_set",
        "jsonb_insert",
        "jsonb_pretty"
    };

    public static String[] POSTGRE_FUNCTIONS_ARRAY = new String[]{
        "array_append",
        "array_cat",
        "array_ndims",
        "array_dims",
        "array_fill",
        "array_length",
        "array_lower",
        "array_position",
        "array_positions",
        "array_prepend",
        "array_remove",
        "array_replace",
        "array_to_string",
        "array_upper",
        "cardinality",
        "string_to_array",
        "unnest"
    };

    public static String[] POSTGRE_FUNCTIONS_INFO = new String[]{
        "current_database",
        "current_query",
        "current_schema",
        "current_schemas",
        "inet_client_addr",
        "inet_client_port",
        "inet_server_addr",
        "inet_server_port",
        "row_security_active",
        "format_type",
        "to_regclass",
        "to_regproc",
        "to_regprocedure",
        "to_regoper",
        "to_regoperator",
        "to_regtype",
        "to_regnamespace",
        "to_regrole",
        "col_description",
        "obj_description",
        "shobj_description",
        "txid_current",
        "txid_current_if_assigned",
        "txid_current_snapshot",
        "txid_snapshot_xip",
        "txid_snapshot_xmax",
        "txid_snapshot_xmin",
        "txid_visible_in_snapshot",
        "txid_status"
    };

    public static String[] POSTGRE_FUNCTIONS_COMPRASION = new String[]{
        "num_nonnulls",
        "num_nulls"
    };

    public static String[] POSTGRE_FUNCTIONS_FORMATTING = new String[]{
        "to_char",
        "to_date",
        "to_number",
        "to_timestamp"
    };

    public static String[] POSTGRE_FUNCTIONS_ENUM = new String[]{
        "enum_first",
        "enum_last",
        "enum_range"
    };

    public static String[] POSTGRE_FUNCTIONS_SEQUENCE = new String[]{
        "currval",
        "lastval",
        "nextval",
        "setval"
    };

    public static String[] POSTGRE_FUNCTIONS_BINARY_STRING = new String[]{
        "bit_count",
        "get_bit",
        "get_byte",
        "set_bit",
        "set_byte",
        "substr"
    };

    public static String[] POSTGRE_FUNCTIONS_CONDITIONAL = new String[]{
        "coalesce",
        "nullif",
        "greatest",
        "least"
    };

    public static String[] POSTGRE_FUNCTIONS_TRIGGER = new String[]{
        "suppress_redundant_updates_trigger",
        "tsvector_update_trigger",
        "tsvector_update_trigger_column"
    };

    public static String[] POSTGRE_FUNCTIONS_SRF = new String[]{
        "generate_series",
        "generate_subscripts"
    };

    //endregion

    private PostgreServerExtension serverExtension;

    public PostgreDialect() {
        super("PostgreSQL", "postgresql");
    }

    public void addExtraKeywords(String... keywords) {
        super.addSQLKeywords(Arrays.asList(keywords));
    }

    public void addExtraFunctions(String... functions) {
        super.addFunctions(Arrays.asList(functions));
    }
    
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);

        addExtraKeywords(
            "SHOW",
            "TYPE",
            "USER",
            "COMMENT",
            "MATERIALIZED",
            "ILIKE",
            "ELSIF",
            "ELSEIF",
            "ANALYSE",
            "ANALYZE",
            "CONCURRENTLY",
            "FREEZE",
            "LANGUAGE",
            "MODULE",
            "OFFSET",
            //"PUBLIC",
            "RETURNING",
            "VARIADIC",
            "PERFORM",
            "FOREACH",
            "LOOP",
            "PERFORM",
            "RAISE",
            "NOTICE",
            "CONFLICT",
            "EXTENSION",

            // "DEBUG", "INFO", "NOTICE", "WARNING", // levels
            // "MESSAGE", "DETAIL", "HINT", "ERRCODE", //options

            "DATATYPE",
            "TABLESPACE",
            "REFRESH"
        );

        addExtraKeywords(POSTGRE_EXTRA_KEYWORDS);
        // Not sure about one char keywords. May confuse users
        //addExtraKeywords(POSTGRE_ONE_CHAR_KEYWORDS);

        addExtraFunctions(PostgreConstants.POSTGIS_FUNCTIONS);

        addExtraFunctions(POSTGRE_FUNCTIONS_ADMIN);
        addExtraFunctions(POSTGRE_FUNCTIONS_AGGREGATE);
        addExtraFunctions(POSTGRE_FUNCTIONS_ARRAY);
        addExtraFunctions(POSTGRE_FUNCTIONS_BINARY_STRING);
        addExtraFunctions(POSTGRE_FUNCTIONS_COMPRASION);
        addExtraFunctions(POSTGRE_FUNCTIONS_CONDITIONAL);
        addExtraFunctions(POSTGRE_FUNCTIONS_DATETIME);
        addExtraFunctions(POSTGRE_FUNCTIONS_ENUM);
        addExtraFunctions(POSTGRE_FUNCTIONS_FORMATTING);
        addExtraFunctions(POSTGRE_FUNCTIONS_GEOMETRY);
        addExtraFunctions(POSTGRE_FUNCTIONS_INFO);
        addExtraFunctions(POSTGRE_FUNCTIONS_JSON);
        addExtraFunctions(POSTGRE_FUNCTIONS_LO);
        addExtraFunctions(POSTGRE_FUNCTIONS_MATH);
        addExtraFunctions(POSTGRE_FUNCTIONS_NETWROK);
        addExtraFunctions(POSTGRE_FUNCTIONS_RANGE);
        addExtraFunctions(POSTGRE_FUNCTIONS_SEQUENCE);
        addExtraFunctions(POSTGRE_FUNCTIONS_SRF);
        addExtraFunctions(POSTGRE_FUNCTIONS_STRING);
        addExtraFunctions(POSTGRE_FUNCTIONS_TEXT_SEARCH);
        addExtraFunctions(POSTGRE_FUNCTIONS_TRIGGER);
        addExtraFunctions(POSTGRE_FUNCTIONS_WINDOW);
        addExtraFunctions(POSTGRE_FUNCTIONS_XML);

        removeSQLKeyword("LENGTH");

        if (dataSource instanceof PostgreDataSource) {
            serverExtension = ((PostgreDataSource) dataSource).getServerType();
            serverExtension.configureDialect(this);
        }

        // #12723 Redshift driver returns wrong infor about unquoted case
        setUnquotedIdentCase(DBPIdentifierCase.LOWER);
    }

    @NotNull
    @Override
    protected DBPIdentifierCase getDefaultIdentifiersCase() {
        return DBPIdentifierCase.LOWER;
    }

    @Override
    public void addKeywords(Collection<String> set, DBPKeywordType type) {
        super.addKeywords(set, type);
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @NotNull
    @Override
    public GlobalVariableInfo[] getGlobalVariables() {
        return GLOBAL_VARIABLES;
    }

    @Override
    public char getStringEscapeCharacter() {
        if (serverExtension != null && serverExtension.supportsBackslashStringEscape()) {
            return '\\';
        }
        return super.getStringEscapeCharacter();
    }

    @Override
    public int getCatalogUsage() {
        return SQLDialect.USAGE_DML;
    }

    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }

    @NotNull
    @Override
    public String[] getParametersPrefixes() {
        return new String[]{"$"};
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return BLOCK_BOUND_KEYWORDS;
    }

    @Override
    public String getCastedAttributeName(@NotNull DBSAttributeBase attribute, String attributeName) {
        // This method actually works for special data types like JSON and XML.
        // Because column names in the condition in a table without key must be also cast, as data in getTypeCast method.
        if (attribute instanceof DBSObject sAttr && !DBUtils.isPseudoAttribute(attribute)) {
            if (!CommonUtils.equalObjects(attributeName, attribute.getName())) {
                // Must use explicit attribute name
                attributeName = DBUtils.getQuotedIdentifier(sAttr.getDataSource(), attributeName);
            } else {
                attributeName = DBUtils.getObjectFullName(sAttr.getDataSource(), attribute, DBPEvaluationContext.DML);
            }
        }
        return getCastedString(attribute, attributeName, true, true);
    }

    @NotNull
    @Override
    public String getTypeCastClause(@NotNull DBSTypedObject attribute, String expression, boolean isInCondition) {
        // Some data for some types of columns data types must be cast. It can be simple casting only with data type name like "::pg_class" or casting with fully qualified names for user defined types like "::schemaName.testType".
        // Or very special clauses with JSON and XML columns, when we have to cast both column data and column name to text.
        return getCastedString(attribute, expression, isInCondition, false);
    }

    private String getCastedString(@NotNull DBSTypedObject attribute, String string, boolean isInCondition, boolean castColumnName) {
        if (attribute instanceof DBSTypedObjectEx toEx) {
            DBSDataType dataType = toEx.getDataType();
            if (dataType instanceof PostgreDataType pdt) {
                String typeCasting = pdt.getConditionTypeCasting(isInCondition, castColumnName);
                if (CommonUtils.isNotEmpty(typeCasting)) {
                    return string + typeCasting;
                }
            }
        }
        return string;
    }

    @NotNull
    @Override
    public String escapeScriptValue(DBSTypedObject attribute, @NotNull Object value, @NotNull String strValue) {
        if (PostgreUtils.isPGObject(value)
            || PostgreConstants.TYPE_BIT.equals(attribute.getTypeName())
            || PostgreConstants.TYPE_INTERVAL.equals(attribute.getTypeName())
            || attribute.getTypeID() == Types.OTHER
            || attribute.getTypeID() == Types.ARRAY
            || attribute.getTypeID() == Types.STRUCT)
        {
            // TODO: we need to add value handlers for all PG data types.
            // For now we use workaround: represent objects as strings
            return '\'' + escapeString(strValue) + '\'';
        }
        if (CommonUtils.isNaN(value) || CommonUtils.isInfinite(value)) {
            // These special values should be quoted
            // https://www.postgresql.org/docs/current/datatype-numeric.html#DATATYPE-NUMERIC-DECIMAL
            return '\'' + String.valueOf(value) + '\'';
        }
        return super.escapeScriptValue(attribute, value, strValue);
    }

    @NotNull
    @Override
    public String[][] getStringQuoteStrings() {
        return PG_STRING_QUOTES;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsAliasInConditions() {
        return false;
    }

    @Override
    public boolean supportsTableDropCascade() {
        return true;
    }

    @Override
    public boolean supportsColumnAutoIncrement() {
        return false;
    }

    @Override
    public boolean supportsCommentQuery() {
        return true;
    }

    @Override
    public boolean supportsNestedComments() {
        return true;
    }

    @Nullable
    @Override
    public SQLExpressionFormatter getCaseInsensitiveExpressionFormatter(@NotNull DBCLogicalOperator operator) {
        if (operator == DBCLogicalOperator.LIKE) {
            return (left, right) -> left + " ILIKE " + right;
        }
        return super.getCaseInsensitiveExpressionFormatter(operator);
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return PostgreBinaryFormatter.INSTANCE;
    }

    @Override
    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        super.loadDataTypesFromDatabase(dataSource);
        addDataTypes(PostgreConstants.DATA_TYPE_ALIASES.keySet());
        addDataTypes(Arrays.asList(POSTGRE_EXTRA_TYPES));
    }

    @NotNull
    @Override
    public String[] getNonTransactionKeywords() {
        return POSTGRE_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    protected boolean isStoredProcedureCallIncludesOutParameters() {
        return false;
    }

    @NotNull
    @Override
    public TPRule[] extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull RulePosition position) {
        if (position == RulePosition.INITIAL || position == RulePosition.PARTITION) {
            boolean ddTagDefault = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PostgreConstants.PROP_DD_TAG_STRING);
            boolean ddTagIsString = dataSource == null
                ? ddTagDefault
                : CommonUtils.getBoolean(dataSource.getActualConnectionConfiguration().getProviderProperty(PostgreConstants.PROP_DD_TAG_STRING), ddTagDefault);

            boolean ddPlainDefault = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PostgreConstants.PROP_DD_PLAIN_STRING);
            boolean ddPlainIsString = dataSource == null
                ? ddPlainDefault
                : CommonUtils.getBoolean(dataSource.getActualConnectionConfiguration().getProviderProperty(PostgreConstants.PROP_DD_PLAIN_STRING), ddPlainDefault);

            return new TPRule[] {
                new SQLDollarQuoteRule(position == RulePosition.PARTITION, true, ddTagIsString, ddPlainIsString),
                new PostgreEscapeStringRule()
            };
        }
        return new TPRule[0];
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return true;
    }

    @Override
    public String convertExternalDataType(@NotNull SQLDialect sourceDialect, @NotNull DBSTypedObject sourceTypedObject, @Nullable DBPDataTypeProvider targetTypeProvider) {
        String externalTypeName = sourceTypedObject.getTypeName().toLowerCase(Locale.ENGLISH);
        String localDataType = null, dataTypeModifies = null;

        switch (externalTypeName) {
            case "xml":
            case "xmltype":
            case "sys.xmltype":
                localDataType = "xml";
                break;
            case "varchar2":
            case "nchar":
            case "nvarchar":
                localDataType = "varchar";
                if (sourceTypedObject.getMaxLength() > 0 &&
                    sourceTypedObject.getMaxLength() != Integer.MAX_VALUE &&
                    sourceTypedObject.getMaxLength() != Long.MAX_VALUE)
                {
                    dataTypeModifies = String.valueOf(sourceTypedObject.getMaxLength());
                }
                break;
            case "json":
            case "jsonb":
                localDataType = "jsonb";
                break;
            case "geometry":
            case "sdo_geometry":
            case "mdsys.sdo_geometry":
                localDataType = "geometry";
                break;
            case "number":
                localDataType = "numeric";
                if (sourceTypedObject.getPrecision() != null) {
                    dataTypeModifies = sourceTypedObject.getPrecision().toString();
                    if (sourceTypedObject.getScale() != null) {
                        dataTypeModifies += "," + sourceTypedObject.getScale();
                    }
                }
                break;
        }
        if (localDataType == null) {
            return null;
        }
        if (targetTypeProvider == null) {
            return localDataType;
        } else {
            DBSDataType dataType = targetTypeProvider.getLocalDataType(localDataType);
            if (dataType == null) {
                return null;
            }
            String targetTypeName = DBUtils.getObjectFullName(dataType, DBPEvaluationContext.DDL);
            if (dataTypeModifies != null) {
                targetTypeName += "(" + dataTypeModifies + ")";
            }
            return targetTypeName;
        }
    }

    @Nullable
    @Override
    public String getAutoIncrementKeyword() {
        return AUTO_INCREMENT_KEYWORD;
    }

    @Override
    public boolean supportsCreateIfExists() {
        return true;
    }

    @NotNull
    @Override
    public String getTimestampDataType() {
        return PostgreConstants.TYPE_TIMESTAMP;
    }

    @NotNull
    @Override
    public String getBigIntegerType() {
        return PostgreConstants.TYPE_BIGINT;
    }

    @NotNull
    @Override
    public String getClobDataType() {
        return PostgreConstants.TYPE_TEXT;
    }

    @NotNull
    @Override
    public String getBlobDataType() {
        return PostgreConstants.TYPE_BYTEA;
    }

    @NotNull
    @Override
    public String getUuidDataType() {
        return PostgreConstants.TYPE_UUID;
    }

    @NotNull
    @Override
    public String getBooleanDataType() {
        return PostgreConstants.TYPE_BOOLEAN;
    }

    @NotNull
    @Override
    public String getAlterColumnOperation() {
        return PostgreConstants.OPERATION_ALTER;
    }

    @Override
    public boolean supportsNoActionIndex() {
        return true;
    }

    @Override
    public boolean supportsAlterColumnSet() {
        return true;
    }

    @Override
    public boolean supportsAlterHasColumn() {
        return true;
    }

    @NotNull
    @Override
    public String getSchemaExistQuery(@NotNull String schemaName) {
        return "SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = " + getQuotedString(schemaName);
    }

    @NotNull
    @Override
    public String getCreateSchemaQuery(@NotNull String schemaName) {
        return "CREATE SCHEMA " + schemaName;
    }

    @Override
    public EnumSet<ProjectionAliasVisibilityScope> getProjectionAliasVisibilityScope() {
        return EnumSet.of(
            ProjectionAliasVisibilityScope.GROUP_BY,
            ProjectionAliasVisibilityScope.ORDER_BY
        );
    }

    @Override
    public boolean isEscapeBackslash() {
        return true;
    }
}
