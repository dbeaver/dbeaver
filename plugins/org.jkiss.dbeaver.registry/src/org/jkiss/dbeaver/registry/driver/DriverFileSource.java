/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriverFileInfo;
import org.jkiss.dbeaver.model.connection.DBPDriverFileSource;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DriverFileSource
 */
public class DriverFileSource implements DBPDriverFileSource
{
    private static final Log log = Log.getLog(DriverFileSource.class);

    public static class FileInfo implements DBPDriverFileInfo {
        private final String name;
        private final String description;
        private final boolean optional;

        FileInfo(IConfigurationElement config) {
            this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
            this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
            this.optional = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_OPTIONAL), false);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
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

    public DriverFileSource(DriverFileSource copyFrom) {
        this.url = copyFrom.url;
        this.name = copyFrom.name;
        this.instructions = copyFrom.instructions;
        this.files.addAll(copyFrom.files);
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getInstructions() {
        return instructions;
    }

    @Override
    public List<FileInfo> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        return url;
    }

}
