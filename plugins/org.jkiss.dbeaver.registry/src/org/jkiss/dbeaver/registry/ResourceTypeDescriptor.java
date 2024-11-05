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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.internal.preferences.AbstractScope;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceTypeDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import java.util.*;

/**
 * ResourceHandlerDescriptor
 */
public class ResourceTypeDescriptor extends AbstractDescriptor implements DBPResourceTypeDescriptor {
    private static final Log log = Log.getLog(ResourceTypeDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceType"; //$NON-NLS-1$

    private final String id;
    private final String name;
    private final DBPImage icon;
    private final DBPImage folderIcon;
    private final boolean managable;
    private final List<IContentType> contentTypes = new ArrayList<>();
    private final List<ObjectType> resourceTypes = new ArrayList<>();
    private final List<String> roots = new ArrayList<>();
    private String defaultRoot;
    private final Map<String, String> projectRoots = new HashMap<>();

    public ResourceTypeDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.folderIcon = iconToImage(config.getAttribute("folderIcon"));
        this.managable = CommonUtils.toBoolean(config.getAttribute(RegistryConstants.ATTR_MANAGABLE));
        for (IConfigurationElement contentTypeBinding : ArrayUtils.safeArray(config.getChildren("contentTypeBinding"))) {
            String contentTypeId = contentTypeBinding.getAttribute("contentTypeId");
            if (!CommonUtils.isEmpty(contentTypeId)) {
                IContentType contentType = Platform.getContentTypeManager().getContentType(contentTypeId);
                if (contentType != null) {
                    contentTypes.add(contentType);
                } else {
                    log.warn("Content type '" + contentTypeId + "' not recognized");
                }
            }
        }
        for (IConfigurationElement resourceTypeBinding : ArrayUtils.safeArray(config.getChildren("resourceTypeBinding"))) {
            String resourceType = resourceTypeBinding.getAttribute("resourceType");
            if (!CommonUtils.isEmpty(resourceType)) {
                resourceTypes.add(new ObjectType(resourceType));
            }
        }
        for (IConfigurationElement rootConfig : ArrayUtils.safeArray(config.getChildren("root"))) {
            String folder = rootConfig.getAttribute("folder");
            if (!CommonUtils.isEmpty(folder)) {
                roots.add(folder);
            }
            if ("true".equals(rootConfig.getAttribute("default"))) {
                defaultRoot = folder;
            }
        }
        if (CommonUtils.isEmpty(defaultRoot) && !CommonUtils.isEmpty(roots)) {
            defaultRoot = roots.get(0);
        }
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public DBPImage getIcon() {
        return icon;
    }

    @Nullable
    @Override
    public DBPImage getFolderIcon() {
        return folderIcon;
    }

    @NotNull
    @Override
    public String[] getFileExtensions() {
        Set<String> extensions = new LinkedHashSet<>();
        for (IContentType contentType : contentTypes) {
            String[] ctExtensions = contentType.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
            if (!ArrayUtils.isEmpty(ctExtensions)) {
                Collections.addAll(extensions, ctExtensions);
            }
        }
        return extensions.toArray(new String[0]);
    }

    @Override
    public Collection<IContentType> getContentTypes() {
        return contentTypes;
    }

    @Override
    public Collection<ObjectType> getResourceTypes() {
        return resourceTypes;
    }

    @Nullable
    public String getDefaultRoot(@Nullable DBPProject project) {
        if (project == null) {
            return defaultRoot;
        }
        synchronized (projectRoots) {
            String root = projectRoots.get(project.getName());
            if (root != null) {
                return root;
            }
        }
        try {
            IEclipsePreferences eclipsePreferences = getResourceHandlerPreferences(project, DBPResourceTypeDescriptor.RESOURCE_ROOT_FOLDER_NODE);
            String root = eclipsePreferences.get(id, defaultRoot);
            boolean isInvalidRoot = root != null && CommonUtils.isEmptyTrimmed(root);
            synchronized (projectRoots) {
                projectRoots.put(project.getName(), isInvalidRoot ? defaultRoot : root);
            }
            if (isInvalidRoot) {
                root = defaultRoot;
                eclipsePreferences.put(id, root);
                try {
                    eclipsePreferences.flush();
                } catch (BackingStoreException e) {
                    log.error(e);
                }
            }
            return root;
        } catch (Exception e) {
            log.error("Can't obtain resource handler preferences", e);
            return defaultRoot;
        }
    }

    @Override
    public void setDefaultRoot(@NotNull DBPProject project, @Nullable String rootPath) {
        IEclipsePreferences resourceHandlers = getResourceHandlerPreferences(project, DBPResourceTypeDescriptor.RESOURCE_ROOT_FOLDER_NODE);
        resourceHandlers.put(getId(), rootPath);
        synchronized (projectRoots) {
            projectRoots.put(project.getName(), rootPath);
        }
        try {
            resourceHandlers.flush();
        } catch (BackingStoreException e) {
            log.error(e);
        }
    }

    @Override
    public boolean isManagable() {
        return managable;
    }

    public List<String> getRoots() {
        return roots;
    }

    @Override
    public String toString() {
        return id;
    }

    private static IEclipsePreferences getResourceHandlerPreferences(DBPProject project, String node) {
        IEclipsePreferences projectSettings = getProjectPreferences(project);
        return (IEclipsePreferences) projectSettings.node(node);
    }

    private static synchronized IEclipsePreferences getProjectPreferences(DBPProject project) {
        return new ProjectScope(project).getNode("org.jkiss.dbeaver.project.resources");
    }

    public static final class ProjectScope extends AbstractScope {
        public static final String SCOPE = "project"; //$NON-NLS-1$
        private final DBPProject project;

        public ProjectScope(DBPProject context) {
            super();
            if (context == null)
                throw new IllegalArgumentException();
            this.project = context;
        }

        @Override
        public IEclipsePreferences getNode(String qualifier) {
            if (qualifier == null)
                throw new IllegalArgumentException();
            IPreferencesService preferencesService = Platform.getPreferencesService();
            Preferences scopeNode = preferencesService.getRootNode().node(SCOPE);
            Preferences projectNode = scopeNode.node(project.getName());
            return (IEclipsePreferences) projectNode.node(qualifier);
        }

        @Override
        public IPath getLocation() {
            IPath location = new Path(project.getAbsolutePath().toString());
            return location.append(EclipsePreferences.DEFAULT_PREFERENCES_DIRNAME);
        }

        @Override
        public String getName() {
            return SCOPE;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ProjectScope projectScope &&  project.equals(projectScope.project);
        }
    }

}
