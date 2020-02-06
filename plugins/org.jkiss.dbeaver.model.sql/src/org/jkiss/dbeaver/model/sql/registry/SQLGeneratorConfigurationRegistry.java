/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.sql.generator.SQLGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class SQLGeneratorConfigurationRegistry
{
    private static final Log log = Log.getLog(SQLGeneratorConfigurationRegistry.class);

    private static final String TAG_GENERATOR = "generator"; //$NON-NLS-1$

    private static SQLGeneratorConfigurationRegistry instance = null;
    private final List<SQLGeneratorDescriptor> generators = new ArrayList<>();

    public synchronized static SQLGeneratorConfigurationRegistry getInstance()
    {
        if (instance == null) {
            instance = new SQLGeneratorConfigurationRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private SQLGeneratorConfigurationRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(SQLGeneratorDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extConfigs) {
            // Load generators
            if (TAG_GENERATOR.equals(ext.getName())) {
                this.generators.add(
                    new SQLGeneratorDescriptor(ext));
            }
        }
    }

    public void dispose()
    {
        generators.clear();
    }

    public List<SQLGeneratorDescriptor> getAllGenerators() {
        return new ArrayList<>(generators);
    }

    public List<SQLGeneratorDescriptor> getApplicableGenerators(Collection<?> objects, Object context) {
        List<SQLGeneratorDescriptor> result = new ArrayList<>();
        for (SQLGeneratorDescriptor gen : generators) {
            for (Object object : objects) {
                if (object instanceof DBPObject && gen.appliesTo((DBPObject) object, context)) {
                    if (gen.isMultiObject() && objects.size() < 2) {
                        continue;
                    }
                    result.add(gen);
                    break;
                }
            }
        }
        result.sort(Comparator.comparingInt(SQLGeneratorDescriptor::getOrder));
        return result;
    }

    public SQLGeneratorDescriptor getGenerator(String id) {
        for (SQLGeneratorDescriptor generator : generators) {
            if (generator.getId().equalsIgnoreCase(id)) {
                return generator;
            }
        }
        return null;
    }

    @Nullable
    public <T> SQLGenerator<T> createGenerator(DBPDataSource dataSource, List<T> objectsd) {
/*
        SQLGeneratorDescriptor formatterDesc = getGenerator(formatterId);
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
*/
        return null;
    }

}
