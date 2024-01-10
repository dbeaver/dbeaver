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
package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.StandardConstants;

import java.text.DateFormat;
import java.util.Date;

public class SQLNewScriptTemplateVariablesResolver extends DataSourceVariableResolver {
    public static final String VAR_DATE = "date";
    public static final String VAR_TIME = "time";
    public static final String VAR_USER = "user";

    public static final String[][] ALL_VARIABLES_INFO = ArrayUtils.concatArrays(
        DBPConnectionConfiguration.CONNECT_VARIABLES,
        new String[][]{
            {VAR_DATE, "current date"},
            {VAR_TIME, "current time"},
            {VAR_USER, "OS user name"},
        }
    );

    @Override
    public boolean isSecure() {
        return false;
    }

    public SQLNewScriptTemplateVariablesResolver(DBPDataSourceContainer dataSourceContainer, DBPConnectionConfiguration configuration) {
        super(dataSourceContainer, configuration);
    }

    @Override
    public String get(String name) {
        switch (name) {
            case VAR_DATE:
                return DateFormat.getDateInstance().format(new Date());
            case VAR_TIME:
                return DateFormat.getTimeInstance().format(new Date());
            case VAR_USER:
                return System.getProperty(StandardConstants.ENV_USER_NAME);
            default:
                return super.get(name);
        }
    }
}
