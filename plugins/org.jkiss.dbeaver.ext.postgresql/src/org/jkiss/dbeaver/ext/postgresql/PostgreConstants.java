/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
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

    public static final String PROP_SHOW_NON_DEFAULT_DB = DBConstants.INTERNAL_PROP_PREFIX + "show-non-default-db@";

    public static final String PROP_SSL_CLIENT_CERT = "clientCert";
    public static final String PROP_SSL_CLIENT_KEY = "clientKey";
    public static final String PROP_SSL_ROOT_CERT = "rootCert";
    public static final String PROP_SSL_MODE = "sslMode";
    public static final String PROP_SSL_FACTORY = "sslFactory";

    public static final DBSEntityType ENTITY_TYPE_SEQUENCE = new DBSEntityType("pg_sequence", "Sequence", DBIcon.TREE_SEQUENCE, true);
    public static final DBSObjectState STATE_UNAVAILABLE = new DBSObjectState("Unavailable", DBIcon.OVER_EXTERNAL);
    public static final DBSEntityConstraintType CONSTRAINT_TRIGGER = new DBSEntityConstraintType("trigger", "TRIGGER", "Trigger constraint", false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType CONSTRAINT_EXCLUSIVE = new DBSEntityConstraintType("exclusive", "EXCLUSIVE", "Exclusive constraint", false, false); //$NON-NLS-1$

    public static final String INFO_SCHEMA_NAME = "information_schema";
    public static final String CATALOG_SCHEMA_NAME = "pg_catalog";
    public static final String TOAST_SCHEMA_PREFIX = "pg_toast";
    public static final String TEMP_SCHEMA_PREFIX = "pg_temp";
    public static final String PUBLIC_SCHEMA_NAME = "public";

    public static final String PG_OBJECT_CLASS = "org.postgresql.util.PGobject";

    public static final DBDPseudoAttribute PSEUDO_ATTR_OID = new DBDPseudoAttribute(DBDPseudoAttributeType.ROWID, "oid",
        "oid", "oid", "Row identifier", false);

    public static final String TYPE_HSTORE = "hstore";
    public static final String HANDLER_SSL = "postgre_ssl";

    public static final String EC_PERMISSION_DENIED = "42501";

    public static final String PG_INSTALL_REG_KEY = "SOFTWARE\\PostgreSQL\\Installations";
    public static final String PG_INSTALL_PROP_BASE_DIRECTORY = "Base Directory";
    public static final String PG_INSTALL_PROP_VERSION = "Version";
    public static final String PG_INSTALL_PROP_BRANDING = "Branding";
    public static final String PG_INSTALL_PROP_DATA_DIRECTORY = "Data Directory";

}
