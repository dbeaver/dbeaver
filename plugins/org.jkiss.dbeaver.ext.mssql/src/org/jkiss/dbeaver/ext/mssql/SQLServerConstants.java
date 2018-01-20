/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mssql;

import org.jkiss.dbeaver.model.DBConstants;

public class SQLServerConstants {

    public static final int DEFAULT_PORT = 1433;
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_DATABASE = "master";

    public static final String DRIVER_JTDS = "mssql_jdbc_jtds";
    public static final String DRIVER_MS = "mssql_jdbc_ms";

    public static final String PROP_CONNECTION_WINDOWS_AUTH = DBConstants.INTERNAL_PROP_PREFIX + "connection-windows-auth@";
    public static final String PROP_SHOW_ALL_SCHEMAS = DBConstants.INTERNAL_PROP_PREFIX + "show-all-schemas@";

    // https://support.microsoft.com/en-us/help/321185/how-to-determine-the-version--edition-and-update-level-of-sql-server-a
    public static final int SQL_SERVER_2016_VERSION_MAJOR = 13;
    public static final int SQL_SERVER_2008_VERSION_MAJOR = 10;
    public static final int SQL_SERVER_2005_VERSION_MAJOR = 9;
}
