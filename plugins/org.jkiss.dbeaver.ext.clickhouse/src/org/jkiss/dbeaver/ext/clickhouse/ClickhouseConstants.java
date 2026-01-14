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
package org.jkiss.dbeaver.ext.clickhouse;

public class ClickhouseConstants {
    public static final String SSL_PARAM = "ssl"; //$NON-NLS-1$
    public static final String SSL_PATH = "sslcert"; //$NON-NLS-1$
    public static final String SSL_KEY_PASSWORD = "sslkey"; //$NON-NLS-1$
    public static final String SSL_MODE_CONF = "ssl.mode"; //$NON-NLS-1$
    public static final String SSL_MODE = "sslmode"; //$NON-NLS-1$

    public static final String PROP_USE_SERVER_TIME_ZONE = "use_server_time_zone"; //$NON-NLS-1$
    public static final String PROP_USE_TIME_ZONE = "use_time_zone"; //$NON-NLS-1$

    public static final String SSL_ROOT_CERTIFICATE = "sslrootcert"; //$NON-NLS-1$

    public static final String DATA_TYPE_IPV4 = "ipv4";
    public static final String DATA_TYPE_IPV6 = "ipv6";
    public static final String DATA_TYPE_STRING = "String";
    public static final String DATA_TYPE_ARRAY = "Array";
    public static final String DATA_TYPE_TUPLE = "Tuple";
    public static final String DATA_TYPE_MAP = "Map";
    public static final String CLICKHOUSE_SETTING_SESSION_ID = "clickhouse_setting_session_id";
    public static final String CLICKHOUSE_SETTING_SESSION_TIMEOUT = "clickhouse_setting_session_timeout";
    public static final String DRIVER_GET_LAST_QUERY_METHOD = "getLastQueryId";
    public static final String SESSION_BUSY_ERROR_CODE_MESSAGE = "Code: 373";

}
