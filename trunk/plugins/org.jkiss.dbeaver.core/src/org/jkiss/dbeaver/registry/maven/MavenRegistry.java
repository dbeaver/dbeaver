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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.List;

public class MavenRegistry
{
    static final Log log = Log.getLog(MavenRegistry.class);

    private static MavenRegistry instance = null;

    public synchronized static MavenRegistry getInstance()
    {
        if (instance == null) {
            instance = new MavenRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<MavenRepository> repositories = new ArrayList<MavenRepository>();

    private MavenRegistry()
    {
    }

    public void loadExtensions(IExtensionRegistry registry)
    {
        // Load data type providers from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(MavenRepository.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                MavenRepository repository = new MavenRepository(ext);
                repositories.add(repository);
            }
        }
    }

    public List<MavenRepository> getRepositories() {
        return repositories;
    }

    public MavenArtifact findArtifact(String mavenUri) {
        int divPos = mavenUri.indexOf('/');
        if (divPos < 0) {
            log.warn("Bad maven uri: " + mavenUri);
            return null;
        }
        mavenUri = mavenUri.substring(divPos + 1);
        divPos = mavenUri.indexOf(':');
        if (divPos < 0) {
            log.warn("Bad maven uri, no group id: " + mavenUri);
            return null;
        }
        String groupId = mavenUri.substring(0, divPos);
        int divPos2 = mavenUri.indexOf(':', divPos + 1);
        if (divPos2 < 0) {
            log.warn("Bad maven uri, no artifact id: " + mavenUri);
            return null;
        }
        String artifactId = mavenUri.substring(divPos + 1, divPos2);
        return findArtifact(groupId, artifactId);
    }

    public MavenArtifact findArtifact(String groupId, String artifactId) {
        for (MavenRepository repository : repositories) {
            MavenArtifact artifact = repository.getArtifact(groupId, artifactId);
            if (artifact != null) {
                return artifact;
            }
        }
        return null;
    }

}
