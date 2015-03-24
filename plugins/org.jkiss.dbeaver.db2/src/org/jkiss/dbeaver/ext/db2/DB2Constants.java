/*
 * Copyright (C) 2013-2015 Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;

/**
 * DB2 constants
 * 
 * @author Denis Forveille
 */
public class DB2Constants {

    // Connection properties
    public static final int DEFAULT_PORT = 50000;

    public static final String PROP_TRACE_ENABLED = DBConstants.INTERNAL_PROP_PREFIX + "trace.enabled";
    public static final String PROP_TRACE_FOLDER = DBConstants.INTERNAL_PROP_PREFIX + "trace.folder";
    public static final String PROP_TRACE_FILE = DBConstants.INTERNAL_PROP_PREFIX + "trace.file";
    public static final String PROP_TRACE_APPEND = DBConstants.INTERNAL_PROP_PREFIX + "trace.append";
    public static final String PROP_TRACE_LEVEL = DBConstants.INTERNAL_PROP_PREFIX + "trace.level";

    // Explain Tables
    public static final String EXPLAIN_SCHEMA_NAME_DEFAULT = "SYSTOOLS";

    // DB2 Versions
    public static final Double DB2v9_1 = 9.1; // Lowest supported version
    public static final Double DB2v9_5 = 9.5;
    public static final Double DB2v9_7 = 9.7;
    public static final Double DB2v10_1 = 10.1;
    public static final Double DB2v10_5 = 10.5;

    // Display Categories
    public static final String CAT_AUDIT = "Audit";
    public static final String CAT_AUTH = "Authorities";
    public static final String CAT_BASEBJECT = "Base Object";
    public static final String CAT_CLIENT = "Client";
    public static final String CAT_CODE = "Code";
    public static final String CAT_COLLATION = "Collation";
    public static final String CAT_COMPILER = "Compiler";
    public static final String CAT_DATETIME = "Date & Time";
    public static final String CAT_OWNER = "Owner";
    public static final String CAT_SOURCE = "Source";
    public static final String CAT_PERFORMANCE = "Performance";
    public static final String CAT_STATS = "Statistics";
    public static final String CAT_TABLESPACE = "Tablespace";

    // Schema for system datatypes
    public static final String SYSTEM_DATATYPE_SCHEMA = "SYSIBM";

    // Keywords

    public static final String[] ADVANCED_KEYWORDS = { "ALIAS", "ALLOW", "APPLICATION", "ASSOCIATE", "ASUTIME", "AUDIT",
        "AUTONOMOUS", "AUX", "AUXILIARY", "BEFORE", "BINARY", "BUFFERPOOL", "CACHE", "CALL", "CALLED", "CAPTURE", "CARDINALITY",
        "CCSID", "CLUSTER", "COLLECTION", "COLLID", "COMMENT", "COMPRESS", "CONCAT", "CONDITION", "CONTAINS", "COUNT_BIG",
        "CURRENT_LC_CTYPE", "CURRENT_PATH", "CURRENT_SERVER", "CURRENT_TIMEZONE", "CYCLE", "DATA", "DATABASE", "DAY", "DAYS",
        "DB2GENERAL", "DB2GENRL", "DB2SQL", "DBINFO", "DEFAULTS", "DEFINITION", "DETERMINISTIC", "DISALLOW", "DO", "DSNHATTR",
        "DSSIZE", "DYNAMIC", "EACH", "EDITPROC", "ELSEIF", "ENCODING", "END-EXEC1", "ERASE", "EXCLUDING", "EXIT", "FENCED",
        "FIELDPROC", "FILE", "FINAL", "FREE", "FUNCTION", "GENERAL", "GENERATED", "GRAPHIC", "HANDLER", "HOLD", "HOUR", "HOURS",
        "IF", "INCLUDING", "INCREMENT", "INDEX", "INHERIT", "INOUT", "INTEGRITY", "ISOBID", "ITERATE", "JAR", "JAVA", "LABEL",
        "LC_CTYPE", "LEAVE", "LINKTYPE", "LOCALE", "LOCATOR", "LOCATORS", "LOCK", "LOCKMAX", "LOCKSIZE", "LONG", "LOOP",
        "MAXVALUE", "MICROSECOND", "MICROSECONDS", "MINUTE", "MINUTES", "MINVALUE", "MODE", "MODIFIES", "MONTH", "MONTHS", "NEW",
        "NEW_TABLE", "NOCACHE", "NOCYCLE", "NODENAME", "NODENUMBER", "NOMAXVALUE", "NOMINVALUE", "NOORDER", "NULLS", "NUMPARTS",
        "OBID", "OLD", "OLD_TABLE", "OPTIMIZATION", "OPTIMIZE", "ORGANIZE", "OUT", "OVERRIDING", "PACKAGE", "PARAMETER", "PART",
        "PARTITION", "PATH", "PIECESIZE", "PLAN", "PRIQTY", "PROGRAM", "PSID", "QUERYNO", "READS", "RECOVERY", "REFERENCING",
        "RELEASE", "RENAME", "REPEAT", "RESET", "RESIGNAL", "RESTART", "RESULT", "RESULT_SET_LOCATOR", "RETURN", "RETURNS",
        "ROUTINE", "ROW", "RRN", "RUN", "SAVEPOINT", "SCRATCHPAD", "SECOND", "SECONDS", "SECQTY", "SECURITY", "SENSITIVE",
        "SIGNAL", "SIMPLE", "SOURCE", "SPECIFIC", "SQLID", "STANDARD", "START", "STATIC", "STAY", "STOGROUP", "STORES", "STYLE",
        "SUBPAGES", "SYNONYM", "SYSPROC", "SYSTEM", "TABLESPACE", "TRIGGER", "TYPE", "UNDO", "UNTIL", "VALIDPROC", "VARIABLE",
        "VARIANT", "VCAT", "VOLATILE", "VOLUMES", "WHILE", "WLM", "YEAR", "YEARS", };

    public static final DBDPseudoAttribute PSEUDO_ATTR_RID_BIT = new DBDPseudoAttribute(DBDPseudoAttributeType.ROWID, "RID_BIT()",
        "RID_BIT($alias)", "rid_bit", "Unique physical row identifier");

}
