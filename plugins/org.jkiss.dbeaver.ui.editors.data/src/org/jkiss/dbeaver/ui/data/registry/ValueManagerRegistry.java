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

package org.jkiss.dbeaver.ui.data.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.MimeType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EntityEditorsRegistry
 */
public class ValueManagerRegistry {

    private static ValueManagerRegistry instance = null;

    public synchronized static ValueManagerRegistry getInstance() {
        if (instance == null) {
            instance = new ValueManagerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<ValueManagerDescriptor> managers = new ArrayList<>();
    private List<StreamValueManagerDescriptor> streamManagers = new ArrayList<>();

    private ValueManagerRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ValueManagerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (ValueManagerDescriptor.TAG_MANAGER.equals(ext.getName())) {
                managers.add(new ValueManagerDescriptor(ext));
            } else if (StreamValueManagerDescriptor.TAG_STREAM_MANAGER.equals(ext.getName())) {
                streamManagers.add(new StreamValueManagerDescriptor(ext));
            }
        }
    }

    @NotNull
    public IValueManager getManager(@Nullable DBPDataSource dataSource, @NotNull DBSTypedObject dataKind, @NotNull Class<?> valueType) {
        // Check starting from most restrictive to less restrictive
        IValueManager manager = findManager(dataSource, dataKind, valueType, true, true);
        if (manager == null) {
            manager = findManager(dataSource, dataKind, valueType, false, true);
        }
        if (manager == null) {
            manager = findManager(dataSource, dataKind, valueType, true, false);
        }
        if (manager == null) {
            manager = findManager(dataSource, dataKind, valueType, false, false);
        }
        if (manager == null) {
            throw new IllegalStateException("Can't find default data manager for " + dataKind);
        }
        return manager;
    }

    private IValueManager findManager(@Nullable DBPDataSource dataSource, DBSTypedObject typedObject, Class<?> valueType, boolean checkDataSource, boolean checkType) {
        for (ValueManagerDescriptor manager : managers) {
            if (manager.supportsType(dataSource, typedObject, valueType, checkDataSource, checkType)) {
                return manager.getInstance();
            }
        }
        return null;
    }

    @NotNull
    public static IValueManager findValueManager(@Nullable DBPDataSource dataSource, @NotNull DBSTypedObject typedObject, @NotNull Class<?> valueType) {
        return getInstance().getManager(dataSource, typedObject, valueType);
    }

    public Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> getApplicableStreamManagers(@NotNull DBRProgressMonitor monitor, @NotNull DBSTypedObject attribute, @Nullable DBDContent value) {
        Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> result = new LinkedHashMap<>();
        for (StreamValueManagerDescriptor contentManager : streamManagers) {
            IStreamValueManager.MatchType matchType = contentManager.getInstance().matchesTo(monitor, attribute, value);
            switch (matchType) {
                case NONE:
                    continue;
                case EXCLUSIVE:
                    result.clear();
                    result.put(contentManager, matchType);
                    return result;
                default:
                    result.put(contentManager, matchType);
                    break;
            }
        }
        return result;
    }

    public Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> getStreamManagersByMimeType(@NotNull String mimeType, String primaryType) {
        MimeType mime = new MimeType(mimeType);
        MimeType primaryMime = primaryType == null ? null : new MimeType(primaryType);

        Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> result = new LinkedHashMap<>();
        for (StreamValueManagerDescriptor contentManager : streamManagers) {
            for (String sm : contentManager.getSupportedMime()) {
                if (!CommonUtils.isEmpty(sm) && mime.match(sm)) {
                    if (!CommonUtils.isEmpty(contentManager.getPrimaryMime()) && primaryMime != null && primaryMime.match(contentManager.getPrimaryMime())) {
                        result.put(contentManager, IStreamValueManager.MatchType.PRIMARY);
                    } else {
                        result.put(contentManager, IStreamValueManager.MatchType.DEFAULT);
                    }
                    break;
                }
            }
        }
        return result;
    }

}
