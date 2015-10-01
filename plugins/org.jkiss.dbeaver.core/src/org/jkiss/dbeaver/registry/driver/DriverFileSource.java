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
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DriverFileSource
 */
public class DriverFileSource
{
    static final Log log = Log.getLog(DriverFileSource.class);

    public static class FileInfo {
        private final String name;
        private final String description;
        private final boolean optional;

        FileInfo(IConfigurationElement config) {
            this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
            this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
            this.optional = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_OPTIONAL), false);
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isOptional() {
            return optional;
        }
    }

    private final String url;
    private final String name;
    private final String instructions;
    private final List<FileInfo> files = new ArrayList<FileInfo>();

    DriverFileSource(IConfigurationElement config) {
        this.url = config.getAttribute(RegistryConstants.ATTR_URL);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.instructions = config.getAttribute("instructions");
        for (IConfigurationElement cfg : config.getChildren(RegistryConstants.TAG_FILE)) {
            files.add(new FileInfo(cfg));
        }
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getInstructions() {
        return instructions;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        return url;
    }

}
