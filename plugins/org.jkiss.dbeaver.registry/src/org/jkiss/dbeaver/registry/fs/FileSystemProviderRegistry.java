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
package org.jkiss.dbeaver.registry.fs;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.fs.DBFFileSystemDescriptor;
import org.jkiss.dbeaver.model.fs.DBFRegistry;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class FileSystemProviderRegistry implements DBFRegistry {
    private static FileSystemProviderRegistry instance = null;

    public synchronized static FileSystemProviderRegistry getInstance() {
        if (instance == null) {
            instance = new FileSystemProviderRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<FileSystemProviderDescriptor> descriptors = new ArrayList<>();

    private FileSystemProviderRegistry(IExtensionRegistry registry) {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(FileSystemProviderDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                descriptors.add(new FileSystemProviderDescriptor(ext));
            }
        }
    }

    public List<FileSystemProviderDescriptor> getProviders() {
        return descriptors;
    }

    public FileSystemProviderDescriptor getProvider(String id) {
        for (FileSystemProviderDescriptor descriptor : descriptors) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    @Override
    public DBFFileSystemDescriptor[] getFileSystemProviders() {
        return descriptors.toArray(new DBFFileSystemDescriptor[0]);
    }

    @Override
    public DBFFileSystemDescriptor getFileSystemProvider(@NotNull String id) {
        return descriptors.stream().filter(d -> d.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public DBFFileSystemDescriptor getFileSystemProviderBySchema(@NotNull String schema) {
        return descriptors.stream().filter(d -> CommonUtils.equalObjects(d.getSchema(), schema))
            .findFirst().orElse(null);
    }
}
