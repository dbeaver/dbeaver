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
package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.utils.CommonUtils;

public abstract class NativeToolUtils {

    public static final String VARIABLE_HOST = "host";
    public static final String VARIABLE_DATABASE = "database";
    public static final String VARIABLE_TABLE = "table";
    public static final String VARIABLE_DATE = "date";
    public static final String VARIABLE_TIMESTAMP = "timestamp";
    public static final String VARIABLE_CONN_TYPE = "connectionType";

    public static boolean isSecureString(AbstractNativeToolSettings settings, String string) {
        String userPassword = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserPassword();
        String toolUserPassword = settings.getToolUserPassword();
        return !CommonUtils.isEmpty(toolUserPassword) && string.endsWith(toolUserPassword) ||
            !CommonUtils.isEmpty(userPassword) && string.endsWith(userPassword);
    }

}
