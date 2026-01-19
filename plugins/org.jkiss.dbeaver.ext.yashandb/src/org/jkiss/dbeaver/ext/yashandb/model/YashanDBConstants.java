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

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

public class YashanDBConstants {

	public static final String[] SYSTEM_SCHEMAS = {
			"SYS",
			"MDSYS",
			"AUDITOR",
			"SECURITOR",
			"XA_SYS"
	};

	public static final String SCHEMA_SYS = "SYS";
	public static final String USER_PUBLIC = "PUBLIC";
	public static final String TYPE_NAME_XML = "XMLTYPE";
	public static final String VIEW_DBA_SOURCE = "DBA_SOURCE";
	public static final String VIEW_ALL_SOURCE = "ALL_SOURCE";
	public static final String TYPE_NAME_GEOMETRY = "PUBLIC.SDO_GEOMETRY";
	public static final String TYPE_FQ_GEOMETRY = "MDSYS.SDO_GEOMETRY";
	public static final String TYPE_FQ_XML = "SYS.XMLTYPE";
	public static final String VIEW_DBA_TAB_PRIVS = "DBA_TAB_PRIVS";

	public static final DBSEntityConstraintType CONSTRAINT_WITH_READ_ONLY = new DBSEntityConstraintType("O",
			"With Read Only", null, false, false, false, false);
	public static final DBSEntityConstraintType CONSTRAINT_WITH_CHECK_OPTION = new DBSEntityConstraintType("V",
			"With Check Option", null, false, false, false, false);

	public static final String PROP_ALWAYS_SHOW_DBA = DBConstants.INTERNAL_PROP_PREFIX + "always-show-dba@";
	public static final String PROP_METADATA_USE_SIMPLE_CONSTRAINTS = DBConstants.INTERNAL_PROP_PREFIX
			+ "meta-use-simple-constraints@";
	public static final String PROP_USE_RULE_HINT = DBConstants.INTERNAL_PROP_PREFIX + "use-rule-hint@";
	public static final String PROP_METADATA_USE_SYS_SCHEMA = DBConstants.INTERNAL_PROP_PREFIX + "meta-use-sys-schema@";
	public static final String PROP_ALWAYS_USE_DBA_VIEWS = DBConstants.INTERNAL_PROP_PREFIX + "always-use-dba-views@";

	public static final String YES = "YES";
	public static final String Y = "Y";
	public static final String PREF_KEY_DDL_FORMAT = "yashan.ddl.format";
	public static final String PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING = "yashan.disable.script.escape";
	public static final String PROP_OBJECT_DEFINITION = "objectDefinitionText";

	public static final String TYPE_NAME_BFILE = "BFILE";
	public static final String TYPE_NAME_CFILE = "CFILE";
	public static final String TYPE_CONTENT_POINTER = "CONTENT POINTER";
	public static final String TYPE_NAME_VARCHAR2 = "VARCHAR2";
	public static final String TYPE_NUMBER = "NUMBER";
	public static final String TYPE_DECIMAL = "DECIMAL";
	public static final String TYPE_LONG = "LONG";
	public static final String TYPE_LONG_RAW = "LONG RAW";
	public static final String TYPE_OCTET = "OCTET";
	public static final String TYPE_INTERVAL_YEAR_MONTH = "INTERVAL YEAR TO MONTH";
	public static final String TYPE_INTERVAL_DAY_SECOND = "INTERVAL DAY TO SECOND";

	public static final int NUMERIC_MAX_PRECISION = 38;
	public static final int INTERVAL_DEFAULT_SECONDS_PRECISION = 6;

	// Index Type
	public static final DBSIndexType INDEX_TYPE_NORMAL = new DBSIndexType("NORMAL", "BTree");
	public static final DBSIndexType INDEX_TYPE_NORMAL_REV = new DBSIndexType("NORMAL/REV", "BTree Reverse");
	public static final DBSIndexType INDEX_TYPE_FUNCTION_BASED_NORMAL = new DBSIndexType("FUNCTION-BASED NORMAL",
			"Function BTree");
	public static final DBSIndexType INDEX_TYPE_FUNCTION_BASED_NORMAL_REV = new DBSIndexType(
			"FUNCTION-BASED NORMAL/REV", "Function BTree Reverse");
	public static final DBSIndexType INDEX_TYPE_LOB = new DBSIndexType("LOB", "LOB");
	public static final DBSIndexType INDEX_TYPE_COLUMNAR = new DBSIndexType("COLUMNAR", "COLUMNAR");
	public static final DBSIndexType INDEX_TYPE_RTREE = new DBSIndexType("RTREE", "RTREE");

	public static final int MAXIMUM_DBMS_OUTPUT_SIZE = 1000000;
}
