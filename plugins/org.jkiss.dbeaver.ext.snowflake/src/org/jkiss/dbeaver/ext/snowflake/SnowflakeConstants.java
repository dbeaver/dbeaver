/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.snowflake;

import org.jkiss.dbeaver.model.DBConstants;

/**
 * Snowflake constants
 */
public class SnowflakeConstants
{
    public static final String PROP_WAREHOUSE = "warehouse";
    public static final String PROP_SCHEMA = DBConstants.INTERNAL_PROP_PREFIX + "schema@";
    public static final String PROP_SCHEMA2 = "schema";
    public static final String PROP_DD_STRING = "ddString";

    public static final String PROP_AUTH_ROLE = "role";
    public static final String PROP_AUTHENTICATOR = "authenticator";

    public static final String DEFAULT_HOST_PREFIX = ".snowflakecomputing.com";
    public static final String DEFAULT_DB_NAME = "TEST_DB";

    public static final String PROP_ROLE_LEGACY = DBConstants.INTERNAL_PROP_PREFIX + "role@";
    public static final String PROP_AUTHENTICATOR_LEGACY = DBConstants.INTERNAL_PROP_PREFIX + "authenticator@";

    public static final String METADATA_COLUMN_CREATED = "CREATED";
    public static final String METADATA_COLUMN_LAST_ALTERED = "LAST_ALTERED";
    public static final String METADATA_COLUMN_COMMENT = "COMMENT";

    public static final String TYPE_NUMBER = "NUMBER";
    public static final String TYPE_NUMERIC = "NUMERIC";
    public static final String TYPE_DECIMAL = "DECIMAL";
    public static final String TYPE_DOUBLE_PRECISION = "DOUBLE PRECISION";
    public static final String TYPE_REAL = "REAL";
    public static final String TYPE_INTEGER = "INTEGER";

    public static final int NUMERIC_MAX_PRECISION = 38;
    public static final int NUMERIC_MAX_SCALE = 35;

}
