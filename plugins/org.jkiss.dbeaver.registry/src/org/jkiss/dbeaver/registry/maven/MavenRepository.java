/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.maven;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maven repository manager.
 */
public class MavenRepository
{
    private static final Log log = Log.getLog(MavenRepository.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.mavenRepository";

    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";

    public enum RepositoryType {
        GLOBAL,     // Globally defined repositories (came from plugin.xml)
        LOCAL,      // Local (deployed locally) repository. It is singleton
        CUSTOM,     // User-defined repository
        EXTERNAL    // POM-defined repository
    }

    private String id;
    private final RepositoryType type;
    private String name;
    private String url;
    private final List<String> scopes = new ArrayList<>();
    private int order;
    private boolean enabled = true;
    private String description;
    private final DBPAuthInfo authInfo = new DBPAuthInfo();

    private Map<String, MavenArtifact> cachedArtifacts = new LinkedHashMap<>();

    public MavenRepository(IConfigurationElement config)
    {
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.order = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_ORDER));
        this.name = CommonUtils.toString(config.getAttribute(RegistryConstants.ATTR_NAME), this.id);
        String urlString = config.getAttribute(RegistryConstants.ATTR_URL);
        if (!urlString.endsWith("/")) urlString += "/";
        this.url = urlString;
        this.type = RepositoryType.GLOBAL;

        for (IConfigurationElement scope : config.getChildren("scope")) {
            final String group = scope.getAttribute("group");
            if (!CommonUtils.isEmpty(group)) {
                scopes.add(group);
            }
        }
    }

    public MavenRepository(String id, String name, String url, RepositoryType type) {
        this.id = id;
        this.type = type;
        this.name = CommonUtils.isEmpty(name) ? id : name;
        if (!url.endsWith("/")) url += "/";
        this.url = url;
    }

    // Copy constructor
    public MavenRepository(MavenRepository source) {
        this.id = source.id;
        this.type = source.type;
        this.name = source.name;
        this.url = source.url;
        this.scopes.addAll(source.scopes);

        this.order = source.order;
        this.enabled = source.enabled;
        this.description = source.description;
        this.authInfo.setUserName(source.authInfo.getUserName());
        this.authInfo.setUserPassword(source.authInfo.getUserPassword());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @NotNull
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes.clear();
        this.scopes.addAll(scopes);
    }

    public RepositoryType getType() {
        return type;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NotNull
    public DBPAuthInfo getAuthInfo() {
        return authInfo;
    }

    public boolean isSecureRepository() {
        if (type == RepositoryType.LOCAL || type == RepositoryType.CUSTOM) {
            return true;
        }
        return url.startsWith("https");
    }

    @Nullable
    public synchronized MavenArtifactVersion findArtifact(@NotNull DBRProgressMonitor monitor, @NotNull MavenArtifactReference ref) {
        boolean newArtifact = false;
        MavenArtifact artifact = cachedArtifacts.get(ref.getId());
        if (artifact == null) {
            artifact = new MavenArtifact(this, ref.getGroupId(), ref.getArtifactId(), ref.getClassifier());
            newArtifact = true;
        }
        try {
            MavenArtifactVersion version = artifact.resolveVersion(monitor, ref.getVersion(), ref.isResolveOptionalDependencies());
            if (newArtifact) {
                cachedArtifacts.put(ref.getId(), artifact);
            }
            return version;
        } catch (IOException e) {
            // Generally it is ok. Artifact not present in this repository
            log.debug("Maven artifact '" + ref + "' not found in repository '" + this + "': " + e.getMessage());
            return null;
        }
    }

    synchronized void resetArtifactCache(@NotNull MavenArtifactReference artifactReference) {
        cachedArtifacts.remove(artifactReference.getId());
    }

    File getLocalCacheDir()
    {
        String extPath;
        switch (type) {
            case EXTERNAL:
                try {
                    URL repoUrl = new URL(this.url);
                    extPath = ".external/" + repoUrl.getHost() + "/" + repoUrl.getPath();
                } catch (MalformedURLException e) {
                    extPath = ".external/" + id;
                }
                break;
            default:
                extPath = id;
                break;
        }
        File homeFolder = new File(DBWorkbench.getPlatform().getCustomDriversHome(), "maven/" + extPath);
        //File homeFolder = new File(DBeaverActivator.getInstance().getStateLocation().toFile(), "maven/" + extPath);
        if (!homeFolder.exists()) {
            if (!homeFolder.mkdirs()) {
                log.warn("Can't create maven repository '" + name + "' cache folder '" + homeFolder + "'");
            }
        }

        return homeFolder;
    }

    @Override
    public String toString() {
        return url;
    }

}
