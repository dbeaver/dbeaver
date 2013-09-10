/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.ext.db2;

import org.jkiss.dbeaver.model.DBConstants;

/**
 * DB2 constants
 */
public class DB2Constants {

    // Display Categories
    public static final int DEFAULT_PORT = 50000;

   // Display Categories
   public static final String   CAT_DATETIME            = "Date & Time";
   public static final String   CAT_OWNER               = "Owner";
   public static final String   CAT_STATS               = "Statistiques";
   public static final String   CAT_TABLESPACE          = "Tablespace";
   public static final String   CAT_AUDIT               = "Audit";
   public static final String   CAT_METRICS             = "Performance";
   public static final String   CAT_COLLATION           = "Collation";
   public static final String   CAT_BASEBJECT           = "Base Object";
   public static final String   CAT_COMPILER            = "Compiler";

   // ------------------
   // TODO DF Sortout those remaining consts..
   // --------------------------
   public static final String   CMD_COMPILE             = "org.jkiss.dbeaver.ext.db2.code.compile";             //$NON-NLS-1$

   public static final String   PROP_CONNECTION_TYPE    = DBConstants.INTERNAL_PROP_PREFIX + "connection-type@";

   public static final String   TYPE_NAME_XML           = "XMLTYPE";
   public static final String   TYPE_FQ_XML             = "SYS.XMLTYPE";

   public static final String   PROP_SOURCE_DEFINITION  = "sourceDefinition";
   public static final String   PROP_SOURCE_DECLARATION = "sourceDeclaration";

   public static final String   COL_OWNER               = "OWNER";
   public static final String   COL_TABLE_NAME          = "TABLE_NAME";
   public static final String   COL_CONSTRAINT_NAME     = "CONSTRAINT_NAME";
   public static final String   COL_CONSTRAINT_TYPE     = "CONSTRAINT_TYPE";

   public static final String[] ADVANCED_KEYWORDS       = { "PACKAGE", "FUNCTION", "TYPE", "TRIGGER", "MATERIALIZED", "IF", "EACH",
            "RETURN", "WRAPPED"                        };

   public static final String   PREF_KEY_DDL_FORMAT     = "db2.ddl.format";

}
