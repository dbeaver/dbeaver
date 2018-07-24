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
package org.jkiss.dbeaver.ext.snowflake;

import org.jkiss.dbeaver.model.DBConstants;

/**
 * Snowflake constants
 */
public class SnowflakeConstants
{
    public static final String PROP_SCHEMA = DBConstants.INTERNAL_PROP_PREFIX + "schema@";
    public static final String PROP_ROLE = DBConstants.INTERNAL_PROP_PREFIX + "role@";

    public static final String DEFAULT_HOST_PREFIX = ".snowflakecomputing.com";
    public static final String DEFAULT_DB_NAME = "TEST_DB";
}
