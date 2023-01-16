/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * NativeClientDescriptor
 */
public class NativeClientDescriptor extends AbstractDescriptor {

    private final List<NativeClientDistributionDescriptor> distributions = new ArrayList<>();
    private String id;
    private String label;

    NativeClientDescriptor(IConfigurationElement config) {
        super(config.getContributor().getName());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        for (IConfigurationElement clientElement : config.getChildren("dist")) {
            this.distributions.add(new NativeClientDistributionDescriptor(clientElement));
        }
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Clients can't work from an AppData folder, if app is the Windows store app we don't want to use any native
     * clients there.
     * This happens due to WinStore apps using their own ms-appdata:// instead of a normal one and no other executables
     * have access to it.
     * see #15361
     * https://docs.microsoft.com/en-us/windows/uwp/get-started/fileio-learning-track
     * @param clientFile client path
     * @return true if path is virtualized
     */
    private boolean isClientsPathVirtualizedByWindows(Path clientFile) {
        final DBPApplication application = DBWorkbench.getPlatform().getApplication();
        return (application.isStandalone()
            && !application.isHeadlessMode()
            && RuntimeUtils.isWindowsStoreApplication()
            && clientFile.startsWith(System.getenv("AppData")));
    }

    public NativeClientDistributionDescriptor findDistribution() {
        OSDescriptor localSystem = DBWorkbench.getPlatform().getLocalSystem();
        final Path driversHome = DriverDescriptor.getCustomDriversHome().toAbsolutePath();

        for (NativeClientDistributionDescriptor distr : distributions) {
            if (distr.getOs().matches(localSystem)
                && !isClientsPathVirtualizedByWindows(driversHome.resolve(distr.getTargetPath()))) {
                return distr;
            }
        }
        return null;

    }

    @Override
    public String toString() {
        return id;
    }

}
