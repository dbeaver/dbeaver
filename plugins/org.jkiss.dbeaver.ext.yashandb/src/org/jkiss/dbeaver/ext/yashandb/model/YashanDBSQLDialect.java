
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
package org.jkiss.dbeaver.ext.yashandb.model;

import java.util.Arrays;
import java.util.Locale;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDataTypeConverter;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLExpressionFormatter;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLParserActionKind;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.SQLTokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateFactory;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicatesCondition;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

public class YashanDBSQLDialect extends JDBCSQLDialect implements SQLDataTypeConverter {

	private static final Log log = Log.getLog(YashanDBSQLDialect.class);

	private static final String[] EXEC_KEYWORDS = new String[] { "call" };

	private static final String[] YASHANDB_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
			BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
			new String[] { "CREATE", "ALTER", "DROP", "ANALYZE", "VALIDATE", });

	private static final String[][] YASHANDB_BEGIN_END_BLOCK = new String[][] {
			{ SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END }, { "IF", SQLConstants.BLOCK_END + " IF" },
			{ "LOOP", SQLConstants.BLOCK_END + " LOOP" },
			{ SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END + " " + SQLConstants.KEYWORD_CASE } };

	private static final String[] YASHANDB_BLOCK_HEADERS = new String[] { "DECLARE", "FUNCTION", "PROCEDURE" };

	private static final String[] YASHANDB_INNER_BLOCK_PREFIXES = new String[] { "AS", "IS" };

	// V$RESERVED_WORDS
	public static final String[] YASHANDB_ALL_KEYWORD = { "ABORT", "ACCESS", "ACCOUNT", "ACTIONS", "ADD", "ADMINISTER",
			"ALL", "ALL_ROWS", "ALTER", "ANALYZE", "AND", "ANY", "APPEND", "ARCHIVE", "ARCHIVELOG", "AS", "ASC", "AT",
			"AUDIT", "AUTHID", "AUTO_LOGIN", "BACKUP", "BASE", "BATCH_MODE", "BEFORE", "BEGIN", "BETWEEN", "BFILE",
			"BIGFILE", "BIGINT", "BINARY", "BINARY_BIGINT", "BINARY_DOUBLE", "BINARY_FLOAT", "BINARY_INTEGER",
			"BINARY_SMALLINT", "BINARY_TINYINT", "BIT", "BLOB", "BOOLEAN", "BOUND", "BUFFER_POOL", "BUILD", "BULK",
			"BULKLOAD", "BY", "CACHE", "CALL", "CANCEL", "CASCADE", "CASE", "CATEGORY", "CELL_FLASH_CACHE", "CHANGE",
			"CHAR", "CHARACTER", "CHAR_CS", "CHECK", "CHECKPOINT", "CHOOSE", "CHUNK", "CLEAN", "CLEAR", "CLOB", "CLOSE",
			"CLUSTER", "COALESCE", "COLUMN", "COLUMNAR", "COMMENT", "COMMIT", "COMPRESS", "COMPRESSION", "COMPUTE",
			"CONDITION", "CONNECT", "CONNECT_BY_ISCYCLE", "CONNECT_BY_ISLEAF", "CONNECT_BY_ROOT", "CONSTRAINT",
			"CONSTRAINTS", "CONTENT", "CONTEXT", "CONTINUE", "CONTROLFILES", "CONVERT", "CREATE", "CROSS", "CUBE",
			"CURRENT", "CURRENT_DATE", "CURRENT_TIMESTAMP", "CYCLE", "DATABASE", "DATAFILE", "DATAOID_REUSE",
			"DATASPACE", "DATE", "DAY", "DECIMAL", "DECLARE", "DECRYPTION", "DEDUPLICATE", "DEFAULT", "DEFINITION",
			"DELETE", "DENSE_RANK", "DESC", "DETERMINISTIC", "DIRECTORY", "DISABLE", "DISTINCT", "DISTRIBUTE",
			"DOCUMENT", "DOUBLE", "DOUBLE_PRECISION", "DOUBLE_WRITE", "DP_MAX_JOIN_TABLES", "DROP", "DUMP", "DUPLICATE",
			"DUPLICATED", "EDITIONABLE", "ELSE", "ELSIF", "ENABLE", "ENCODING", "ENCRYPT", "ENCRYPTION", "END",
			"ENGINE_COL", "ENGINE_ROW", "ESCAPE", "EXCEPT", "EXCEPTION", "EXCHANGE", "EXEC", "EXECUTE", "EXISTS",
			"EXIT", "EXPLAIN", "EXTEND", "EXTERNAL", "FAILOVER", "FALSE", "FETCH", "FIRST", "FIRST_ROWS", "FLASHBACK",
			"FLASH_CACHE", "FLOAT", "FLOOR", "FLUSH", "FOLLOWING", "FOR", "FORALL", "FORCE", "FOREIGN", "FORMAT",
			"FREELIST", "FREELISTS", "FROM", "FULL", "FUNCTION", "GENERATED", "GLOBAL", "GOTO", "GRANT", "GROUP",
			"GROUPING", "HASH_AJ", "HASH_SJ", "HAVING", "HEAP", "HORDER", "HOUR", "IDENTIFIED", "IF", "IGNORE",
			"IMMEDIATE", "IN", "INCLUDE", "INCREMENT", "INCREMENTAL", "INDEPEND", "INDEX", "INDEX_ASC", "INDEX_DESC",
			"INDEX_FFS", "INDEX_JOIN", "INDEX_SS", "INDEX_SS_ASC", "INDEX_SS_DESC", "INITIAL", "INITRANS", "INMEMORY",
			"INNER", "INSERT", "INSTANCE", "INSTANCES", "INT", "INTEGER", "INTERSECT", "INTERVAL", "INTO", "INVALIDATE",
			"INVISIBLE", "IS", "JOIN", "JSON", "KEEP", "KEEP_DUPLICATES", "KEY", "KEYSTORE", "KILL", "LEADING", "LEFT",
			"LEVEL", "LIBRARY", "LIKE", "LIMIT", "LINK", "LOAD", "LOB", "LOCAL", "LOCK", "LOG", "LOGFILE", "LOGGING",
			"LOGOFF", "LOGON", "LOOP", "LSC", "MANAGEMENT", "MATCHED", "MATERIALIZED", "MAXDATABUCKETS", "MAXDATAFILES",
			"MAXEXTENTS", "MAXINSTANCES", "MAXLOGFILES", "MAXLOGHISTORY", "MAXSIZE", "MAXTRANS", "MAXVALUE",
			"MAX_WORKERS_PER_EXEC", "MCOL", "MERGE", "MERGE_AJ", "MERGE_SJ", "MINEXTENTS", "MINUS", "MINUTE",
			"MINVALUE", "MOD", "MODIFY", "MONTH", "MOUNT", "NATIONAL", "NATURAL", "NCHAR", "NCHAR_CS", "NCLOB",
			"NESTED", "NEW", "NEXT", "NEXTVAL", "NL_AJ", "NL_SJ", "NO", "NOAPPEND", "NOARCHIVELOG", "NOAUDIT",
			"NOCACHE", "NOCOMPRESS", "NOCYCLE", "NOKEEP", "NOLOGGING", "NOMAXVALUE", "NOMINVALUE", "NONEDITIONABLE",
			"NOORDER", "NOPARALLEL", "NOREVERSE", "NORMAL", "NOSCALE", "NOT", "NOVALIDATE", "NOWAIT", "NO_INDEX",
			"NO_INDEX_FFS", "NO_INDEX_SS", "NO_USE_HASH", "NO_USE_MERGE", "NO_USE_NL", "NULL", "NULLS", "NUMBER",
			"NUMERIC", "NVARCHAR", "NVARCHAR2", "OBJNO_REUSE", "OF", "OFFLINE", "OFFSET", "ON", "ONLINE", "ONLY",
			"OPEN", "OR", "ORDER", "ORDERED", "ORGANIZATION", "OUTER", "OUTLINE", "OVER", "OVERLAPS", "PACKAGE",
			"PARALLEL", "PARALLELISM", "PARTITION", "PARTITIONS", "PASSWORD", "PCTFREE", "PCTINCREASE", "PCTUSED",
			"PIPE", "PIVOT", "PLACE_GROUP_BY", "PLS_INTEGER", "PRAGMA", "PRECEDING", "PREPARE", "PRESERVE", "PRIMARY",
			"PRIOR", "PRIVATE", "PRIVILEGE", "PRIVILEGES", "PROCEDURE", "PROFILE", "PUBLIC", "PURGE", "QUOTA", "RAISE",
			"RANGE", "RAW", "READ", "READONLY", "READWRITE", "REAL", "REBUILD", "RECLAIM", "RECOVER", "REFERENCES",
			"REFRESH", "REGISTER", "RELEASE", "RENAME", "RESETLOGS", "RESPECT", "RESTART", "RESTORE", "RESTRICT",
			"RETURN", "RETURNING", "REUSE", "REVERSE", "REVOKE", "RIGHT", "RLIKE", "ROLE", "ROLES", "ROLLBACK",
			"ROLLUP", "ROW", "ROWID", "ROWNUM", "ROWS", "RTREE", "RULE", "SAMPLE", "SAVEPOINT", "SCHEMA", "SCN",
			"SECOND", "SECTION", "SEGMENT", "SELECT", "SELECTIVITY", "SEPARATOR", "SEQUENCE", "SESSION", "SET", "SETS",
			"SHARDED", "SHARED", "SHRINK", "SHUTDOWN", "SIBLINGS", "SKIP", "SLICE", "SMALLFILE", "SMALLINT", "SOME",
			"SPLIT", "SQL", "SQLMAP", "STABLE", "STANDBY", "START", "STATISTICS", "STOP", "STORAGE", "SUBPARTITION",
			"SUBPARTITIONS", "SUBTYPE", "SUCCESSFUL", "SUPPLEMENTAL", "SWAP", "SWITCH", "SWITCHOVER", "SYNONYM",
			"SYSAUX", "SYSDATE", "SYSTEM", "SYSTIMESTAMP", "SYS_REFCURSOR", "TABLE", "TABLESPACE", "TAC", "TAG", "TDE",
			"TEMPFILE", "TEMPORARY", "THEN", "TIME", "TIMESTAMP", "TINYINT", "TO", "TRANSACTION", "TRANSPORT",
			"TRIGGER", "TRUE", "TRUNCATE", "TTL", "TYPE", "UNBOUNDED", "UNDO", "UNDO_SEGMENTS", "UNION", "UNIQUE",
			"UNLOCK", "UNPIVOT", "UNUSABLE", "UPDATE", "UPGRADE", "UROWID", "USABLE", "USE", "USER", "USE_HASH",
			"USE_MERGE", "USE_NL", "USING", "UTC_TIMESTAMP", "VALIDATE", "VALUES", "VARCHAR", "VARCHAR2", "VIEW",
			"VISIBLE", "WAIT", "WELLFORMED", "WHEN", "WHENEVER", "WHERE", "WHILE", "WITH", "WITHOUT", "WRAPPED", "YEAR",
			"ZONE", "ZORDER" };

	public static final String[] ADVANCED_KEYWORDS = { "SYNONYM", "CREATE OR REPLACE", "NEXTVAL", "REPLACEex",
			"PACKAGE", "FUNCTION", "TYPE", "BODY", "RECORD", "TRIGGER", "MATERIALIZED", "IF", "EACH", "RETURN",
			"WRAPPED", "AFTER", "BEFORE", "DATABASE", "ANALYZE", "VALIDATE", "STRUCTURE", "COMPUTE", "STATISTICS",
			"LOOP", "WHILE", "BULK", "ELSIF", "EXIT", };

	private DBPPreferenceStore preferenceStore;

	private SQLTokenPredicateSet cachedDialectSkipTokenPredicates = null;

	public YashanDBSQLDialect() {
		super("YashanDB", "yashandb");
		log.debug(">>>Initialize {YashanDBSQLDialect}....");
		setUnquotedIdentCase(DBPIdentifierCase.UPPER);
	}

	public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
		super.initDriverSettings(session, dataSource, metaData);
		preferenceStore = dataSource.getContainer().getPreferenceStore();

		setIdentifierQuoteString(DEFAULT_IDENTIFIER_QUOTES);

		// V$FUNCTION
		addFunctions(Arrays.asList("DBMS_OUTPUT.PUT_LINE", "ABS", "ACOS", "ADD_MONTHS", "AGE", "ARRAY_APPEND",
				"ARRAY_LENGTH", "ARRAY_NDIMS", "ARRAY_POSITION", "ARRAY_REMOVE", "ARRAY_REPLACE", "ARRAY_TO_STRING",
				"ARRAY_UPPER", "ASCII", "ASCIISTR", "ASIN", "ATAN", "ATAN2", "AVG", "BFILENAME", "BIN", "BIN_TO_NUM",
				"BITAND", "BITOR", "BITXOR", "BIT_LENGTH", "CAST", "CEIL", "CHARACTER_LENGTH", "CHARTOROWID",
				"CHAR_LENGTH", "CHAR_TO_LABEL", "CHECK_AUDIT_THRESHOLD", "CHECK_SYS_PRIVILEGE", "CHR", "COALESCE",
				"CONCAT", "CONCAT_WS", "CONVERT", "COS", "COT", "COUNT", "CREATEXMLPLUGIN", "CRYPT_ASYM_DECRYPT",
				"CRYPT_ASYM_ENCRYPT", "CRYPT_DECRYPT", "CRYPT_ENCRYPT", "CRYPT_HASH", "CRYPT_HMAC", "CRYPT_KEY",
				"CRYPT_RANDOM", "CRYPT_SELFTEST", "CRYPT_SIGN", "CRYPT_VERIFY", "CURRENT_TIMESTAMP", "DATE", "DATE_ADD",
				"DATE_FORMAT", "DATE_SUB", "DAYOFWEEK", "DBTIMEZONE", "DECODE", "DECRYPT_AES128", "DIV", "DUMP",
				"EMPTY_BLOB", "EMPTY_CLOB", "ENCRYPT_AES128", "EXISTSNODE", "EXP", "EXTRACT", "EXTRACTVALUE",
				"FIND_IN_SET", "FLOOR", "FROM_TZ", "GEOMETRYTYPE", "GETXMLCLOB", "GET_TYPE_NAME", "GREATEST",
				"GROUPING", "GROUPING_ID", "GROUP_CONCAT", "GROUP_ID", "HEXTORAW", "IF", "IFNULL", "INITCAP", "INSTR",
				"INSTRB", "ISNULL", "JSON", "JSON_ARRAY_GET", "JSON_ARRAY_LENGTH", "JSON_EXISTS", "JSON_FORMAT",
				"JSON_PARSE", "JSON_QUERY", "JSON_SERIALIZE", "JSON_VALUE", "LABEL_TO_CHAR", "LAST_DAY",
				"LAST_INSERT_ID", "LEAST", "LEFT", "LENGTH", "LENGTH2", "LENGTHB", "LISTAGG", "LN", "LNNVL",
				"LOCALTIME", "LOCALTIMESTAMP", "LOG", "LOWER", "LPAD", "LSFA_LISTAGG", "LTRIM", "MAX", "MD5", "MEDIAN",
				"MIN", "MOD", "MONTHS_BETWEEN", "MULTISET", "NEXT_DAY", "NLSSORT", "NLS_LOWER", "NOW", "NULLIF",
				"NUMTODSINTERVAL", "NUMTOYMINTERVAL", "NVL", "NVL2", "OCTET_LENGTH", "ORA_HASH", "PERCENTILE_CONT",
				"PI", "POSITION", "POW", "POWER", "RANDOM", "RAWTOHEX", "REGEXP_COUNT", "REGEXP_INSTR", "REGEXP_LIKE",
				"REGEXP_REPLACE", "REGEXP_SUBSTR", "REPLACE", "RETURNXMLTYPE", "REVERSE", "RIGHT", "RLIKE_FILTER",
				"ROUND", "ROWIDTOCHAR", "RPAD", "RTRIM", "SCN_TO_TIMESTAMP", "SECURITY_CLEAR_CSP",
				"SECURITY_MOD_STATUS", "SECURITY_MOD_VERSION", "SESSIONTIMEZONE", "SIGN", "SIN", "SINH", "SOUNDEX",
				"SPLIT", "SQLCODE", "SQLERRM", "SQRT", "STDDEV", "STDDEV_POP", "STDDEV_SAMP", "STRING_AGG",
				"STRING_TO_ARRAY", "STRPOS", "ST_AREA", "ST_ASBINARY", "ST_ASEWKB", "ST_ASGEOJSON", "ST_ASHEXEWKB",
				"ST_ASLATLONTEXT", "ST_ASTEXT", "ST_BOUNDARY", "ST_BUFFER", "ST_BUILDAREA", "ST_CENTROID",
				"ST_CLIPBYBOX2D", "ST_CLOSESTPOINT", "ST_COLLECT", "ST_COLLECTIONEXTRACT", "ST_CONCAVEHULL",
				"ST_CONTAINS", "ST_CONTAINSPROPERLY", "ST_COVEREDBY", "ST_COVERS", "ST_CROSSES", "ST_DIFFERENCE",
				"ST_DISJOINT", "ST_DISTANCE", "ST_DISTANCE_SPHERE", "ST_DUMP", "ST_DWITHIN", "ST_ENVELOPE", "ST_EQUALS",
				"ST_EXTENT", "ST_GEOMCOLLFROMTEXT", "ST_GEOMETRICMEDIAN", "ST_GEOMETRYFROMTEXT", "ST_GEOMETRYTYPE",
				"ST_GEOMFROMEWKB", "ST_GEOMFROMGEOJSON", "ST_GEOMFROMTEXT", "ST_GEOMFROMWKB", "ST_INTERSECTION",
				"ST_INTERSECTS", "ST_ISCLOSED", "ST_ISEMPTY", "ST_ISSIMPLE", "ST_ISVALID", "ST_LENGTH",
				"ST_LINEFROMTEXT", "ST_LINEMERGE", "ST_LONGESTLINE", "ST_MAKEENVELOPE", "ST_MAKELINE", "ST_MAKEPOINT",
				"ST_MAKEVALID", "ST_MAXDISTANCE", "ST_MULTI", "ST_OVERLAPS", "ST_PERIMETER", "ST_POINT",
				"ST_POINTONSURFACE", "ST_POINTZ", "ST_POLYGON", "ST_RELATE", "ST_SETSRID", "ST_SHORTESTLINE",
				"ST_SIMPLIFY", "ST_SPLIT", "ST_SRID", "ST_TOUCHES", "ST_TRANSFORM", "ST_UNION", "ST_WITHIN", "ST_X",
				"ST_Y", "ST_Z", "SUBSTR", "SUBSTRB", "SUBSTRING", "SUBSTRING_INDEX", "SUM", "SYSDATE", "SYSTIMESTAMP",
				"SYS_CONNECT_BY_PATH", "SYS_CONTEXT", "SYS_EXTRACT_UTC", "SYS_GUID", "TAN", "TANH", "TIME", "TIMEDIFF",
				"TIMESTAMP", "TIMESTAMPDIFF", "TIMESTAMP_TO_SCN", "TO_BASE64", "TO_CHAR", "TO_CLOB", "TO_DATE",
				"TO_DSINTERVAL", "TO_MULTI_BYTE", "TO_NUMBER", "TO_SINGLE_BYTE", "TO_TIMESTAMP", "TO_TIMESTAMP_TZ",
				"TO_YMINTERVAL", "TRANSFORMPLUGIN", "TRANSLATE", "TREAT", "TRIM", "TRUNC", "TYPEOF", "TZ_OFFSET",
				"UNISTR", "UNSUPPORT_ERROR", "UPPER", "USERENV", "UTC_TIMESTAMP", "VARIANCE", "VAR_POP", "VAR_SAMP",
				"WM_CONCAT", "XMLAGG", "XMLAPPENDCHILD", "XMLATTRMAKENODE", "XMLCREATEELEMENT", "XMLCREATETEXTNODE",
				"XMLDOCISNULL", "XMLDOCMAKENODE", "XMLELEMENTMAKENODE", "XMLEXTRACT", "XMLFREEDOCUMENT",
				"XMLFREEPARSER", "XMLGETATTRIBUTE", "XMLGETCHILDNODES", "XMLGETCHILDRENBYTAGNAME", "XMLGETDOCELEMENT",
				"XMLGETDOCELEMENTSBYTAGNAME", "XMLGETELEMENTSBYTAGNAME", "XMLGETFIRSTCHILD", "XMLGETLENGTH",
				"XMLGETNODENAME", "XMLGETNODEVALUE", "XMLGETPARENTNODE", "XMLGETVALUE", "XMLITEM", "XMLMAKEELEMENT",
				"XMLNEWDOMDOCUMENT", "XMLNEWEMPTYDOMDOCUMENT", "XMLNEWPARSER", "XMLPARSE", "XMLPARSEBUFFER",
				"XMLPARSECLOB", "XMLPARSERGETDOC", "XMLPARSERPARSEURL", "XMLPARSEURL", "XMLSEQUENCEPLUGIN",
				"XMLSETATTRIBUTE", "XMLSETNODEVALUE", "XMLTABLEPLUGIN", "XMLTEXTMAKENODE", "XMLTYPEPLUGIN",
				"XMLWRITEDOCTOBUF", "XMLWRITENODETOBUF", "YASDECODE", "__GEOM_CHECK_MODIFIER__", "__MAKE_RTREE_KEY3__",
				"__MAKE_RTREE_KEY__", "__MY_COLLATE_SORT"));

		for (String kw : ADVANCED_KEYWORDS) {
			addSQLKeyword(kw);
		}

		turnFunctionIntoKeyword("TRUNCATE");

		cachedDialectSkipTokenPredicates = makeDialectSkipTokenPredicates(dataSource);
	}

	@Override
	public String[] getDMLKeywords() {
		return YASHANDB_ALL_KEYWORD;
	}

	@Override
	public String[][] getBlockBoundStrings() {
		return YASHANDB_BEGIN_END_BLOCK;
	}

	@Override
	public String[] getBlockHeaderStrings() {
		return YASHANDB_BLOCK_HEADERS;
	}

	@Nullable
	@Override
	public String[] getInnerBlockPrefixes() {
		return YASHANDB_INNER_BLOCK_PREFIXES;
	}

	@NotNull
	@Override
	public String[] getExecuteKeywords() {
		return EXEC_KEYWORDS;
	}

	@NotNull
	@Override
	public MultiValueInsertMode getDefaultMultiValueInsertMode() {
		return MultiValueInsertMode.GROUP_ROWS;
	}

	@Override
	public String getLikeEscapeClause(@NotNull String escapeChar) {
		return " ESCAPE " + getQuotedString(escapeChar);
	}

	@NotNull
	@Override
	public String escapeScriptValue(DBSTypedObject attribute, @NotNull Object value, @NotNull String strValue) {
		if (CommonUtils.isNaN(value) || CommonUtils.isInfinite(value)) {
			return '\'' + String.valueOf(value) + '\'';
		}

		String fullTypeName = attribute.getFullTypeName();
		if (fullTypeName.contains("INTERVAL") || fullTypeName.contains("TIME"))
			return '\'' + String.valueOf(value) + '\'';

		return super.escapeScriptValue(attribute, value, strValue);
	}

	@Override
	public boolean supportsAliasInSelect() {
		return true;
	}

	@Override
	public boolean supportsAliasInUpdate() {
		return true;
	}

	@Override
	public boolean supportsTableDropCascade() {
		return true;
	}

	@Nullable
	@Override
	public SQLExpressionFormatter getCaseInsensitiveExpressionFormatter(@NotNull DBCLogicalOperator operator) {
		if (operator == DBCLogicalOperator.LIKE) {
			return (left, right) -> "UPPER(" + left + ") LIKE UPPER(" + right + ")";
		}
		return super.getCaseInsensitiveExpressionFormatter(operator);
	}

	@Override
	public boolean isDelimiterAfterBlock() {
		return true;
	}

	@Nullable
	@Override
	public String getDualTableName() {
		return "DUAL";
	}

	@NotNull
	@Override
	public String[] getNonTransactionKeywords() {
		return YASHANDB_NON_TRANSACTIONAL_KEYWORDS;
	}

	@Override
	protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
		String schemaName = proc.getParentObject().getName();
		return "CALL " + schemaName + "." + proc.getName();
	}

	@Override
	public boolean isDisableScriptEscapeProcessing() {
		return preferenceStore == null
				|| preferenceStore.getBoolean(YashanDBConstants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING);
	}

	@NotNull
	@Override
	public String[] getScriptDelimiters() {
		return super.getScriptDelimiters();
	}

	@Override
	public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column,
			@NotNull String typeName, @NotNull DBPDataKind dataKind) {
		Integer scale;
		switch (typeName) {
		case YashanDBConstants.TYPE_NUMBER:
		case YashanDBConstants.TYPE_DECIMAL:
			DBSDataType dataType = DBUtils.getDataType(column);
			scale = column.getScale();
			int precision = CommonUtils.toInt(column.getPrecision());
			if (precision == 0 && dataType != null && scale != null && scale == dataType.getMinScale()) {
				return "";
			}
			if (precision == 0 || precision > YashanDBConstants.NUMERIC_MAX_PRECISION) {
				precision = YashanDBConstants.NUMERIC_MAX_PRECISION;
			}
			if (scale != null && precision > 0) {
				return "(" + precision + ',' + scale + ")";
			}
			break;
		case YashanDBConstants.TYPE_INTERVAL_DAY_SECOND:
			scale = column.getScale();
			if (scale == null) {
				return "";
			}
			if (scale < 0 || scale > 9) {
				scale = YashanDBConstants.INTERVAL_DEFAULT_SECONDS_PRECISION;
			}
			return "(" + scale + ")";
		case YashanDBConstants.TYPE_NAME_BFILE:
		case YashanDBConstants.TYPE_NAME_CFILE:
		case YashanDBConstants.TYPE_CONTENT_POINTER:
		case YashanDBConstants.TYPE_LONG:
		case YashanDBConstants.TYPE_LONG_RAW:
		case YashanDBConstants.TYPE_OCTET:
		case YashanDBConstants.TYPE_INTERVAL_YEAR_MONTH:
			return "";
		}
		return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
	}

	@Override
	public String convertExternalDataType(@NotNull SQLDialect sourceDialect, @NotNull DBSTypedObject sourceTypedObject,
			@Nullable DBPDataTypeProvider targetTypeProvider) {
		String type = super.convertExternalDataType(sourceDialect, sourceTypedObject, targetTypeProvider);
		if (type != null) {
			return type;
		}
		String externalTypeName = sourceTypedObject.getTypeName().toUpperCase(Locale.ENGLISH);
		String localDataType = null, dataTypeModifies = null;

		switch (externalTypeName) {
		case "VARCHAR":
			localDataType = YashanDBConstants.TYPE_NAME_VARCHAR2;
			break;
		case "XML":
		case "XMLTYPE":
			localDataType = YashanDBConstants.TYPE_FQ_XML;
			break;
		case "JSON":
		case "JSONB":
			localDataType = "JSON";
			break;
		case "GEOMETRY":
		case "GEOGRAPHY":
		case "SDO_GEOMETRY":
			localDataType = YashanDBConstants.TYPE_FQ_GEOMETRY;
			break;
		case "NUMERIC":
			localDataType = YashanDBConstants.TYPE_NUMBER;
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
		if (targetTypeProvider != null) {
			try {
				DBSDataType dataType = targetTypeProvider.resolveDataType(new VoidProgressMonitor(), localDataType);
				if (dataType == null) {
					return null;

				}
				String targetTypeName = DBUtils.getObjectFullName(dataType, DBPEvaluationContext.DDL);
				if (dataTypeModifies != null) {
					targetTypeName += "(" + dataTypeModifies + ")";
				}
				return targetTypeName;
			} catch (DBException e) {
				log.debug("Error resolving local data type", e);
				return null;
			}
		}
		return localDataType;
	}

	@Override
	@NotNull
	public SQLTokenPredicateSet getSkipTokenPredicates() {
		return cachedDialectSkipTokenPredicates == null ? super.getSkipTokenPredicates()
				: cachedDialectSkipTokenPredicates;
	}

	@NotNull
	private SQLTokenPredicateSet makeDialectSkipTokenPredicates(JDBCDataSource dataSource) {
		SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
		syntaxManager.init(this, dataSource.getContainer().getPreferenceStore());
		SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
		ruleManager.loadRules(dataSource, false);
		TokenPredicateFactory tt = TokenPredicateFactory.makeDialectSpecificFactory(ruleManager);
		TokenPredicateSet conditions = TokenPredicateSet.of(
				new TokenPredicatesCondition(SQLParserActionKind.BEGIN_BLOCK,
						tt.sequence("CREATE", tt.optional("OR", "REPLACE"),
								tt.optional(tt.alternative("EDITIONABLE", "NONEDITIONABLE")), "PACKAGE", "BODY"),
						tt.sequence()),
				new TokenPredicatesCondition(SQLParserActionKind.SKIP_SUFFIX_TERM,
						tt.sequence("CREATE", tt.optional("OR", "REPLACE"),
								tt.optional(tt.alternative("EDITIONABLE", "NONEDITIONABLE")),
								tt.alternative("FUNCTION", "PROCEDURE")),
						tt.sequence(tt.alternative(tt.sequence("RETURN", SQLTokenType.T_TYPE), "deterministor",
								"pipelined", "parallel_enable", "result_cache", ")",
								tt.sequence("procedure", SQLTokenType.T_OTHER),
								tt.sequence(SQLTokenType.T_OTHER, SQLTokenType.T_TYPE)), ";")));

		conditions.add(new TokenPredicatesCondition(SQLParserActionKind.SKIP_SUFFIX_TERM, tt.token("WITH"),
				tt.sequence("END", ";")));

		return conditions;
	}

	@Override
	public boolean hasCaseSensitiveFiltration() {
		return true;
	}

	@Override
	public boolean supportsAliasInConditions() {
		return false;
	}
}
