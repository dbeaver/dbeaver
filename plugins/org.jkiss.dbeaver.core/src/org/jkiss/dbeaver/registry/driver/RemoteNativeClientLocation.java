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
package org.jkiss.dbeaver.registry.driver;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.NativeClientDescriptor;
import org.jkiss.dbeaver.registry.NativeClientDistributionDescriptor;

import java.io.File;

/**
 * LocalNativeClientLocation
 */
public class RemoteNativeClientLocation implements DBPNativeClientLocation {
    private final NativeClientDescriptor clientDescriptor;

    public RemoteNativeClientLocation(NativeClientDescriptor clientDescriptor) {
        this.clientDescriptor = clientDescriptor;
    }

    @Override
    public String getName() {
        return clientDescriptor.getId();
    }

    @Override
    public File getPath() {
        NativeClientDistributionDescriptor distribution = clientDescriptor.findDistribution();
        if (distribution != null) {
            File driversHome = DriverDescriptor.getCustomDriversHome();
            return new File(driversHome, distribution.getTargetPath());
        }
        return new File(getName());
    }

    @Override
    public String getDisplayName() {
        return clientDescriptor.getLabel();
    }

    @Override
    public boolean validateFilesPresence(DBRProgressMonitor progressMonitor) throws DBException {
        NativeClientDistributionDescriptor distribution = clientDescriptor.findDistribution();
        if (distribution != null) {
            return distribution.downloadFiles(progressMonitor);
        }
        return false;
    }

}
