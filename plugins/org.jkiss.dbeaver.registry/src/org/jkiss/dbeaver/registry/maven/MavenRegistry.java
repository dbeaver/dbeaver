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
package org.jkiss.dbeaver.registry.maven;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.encode.SimpleStringEncrypter;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MavenRegistry {
    private static final Log log = Log.getLog(MavenRegistry.class);

    public static final String MAVEN_LOCAL_REPO_ID = "local";
    public static final String MAVEN_LOCAL_REPO_NAME = "Local Repository";
    public static final String MAVEN_LOCAL_REPO_FOLDER = "maven-local";
    public static final String MAVEN_REPOSITORIES_CONFIG = "maven-repositories.xml";

    private static MavenRegistry instance = null;
    private final List<String> ignoredArtifactVersions = new ArrayList<>();

    public synchronized static MavenRegistry getInstance() {
        if (instance == null) {
            instance = new MavenRegistry();
            instance.init();
        }
        return instance;
    }

    private final List<MavenRepository> repositories = new ArrayList<>();
    private MavenRepository localRepository;
    // Cache for not found artifact ids. Avoid multiple remote metadata reading
    private final Map<String, MavenArtifactVersion> notFoundArtifacts = new HashMap<>();

    private MavenRegistry() {
    }

    boolean isVersionIgnored(String ref) {
        for (String ver : ignoredArtifactVersions) {
            if (ref.startsWith(ver)) {
                return true;
            }
        }
        return false;
    }

    private void init() {
        loadStandardRepositories();
        loadCustomRepositories();
        sortRepositories();
    }

    private void loadStandardRepositories() {
        // Load repositories info
        {
            IConfigurationElement[] extElements = Platform.getExtensionRegistry().getConfigurationElementsFor(MavenRepository.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                if ("repository".equals(ext.getName())) {
                    MavenRepository repository = new MavenRepository(ext);
                    repositories.add(repository);
                } else if ("ignoreArtifactVersion".equals(ext.getName())) {
                    ignoredArtifactVersions.add(ext.getAttribute("id"));
                }
            }
        }
        // Create local repository
        String localRepoURL;
        try {
            localRepoURL = Platform.getInstallLocation().getDataArea(MAVEN_LOCAL_REPO_FOLDER).toString();
        } catch (IOException e) {
            localRepoURL = Platform.getInstallLocation().getURL().toString() + "/" + MAVEN_LOCAL_REPO_FOLDER;
        }
        localRepository = new MavenRepository(
            MAVEN_LOCAL_REPO_ID,
            MAVEN_LOCAL_REPO_NAME,
            localRepoURL,
            MavenRepository.RepositoryType.LOCAL);
    }

    public void setCustomRepositories(List<MavenRepository> customRepositories) {
        // Clear not-found cache
        notFoundArtifacts.clear();
        // Remove old custom repos
        for (Iterator<MavenRepository> iter = this.repositories.iterator(); iter.hasNext(); ) {
            if (iter.next().getType() == MavenRepository.RepositoryType.CUSTOM) {
                iter.remove();
            }
        }
        // Add new and reorder
        this.repositories.addAll(customRepositories);
        sortRepositories();
    }

    public void loadCustomRepositories() {
        try {
            String config = DBWorkbench.getPlatform().getConfigurationController().loadConfigurationFile(MAVEN_REPOSITORIES_CONFIG);
            if (CommonUtils.isEmpty(config)) {
                return;
            }
            final Document reposDocument = XMLUtils.parseDocument(new StringReader(config));
            for (Element repoElement : XMLUtils.getChildElementList(reposDocument.getDocumentElement(), "repository")) {
                String repoID = repoElement.getAttribute("id");
                MavenRepository repo = findRepository(repoID);
                if (repo == null) {
                    String repoName = repoElement.getAttribute("name");
                    String repoURL = repoElement.getAttribute("url");
                    repo = new MavenRepository(
                        repoID,
                        repoName,
                        repoURL,
                        MavenRepository.RepositoryType.CUSTOM);
                    boolean snapshot = CommonUtils.toBoolean(repoElement.getAttribute("snapshot"));
                    repo.setIsSnapshot(snapshot);
                    List<String> scopes = new ArrayList<>();
                    for (Element scopeElement : XMLUtils.getChildElementList(repoElement, "scope")) {
                        scopes.add(scopeElement.getAttribute("group"));
                    }
                    repo.setScopes(scopes);

                    repositories.add(repo);
                }

                repo.setOrder(CommonUtils.toInt(repoElement.getAttribute("order")));
                repo.setEnabled(CommonUtils.toBoolean(repoElement.getAttribute("enabled")));

                DBSSecretController secrets = DBSSecretController.getGlobalSecretController();
                String authUser = secrets.getPrivateSecretValue("maven/" + repoID + "/auth-user");
                String authPassword = secrets.getPrivateSecretValue("maven/" + repoID + "/auth-password");

                // Backward compatibility
                if (CommonUtils.isEmpty(authUser)) {
                    authUser = repoElement.getAttribute("auth-user");
                    authPassword = repoElement.getAttribute("auth-password");
                    if (!CommonUtils.isEmpty(authPassword)) {
                        authPassword = SimpleStringEncrypter.INSTANCE.decrypt(authPassword);
                    }
                }

                if (CommonUtils.isNotEmpty(authUser)) {
                    repo.getAuthInfo().setUserName(authUser);
                    if (CommonUtils.isNotEmpty(authPassword)) {
                        repo.getAuthInfo().setUserPassword(authPassword);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing maven repositories configuration", e);
        }
    }

    @NotNull
    public List<MavenRepository> getRepositories() {
        return repositories;
    }

    @Nullable
    public MavenArtifactVersion findArtifact(@NotNull DBRProgressMonitor monitor, @Nullable MavenArtifactVersion owner, @NotNull MavenArtifactReference ref) {
        String fullId = ref.getId();
        MavenArtifactVersion notFoundVersion = notFoundArtifacts.get(fullId);
        if (notFoundVersion != null) {
            return notFoundVersion;
        }
        MavenArtifactVersion artifact = findInRepositories(monitor, owner, ref);
        if (artifact != null) {
            return artifact;
        }

        // Not found
        notFoundVersion = MavenArtifactVersion.createInvalidVersion(
            new MavenArtifact(
                MavenRepository.UnknownRepository,
                ref.getGroupId(),
                ref.getArtifactId(),
                ref.getClassifier(),
                ref.getFallbackVersion()),
            ref.getVersion());
        notFoundArtifacts.put(fullId, notFoundVersion);
        return notFoundVersion;
    }

    public void resetArtifactInfo(MavenArtifactReference artifactReference) {
        notFoundArtifacts.remove(artifactReference.getId());

        for (MavenRepository repository : repositories) {
            repository.resetArtifactCache(artifactReference);
        }
        localRepository.resetArtifactCache(artifactReference);
    }

    @Nullable
    private MavenArtifactVersion findInRepositories(@NotNull DBRProgressMonitor monitor, MavenArtifactVersion owner, @NotNull MavenArtifactReference ref) {
        MavenRepository currentRepository = owner == null ? null : owner.getArtifact().getRepository();
        if (currentRepository != null) {
            MavenArtifactVersion artifact = currentRepository.findArtifact(monitor, ref);
            if (artifact != null) {
                return artifact;
            }
        }

        // Try all available repositories (without resolve)
        for (MavenRepository repository : repositories) {
            if (!repository.isEnabled()) {
                continue;
            }
            if (repository != currentRepository) {
                if (!repository.getScopes().isEmpty()) {
                    // Check scope (group id)
                    if (!repository.getScopes().contains(ref.getGroupId())) {
                        continue;
                    }
                }
                MavenArtifactVersion artifact = repository.findArtifact(monitor, ref);
                if (artifact != null) {
                    return artifact;
                }
            }
        }
        if (owner != null) {
            // Try context repositories
            for (MavenRepository repository : owner.getActiveRepositories()) {
                if (repository != currentRepository) {
                    MavenArtifactVersion artifact = repository.findArtifact(monitor, ref);
                    if (artifact != null) {
                        return artifact;
                    }
                }
            }
        }

        if (localRepository != currentRepository) {
            MavenArtifactVersion artifact = localRepository.findArtifact(monitor, ref);
            if (artifact != null) {
                return artifact;
            }
        }

        log.warn("Maven artifact '" + ref + "' not found in any available repository.");

        return null;
    }

    public MavenRepository findRepository(String id) {
        for (MavenRepository repository : repositories) {
            if (repository.getId().equals(id)) {
                return repository;
            }
        }
        return null;
    }

    public void saveConfiguration() throws DBException, IOException {
        sortRepositories();

        DBSSecretController secrets = DBSSecretController.getGlobalSecretController();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLBuilder xml = new XMLBuilder(baos, GeneralUtils.UTF8_ENCODING);
        xml.setButify(true);
        try (final XMLBuilder.Element e1 = xml.startElement("maven")) {
            for (MavenRepository repository : repositories) {
                try (final XMLBuilder.Element e2 = xml.startElement("repository")) {
                    xml.addAttribute("id", repository.getId());
                    xml.addAttribute("order", repository.getOrder());
                    xml.addAttribute("enabled", repository.isEnabled());
                    if (repository.getType() != MavenRepository.RepositoryType.GLOBAL) {
                        xml.addAttribute("url", repository.getUrl());
                        xml.addAttribute("name", repository.getName());
                        if (!CommonUtils.isEmpty(repository.getDescription())) {
                            xml.addAttribute("description", repository.getDescription());
                        }
                        for (String scope : repository.getScopes()) {
                            try (final XMLBuilder.Element e3 = xml.startElement("scope")) {
                                xml.addAttribute("group", scope);
                            }
                        }
                        xml.addAttribute(RegistryConstants.ATTR_SNAPSHOT, repository.isSnapshot());
                        final DBPAuthInfo authInfo = repository.getAuthInfo();
                        if (!CommonUtils.isEmpty(authInfo.getUserName())) {
                            secrets.setPrivateSecretValue("maven/" + repository.getId() + "/auth-user", authInfo.getUserName());
                            if (!CommonUtils.isEmpty(authInfo.getUserPassword())) {
                                secrets.setPrivateSecretValue("maven/" + repository.getId() + "/auth-password", authInfo.getUserPassword());
                            }
                            secrets.flushChanges();
                        }
                    }
                }
            }
        }
        xml.flush();

        DBWorkbench.getPlatform().getConfigurationController().saveConfigurationFile(
            MAVEN_REPOSITORIES_CONFIG,
            baos.toString(StandardCharsets.UTF_8));
    }

    private void sortRepositories() {
        repositories.sort(Comparator.comparingInt(MavenRepository::getOrder));
    }

}
