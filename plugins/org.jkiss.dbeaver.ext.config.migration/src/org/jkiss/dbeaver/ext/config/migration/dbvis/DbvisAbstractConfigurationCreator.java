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
package org.jkiss.dbeaver.ext.config.migration.dbvis;

import org.jkiss.dbeaver.ext.config.migration.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class DbvisAbstractConfigurationCreator implements DbvisConfigurationCreator {

    static Pattern PATTERN_PROTOCOL = Pattern.compile("<protocol>"); //$NON-NLS-1$
    static Pattern PATTERN_HOST = Pattern.compile("<server>"); //$NON-NLS-1$
    static Pattern PATTERN_PORT = Pattern.compile("<port([0-9]*)>"); //$NON-NLS-1$
    static Pattern PATTERN_DATABASE = Pattern.compile("<database>|<databaseName>|<sid>|<datasource>"); //$NON-NLS-1$

    protected void adaptSampleUrl(ImportDriverInfo driverInfo) {
        String port = null;
        String sampleURL = driverInfo.getSampleURL();
        sampleURL = PATTERN_PROTOCOL.matcher(sampleURL).replaceAll("{protocol}");
        sampleURL = PATTERN_HOST.matcher(sampleURL).replaceAll("{host}");
        final Matcher portMatcher = PATTERN_PORT.matcher(sampleURL);
        if (portMatcher.find()) {
            final String portString = portMatcher.group(1);
            if (!CommonUtils.isEmpty(portString)) {
                port = portString;
            }
        }
        sampleURL = portMatcher.replaceAll("{port}");
        sampleURL = PATTERN_DATABASE.matcher(sampleURL).replaceAll("{database}");

        driverInfo.setSampleURL(sampleURL);
        if (port != null) {
            driverInfo.setDefaultPort(port);
        }
    }

    public File getConfigurationAsset(File folder) {
        File configFile = null;
        File configFolder = new File(folder, getConfigurationFolder());
        if (configFolder.exists()) {
            configFile = new File(configFolder, getConfigurationFile());
            if (configFile.exists()) {
                return configFile;
            }
        }
        return configFile;
    }

    public DriverDescriptor getDriverByName(String name) {
        String driverName = substituteDriverName(name);
        final DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        List<DriverDescriptor> descriptors = registry.getDataSourceProviders().stream()
            .flatMap(provider -> provider.getEnabledDrivers().stream())
            .collect(Collectors.toList());
        Optional<DriverDescriptor> descriptor = descriptors.stream()
            .filter(d -> d.getName().equals(driverName))
            .findFirst();
        if (descriptor.isEmpty()) {
            descriptor = descriptors.stream()
                .filter(d -> d.getName().contains(driverName))
                .findFirst();
        }
        return descriptor.orElse(null);
    }

    protected abstract String substituteDriverName(String name);
}
