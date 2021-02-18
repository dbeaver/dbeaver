/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.bigquery.model;

/**
 * Snowflake constants
 */
public class BigQueryConstants
{

    //public static final String PROP_OAUTH_TYPE = DBConstants.INTERNAL_PROP_PREFIX + "oauth-type@";
    //public static final String PROP_OAUTH_KEY_PATH = DBConstants.INTERNAL_PROP_PREFIX + "oauth-key-path@";

    public static final String DEFAULT_HOST_NAME = "https://www.googleapis.com/bigquery/v2";
    public static final int DEFAULT_PORT = 433;

    public static final String DRIVER_PROP_ADDITIONAL_PROJECTS = "AdditionalProjects";
    public static final String DRIVER_PROP_OAUTH_TYPE = "OAuthType";
    public static final String DRIVER_PROP_OAUTH_PVT_KEYPATH = "OAuthPvtKeyPath";

    public static final String DRIVER_PROP_ACCOUNT = "OAuthServiceAcctEmail";
    public static final String DRIVER_PROP_PROJECT_ID = "ProjectId";

}
