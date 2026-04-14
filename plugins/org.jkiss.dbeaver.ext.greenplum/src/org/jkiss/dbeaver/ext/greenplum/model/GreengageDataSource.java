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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.osgi.framework.Version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GreengageDataSource
        extends GreenplumDataSource {

    public GreengageDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        super(monitor, container);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
        if (serverVersion != null) {
            gpVersion = getDatabaseVersion("Greengage Database ([0-9\\.]+)");
            if (gpVersion == null) {
                //fallback to greenplum version for backward compatibility
                gpVersion = getDatabaseVersion("Greenplum Database ([0-9\\.]+)");
            }
        }
        if (gpVersion == null) {
            gpVersion = new Version(4, 2, 0);
        }
    }

    @Nullable
    private Version getDatabaseVersion(@NotNull String versionPattern) {
        Matcher matcher = Pattern.compile(versionPattern).matcher(serverVersion);
        if (matcher.find()) {
            return new Version(matcher.group(1));
        }
        return null;
    }
}
