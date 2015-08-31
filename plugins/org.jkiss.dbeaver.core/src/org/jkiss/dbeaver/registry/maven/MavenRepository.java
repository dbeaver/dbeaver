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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.registry.RegistryConstants;

import java.io.File;
import java.io.IOException;

/**
 * Maven repository manager.
 */
public class MavenRepository
{
    static final Log log = Log.getLog(MavenRepository.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.mavenRepository";

    private String id;
    private String name;
    private String url;

    public MavenRepository(IConfigurationElement config)
    {
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.url = config.getAttribute(RegistryConstants.ATTR_URL);
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public MavenArtifact getArtifact(String groupId, String artifactId) {
        try {
            return new MavenArtifact(this, groupId, artifactId);
        } catch (IOException e) {
            log.debug(e);
            return null;
        }
    }

    public File getCacheLocation()
    {
        File homeFolder = new File(DBeaverActivator.getInstance().getStateLocation().toFile(), "maven/" + id + "/cache");
        if (!homeFolder.exists()) {
            if (!homeFolder.mkdirs()) {
                log.warn("Can't create maven repository '" + name + "' cache folder '" + homeFolder + "'");
            }
        }

        return homeFolder;
    }

}
