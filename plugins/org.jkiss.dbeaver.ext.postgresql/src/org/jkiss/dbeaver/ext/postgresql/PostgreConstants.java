/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PostgreConstants
 */
public class PostgreConstants {

    public static final int DEFAULT_PORT = 5432;
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_DATABASE = "postgres";
    public static final String DEFAULT_DATA_TYPE = "varchar";
    public static final String DEFAULT_USER = "postgres";
    public static final String USER_VARIABLE = "$user";

    public static final String PROP_CHOSEN_ROLE = DBConstants.INTERNAL_PROP_PREFIX + "chosen-role@";
    public static final String PROP_SHOW_NON_DEFAULT_DB = DBConstants.INTERNAL_PROP_PREFIX + "show-non-default-db@";
    public static final String PROP_SHOW_UNAVAILABLE_DB = DBConstants.INTERNAL_PROP_PREFIX + "show-unavailable-db@";
    public static final String PROP_SHOW_TEMPLATES_DB = DBConstants.INTERNAL_PROP_PREFIX + "show-template-db@";
    public static final String PROP_READ_ALL_DATA_TYPES = DBConstants.INTERNAL_PROP_PREFIX + "read-all-data-types-db@";
    public static final String PROP_READ_KEYS_WITH_COLUMNS = "read-keys-with-columns";
    public static final String PROP_USE_PREPARED_STATEMENTS = DBConstants.INTERNAL_PROP_PREFIX + "use-prepared-statements-db@";
    public static final String PROP_DD_PLAIN_STRING = "postgresql.dd.plain.string";
    public static final String PROP_DD_TAG_STRING = "postgresql.dd.tag.string";
    public static final String PROP_SHOW_DATABASE_STATISTICS = "show-database-statistics";

    public static final String PROP_SSL = "ssl";

    /** @deprecated Use {@link SSLHandlerTrustStoreImpl#PROP_SSL_CLIENT_CERT} instead */
    @Deprecated
    public static final String PROP_SSL_CLIENT_CERT = "clientCert";
    /** @deprecated Use {@link SSLHandlerTrustStoreImpl#PROP_SSL_CLIENT_KEY} instead */
    @Deprecated
    public static final String PROP_SSL_CLIENT_KEY = "clientKey";
    /** @deprecated Use {@link SSLHandlerTrustStoreImpl#PROP_SSL_CA_CERT} instead */
    @Deprecated
    public static final String PROP_SSL_ROOT_CERT = "rootCert";
    public static final String PROP_SSL_MODE = "sslMode";
    public static final String PROP_SSL_FACTORY = "sslFactory";
    public static final String PROP_SSL_PROXY = "sslProxyServer";
    public static final String PROP_SERVER_TYPE = "serverType";

    public static final DBSObjectState STATE_UNAVAILABLE = new DBSObjectState("Unavailable", DBIcon.OVER_EXTERNAL);
    public static final DBSEntityConstraintType CONSTRAINT_TRIGGER = new DBSEntityConstraintType("trigger", "TRIGGER", "Trigger constraint", false, false, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType CONSTRAINT_EXCLUSIVE = new DBSEntityConstraintType("exclusive", "EXCLUSIVE", "Exclusive constraint", false, false, false, false); //$NON-NLS-1$

    public static final String INFO_SCHEMA_NAME = "information_schema";
    public static final String SYSTEM_SCHEMA_PREFIX = "pg_";
    public static final String CATALOG_SCHEMA_NAME = "pg_catalog";
    public static final String TEMP_SCHEMA_NAME = "pg_temp";
    public static final String TOAST_SCHEMA_PREFIX = "pg_toast";
    public static final String TEMP_SCHEMA_PREFIX = "pg_temp_";
    public static final String PUBLIC_SCHEMA_NAME = "public";

    // Settings names from 'pg_options' view
    public static final String OPTION_CLIENT_MIN_MESSAGES = "client_min_messages";
    public static final String OPTION_STANDARD_CONFORMING_STRINGS = "standard_conforming_strings";

    public static final String PG_OBJECT_CLASS = "org.postgresql.util.PGobject";
    public static final String PG_ARRAY_CLASS = "org.postgresql.jdbc.PgArray";
    public static final String PG_INTERVAL_CLASS = "org.postgresql.util.PGInterval";
    public static final String PG_GEOMETRY_CLASS = "org.postgis.PGgeometry";

    // Workaround for Redshift 2.x
    public static final String RS_OBJECT_CLASS = "com.amazon.redshift.util.RedshiftObject";
    // Workaround for EnterpriseDB
    public static final String EDB_OBJECT_CLASS = "com.edb.util.PGobject";

    public static final DBDPseudoAttribute PSEUDO_ATTR_OID = new DBDPseudoAttribute(DBDPseudoAttributeType.ROWID, "oid",
        "oid", "oid", "Row identifier", false);

    public static final String TYPE_CHAR = "char";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_UUID = "uuid";
    public static final String TYPE_BPCHAR = "bpchar";
    public static final String TYPE_VARCHAR = "varchar";
    public static final String TYPE_HSTORE = "hstore";
    public static final String TYPE_JSON = "json";
    public static final String TYPE_JSONB = "jsonb";
    public static final String TYPE_BIT = "bit";
    public static final String TYPE_VARBIT = "varbit";
    public static final String TYPE_REFCURSOR = "refcursor";
    public static final String TYPE_MONEY = "money";
    public static final String TYPE_GEOMETRY = "geometry";
    public static final String TYPE_GEOGRAPHY = "geography";
    public static final String TYPE_INTERVAL = "interval";
    public static final String TYPE_TIME = "time";
    public static final String TYPE_TIMESTAMP = "timestamp";
    public static final String TYPE_TIMETZ = "timetz";
    public static final String TYPE_TIMESTAMPTZ = "timestamptz";
    public static final String TYPE_XML = "xml";

    public static final String HANDLER_SSL = "postgre_ssl";

    /**
     * @see [https://www.postgresql.org/docs/9.2/static/errcodes-appendix.html]
     */
    public static final String EC_PERMISSION_DENIED = "42501"; //$NON-NLS-1$
    public static final String EC_QUERY_CANCELED = "57014"; //$NON-NLS-1$

    public static final String PG_INSTALL_REG_KEY = "SOFTWARE\\PostgreSQL\\Installations";
    public static final String PG_INSTALL_PROP_BASE_DIRECTORY = "Base Directory";
    public static final String PG_INSTALL_PROP_BRANDING = "Branding";
    public static final String BIN_FOLDER = "bin";

    public static final Map<String, String> SERIAL_TYPES = new LinkedHashMap<>();
    public static final Map<String, String> DATA_TYPE_ALIASES = new HashMap<>();
    public static final Map<String, String> DATA_TYPE_CANONICAL_NAMES = new HashMap<>();

    public static final String TYPE_BOOL = "bool";
    public static final String TYPE_INT2 = "int2";
    public static final String TYPE_INT4 = "int4";
    public static final String TYPE_INT8 = "int8";
    public static final String TYPE_BIGINT = "bigint";

    public static final String TYPE_FLOAT4 = "float4";
    public static final String TYPE_FLOAT8 = "float8";

    public static final String ERROR_ADMIN_SHUTDOWN = "57P01";
    public static final String ERROR_TRANSACTION_ABORTED = "25P02";

    public static final String PSQL_EXCEPTION_CLASS_NAME = "org.postgresql.util.PSQLException";
    public static final String COLLATION_DEFAULT = "default";
    public static final String DEFAULT_ARRAY_DELIMITER = " ";
    public static final String PG_PASS_HOSTNAME = "overriddenUsername";

    static {
        DATA_TYPE_ALIASES.put("boolean", TYPE_BOOL);
        DATA_TYPE_ALIASES.put("integer", TYPE_INT4);
        DATA_TYPE_ALIASES.put("int", TYPE_INT4);
        DATA_TYPE_ALIASES.put("bigint", TYPE_INT8);
        DATA_TYPE_ALIASES.put("smallint", TYPE_INT2);

        DATA_TYPE_ALIASES.put("character", TYPE_BPCHAR);
        DATA_TYPE_ALIASES.put("character varying", TYPE_VARCHAR);
        DATA_TYPE_ALIASES.put("char varying", TYPE_VARCHAR);
        DATA_TYPE_ALIASES.put("bit varying", TYPE_VARBIT);

        DATA_TYPE_ALIASES.put("double precision", TYPE_FLOAT8);
        DATA_TYPE_ALIASES.put("real", TYPE_FLOAT4);
        DATA_TYPE_ALIASES.put("decimal", "numeric");
        DATA_TYPE_ALIASES.put("void", "void");

        DATA_TYPE_ALIASES.put("time with time zone", TYPE_TIMETZ);
        DATA_TYPE_ALIASES.put("time without time zone", TYPE_TIME);
        DATA_TYPE_ALIASES.put("timestamp with time zone", TYPE_TIMESTAMPTZ);
        DATA_TYPE_ALIASES.put("timestamp without time zone", TYPE_TIMESTAMP);

        DATA_TYPE_ALIASES.put("interval", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval year", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval month", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval day", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval hour", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval minute", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval second", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval year to month", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval day to hour", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval day to minute", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval day to second", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval hour to minute", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval hour to second", TYPE_INTERVAL);
        DATA_TYPE_ALIASES.put("interval minute to second", TYPE_INTERVAL);

        SERIAL_TYPES.put("serial", TYPE_INT4);
        SERIAL_TYPES.put("serial8", TYPE_INT8);
        SERIAL_TYPES.put("serial4", TYPE_INT4);
        SERIAL_TYPES.put("serial2", TYPE_INT2);
        SERIAL_TYPES.put("smallserial", TYPE_INT2);
        SERIAL_TYPES.put("bigserial", TYPE_INT8);

        DATA_TYPE_CANONICAL_NAMES.put(TYPE_INT4, "integer");
        DATA_TYPE_CANONICAL_NAMES.put(TYPE_INT8, "bigint");
        DATA_TYPE_CANONICAL_NAMES.put(TYPE_INT2, "smallint");
        DATA_TYPE_CANONICAL_NAMES.put(TYPE_FLOAT4, "real");
        DATA_TYPE_CANONICAL_NAMES.put("character varying", "varchar");
    }


}
