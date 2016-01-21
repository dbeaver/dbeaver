/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

/**
 * PostgreConstants
 */
public class PostgreConstants {

    public static final int DEFAULT_PORT = 5432;
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_DATABASE = "postgres";
    public static final String DEFAULT_DATA_TYPE = "varchar";

    public static final String PROP_USE_SSL = DBConstants.INTERNAL_PROP_PREFIX + "ssl@";
    public static final String PROP_SSL_CERT = DBConstants.INTERNAL_PROP_PREFIX + "ssl-cert@";

    public static final DBSEntityType ENTITY_TYPE_SEQUENCE = new DBSEntityType("pg_sequence", "Sequence", DBIcon.TREE_SEQUENCE, true);
    public static final DBSObjectState STATE_UNAVAILABLE = new DBSObjectState("Unavailable", DBIcon.OVER_EXTERNAL);
    public static final DBSEntityConstraintType CONSTRAINT_TRIGGER = new DBSEntityConstraintType("trigger", "TRIGGER", "Trigger constraint", false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType CONSTRAINT_EXCLUSIVE = new DBSEntityConstraintType("exclusive", "EXCLUSIVE", "Exclusive constraint", false, false); //$NON-NLS-1$

    public static final String INFO_SCHEMA_NAME = "information_schema";
    public static final String CATALOG_SCHEMA_NAME = "pg_catalog";

    public static final String META_TABLE_ROUTINES = INFO_SCHEMA_NAME + ".ROUTINES";

    public static final String COL_SCHEMA_NAME = "SCHEMA_NAME";

    public static final String COL_ROUTINE_SCHEMA = "ROUTINE_SCHEMA";
    public static final String COL_ROUTINE_NAME = "ROUTINE_NAME";
    public static final String COL_ROUTINE_TYPE = "ROUTINE_TYPE";
    public static final String COL_DTD_IDENTIFIER = "DTD_IDENTIFIER";
    public static final String COL_ROUTINE_BODY = "ROUTINE_BODY";
    public static final String COL_ROUTINE_DEFINITION = "ROUTINE_DEFINITION";
    public static final String COL_IS_DETERMINISTIC = "IS_DETERMINISTIC";
    public static final String COL_ROUTINE_COMMENT = "ROUTINE_COMMENT";
    public static final String COL_DEFINER = "DEFINER";
    public static final String COL_CHARACTER_SET_CLIENT = "CHARACTER_SET_CLIENT";

}
