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
package org.jkiss.dbeaver.registry.project;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.nio.file.Path;
import java.util.Collection;

public class LocalProjectImpl extends BaseProjectImpl {
    @NotNull
    protected final Path projectPath;

    public LocalProjectImpl(
        @NotNull DBPWorkspace workspace,
        @Nullable SMSessionContext sessionContext,
        @NotNull Path projectPath
    ) {
        super(workspace, sessionContext);
        this.projectPath = projectPath;
    }


    @Override
    public boolean isVirtual() {
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        Object projectName = this.getProjectProperty(PROP_PROJECT_NAME);
        if (projectName != null) {
            return projectName.toString();
        }
        return projectPath.getFileName().toString();
    }

    @NotNull
    @Override
    public Path getAbsolutePath() {
        return projectPath;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void ensureOpen() {

    }

    @Override
    public boolean isUseSecretStorage() {
        return false;
    }

    public Path getMetadataFilePath() {
        return getMetadataPath().resolve(METADATA_STORAGE_FILE);
    }

    /**
     * Method for Bulk Update of resources properties paths
     *
     * @param oldToNewPaths collection of OldPath to NewPath pairs
     */
    public void moveResourcePropertiesBatch(@NotNull Collection<Pair<String, String>> oldToNewPaths) {
        loadMetadata();
        synchronized (metadataSync) {
            for (var pathsPair : oldToNewPaths) {
                final var oldResourcePath = CommonUtils.normalizeResourcePath(pathsPair.getFirst());
                final var newResourcePath = CommonUtils.normalizeResourcePath(pathsPair.getSecond());
                final var resProps = resourceProperties.remove(oldResourcePath);
                if (resProps != null) {
                    resourceProperties.put(newResourcePath, resProps);
                }
            }
        }
        flushMetadata();
    }

    /**
     * Method for Bulk Remove of resources properties
     */
    public boolean resetResourcesPropertiesBatch(@NotNull Collection<String> resourcesPaths) {
        loadMetadata();
        boolean propertiesChanged = false;
        synchronized (metadataSync) {
            for (var resourcePath : resourcesPaths) {
                var removedProperties = resourceProperties.remove(CommonUtils.normalizeResourcePath(resourcePath));
                if (removedProperties != null) {
                    propertiesChanged = true;
                }
            }
        }
        if (propertiesChanged) {
            flushMetadata();
        }
        return propertiesChanged;
    }

    public boolean canUpdateProjectName() {
        return false;
    }

    @NotNull
    @Override
    protected DBPDataSourceRegistry createDataSourceRegistry() {
        return new DataSourceRegistry<>(this);
    }
}
