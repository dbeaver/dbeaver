/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.debug.internal;

import org.eclipse.osgi.util.NLS;

public class PostgreDebugCoreMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.postgresql.debug.internal.PostgreDebugCoreMessages"; //$NON-NLS-1$
    public static String PgSqlDebugController_connection_application_name;
    public static String PgSqlDebugController_e_failed_session_close;
    public static String PgSqlDebugController_e_failed_session_open;

    public static String PostgreSqlDebugCore_e_procedure_required;
    public static String PostgreSqlDebugCore_launch_configuration_name;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, PostgreDebugCoreMessages.class);
    }

    private PostgreDebugCoreMessages()
    {
    }
}
