/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.debug;

public class PostgreDebugConstants {

    public static final String ATTR_ATTACH_KIND = "PG.ATTR_ATTACH_KIND"; //$NON-NLS-1$
    public static final String ATTR_ATTACH_PROCESS = "PG.ATTACH_PROCESS"; //$NON-NLS-1$
    public static final String ATTR_DATABASE_NAME = "PG.ATTR_DATABASE_NAME"; //$NON-NLS-1$
    public static final String ATTR_SCHEMA_NAME = "PG.ATTR_SCHEMA_NAME"; //$NON-NLS-1$
    public static final String ATTR_FUNCTION_OID = "PG.ATTR_FUNCTION_OID"; //$NON-NLS-1$
    public static final String ATTR_FUNCTION_PARAMETERS = "PG.ATTR_FUNCTION_PARAMETERS"; //$NON-NLS-1$

    public static final String ATTACH_KIND_LOCAL = "LOCAL"; //$NON-NLS-1$
    public static final String ATTACH_KIND_GLOBAL = "GLOBAL"; //$NON-NLS-1$

    public static final String DEBUG_TYPE_FUNCTION = "org.jkiss.dbeaver.ext.postgresql.debug.function";

}
