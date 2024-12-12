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

package org.jkiss.dbeaver.ext.mysql;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

/**
 * MySQL constants
 */
public class MySQLConstants {
    public static final int DEFAULT_PORT = 3306;
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_USER = "root";

    public static final String DRIVER_ID_MYSQL = "mysql5";
    public static final String DRIVER_ID_MYSQL8 = "mysql8";
    public static final String DRIVER_ID_MARIA_DB = "mariaDB";
    public static final String DRIVER_CLASS_MARIA_DB = "org.mariadb.jdbc.Driver";
    public static final String DRIVER_PARAM_CLIENTS = "supportsClients";

    public static final String HANDLER_SSL = "mysql_ssl";

    public static final String PROP_SERVER_TIMEZONE = DBConstants.INTERNAL_PROP_PREFIX + "serverTimezone@";

    public static final String PROP_ZERO_DATETIME_BEHAVIOR = "zeroDateTimeBehavior";
    public static final String PROP_REQUIRE_SSL = "ssl.require";
    public static final String PROP_VERIFY_SERVER_SERT = "ssl.verify.server";
    public static final String PROP_SSL_CIPHER_SUITES = "ssl.cipher.suites";
    public static final String PROP_SSL_PUBLIC_KEY_RETRIEVE = "ssl.public.key.retrieve";
    /** @deprecated Use {@link SSLHandlerTrustStoreImpl#PROP_SSL_CLIENT_CERT} instead */
    @Deprecated
    public static final String PROP_SSL_CLIENT_CERT = "ssl.client.cert";
    /** @deprecated Use {@link SSLHandlerTrustStoreImpl#PROP_SSL_CLIENT_KEY} instead */
    @Deprecated
    public static final String PROP_SSL_CLIENT_KEY = "ssl.client.key";
    /** @deprecated Use {@link SSLHandlerTrustStoreImpl#PROP_SSL_CA_CERT} instead */
    @Deprecated
    public static final String PROP_SSL_CA_CERT = "ssl.ca.cert";
    public static final String PROP_SSL_DEBUG = "ssl.debug";

    public static final String PROP_CACHE_META_DATA = "cache-meta-data";

    public static final String[] TABLE_TYPES = new String[]{"TABLE", "VIEW", "LOCAL TEMPORARY"};

    public static final String INFO_SCHEMA_NAME = "information_schema";
    public static final String PERFORMANCE_SCHEMA_NAME = "performance_schema";
    public static final String MYSQL_SCHEMA_NAME = "mysql";

    public static final String META_TABLE_ENGINES = INFO_SCHEMA_NAME + ".ENGINES";
    public static final String META_TABLE_SCHEMATA = INFO_SCHEMA_NAME + ".SCHEMATA";
    public static final String META_TABLE_TABLES = INFO_SCHEMA_NAME + ".TABLES";
    public static final String META_TABLE_ROUTINES = INFO_SCHEMA_NAME + ".ROUTINES";
    public static final String META_TABLE_TRIGGERS = INFO_SCHEMA_NAME + ".TRIGGERS";
    public static final String META_TABLE_COLUMNS = INFO_SCHEMA_NAME + ".COLUMNS";
    public static final String META_TABLE_TABLE_CONSTRAINTS = INFO_SCHEMA_NAME + ".TABLE_CONSTRAINTS";
    public static final String META_TABLE_KEY_COLUMN_USAGE = INFO_SCHEMA_NAME + ".KEY_COLUMN_USAGE";
    public static final String META_TABLE_STATISTICS = INFO_SCHEMA_NAME + ".STATISTICS";
    public static final String META_TABLE_PARTITIONS = INFO_SCHEMA_NAME + ".PARTITIONS";
    public static final String META_TABLE_VIEWS = INFO_SCHEMA_NAME + ".VIEWS";

    public static final String COL_ENGINE_NAME = "ENGINE";
    public static final String COL_ENGINE_SUPPORT = "SUPPORT";
    public static final String COL_ENGINE_DESCRIPTION = "COMMENT";
    public static final String COL_ENGINE_SUPPORT_TXN = "TRANSACTIONS";
    public static final String COL_ENGINE_SUPPORT_XA = "XA";
    public static final String COL_ENGINE_SUPPORT_SAVEPOINTS = "SAVEPOINTS";

    public static final String COL_CATALOG_NAME = "CATALOG_NAME";
    public static final String COL_DATABASE_NAME = "`Database`";
    public static final String COL_SCHEMA_NAME = "SCHEMA_NAME";
    public static final String COL_DEFAULT_CHARACTER_SET_NAME = "DEFAULT_CHARACTER_SET_NAME";
    public static final String COL_DEFAULT_COLLATION_NAME = "DEFAULT_COLLATION_NAME";
    public static final String COL_SQL_PATH = "SQL_PATH";

    public static final String COL_TABLE_SCHEMA = "TABLE_SCHEMA";
    public static final String COL_TABLE_NAME = "TABLE_NAME";
    public static final String COL_TABLE_TYPE = "TABLE_TYPE";
    public static final String COL_ENGINE = "ENGINE";
    public static final String COL_VERSION = "VERSION";
    public static final String COL_ROWS = "ROWS";
    public static final String COL_TABLE_ROWS = "TABLE_ROWS";
    public static final String COL_AUTO_INCREMENT = "AUTO_INCREMENT";
    public static final String COL_TABLE_COMMENT = "COMMENT";
    public static final String COL_COLUMNS_NAME = "COLUMNS_NAME";
    public static final String COL_ORDINAL_POSITION = "ORDINAL_POSITION";
    public static final String COL_CREATE_TIME = "CREATE_TIME";
    public static final String COL_UPDATE_TIME = "UPDATE_TIME";
    public static final String COL_CHECK_TIME = "CHECK_TIME";
    public static final String COL_COLLATION = "COLLATION";
    public static final String COL_COLLATION_NAME = "COLLATION_NAME";
    public static final String COL_NULLABLE = "NULLABLE";
    public static final String COL_SUB_PART = "SUB_PART";
    public static final String COL_AVG_ROW_LENGTH = "AVG_ROW_LENGTH";
    public static final String COL_DATA_LENGTH = "DATA_LENGTH";
    public static final String COL_INDEX_NAME = "INDEX_NAME";
    public static final String COL_INDEX_TYPE = "INDEX_TYPE";
    public static final String COL_SEQ_IN_INDEX = "SEQ_IN_INDEX";
    public static final String COL_NON_UNIQUE = "NON_UNIQUE";
    public static final String COL_COMMENT = "COMMENT";
    public static final String COL_CHECK_CLAUSE = "CHECK_CLAUSE";
    
    public static final String COL_COLUMN_NAME = "COLUMN_NAME";
    public static final String COL_COLUMN_KEY = "COLUMN_KEY";
    public static final String COL_DATA_TYPE = "DATA_TYPE";
    public static final String COL_CHARACTER_MAXIMUM_LENGTH = "CHARACTER_MAXIMUM_LENGTH";
    public static final String COL_CHARACTER_OCTET_LENGTH = "CHARACTER_OCTET_LENGTH";
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
    public static final String COL_COLUMN_GENERATION_EXPRESSION = "GENERATION_EXPRESSION"; //$NON-NLS-1$
    public static final String COL_EXTERNAL_NAME = "EXTERNAL_NAME";
    public static final String COL_EXTERNAL_LANGUAGE = "EXTERNAL_LANGUAGE";
    public static final String COL_PARAMETER_STYLE = "PARAMETER_STYLE";
    public static final String COL_IS_DETERMINISTIC = "IS_DETERMINISTIC";
    public static final String COL_SQL_DATA_ACCESS = "SQL_DATA_ACCESS";
    public static final String COL_SECURITY_TYPE = "SECURITY_TYPE";
    public static final String COL_ROUTINE_COMMENT = "ROUTINE_COMMENT";
    public static final String COL_DEFINER = "DEFINER";
    public static final String COL_CHARACTER_SET_CLIENT = "CHARACTER_SET_CLIENT";
    public static final String COL_SRS_ID = "SRS_ID";

    public static final String COL_TRIGGER_SCHEMA = "TRIGGER_SCHEMA";
    public static final String COL_TRIGGER_NAME = "TRIGGER_NAME";
    public static final String COL_TRIGGER_EVENT_MANIPULATION = "EVENT_MANIPULATION";
    public static final String COL_TRIGGER_EVENT_OBJECT_SCHEMA = "EVENT_OBJECT_SCHEMA";
    public static final String COL_TRIGGER_EVENT_OBJECT_TABLE = "EVENT_OBJECT_TABLE";
    public static final String COL_TRIGGER_ACTION_ORDER = "ACTION_ORDER";
    public static final String COL_TRIGGER_ACTION_CONDITION = "ACTION_CONDITION";
    public static final String COL_TRIGGER_ACTION_STATEMENT = "ACTION_STATEMENT";
    public static final String COL_TRIGGER_ACTION_ORIENTATION = "ACTION_ORIENTATION";
    public static final String COL_TRIGGER_ACTION_TIMING = "ACTION_TIMING";
    public static final String COL_TRIGGER_SQL_MODE = "SQL_MODE";
    public static final String COL_TRIGGER_DEFINER = "DEFINER";
    public static final String COL_TRIGGER_CHARACTER_SET_CLIENT = "CHARACTER_SET_CLIENT";
    public static final String COL_TRIGGER_COLLATION_CONNECTION = "COLLATION_CONNECTION";
    public static final String COL_TRIGGER_DATABASE_COLLATION = "DATABASE_COLLATION";
    
    public static final String COL_CONSTRAINT_NAME = "CONSTRAINT_NAME";
    public static final String COL_CONSTRAINT_TYPE = "CONSTRAINT_TYPE";

    public static final String CONSTRAINT_FOREIGN_KEY = "FOREIGN KEY";
    public static final String CONSTRAINT_PRIMARY_KEY_NAME = "PRIMARY";
    public static final String CONSTRAINT_UNIQUE = "UNIQUE KEY";
    public static final String CONSTRAINT_CHECK = "CHECK";

    public static final String INDEX_PRIMARY = "PRIMARY";

    public static final String EXTRA_AUTO_INCREMENT = "auto_increment";

    public static final String TYPE_NAME_ENUM = "enum";
    public static final String TYPE_NAME_SET = "set";

    public static final DBSIndexType INDEX_TYPE_BTREE = new DBSIndexType("BTREE", "BTree");
    public static final DBSIndexType INDEX_TYPE_FULLTEXT = new DBSIndexType("FULLTEXT", "Full Text");
    public static final DBSIndexType INDEX_TYPE_HASH = new DBSIndexType("HASH", "Hash");
    public static final DBSIndexType INDEX_TYPE_RTREE = new DBSIndexType("RTREE", "RTree");

    public static final String COL_CHARSET = "CHARSET";
    public static final String COL_DESCRIPTION = "DESCRIPTION";
    public static final String COL_MAX_LEN = "MAXLEN";
    public static final String COL_ID = "ID";
    public static final String COL_DEFAULT = "DEFAULT";
    public static final String COL_COMPILED = "COMPILED";
    public static final String COL_SORT_LENGTH = "SORTLEN";

    public static final String COL_PARTITION_NAME = "PARTITION_NAME";
    public static final String COL_SUBPARTITION_NAME = "SUBPARTITION_NAME";
    public static final String COL_PARTITION_ORDINAL_POSITION = "PARTITION_ORDINAL_POSITION";
    public static final String COL_SUBPARTITION_ORDINAL_POSITION = "SUBPARTITION_ORDINAL_POSITION";
    public static final String COL_PARTITION_METHOD = "PARTITION_METHOD";
    public static final String COL_SUBPARTITION_METHOD = "SUBPARTITION_METHOD";
    public static final String COL_PARTITION_EXPRESSION = "PARTITION_EXPRESSION";
    public static final String COL_SUBPARTITION_EXPRESSION = "SUBPARTITION_EXPRESSION";
    public static final String COL_PARTITION_DESCRIPTION = "PARTITION_DESCRIPTION";
    public static final String COL_PARTITION_COMMENT = "PARTITION_COMMENT";

    public static final String COL_MAX_DATA_LENGTH = "MAX_DATA_LENGTH";
    public static final String COL_INDEX_LENGTH = "INDEX_LENGTH";
    public static final String COL_NODEGROUP = "NODEGROUP";
    public static final String COL_DATA_FREE = "DATA_FREE";
    public static final String COL_CHECKSUM = "CHECKSUM";
    public static final String COL_CHECK_OPTION = "CHECK_OPTION";
    public static final String COL_VIEW_DEFINITION = "VIEW_DEFINITION";

    public static final String TYPE_VARCHAR = "varchar";
    public static final String TYPE_JSON = "json";
    public static final String TYPE_GEOMETRY = "geometry";
    public static final String TYPE_POINT = "point";
    public static final String TYPE_LINESTRING = "linestring";
    public static final String TYPE_POLYGON = "polygon";
    public static final String TYPE_MULTIPOINT = "multipoint";
    public static final String TYPE_MULTILINESTRING = "multilinestring";
    public static final String TYPE_MULTIPOLYGON = "multipolygon";
    public static final String TYPE_GEOMETRYCOLLECTION = "geometrycollection";
    public static final String TYPE_GEOGRAPHY = "geography";
    public static final String TYPE_GEOGRAPHYPOINT = "geographypoint";

    public static final String TYPE_YEAR = "year";
    public static final String TYPE_ENUM = "enum";
    public static final String TYPE_SET = "set";
    public static final String TYPE_VARBINARY = "VARBINARY";
    public static final String TYPE_UUID = "uuid";

    public static final String BIN_FOLDER = "bin";
    public static final String ENV_VAR_MYSQL_PWD = "MYSQL_PWD";

    public static final String FLAG_VERSION = "-V";

    public static final String EXTRA_INFO_VIRTUAL_GENERATED = "VIRTUAL GENERATED";
    public static final String EXTRA_INFO_DEFAULT_GENERATED = "DEFAULT_GENERATED";

    public static final String PRIVILEGE_GRANT_OPTION_NAME = "Grant option";

    // https://dev.mysql.com/doc/mysql-errors/8.4/en/server-error-reference.html
    public static final int ER_MUST_CHANGE_PASSWORD_LOGIN = 1862;
}
