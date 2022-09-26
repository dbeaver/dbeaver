/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSetting;

/**
 * PostgreServerPostgreSQL
 */
public class PostgreServerPostgreSQL extends PostgreServerExtensionBase {
    public static final String TYPE_ID = "postgresql";

    public PostgreServerPostgreSQL(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public boolean supportsEntityMetadataInResults() {
        return true;
    }

    @Override
    public String getServerTypeName() {
        return "PostgreSQL";
    }

    @Override
    public boolean supportsPGConstraintExpressionColumn() {
        return !dataSource.isServerVersionAtLeast(12, 0);
    }

    @Override
    public boolean supportsHasOidsColumn() {
        return !dataSource.isServerVersionAtLeast(12, 0);
    }

    @Override
    public boolean supportsRowLevelSecurity() {
        return dataSource.isServerVersionAtLeast(10, 0);
    }

    @Override
    public boolean supportsDatabaseSize() {
        return true;
    }

    @Override
    public boolean supportsBackslashStringEscape() {
        final PostgreSetting setting = dataSource.getSetting(PostgreConstants.OPTION_STANDARD_CONFORMING_STRINGS);
        return setting != null && "off".equals(setting.getValue());
    }

    @Override
    public boolean supportsDisablingAllTriggers() {
        return true;
    }

    @Override
    public boolean supportsGeneratedColumns() {
        return dataSource.isServerVersionAtLeast(12, 0);
    }

    @Override
    public boolean supportsKeyAndIndexRename() {
        return dataSource.isServerVersionAtLeast(9, 2);
    }

    @Override
    public boolean supportsAlterUserChangePassword() {
        return true;
    }

    @Override
    public boolean supportsCopyFromStdIn() {
        return true;
    }

    @Override
    public boolean supportsEventTriggers() {
        return dataSource.isServerVersionAtLeast(9, 3);
    }
}
