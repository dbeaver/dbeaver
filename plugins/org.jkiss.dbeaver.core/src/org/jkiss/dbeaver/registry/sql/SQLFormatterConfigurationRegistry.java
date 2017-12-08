/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry.sql;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterRegistry;

import java.util.ArrayList;
import java.util.List;

public class SQLFormatterConfigurationRegistry implements SQLFormatterRegistry
{
    private static final Log log = Log.getLog(SQLFormatterConfigurationRegistry.class);

    private static final String TAG_FORMATTER = "formatter"; //$NON-NLS-1$

    private static SQLFormatterConfigurationRegistry instance = null;

    public synchronized static SQLFormatterConfigurationRegistry getInstance()
    {
        if (instance == null) {
            instance = new SQLFormatterConfigurationRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<SQLFormatterDescriptor> formatters = new ArrayList<>();

    private SQLFormatterConfigurationRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(SQLFormatterDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extConfigs) {
            // Load formatters
            if (TAG_FORMATTER.equals(ext.getName())) {
                this.formatters.add(
                    new SQLFormatterDescriptor(ext));
            }
        }
    }

    public void dispose()
    {
        formatters.clear();
    }

    public List<SQLFormatterDescriptor> getFormatters() {
        return formatters;
    }

    public SQLFormatterDescriptor getFormatter(String id) {
        for (SQLFormatterDescriptor formatter : formatters) {
            if (formatter.getId().equalsIgnoreCase(id)) {
                return formatter;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public SQLFormatter createFormatter(SQLFormatterConfiguration configuration) {
        final String formatterId = configuration.getFormatterId();
        SQLFormatterDescriptor formatterDesc = getFormatter(formatterId);
        if (formatterDesc == null) {
            log.error("Formatter '" + formatterId + "' not found");
            return null;
        }
        try {
            return formatterDesc.createFormatter();
        } catch (DBException e) {
            log.error("Error creating formatter", e);
            return null;
        }
    }

    @Override
    @Nullable
    public SQLFormatter createAndConfigureFormatter(SQLFormatterConfiguration configuration) {
        final String formatterId = configuration.getFormatterId();
        SQLFormatterDescriptor formatterDesc = getFormatter(formatterId);
        if (formatterDesc == null) {
            log.error("Formatter '" + formatterId + "' not found");
            return null;
        }
        try {
            SQLFormatter formatter = formatterDesc.createFormatter();
            SQLFormatterConfigurer configurer = formatterDesc.createConfigurer();
            if (configurer != null) {
                if (!configurer.configure(formatterDesc.getLabel(), formatter, configuration)) {
                    return null;
                }
            }
            return formatter;
        } catch (DBException e) {
            log.error("Error creating and configuring formatter", e);
            return null;
        }
    }

}
