/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions.exec;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;

import java.util.ArrayList;
import java.util.List;

public class SQLNativeExecutorRegistry {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sql.executors"; //NON-NLS-1

    private static final String TAG_EXECUTOR = "executor"; //$NON-NLS-1$

    private static SQLNativeExecutorRegistry instance;
    private final List<SQLNativeExecutorDescriptor> executors = new ArrayList<>();

    private SQLNativeExecutorRegistry() {
        // prevents instantiation
    }

    /**
     * Get registry instance
     */
    @NotNull
    public static synchronized SQLNativeExecutorRegistry getInstance() {
        if (instance == null) {
            instance = new SQLNativeExecutorRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private void loadExtensions(@NotNull IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement viewElement : extElements) {
            if (viewElement.getName().equals(TAG_EXECUTOR)) {
                this.executors.add(new SQLNativeExecutorDescriptor(viewElement));
            }
        }
    }

    @NotNull
    public List<SQLNativeExecutorDescriptor> getExecutors() {
        return new ArrayList<>(executors);
    }

    /**
     * Returns the first possible supported executor for datasource
     */
    @Nullable
    public SQLNativeExecutorDescriptor getExecutorDescriptor(@NotNull DBPDataSourceContainer container) {
        DBPDataSourceProviderDescriptor provider = container.getDriver().getProviderDescriptor();
        for (DBPDataSourceProviderDescriptor pd = provider; pd != null; pd = pd.getParentProvider()) {
            for (SQLNativeExecutorDescriptor executor : executors) {
                if (executor.getDataSourceId().equals(pd.getId())) {
                    return executor;
                }
            }
        }
        return null;
    }

}
