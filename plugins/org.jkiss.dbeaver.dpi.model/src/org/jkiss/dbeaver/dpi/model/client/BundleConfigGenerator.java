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
package org.jkiss.dbeaver.dpi.model.client;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.loader.EquinoxClassLoader;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.dpi.DPIController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BundleConfigGenerator {


    public static void generateBundleConfig(List<Bundle> bundles) {
        Set<Bundle> dependencies = new LinkedHashSet<>();
        for (Bundle bundle : bundles) {
            collectBundleDependencies(bundle, dependencies);
        }
        System.out.println(dependencies);
    }

    private static void collectBundleDependencies(Bundle bundle, Set<Bundle> dependencies) {
        dependencies.add(bundle);

/*
        EquinoxFwAdminImpl qa = new EquinoxFwAdminImpl();
        qa.activate(bundle.getBundleContext());
        qa.launch(qa.getManipulator(), )
*/
    }

    public static BundleProcessConfig generateBundleConfig(DBRProgressMonitor monitor, DBPDataSourceContainer dataSourceContainer) throws IOException {
        BundleProcessConfig processConfig = new BundleProcessConfig(monitor, dataSourceContainer.getId());
        addBundleFromClass(dataSourceContainer.getDriver().getDataSourceProvider().getClass(), processConfig);
        if (!processConfig.isValid()) {
            throw new IOException("No OSGI bundles were configured");
        }

        addBundleFromClass(DPIController.class, processConfig);

        DBAAuthModel<DBAAuthCredentials> authModel = dataSourceContainer.getConnectionConfiguration().getAuthModel();
        addBundleFromClass(authModel.getClass(), processConfig);

        addBundleByName("org.jkiss.dbeaver.launcher", processConfig);
        addBundleByName("org.jkiss.dbeaver.dpi.app", processConfig);
        if (!DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            // Add slf4j adapter for desktop applications
            addBundleByName("org.jkiss.dbeaver.slf4j", processConfig);
        } else {
            addBundleByName("io.cloudbeaver.slf4j", processConfig);
        }
        addBundleByName("com.dbeaver.resources.drivers.jdbc", processConfig);

        processConfig.generateApplicationConfiguration();

        return processConfig;
    }

    private static void addBundleByName(String bundleName, BundleProcessConfig processConfig) {
        Bundle bundle = Platform.getBundle(bundleName);
        if (bundle instanceof EquinoxBundle) {
            ModuleWiring wiring = ((EquinoxBundle) bundle).getModule().getCurrentRevision().getWiring();
            if (wiring != null) {
                collectModuleWirings(wiring, processConfig);
            }
        }
    }


    private static void addBundleFromClass(Class<?> bundleClass, BundleProcessConfig processConfig) {
        ClassLoader classLoader = bundleClass.getClassLoader();
        if (classLoader instanceof EquinoxClassLoader) {
            ModuleWiring wiring = ((EquinoxClassLoader) classLoader).getBundleLoader().getWiring();
            if (wiring != null) {
                collectModuleWirings(wiring, processConfig);
            }
        }
    }

    private static void collectModuleWirings(ModuleWiring wiring, BundleProcessConfig processConfig) {
        if (processConfig.containsWiring(wiring)) {
            return;
        }
        processConfig.addWiring(wiring);
        List<ModuleWire> requiredModuleWires = wiring.getRequiredModuleWires("osgi.wiring.bundle");
        for (ModuleWire dWire : requiredModuleWires) {
            ModuleWiring providerWiring = dWire.getProviderWiring();
            collectModuleWirings(providerWiring, processConfig);
        }
        for (ModuleWire dWire : wiring.getRequiredModuleWires("osgi.wiring.package")) {
            ModuleWiring providerWiring = dWire.getProviderWiring();
            collectModuleWirings(providerWiring, processConfig);
        }
    }

}
