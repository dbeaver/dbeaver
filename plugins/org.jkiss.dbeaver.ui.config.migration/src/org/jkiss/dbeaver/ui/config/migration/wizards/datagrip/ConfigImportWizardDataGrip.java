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

import org.jkiss.dbeaver.ui.config.migration.wizards.ConfigImportWizard;

import java.io.File;

/**
 * DataGrip (JetBrains) connection import wizard.
 * 
 * This wizard allows importing database connections from DataGrip's exported IDE settings.
 * 
 * How to use:
 * 1. In DataGrip: File → Export Settings... → Select "Database" or "All Settings" → Export to ZIP
 * 2. In DBeaver: File → Import → Third-party → DataGrip
 * 3. Select the exported ZIP file (will be automatically extracted)
 * 
 * The wizard will parse the dataSources.xml file from the settings/options/ folder
 * and convert DataGrip connection definitions to DBeaver format.
 * 
 * Supported features:
 * - Multiple database types (PostgreSQL, MySQL, Oracle, SQL Server, SQLite, etc.)
 * - Connection names and descriptions
 * - Host, port, and database information
 * - Read-only connection settings
 * - Direct ZIP file import (automatically extracts to temp directory)
 * 
 * Limitations:
 * - Passwords are not included in DataGrip exports for security reasons
 * - Advanced connection properties may need manual configuration
 * - SSH tunnels and SSL settings are not imported (DataGrip stores these separately)
 */
public class ConfigImportWizardDataGrip extends ConfigImportWizard {

    private ConfigImportWizardPageDataGripSettings pageSettings;

    public ConfigImportWizardDataGrip() {
        setWindowTitle("Import DataGrip Connections");
    }

    @Override
    protected ConfigImportWizardPageDataGrip createMainPage() {
        return new ConfigImportWizardPageDataGrip();
    }

    @Override
    public void addPages() {
        pageSettings = new ConfigImportWizardPageDataGripSettings();
        addPage(pageSettings);
        super.addPages();
    }

    public ConfigImportWizardPageDataGripSettings getPageSettings() {
        return pageSettings;
    }

    public File getInputFile() {
        return pageSettings.getInputFile();
    }
}
