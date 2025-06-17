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

package org.jkiss.dbeaver.ext.kingbase;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KingbaseConstants
 */
public class KingbaseConstants {

    public static final int DEFAULT_PORT = 54321;
    public static final String DEFAULT_DATABASE = "test";
    public static final String DEFAULT_USER = "system";
    
    public static final String ANON_NAME = "anon";
    public static final String DBMS_SQL_NAME = "dbms_sql";
    public static final String PERF_NAME = "perf";
    public static final String SRC_RESTRICT_NAME = "src_restrict";
    public static final String SYS_NAME = "sys";
    public static final String SYSAUDIT_NAME = "sysaudit";
    public static final String SYSMAC_NAME = "sysmac";
    public static final String WMSYS_NAME = "wmsys";
    public static final String XLOG_RECORD_READ_NAME = "xlog_record_read";
    
    public static final String SYS_SYSTEM_SCHEMA_PREFIX = "sys_";
    public static final String SYS_CATALOG_SCHEMA_NAME = "sys_catalog";
    public static final String SYS_TEMP_SCHEMA_NAME = "sys_temp";
    public static final String SYS_TOAST_SCHEMA_PREFIX = "sys_toast";
    public static final String SYS_TEMP_SCHEMA_PREFIX = "sys_temp_";

    public static final String KB_OBJECT_CLASS = "com.kingbase8.util.KBobject";
    public static final String KB_ARRAY_CLASS = "com.kingbase8.jdbc.KbArray";
    public static final String KB_INTERVAL_CLASS = "com.kingbase8.util.KBInterval";
    public static final String KSQL_EXCEPTION_CLASS_NAME = "com.kingbase8.util.KSQLException";

    public static final String HANDLER_SSL = "kingbase_ssl";
}
