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
package org.jkiss.dbeaver.ext.import_config.wizards.custom;

import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizard;
import org.jkiss.dbeaver.model.connection.DBPDriver;

import java.io.File;

public class ConfigImportWizardCustom extends ConfigImportWizard {

    private ConfigImportWizardPageCustomDriver pageDriver;
    private ConfigImportWizardPageCustomSettings pageSettings;

    enum ImportType {
        CSV,
        XML
    }

    @Override
    protected ConfigImportWizardPageCustomConnections createMainPage() {
        return new ConfigImportWizardPageCustomConnections();
    }

    @Override
    public void addPages() {
        pageDriver = new ConfigImportWizardPageCustomDriver();
        pageSettings = new ConfigImportWizardPageCustomSettings();

        addPage(pageDriver);
        addPage(pageSettings);
        super.addPages();
    }

    public DBPDriver getDriver() {
        return pageDriver.getSelectedDriver();
    }

    public ConfigImportWizardCustom.ImportType getImportType() {
        return pageSettings.getImportType();
    }

    public File getInputFile() {
        return pageSettings.getInputFile();
    }

    public String getInputFileEncoding() {
        return pageSettings.getInputFileEncoding();
    }

}