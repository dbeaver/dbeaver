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

package org.jkiss.dbeaver.ui.config.migration.wizards.datagrip;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ui.config.migration.datagrip.api.DataGripDataSourceConfigXmlService;
import org.jkiss.dbeaver.ui.config.migration.datagrip.impl.DataGripDataSourceConfigXmlServiceImpl;
import org.jkiss.dbeaver.ui.config.migration.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


public class ConfigImportWizardPageDataGripConnections extends ConfigImportWizardPage {
    private static final Log log = Log.getLog(ConfigImportWizardPageDataGripConnections.class);
    DataGripDataSourceConfigXmlService dataGripDataSourceConfigXmlService = DataGripDataSourceConfigXmlServiceImpl.INSTANCE;


    public ConfigImportWizardPageDataGripConnections() {
        super(ImportConfigMessages.config_import_wizard_page_caption_connections);
        setTitle(ImportConfigMessages.config_import_wizard_page_caption_connections);
        setDescription(ImportConfigMessages.config_import_wizard_header_import_configuration);
    }

    @Override
    protected void loadConnections(ImportData importData) throws DBException {
        setErrorMessage(null);
        try {
            tryLoadConnection(importData);
        } catch (Exception e) {
            log.warn("Exception during to import connections", e);
            setErrorMessage(e.getMessage());
        }
    }

    private void tryLoadConnection(ImportData importData) throws Exception {

        ConfigImportWizardDataGrip wizard = (ConfigImportWizardDataGrip) getWizard();
        Path ideaDirectory = wizard.getInputFile();
        if (!Files.exists(ideaDirectory)) {
            return;
        }
        Map<String, Map<String, String>> uuidToDataSourceProps = dataGripDataSourceConfigXmlService.buildIdeaConfigProps(
                ideaDirectory.toString());
        for (Map<String, String> dataSourceProps : uuidToDataSourceProps.values()) {
            ImportConnectionInfo connectionInfo = dataGripDataSourceConfigXmlService.buildIdeaConnectionFromProps(dataSourceProps);
            importData.addDriver(connectionInfo.getDriverInfo());
            importData.addConnection(connectionInfo);
        }
    }
}
