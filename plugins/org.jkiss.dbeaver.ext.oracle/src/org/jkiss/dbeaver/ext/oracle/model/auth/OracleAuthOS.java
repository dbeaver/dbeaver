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

package org.jkiss.dbeaver.ext.oracle.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.auth.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.StandardConstants;

import java.util.Properties;

/**
 * Oracle OS auth model.
 */
public class OracleAuthOS implements DBAAuthModel {

    public static final String ID = "oracle_os";

    @Override
    public void initAuthentication(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connProperties) throws DBException {
        connProperties.remove(DBConstants.DATA_SOURCE_PROPERTY_USER);
        connProperties.remove(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD);

        connProperties.put("v$session.osuser", System.getProperty(StandardConstants.ENV_USER_NAME));
    }

    @Override
    public void endAuthentication(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connProperties) {

    }

}
