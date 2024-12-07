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
import org.jkiss.dbeaver.registry.data.hints.AbstractValueBindingRegistry;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.managers.DefaultValueManager;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.MimeType;

import java.util.*;

/**
 * ValueManagerRegistry
 */
public class ValueManagerRegistry extends AbstractValueBindingRegistry<IValueManager, ValueManagerDescriptor> {

    private static ValueManagerRegistry instance = null;

    public synchronized static ValueManagerRegistry getInstance() {
        if (instance == null) {
            instance = new ValueManagerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<ValueManagerDescriptor> managers = new ArrayList<>();
    private final Map<String, StreamValueManagerDescriptor> streamManagers = new HashMap<>();

    private ValueManagerRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ValueManagerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (ValueManagerDescriptor.TAG_MANAGER.equals(ext.getName())) {
                managers.add(new ValueManagerDescriptor(ext));
            } else if (StreamValueManagerDescriptor.TAG_STREAM_MANAGER.equals(ext.getName())) {
                final StreamValueManagerDescriptor descriptor = new StreamValueManagerDescriptor(ext);
                streamManagers.put(descriptor.getId(), descriptor);
            }
        }
    }

    @NotNull
    @Override
    protected List<ValueManagerDescriptor> getDescriptors() {
        return managers;
    }

    @NotNull
    @Override
    protected IValueManager getDefaultValueBinding() {
        return DefaultValueManager.INSTANCE;
    }

    @NotNull
    public static IValueManager findValueManager(@Nullable DBPDataSource dataSource, @NotNull DBSTypedObject typedObject, @NotNull Class<?> valueType) {
        return getInstance().getValueBinding(dataSource, typedObject, valueType);
    }

    @NotNull
    public Collection<StreamValueManagerDescriptor> getAllStreamManagers() {
        return Collections.unmodifiableCollection(streamManagers.values());
    }

    @Nullable
    public StreamValueManagerDescriptor getStreamManager(@NotNull String id) {
        return streamManagers.get(id);
    }

    public Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> getApplicableStreamManagers(@NotNull DBRProgressMonitor monitor, @NotNull DBSTypedObject attribute, @Nullable DBDContent value) {
        boolean isTextContent = ContentUtils.isTextContent(value);
        Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> result = new LinkedHashMap<>();
        for (StreamValueManagerDescriptor contentManager : streamManagers.values()) {
            if (isTextContent && !contentManager.supportsText()) {
                // Skip different kind of manager
                continue;
            }
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
        for (StreamValueManagerDescriptor contentManager : streamManagers.values()) {
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
