/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
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
        for (DataSourceProviderDescriptor provider : DataSourceProviderRegistry.getInstance().getDataSourceProviders()) {
            if (!provider.isDriversManagable()) {
                SQLContextTypeProvider contextType = new SQLContextTypeProvider(provider);
                addContextType(contextType);
                provider.loadTemplateVariableResolvers(contextType);
            } else {
                Set<String> categoriesAdded = new HashSet<String>();
                for (DriverDescriptor driver : provider.getEnabledDrivers()) {
                    if (!CommonUtils.isEmpty(driver.getCategory())) {
                        if (categoriesAdded.contains(driver.getCategory())) {
                            continue;
                        }
                        categoriesAdded.add(driver.getCategory());
                    }
                    SQLContextTypeDriver contextType = new SQLContextTypeDriver(driver);
                    addContextType(contextType);
                    provider.loadTemplateVariableResolvers(contextType);
                }
            }
        }
    }

}

