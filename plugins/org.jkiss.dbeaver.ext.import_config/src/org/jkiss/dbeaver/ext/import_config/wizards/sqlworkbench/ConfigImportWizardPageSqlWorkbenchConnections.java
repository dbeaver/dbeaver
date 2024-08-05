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

package org.jkiss.dbeaver.ext.import_config.wizards.sqlworkbench;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.import_config.wizards.*;

import java.io.*;


public class ConfigImportWizardPageSqlWorkbenchConnections extends ConfigImportWizardPage {
    private static final Log log = Log.getLog(ConfigImportWizardPageSqlWorkbenchConnections.class);
    private final SqlWorkbenchImportConfigurationService sqlWorkbenchImportConfigurationService =
        SqlWorkbenchImportConfigurationService.INSTANCE;

    protected ConfigImportWizardPageSqlWorkbenchConnections() {
        super("Connections");
        setTitle("Connections");
        setDescription("Import connections");
    }

    @Override
    protected void loadConnections(@NotNull ImportData importData) throws DBException {
        setErrorMessage(null);

        ConfigImportWizardSqlWorkbench wizard = (ConfigImportWizardSqlWorkbench) getWizard();
        File inputFile = wizard.getInputFile();
        try (InputStream is = new FileInputStream(inputFile)) {
            try (Reader reader = new InputStreamReader(is)) {
                sqlWorkbenchImportConfigurationService.importXML(importData, reader);
            }
        } catch (Exception e) {
            log.warn("Exception during loading connections", e);
            setErrorMessage(e.getMessage());
        }
    }
}
