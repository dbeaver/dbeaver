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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreServerYugabyteDB
 */
public class PostgreServerYugabyteDB extends PostgreServerExtensionBase {

    private static final Log log = Log.getLog(PostgreServerYugabyteDB.class);

    private Version yugabyteVersion;

    public PostgreServerYugabyteDB(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getServerTypeName() {
        return "YugabyteDB";
    }

    private boolean isYugabyteVersionAtLeast(int major, int minor) {
        if (yugabyteVersion == null) {
            String serverVersion = dataSource.getServerVersion();
            if (!CommonUtils.isEmpty(serverVersion)) {
                try {
                    Matcher matcher = Pattern.compile(".*-YB-([0-9]*\\.[0-9]*).*").matcher(serverVersion);
                    if (matcher.find()) {
                        String versionStr = matcher.group(1);
                        if (!CommonUtils.isEmpty(versionStr)) {
                            yugabyteVersion = new Version(versionStr);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error reading YugabyteDB version", e);
                    yugabyteVersion = new Version(2, 0,0);
                }
            }
        }
        if (yugabyteVersion != null) {
            if (yugabyteVersion.getMajor() > major) {
                return true;
            } else if (yugabyteVersion.getMajor() == major) {
                if (yugabyteVersion.getMinor() >= minor) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean supportsOids() {
        return true;
    }

    @Override
    public boolean supportsIndexes() {
        return true;
    }

    @Override
    public boolean supportsMaterializedViews() {
        return isYugabyteVersionAtLeast(2, 13);
    }

    @Override
    public boolean supportsPartitions() {
        return isYugabyteVersionAtLeast(2, 4);
    }

    @Override
    public boolean supportsInheritance() {
        return isYugabyteVersionAtLeast(2, 4);
    }

    @Override
    public boolean supportsTriggers() {
        return true;
    }

    @Override
    public boolean supportsFunctionCreate() {
        return true;
    }

    @Override
    public boolean supportsRules() {
        return true;
    }

    @Override
    public boolean supportsRowLevelSecurity() {
        return true;
    }

    @Override
    public boolean supportsExtensions() {
        return true;
    }

    @Override
    public boolean supportsEncodings() {
        return true;
    }

    @Override
    public boolean supportsTablespaces() {
        return isYugabyteVersionAtLeast(2, 12);
    }

    @Override
    public boolean supportsSequences() {
        return true;
    }

    @Override
    public boolean supportsRoles() {
        return true;
    }

    @Override
    public boolean supportsSessionActivity() {
        return true;
    }

    @Override
    public boolean supportsLocks() {
        return true;
    }

    @Override
    public boolean supportsForeignServers() {
        return false;
    }

    @Override
    public boolean supportsAggregates() {
        return true;
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return false;
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return true;
    }

    @Override
    public boolean supportsExplainPlan() {
        return true;
    }

    @Override
    public boolean supportsExplainPlanVerbose() {
        return true;
    }

    @Override
    public boolean supportsTablespaceLocation() {
        // The pg_tablespace_location function exists, but locations are not supported in the creation statement
        return false;
    }

    @Override
    public boolean supportsExplainPlanXML() {
        return true;
    }
}

