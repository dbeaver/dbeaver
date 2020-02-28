/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

public class SQLServerMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mssql.SQLServerMessages"; //$NON-NLS-1$

    public static String authentication_sql_server_title;
    public static String authentication_sql_server_description;
    public static String authentication_windows_title;
    public static String authentication_windows_description;
    public static String authentication_ad_password_title;
    public static String authentication_ad_password_description;
    public static String authentication_ad_msi_title;
    public static String authentication_ad_msi_description;
    public static String authentication_ad_integrated_title;
    public static String authentication_ad_integrated_description;
    public static String authentication_kerberos_title;
    public static String authentication_kerberos_description;
    public static String authentication_other_title;
    public static String authentication_other_description;

    public static String index_type_Heap;
    public static String index_type_NonClustered;
    public static String index_type_XML;
    public static String index_type_Spatial;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, SQLServerMessages.class);
    }

    private SQLServerMessages() {
    }


}
