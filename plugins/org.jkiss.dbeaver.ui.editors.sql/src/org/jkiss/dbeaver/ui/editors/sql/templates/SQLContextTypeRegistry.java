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
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.HashSet;
import java.util.Set;


/**
 * SQLContextTypeRegistry
 */
public class SQLContextTypeRegistry extends ContextTypeRegistry {

	public SQLContextTypeRegistry() {
        loadContextTypes();
    }

    private void loadContextTypes()
    {
        addContextType(new SQLContextTypeBase());
        for (DBPDataSourceProviderDescriptor provider : DBWorkbench.getPlatform().getDataSourceProviderRegistry().getDataSourceProviders()) {
            if (!provider.isDriversManageable()) {
                SQLContextTypeProvider contextType = new SQLContextTypeProvider(provider);
                addContextType(contextType);
                //provider.loadTemplateVariableResolvers(contextType);
            } else {
                Set<String> categoriesAdded = new HashSet<>();
                for (DBPDriver driver : provider.getDrivers()) {
                    if (driver.getReplacedBy() != null) {
                        continue;
                    }
                    if (!CommonUtils.isEmpty(driver.getCategory())) {
                        if (categoriesAdded.contains(driver.getCategory())) {
                            continue;
                        }
                        categoriesAdded.add(driver.getCategory());
                    }
                    SQLContextTypeDriver contextType = new SQLContextTypeDriver(driver);
                    addContextType(contextType);
                    //provider.loadTemplateVariableResolvers(contextType);
                }
            }
        }
    }

}

