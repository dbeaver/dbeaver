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

package org.jkiss.dbeaver.ext.config.migration.wizards.dbvis;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.config.migration.Activator;
import org.jkiss.dbeaver.ext.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ext.config.migration.dbvis.DbvisConfigurationImporter;
import org.jkiss.dbeaver.ext.config.migration.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportData;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;

public class ConfigImportWizardPageDbvis extends ConfigImportWizardPage {

    public static final String DBVIS_HOME_FOLDER = ".dbvis";
    
    protected ConfigImportWizardPageDbvis() {
        super(ImportConfigMessages.config_import_wizard_dbvis_name);
        setTitle(ImportConfigMessages.config_import_wizard_dbvis_name);
        setDescription(ImportConfigMessages.config_import_wizard_dbvis_description);
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

    @Override
    public boolean isPageComplete() {
        if (getConnectionTable() == null) {
            return false;
        }
        setErrorMessage(null);
        boolean isCompleted = false;
        for (TableItem item : getConnectionTable().getItems()) {
            if (item.getChecked() && item.getData() instanceof ImportConnectionInfo importConnection) {
                if (importConnection.getDriverInfo() == null) {
                    isCompleted = false;
                    setErrorMessage(NLS.bind(ImportConfigMessages.config_import_wizard_error, importConnection.getAlias()));
                    break;
                } else {
                    isCompleted = true;
                }
            }
        }
        return isCompleted;
    }
}
