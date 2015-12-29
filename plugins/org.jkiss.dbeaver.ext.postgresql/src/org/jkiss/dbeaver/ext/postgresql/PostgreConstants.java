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
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

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
    public static final String META_TABLE_COLUMNS = INFO_SCHEMA_NAME + ".COLUMNS";
    public static final String META_TABLE_STATISTICS = INFO_SCHEMA_NAME + ".STATISTICS";
    public static final String META_TABLE_VIEWS = INFO_SCHEMA_NAME + ".VIEWS";

    public static final String COL_SCHEMA_NAME = "SCHEMA_NAME";

    public static final String COL_TABLE_SCHEMA = "TABLE_SCHEMA";
    public static final String COL_TABLE_NAME = "TABLE_NAME";
    public static final String COL_TABLE_ROWS = "ROWS";
    public static final String COL_AUTO_INCREMENT = "AUTO_INCREMENT";
    public static final String COL_TABLE_COMMENT = "COMMENT";
    public static final String COL_ORDINAL_POSITION = "ORDINAL_POSITION";
    public static final String COL_CREATE_TIME = "CREATE_TIME";
    public static final String COL_COLLATION = "COLLATION";
    public static final String COL_NULLABLE = "NULLABLE";
    public static final String COL_AVG_ROW_LENGTH = "AVG_ROW_LENGTH";
    public static final String COL_DATA_LENGTH = "DATA_LENGTH";
    public static final String COL_INDEX_NAME = "INDEX_NAME";
    public static final String COL_INDEX_TYPE = "INDEX_TYPE";
    public static final String COL_SEQ_IN_INDEX = "SEQ_IN_INDEX";
    public static final String COL_NON_UNIQUE = "NON_UNIQUE";
    public static final String COL_COMMENT = "COMMENT";

    public static final String COL_COLUMN_NAME = "COLUMN_NAME";
    public static final String COL_COLUMN_KEY = "COLUMN_KEY";
    public static final String COL_DATA_TYPE = "DATA_TYPE";
    public static final String COL_CHARACTER_MAXIMUM_LENGTH = "CHARACTER_MAXIMUM_LENGTH";
    public static final String COL_NUMERIC_PRECISION = "NUMERIC_PRECISION";
    public static final String COL_NUMERIC_SCALE = "NUMERIC_SCALE";
    public static final String COL_COLUMN_DEFAULT = "COLUMN_DEFAULT";
    public static final String COL_IS_NULLABLE = "IS_NULLABLE";
    public static final String COL_IS_UPDATABLE = "IS_UPDATABLE";
    public static final String COL_COLUMN_COMMENT = "COLUMN_COMMENT";
    public static final String COL_COLUMN_EXTRA = "EXTRA";
    public static final String COL_COLUMN_TYPE = "COLUMN_TYPE";

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

    public static final String COL_TRIGGER_SQL_MODE = "SQL_MODE";
    public static final String COL_TRIGGER_CHARACTER_SET_CLIENT = "CHARACTER_SET_CLIENT";

    public static final String COL_CONSTRAINT_NAME = "CONSTRAINT_NAME";

    public static final String EXTRA_AUTO_INCREMENT = "auto_increment";

    public static final String TYPE_NAME_ENUM = "enum";
    public static final String TYPE_NAME_SET = "set";

    public static final DBSIndexType INDEX_TYPE_BTREE = new DBSIndexType("BTREE", "BTree");
    public static final DBSIndexType INDEX_TYPE_FULLTEXT = new DBSIndexType("FULLTEXT", "Full Text");
    public static final DBSIndexType INDEX_TYPE_HASH = new DBSIndexType("HASH", "Hash");
    public static final DBSIndexType INDEX_TYPE_RTREE = new DBSIndexType("RTREE", "RTree");

    public static final String COL_CHECK_OPTION = "CHECK_OPTION";
    public static final String COL_VIEW_DEFINITION = "VIEW_DEFINITION";

}
