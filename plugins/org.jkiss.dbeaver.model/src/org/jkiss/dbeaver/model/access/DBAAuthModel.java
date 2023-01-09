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

package org.jkiss.dbeaver.model.access;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Properties;

/**
 * Auth model.
 */
public interface DBAAuthModel<CREDENTIALS extends DBAAuthCredentials> {

    @NotNull
    CREDENTIALS createCredentials();

    /**
     * Create credentials from datasource configuration
     */
    @NotNull
    CREDENTIALS loadCredentials(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration);

    /**
     * Save credentials into connection configuration
     */
    void saveCredentials(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull CREDENTIALS credentials);

    /**
     * Called before connection opening. May modify any connection configuration properties
     *
     * @param dataSource  data source
     * @param credentials auth credentials
     * @param configuration connection configuration
     * @param connProperties auth model specific options.
     * @throws DBException on error
     * @return auth token. In most cases it is the same credentials object
     */
    Object initAuthentication(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull CREDENTIALS credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connProperties) throws DBException;

    void endAuthentication(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connProperties);

    /**
     * Refresh credentials in current session
     * @param monitor progress monitor
     * @param credentials
     */
    void refreshCredentials(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull CREDENTIALS credentials
    )
        throws DBException;

    /**
     * Validate that connection contains necessary credentials
     *
     * @param project
     * @param configuration
     */
    boolean isDatabaseCredentialsPresent(DBPProject project, DBPConnectionConfiguration configuration);
}
