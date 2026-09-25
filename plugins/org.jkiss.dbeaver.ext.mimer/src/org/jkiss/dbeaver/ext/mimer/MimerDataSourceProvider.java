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
package org.jkiss.dbeaver.ext.mimer;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mimer Information Technology
 */
public class MimerDataSourceProvider extends GenericDataSourceProvider<MimerDataSource> {

    // Matches the plain sampleURL-generated "jdbc:mimer://host:port/db" (or already
    // "jdbc:mimer:local://host:port/db" on a re-generate) and captures the "/db..." tail.
    private static final Pattern URL_HOST_PATTERN = Pattern.compile("^jdbc:mimer:(?:local:)?//[^/]*(/.*)?$");

    public MimerDataSourceProvider() {
        super(MimerDataSource.class);
    }

    /**
     * Mimer SQL's "local" protocol (shared-memory IPC to a server on the same machine,
     * see {@link MimerConstants#PROP_PROTOCOL}) is selected by inserting "local:" right
     * after "jdbc:mimer:" - it also has no host component at all (the Mimer SQL JDBC Driver
     * Guide's own example is {@code jdbc:mimer:local://user:pass@/database}, i.e. an
     * empty server name), so the host/port the connection page filled into the sample
     * URL template gets stripped rather than just left in place pointing nowhere useful.
     */
    @NotNull
    @Override
    public String getConnectionURL(@NotNull DBPDriver driver, @NotNull DBPConnectionConfiguration connectionInfo) throws DBException {
        String url = super.getConnectionURL(driver, connectionInfo);
        if (!MimerConstants.PROTOCOL_LOCAL.equals(connectionInfo.getProviderProperty(MimerConstants.PROP_PROTOCOL))) {
            return url;
        }
        Matcher matcher = URL_HOST_PATTERN.matcher(url);
        if (matcher.matches()) {
            String tail = matcher.group(1);
            return "jdbc:mimer:local:" + (tail == null ? "" : tail);
        }
        return url;
    }
}
