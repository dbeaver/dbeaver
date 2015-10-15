/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.registry.maven;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.connection.DBPDriverContext;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maven repository manager.
 */
public class MavenRepository
{
    static final Log log = Log.getLog(MavenRepository.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.mavenRepository";

    public static final String METADATA_CACHE_FILE = "metadata-cache.xml";

    public static final String TAG_CACHE = "cache";
    public static final String TAG_ARTIFACT = "artifact";
    public static final String TAG_VERSION = "version";

    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_URL = "url";
    public static final String ATTR_GROUP_ID = "groupId";
    public static final String ATTR_ARTIFACT_ID = "artifactId";
    public static final String ATTR_VERSION = "version";

    public enum RepositoryType {
        GLOBAL,     // Globally defined repositories (came from plugin.xml)
        LOCAL,      // Local (deployed locally) repository. It is singleton
        CUSTOM,     // User-defined repository
        EXTERNAL    // POM-defined repository
    }

    private final String id;
    private final String name;
    private final String url;
    private final RepositoryType type;

    private transient volatile boolean needsToSave = false;

    private Map<String, MavenArtifact> cachedArtifacts = new LinkedHashMap<>();

    public MavenRepository(IConfigurationElement config)
    {
        this(
            config.getAttribute(RegistryConstants.ATTR_ID),
            config.getAttribute(RegistryConstants.ATTR_NAME),
            config.getAttribute(RegistryConstants.ATTR_URL),
            RepositoryType.GLOBAL);
    }

    public MavenRepository(String id, String name, String url, RepositoryType type) {
        this.id = id;
        this.name = CommonUtils.isEmpty(name) ? id : name;
        if (!url.endsWith("/")) url += "/";
        this.url = url;
        this.type = type;
    }

    public void flushCache() {
        needsToSave = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public RepositoryType getType() {
        return type;
    }

    @Nullable
    public synchronized MavenArtifactVersion findArtifact(DBPDriverContext context, @NotNull MavenArtifactReference ref) {
        boolean newArtifact = false;
        MavenArtifact artifact = cachedArtifacts.get(ref.getId());
        if (artifact == null) {
            artifact = new MavenArtifact(this, ref.getGroupId(), ref.getArtifactId());
            newArtifact = true;
        }
        try {
            MavenArtifactVersion version = artifact.resolveVersion(context, ref.getVersion());
            if (newArtifact) {
                cachedArtifacts.put(ref.getId(), artifact);
                flushCache();
            }
            return version;
        } catch (IOException e) {
            // Generally it is ok. Artifact not present in this repository
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
        File homeFolder = new File(DBeaverActivator.getInstance().getStateLocation().toFile(), "maven/" + extPath);
        if (!homeFolder.exists()) {
            if (!homeFolder.mkdirs()) {
                log.warn("Can't create maven repository '" + name + "' cache folder '" + homeFolder + "'");
            }
        }

        return homeFolder;
    }

/*
    synchronized void loadCache() {
        File cacheFile = new File(getLocalCacheDir(), METADATA_CACHE_FILE);
        if (!cacheFile.exists()) {
            return;
        }
        try (final DBPDriverContext context = new DBPDriverContext(VoidProgressMonitor.INSTANCE)) {
            InputStream mdStream = new FileInputStream(cacheFile);
            try {
                SAXReader reader = new SAXReader(mdStream);
                reader.parse(new SAXListener() {
                    MavenArtifact lastArtifact;
                    @Override
                    public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) throws XMLException {
                        if (TAG_ARTIFACT.equals(localName)) {
                            lastArtifact = new MavenArtifact(
                                MavenRepository.this,
                                atts.getValue(ATTR_GROUP_ID),
                                atts.getValue(ATTR_ARTIFACT_ID));
                            cachedArtifacts.put(
                                MavenArtifactReference.makeId(lastArtifact.getGroupId(), lastArtifact.getArtifactId()),
                                lastArtifact);
                        } else if (TAG_VERSION.equals(localName) && lastArtifact != null) {
                            String versionNumber = atts.getValue(ATTR_VERSION);
                            try {
                                MavenArtifactVersion version = new MavenArtifactVersion(
                                    context,
                                    lastArtifact,
                                    versionNumber
                                );
                                lastArtifact.addVersion(version);
                            } catch (IOException e) {
                                log.warn("Error loading cached artifact version " + lastArtifact + ":" + versionNumber, e);
                            }
                        }
                    }
                    @Override
                    public void saxText(SAXReader reader, String data) throws XMLException {
                    }

                    @Override
                    public void saxEndElement(SAXReader reader, String namespaceURI, String localName) throws XMLException {
                        if (TAG_ARTIFACT.equals(localName)) {
                            lastArtifact = null;
                        }
                    }
                });
            } catch (XMLException e) {
                log.warn("Error parsing cached Maven repository '" + id + "'", e);
            } finally {
                IOUtils.close(mdStream);
            }
        } catch (IOException e) {
            log.warn("IO error while reading cached Maven repository '" + id + "'", e);
        }
    }

    void saveCacheIfNeeded() {
        if (needsToSave) {
            saveCache();
            needsToSave = false;
        }
    }

    synchronized void saveCache() {
        try {
            File cacheDir = getLocalCacheDir();
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    throw new IOException("Can't create cache directory '" + cacheDir.getAbsolutePath() + "'");
                }
            }
            File cacheFile = new File(cacheDir, METADATA_CACHE_FILE);
            OutputStream out = new FileOutputStream(cacheFile);
            try {
                XMLBuilder xml = new XMLBuilder(out, "utf-8");
                xml.setButify(true);
                try (XMLBuilder.Element e = xml.startElement(TAG_CACHE)) {
                    xml.addAttribute(ATTR_ID, id);
                    xml.addAttribute(ATTR_NAME, name);
                    xml.addAttribute(ATTR_URL, url);

                    for (MavenArtifact artifact : cachedArtifacts.values()) {
                        try (XMLBuilder.Element e1 = xml.startElement(TAG_ARTIFACT)) {
                            xml.addAttribute(ATTR_GROUP_ID, artifact.getGroupId());
                            xml.addAttribute(ATTR_ARTIFACT_ID, artifact.getArtifactId());
                        }
                    }
                }

                xml.flush();
            } finally {
                IOUtils.close(out);
            }
        } catch (IOException e) {
            log.warn("Error saving local Maven cache", e);
        }
    }
*/

    @Override
    public String toString() {
        return url;
    }

}
