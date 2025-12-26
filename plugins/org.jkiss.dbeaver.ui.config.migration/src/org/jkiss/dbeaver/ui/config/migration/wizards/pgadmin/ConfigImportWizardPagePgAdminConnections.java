/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.config.migration.wizards.pgadmin;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ui.config.migration.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportData;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;

public class ConfigImportWizardPagePgAdminConnections extends ConfigImportWizardPage {
    private static final Log log = Log.getLog(ConfigImportWizardPagePgAdminConnections.class);

    private static final PgAdminImportConfigurationService IMPORT_CONFIGURATION_SERVICE = PgAdminImportConfigurationService.INSTANCE;

    public ConfigImportWizardPagePgAdminConnections() {
        super(ImportConfigMessages.config_import_wizard_page_caption_connections);
        setTitle(ImportConfigMessages.config_import_wizard_page_caption_connections);
        setDescription(ImportConfigMessages.config_import_wizard_header_import_configuration);
    }

    @Override
    protected void loadConnections(@NotNull ImportData importData) throws DBException {
        setErrorMessage(null);

        ConfigImportWizardPgAdmin wizard = (ConfigImportWizardPgAdmin) getWizard();
        File inputFile = wizard.getInputFile();
        try (Reader reader = Files.newBufferedReader(inputFile.toPath())) {
            IMPORT_CONFIGURATION_SERVICE.importJSON(importData, reader);
        } catch (Exception e) {
            log.warn("Exception during loading connections", e);
            setErrorMessage(e.getMessage());
        }
    }
}
