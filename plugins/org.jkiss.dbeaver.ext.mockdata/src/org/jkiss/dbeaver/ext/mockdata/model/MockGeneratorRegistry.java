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
package org.jkiss.dbeaver.ext.mockdata.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class MockGeneratorRegistry
{
    static final String TAG_GENERATOR = "generator"; //$NON-NLS-1$

    public static final String FK_GENERATOR_ID = "fkGenerator"; //$NON-NLS-1$

    private static final Log log = Log.getLog(MockGeneratorRegistry.class);

    private static MockGeneratorRegistry instance = null;

    public synchronized static MockGeneratorRegistry getInstance()
    {
        if (instance == null) {
            instance = new MockGeneratorRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<MockGeneratorDescriptor> generators = new ArrayList<>();

    private MockGeneratorRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        Set<String> replacedSet = new HashSet<>();

        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(MockGeneratorDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extConfigs) {
            // Load functions
            if (TAG_GENERATOR.equals(ext.getName())) {
                MockGeneratorDescriptor generatorDescriptor = new MockGeneratorDescriptor(ext);
                this.generators.add(generatorDescriptor);

                String[] replaces = generatorDescriptor.getReplaces();
                if (replaces != null) {
                    replacedSet.addAll(Arrays.asList(replaces));
                }

                if (!CommonUtils.isEmpty(generatorDescriptor.getPresets())) {
                    for (MockGeneratorDescriptor.Preset preset : generatorDescriptor.getPresets()) {
                        this.generators.add(new MockGeneratorDescriptor(ext, preset));
                    }
                }
            }
        }

        for (String replaced : replacedSet) {
            MockGeneratorDescriptor generator = getGenerator(replaced);
            if (generator != null) {
                this.generators.remove(generator);
            }
        }
    }

    public void dispose()
    {
        generators.clear();
    }

    /**
     * Find generator by data source and some typed object (e.g. attribute)
     */
    @Nullable
    public MockGeneratorDescriptor findGenerator(DBPDataSource dataSource, DBSTypedObject typedObject) {
        for (MockGeneratorDescriptor descriptor : generators) {

            if (((!descriptor.isGlobal() && descriptor.supportsDataSource(dataSource) && descriptor.supportsType(typedObject)) ||
                    (descriptor.isGlobal() && descriptor.supportsType(typedObject))))
            {
                return descriptor;
            }
        }
        return null;
    }

    public List<MockGeneratorDescriptor> findAllGenerators(DBPDataSource dataSource, DBSTypedObject typedObject) {

        List<MockGeneratorDescriptor> result = new ArrayList<>();
        for (MockGeneratorDescriptor descriptor : generators) {
            if (FK_GENERATOR_ID.equalsIgnoreCase(descriptor.getId())) {
                continue;
            }

            if (((!descriptor.isGlobal() && descriptor.supportsDataSource(dataSource) && descriptor.supportsType(typedObject)) ||
                (descriptor.isGlobal() && descriptor.supportsType(typedObject))))
            {
                result.add(descriptor);
            }
        }
        return result;
    }

    /**
     * Find generator by id
     */
    public MockGeneratorDescriptor getGenerator(String id) {
        for (MockGeneratorDescriptor descriptor : generators) {
            if (id.equals(descriptor.getId())) {
                return descriptor;
            }
        }
        return null;
    }

}
