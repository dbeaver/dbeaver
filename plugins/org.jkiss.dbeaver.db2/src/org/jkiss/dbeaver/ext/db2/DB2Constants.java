/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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

/**
 * DB2 constants
 *
 * @author Denis Forveille
 */
public class DB2Constants {

    // Connection properties
    public static final int DEFAULT_PORT = 50000;

    // Display Categories
    public static final String CAT_DATETIME = "Date & Time";
    public static final String CAT_OWNER = "Owner";
    public static final String CAT_STATS = "Statistics";
    public static final String CAT_TABLESPACE = "Tablespace";
    public static final String CAT_AUDIT = "Audit";
    public static final String CAT_PERFORMANCE = "Performance";
    public static final String CAT_COLLATION = "Collation";
    public static final String CAT_BASEBJECT = "Base Object";
    public static final String CAT_COMPILER = "Compiler";
    public static final String CAT_CLIENT = "Client";

    // ------------------
    // TODO DF Sortout those remaining consts..
    // --------------------------
    public static final String CMD_COMPILE = "org.jkiss.dbeaver.ext.db2.code.compile"; //$NON-NLS-1$
    //
    public static final String PROP_SOURCE_DEFINITION = "sourceDefinition";
    public static final String PROP_SOURCE_DECLARATION = "sourceDeclaration";

    public static final String[] ADVANCED_KEYWORDS = {
        "PACKAGE",
        "FUNCTION",
        "TYPE",
        "TRIGGER",
        "MATERIALIZED",
        "IF",
        "EACH",
        "RETURN",
        "WRAPPED"};

    public static final String PREF_KEY_DDL_FORMAT = "db2.ddl.format";

    public static final String PROP_TRACE_ENABLED = DBConstants.INTERNAL_PROP_PREFIX + "trace.enabled";
    public static final String PROP_TRACE_FOLDER = DBConstants.INTERNAL_PROP_PREFIX + "trace.folder";
    public static final String PROP_TRACE_FILE = DBConstants.INTERNAL_PROP_PREFIX + "trace.file";
    public static final String PROP_TRACE_APPEND = DBConstants.INTERNAL_PROP_PREFIX + "trace.append";
    public static final String PROP_TRACE_LEVEL = DBConstants.INTERNAL_PROP_PREFIX + "trace.level";

}
