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

package org.jkiss.dbeaver.ext.import_config.wizards.dbvis;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.Activator;
import org.jkiss.dbeaver.ext.import_config.ImportConfigMessages;
import org.jkiss.dbeaver.ext.import_config.dbvis.DbvisConfigurationImporter;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConfigImportWizardPageDbvis extends ConfigImportWizardPage {

    public static final String DBVIS_HOME_FOLDER = ".dbvis";

    protected ConfigImportWizardPageDbvis() {
        super("DBVisualizer");
        setTitle("DBVisualizer");
        setDescription("Import DBVisualizer connections");
        setImageDescriptor(Activator.getImageDescriptor("icons/dbvis_big.png"));
    }

    @Override
    protected void loadConnections(ImportData importData) throws DBException {
        File homeFolder = RuntimeUtils.getUserHomeDir();
        File dbvisConfigHome = new File(homeFolder, DBVIS_HOME_FOLDER);
        if (!dbvisConfigHome.exists()) {
            throw new DBException(ImportConfigMessages.config_import_wizard_page_dbvis_label_installation_not_found);
        }
        DbvisConfigurationImporter configurationImporter = new DbvisConfigurationImporter();
        importData = configurationImporter.importConfiguration(importData, dbvisConfigHome);
    }
}
