/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    public static final DBSObjectState STATE_UNAVAILABLE = new DBSObjectState("Unavailable", DBIcon.OVER_EXTERNAL);
    public static final DBSEntityConstraintType CONSTRAINT_TRIGGER = new DBSEntityConstraintType("trigger", "TRIGGER", "Trigger constraint", false, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType CONSTRAINT_EXCLUSIVE = new DBSEntityConstraintType("exclusive", "EXCLUSIVE", "Exclusive constraint", false, false, false); //$NON-NLS-1$

    public static final String INFO_SCHEMA_NAME = "information_schema";
    public static final String CATALOG_SCHEMA_NAME = "pg_catalog";
    public static final String TEMP_SCHEMA_NAME = "pg_temp";
    public static final String TOAST_SCHEMA_PREFIX = "pg_toast";
    public static final String TEMP_SCHEMA_PREFIX = "pg_temp_";
    public static final String PUBLIC_SCHEMA_NAME = "public";

    public static final String PG_OBJECT_CLASS = "org.postgresql.util.PGobject";
    public static final String PG_ARRAY_CLASS = "org.postgresql.jdbc.PgArray";

    public static final DBDPseudoAttribute PSEUDO_ATTR_OID = new DBDPseudoAttribute(DBDPseudoAttributeType.ROWID, "oid",
        "oid", "oid", "Row identifier", false);

    public static final String TYPE_VARCHAR = "varchar";
    public static final String TYPE_HSTORE = "hstore";
    public static final String TYPE_JSON = "json";
    public static final String TYPE_JSONB = "jsonb";
    public static final String TYPE_BIT = "bit";
    public static final String TYPE_REFCURSOR = "refcursor";
    public static final String TYPE_MONEY = "money";

    public static final String HANDLER_SSL = "postgre_ssl";

    public static final String EC_PERMISSION_DENIED = "42501";

    public static final String PG_INSTALL_REG_KEY = "SOFTWARE\\PostgreSQL\\Installations";
    public static final String PG_INSTALL_PROP_BASE_DIRECTORY = "Base Directory";
    public static final String PG_INSTALL_PROP_VERSION = "Version";
    public static final String PG_INSTALL_PROP_BRANDING = "Branding";
    public static final String PG_INSTALL_PROP_DATA_DIRECTORY = "Data Directory";
    public static final String BIN_FOLDER = "bin";

    public static Map<String, String> SERIAL_TYPES = new HashMap<>();
    public static Map<String, String> DATA_TYPE_ALIASES = new HashMap<>();

    public static final String TYPE_INT2 = "int2";
    public static final String TYPE_INT4 = "int4";
    public static final String TYPE_INT8 = "int8";

    static {
        DATA_TYPE_ALIASES.put("integer", TYPE_INT4);
        DATA_TYPE_ALIASES.put("int", TYPE_INT4);
        DATA_TYPE_ALIASES.put("bigint", TYPE_INT8);
        DATA_TYPE_ALIASES.put("bigserial", TYPE_INT8);
        DATA_TYPE_ALIASES.put("smallint", TYPE_INT2);

        DATA_TYPE_ALIASES.put("double precision", "float8");
        DATA_TYPE_ALIASES.put("real", "float4");

        SERIAL_TYPES.put("serial", TYPE_INT4);
        SERIAL_TYPES.put("serial8", TYPE_INT8);
        SERIAL_TYPES.put("serial2", TYPE_INT2);
        SERIAL_TYPES.put("smallserial", TYPE_INT2);
        SERIAL_TYPES.put("bigserial", TYPE_INT8);
    }
}
