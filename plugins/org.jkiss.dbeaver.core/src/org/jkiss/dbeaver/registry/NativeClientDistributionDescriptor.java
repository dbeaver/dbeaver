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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * NativeClientDistributionDescriptor
 */
public class NativeClientDistributionDescriptor {
    private final List<NativeClientFileDescriptor> files = new ArrayList<>();
    private OSDescriptor os;
    private String targetPath;

    public NativeClientDistributionDescriptor(IConfigurationElement config) {
        String osName = config.getAttribute(RegistryConstants.ATTR_OS);
        this.os = osName == null ? null : new OSDescriptor(
            osName,
            config.getAttribute(RegistryConstants.ATTR_ARCH));

        this.targetPath = config.getAttribute("targetPath");
        for (IConfigurationElement fileElement : config.getChildren("file")) {
            if (DriverUtils.matchesBundle(fileElement)) {
                this.files.add(new NativeClientFileDescriptor(fileElement));
            }
        }
    }

    public OSDescriptor getOs() {
        return os;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public boolean downloadFiles(DBRProgressMonitor monitor) throws DBException {
        return true;
    }

    @Override
    public String toString() {
        return os.toString();
    }
}
