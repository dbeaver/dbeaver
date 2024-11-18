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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.model.data.KingbaseBinaryFormatter;
import org.jkiss.dbeaver.ext.kingbase.sql.KingbaseEscapeStringRule;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDataTypeConverter;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectDDLExtension;
import org.jkiss.dbeaver.model.sql.SQLDialectSchemaController;
import org.jkiss.dbeaver.model.sql.SQLExpressionFormatter;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDollarQuoteRule;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Kingbase dialect
 */
public class KingbaseDialect extends JDBCSQLDialect implements TPRuleProvider, SQLDataTypeConverter,
    SQLDialectDDLExtension, SQLDialectSchemaController {
    public static final String[] KINGBASE_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "SHOW", "SET"
        }
    );

    private static final String[][] KB_STRING_QUOTES = {
        {"'", "'"}
    };

    
    public static final String[][] BLOCK_BOUND_KEYWORDS = {
    };

    private static final String[] EXEC_KEYWORDS = {
        "CALL"
    };

    private static final String[] OTHER_TYPES_FUNCTION = {
        "current_date",
        "current_time",
        "current_timestamp",
        "current_role",
        "current_user",
    };
    public static final String AUTO_INCREMENT_KEYWORD = "AUTO_INCREMENT";

    //region KeyWords

    public static String[] KINGBASE_EXTRA_KEYWORDS = new String[]{
        "ABSENT",
        "ACCORDING",
        "ADA",
        "ADMIN",
        "BASE64",
        "BEGIN_FRAME",
        "BEGIN_PARTITION",
        "BERNOULLI",
        "BIT_LENGTH",
        "BLOCKED",
        "BOM",
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
        "FIRST_VALUE",
        "FRAME_ROW",
        "FS",
        "GROUPS",
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
        "NAMESPACE",
        "NIL",
        "NTH_VALUE",
        "NTILE",
        "NULLABLE",
        "OCCURRENCES_REGEX",
        "PASSTHROUGH",
        "PERCENT",
        "PERIOD",
        "PERMISSION",
        "POSITION_REGEX",
        "PRECEDES",
        "PROCEDURES",
        "RECOVERY",
        "REQUIRING",
        "RESPECT",
        "RESTORE",
        "RULE",
        "SIMPLE",
        "SQLCODE",
        "SQLERROR",
        "SUCCEEDS",
        "TOKEN",
        "UNLINK",
        "UNTYPED",
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
    
    public static String[] KINGBASE_EXTRA_TYPES = new String[]{
        "UUID",
    };

    public static String[] KINGBASE_ONE_CHAR_KEYWORDS = new String[]{
        "C",
        "G",
        "K",
        "M",
        "T",
        "P"
    };

    public static String[] KINGBASE_FUNCTIONS_AGGREGATE = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_WINDOW = new String[]{
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


    public static String[] KINGBASE_FUNCTIONS_MATH = new String[]{
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
    public static String[] KINGBASE_FUNCTIONS_STRING = new String[]{
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
        "sys_client_encoding",
        "sys_backend_pid",
        "sys_database_size",
        "sys_sleep",
        "sys_terminate_backend",
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

    public static String[] KINGBASE_FUNCTIONS_DATETIME = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_GEOMETRY = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_NETWROK = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_LO = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_ADMIN = new String[]{
        "current_setting",
        "set_config",
        "brin_summarize_new_values",
        "brin_summarize_range",
        "brin_desummarize_range",
        "gin_clean_pending_list",
        "sys_cancel_backend",
        "sys_log_backend_memory_contexts",
        "sys_reload_conf",
        "sys_rotate_logfile",
        "sys_create_restore_point",
        "sys_current_wal_flush_lsn",
        "sys_current_wal_insert_lsn",
        "sys_current_wal_lsn",
        "sys_backup_start",
        "sys_backup_stop",
        "sys_switch_wal",
        "sys_walfile_name",
        "sys_walfile_name_offset",
        "sys_split_walfile_name",
        "sys_wal_lsn_diff",
        "sys_is_in_recovery",
        "sys_last_wal_receive_lsn",
        "sys_last_wal_replay_lsn",
        "sys_last_xact_replay_timestamp",
        "sys_get_wal_resource_managers",
        "sys_is_wal_replay_paused",
        "sys_get_wal_replay_pause_state",
        "sys_promote",
        "sys_wal_replay_pause",
        "sys_wal_replay_resume",
        "sys_export_snapshot",
        "sys_log_standby_snapshot",
        "sys_create_physical_replication_slot",
        "sys_drop_replication_slot",
        "sys_create_logical_replication_slot",
        "sys_copy_physical_replication_slot",
        "sys_copy_logical_replication_slot",
        "sys_logical_slot_get_changes",
        "sys_logical_slot_peek_changes",
        "sys_logical_slot_get_binary_changes",
        "sys_logical_slot_peek_binary_changes",
        "sys_replication_slot_advance",
        "sys_replication_origin_create",
        "sys_replication_origin_drop",
        "sys_replication_origin_oid",
        "sys_replication_origin_session_setup",
        "sys_replication_origin_session_reset",
        "sys_replication_origin_session_is_setup",
        "sys_replication_origin_session_progress",
        "sys_replication_origin_xact_setup",
        "sys_replication_origin_xact_reset",
        "sys_replication_origin_advance",
        "sys_replication_origin_progress",
        "sys_logical_emit_message",
        "sys_column_size",
        "sys_column_compression",
        "sys_indexes_size",
        "sys_relation_size",
        "sys_size_bytes",
        "sys_size_pretty",
        "sys_table_size",
        "sys_tablespace_size",
        "sys_total_relation_size",
        "sys_relation_filenode",
        "sys_relation_filepath",
        "sys_filenode_relation",
        "sys_collation_actual_version",
        "sys_database_collation_actual_version",
        "sys_import_system_collations",
        "sys_partition_tree",
        "sys_partition_ancestors",
        "sys_partition_root",
        "sys_ls_dir",
        "sys_ls_logdir",
        "sys_ls_waldir",
        "sys_ls_logicalmapdir",
        "sys_ls_logicalsnapdir",
        "sys_ls_replslotdir",
        "sys_ls_archive_statusdir",
        "sys_ls_tmpdir",
        "sys_read_file",
        "sys_read_binary_file",
        "sys_stat_file",
        "sys_advisory_lock",
        "sys_advisory_lock_shared",
        "sys_advisory_unlock",
        "sys_advisory_unlock_all",
        "sys_advisory_unlock_shared",
        "sys_advisory_xact_lock",
        "sys_advisory_xact_lock_shared",
        "sys_try_advisory_lock",
        "sys_try_advisory_lock_shared",
        "sys_try_advisory_xact_lock",
        "sys_try_advisory_xact_lock_shared"
    };

    public static String[] KINGBASE_FUNCTIONS_RANGE = new String[]{
        "isempty",
        "lower_inc",
        "upper_inc",
        "lower_inf",
        "upper_inf",
        "range_merge"
    };

    public static String[] KINGBASE_FUNCTIONS_TEXT_SEARCH = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_XML = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_JSON = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_ARRAY = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_INFO = new String[]{
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

    public static String[] KINGBASE_FUNCTIONS_COMPRASION = new String[]{
        "num_nonnulls",
        "num_nulls"
    };

    public static String[] KINGBASE_FUNCTIONS_FORMATTING = new String[]{
        "to_char",
        "to_date",
        "to_number",
        "to_timestamp"
    };

    public static String[] KINGBASE_FUNCTIONS_ENUM = new String[]{
        "enum_first",
        "enum_last",
        "enum_range"
    };

    public static String[] KINGBASE_FUNCTIONS_SEQUENCE = new String[]{
        "currval",
        "lastval",
        "nextval",
        "setval"
    };

    public static String[] KINGBASE_FUNCTIONS_BINARY_STRING = new String[]{
        "bit_count",
        "get_bit",
        "get_byte",
        "set_bit",
        "set_byte",
        "substr"
    };

    public static String[] KINGBASE_FUNCTIONS_CONDITIONAL = new String[]{
        "coalesce",
        "nullif",
        "greatest",
        "least"
    };

    public static String[] KINGBASE_FUNCTIONS_TRIGGER = new String[]{
        "suppress_redundant_updates_trigger",
        "tsvector_update_trigger",
        "tsvector_update_trigger_column"
    };

    public static String[] KINGBASE_FUNCTIONS_SRF = new String[]{
        "generate_series",
        "generate_subscripts"
    };

    private KingbaseServerExtension serverExtension;

    public KingbaseDialect() {
        super("Kingbase", "kingbase");
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
            "DATATYPE",
            "TABLESPACE",
            "REFRESH"
        );

        addExtraKeywords(KINGBASE_EXTRA_KEYWORDS);

        addKeywords(Arrays.asList(OTHER_TYPES_FUNCTION), DBPKeywordType.OTHER);

        addExtraFunctions(KINGBASE_FUNCTIONS_ADMIN);
        addExtraFunctions(KINGBASE_FUNCTIONS_AGGREGATE);
        addExtraFunctions(KINGBASE_FUNCTIONS_ARRAY);
        addExtraFunctions(KINGBASE_FUNCTIONS_BINARY_STRING);
        addExtraFunctions(KINGBASE_FUNCTIONS_COMPRASION);
        addExtraFunctions(KINGBASE_FUNCTIONS_CONDITIONAL);
        addExtraFunctions(KINGBASE_FUNCTIONS_DATETIME);
        addExtraFunctions(KINGBASE_FUNCTIONS_ENUM);
        addExtraFunctions(KINGBASE_FUNCTIONS_FORMATTING);
        addExtraFunctions(KINGBASE_FUNCTIONS_GEOMETRY);
        addExtraFunctions(KINGBASE_FUNCTIONS_INFO);
        addExtraFunctions(KINGBASE_FUNCTIONS_JSON);
        addExtraFunctions(KINGBASE_FUNCTIONS_LO);
        addExtraFunctions(KINGBASE_FUNCTIONS_MATH);
        addExtraFunctions(KINGBASE_FUNCTIONS_NETWROK);
        addExtraFunctions(KINGBASE_FUNCTIONS_RANGE);
        addExtraFunctions(KINGBASE_FUNCTIONS_SEQUENCE);
        addExtraFunctions(KINGBASE_FUNCTIONS_SRF);
        addExtraFunctions(KINGBASE_FUNCTIONS_STRING);
        addExtraFunctions(KINGBASE_FUNCTIONS_TEXT_SEARCH);
        addExtraFunctions(KINGBASE_FUNCTIONS_TRIGGER);
        addExtraFunctions(KINGBASE_FUNCTIONS_WINDOW);
        addExtraFunctions(KINGBASE_FUNCTIONS_XML);

        removeSQLKeyword("LENGTH");

        if (dataSource instanceof KingbaseDataSource) {
            serverExtension = ((KingbaseDataSource) dataSource).getServerType();
            serverExtension.configureDialect(this);
        }
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
       
        if (attribute instanceof DBSObject && !DBUtils.isPseudoAttribute(attribute)) {
            if (!CommonUtils.equalObjects(attributeName, attribute.getName())) {
                // Must use explicit attribute name
                attributeName = DBUtils.getQuotedIdentifier(((DBSObject) attribute).getDataSource(), attributeName);
            } else {
                attributeName = DBUtils.getObjectFullName(((DBSObject) attribute).getDataSource(), attribute, DBPEvaluationContext.DML);
            }
        }
        return getCastedString(attribute, attributeName, true, true);
    }

    @NotNull
    @Override
    public String getTypeCastClause(@NotNull DBSTypedObject attribute, String expression, boolean isInCondition) {
        return getCastedString(attribute, expression, isInCondition, false);
    }

    private String getCastedString(@NotNull DBSTypedObject attribute, String string, boolean isInCondition, boolean castColumnName) {
        if (attribute instanceof DBSTypedObjectEx) {
            DBSDataType dataType = ((DBSTypedObjectEx) attribute).getDataType();
            if (dataType instanceof KingbaseDataType) {
                String typeCasting = ((KingbaseDataType) dataType).getConditionTypeCasting(isInCondition, castColumnName);
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
        if (KingbaseUtils.isKBObject(value)
            || KingbaseConstants.TYPE_BIT.equals(attribute.getTypeName())
            || KingbaseConstants.TYPE_INTERVAL.equals(attribute.getTypeName())
            || attribute.getTypeID() == Types.OTHER
            || attribute.getTypeID() == Types.ARRAY
            || attribute.getTypeID() == Types.STRUCT)
        {
            return '\'' + escapeString(strValue) + '\'';
        }
        if (CommonUtils.isNaN(value) || CommonUtils.isInfinite(value)) {
          
            return '\'' + String.valueOf(value) + '\'';
        }
        return super.escapeScriptValue(attribute, value, strValue);
    }

    @NotNull
    @Override
    public String[][] getStringQuoteStrings() {
        return KB_STRING_QUOTES;
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
        return KingbaseBinaryFormatter.INSTANCE;
    }

    @Override
    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        super.loadDataTypesFromDatabase(dataSource);
        addDataTypes(KingbaseConstants.DATA_TYPE_ALIASES.keySet());
        addDataTypes(Arrays.asList(KINGBASE_EXTRA_TYPES));
    }

    @NotNull
    @Override
    public String[] getNonTransactionKeywords() {
        return KINGBASE_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    protected boolean isStoredProcedureCallIncludesOutParameters() {
        return false;
    }

    @NotNull
    @Override
    public TPRule[] extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull RulePosition position) {
        if (position == RulePosition.INITIAL || position == RulePosition.PARTITION) {
            boolean ddTagDefault = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(KingbaseConstants.PROP_DD_TAG_STRING);
            boolean ddTagIsString = dataSource == null
                ? ddTagDefault
                : CommonUtils.getBoolean(dataSource.getActualConnectionConfiguration().getProviderProperty(KingbaseConstants.PROP_DD_TAG_STRING), ddTagDefault);

            boolean ddPlainDefault = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(KingbaseConstants.PROP_DD_PLAIN_STRING);
            boolean ddPlainIsString = dataSource == null
                ? ddPlainDefault
                : CommonUtils.getBoolean(dataSource.getActualConnectionConfiguration().getProviderProperty(KingbaseConstants.PROP_DD_PLAIN_STRING), ddPlainDefault);

            return new TPRule[] {
                new SQLDollarQuoteRule(position == RulePosition.PARTITION, true, ddTagIsString, ddPlainIsString),
                new KingbaseEscapeStringRule()
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
        return KingbaseConstants.TYPE_TIMESTAMP;
    }

    @NotNull
    @Override
    public String getBigIntegerType() {
        return KingbaseConstants.TYPE_BIGINT;
    }

    @NotNull
    @Override
    public String getClobDataType() {
        return KingbaseConstants.TYPE_TEXT;
    }

    @NotNull
    @Override
    public String getBlobDataType() {
        return KingbaseConstants.TYPE_BYTEA;
    }

    @NotNull
    @Override
    public String getUuidDataType() {
        return KingbaseConstants.TYPE_UUID;
    }

    @NotNull
    @Override
    public String getBooleanDataType() {
        return KingbaseConstants.TYPE_BOOLEAN;
    }

    @NotNull
    @Override
    public String getAlterColumnOperation() {
        return KingbaseConstants.OPERATION_ALTER;
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
}
