/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.dbeaver.ext.generic.model.GenericDataSourceInfo;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.osgi.framework.Version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuckDBDataSourceInfo extends GenericDataSourceInfo {
    private static final Pattern DATABASE_VERSION_PATTERN = Pattern.compile("^\\D*(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*$");

    private final Version databaseVersion;

    public DuckDBDataSourceInfo(DBPDriver driver, JDBCDatabaseMetaData metaData) {
        super(driver, metaData);
        this.databaseVersion = resolveDatabaseVersion();
    }

    @Override
    public Version getDatabaseVersion() {
        return databaseVersion;
    }

    private Version resolveDatabaseVersion() {
        var productVersion = getDatabaseProductVersion();
        if (productVersion != null) {
            Matcher matcher = DATABASE_VERSION_PATTERN.matcher(productVersion);
            if (matcher.matches()) {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                int micro = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
                return new Version(major, minor, micro);
            }
        }
        return super.getDatabaseVersion();
    }
}
