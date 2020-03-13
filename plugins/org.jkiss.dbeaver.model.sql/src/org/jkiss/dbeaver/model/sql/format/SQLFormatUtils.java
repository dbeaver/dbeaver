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
package org.jkiss.dbeaver.model.sql.format;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.registry.SQLFormatterConfigurationRegistry;

/**
 * SQL Formatter
 */
public class SQLFormatUtils {

    public static String formatSQL(DBPDataSource dataSource, String query)
    {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dataSource.getSQLDialect(), dataSource.getContainer().getPreferenceStore());
        SQLFormatterConfiguration configuration = new SQLFormatterConfiguration(dataSource, syntaxManager);
        SQLFormatter formatter = SQLFormatterConfigurationRegistry.getInstance().createFormatter(configuration);
        if (formatter == null) {
            return query;
        }
        return formatter.format(query, configuration);
    }

}
