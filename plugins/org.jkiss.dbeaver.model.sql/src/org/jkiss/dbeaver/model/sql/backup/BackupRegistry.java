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
package org.jkiss.dbeaver.model.sql.backup;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLDialect;

import java.util.ArrayList;
import java.util.List;

public class BackupRegistry {
    public static final String SQL_BACKUP_EXTENSION_ID = "org.jkiss.dbeaver.sqlBackup";
    private static BackupRegistry instance = null;

    private final List<BackupSettingDescriptor> descriptors = new ArrayList<>();

    public synchronized static BackupRegistry getInstance() {
        if (instance == null) {
            instance = new BackupRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private BackupRegistry() {
    }

    private synchronized void loadExtensions(IExtensionRegistry registry) {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(SQL_BACKUP_EXTENSION_ID);

        for (IConfigurationElement ext : extConfigs) {
            if (BackupSettingDescriptor.TAG_BACKUP.equals(ext.getName())) {
                parseAttribute(ext);
            }
        }
    }

    private void parseAttribute(IConfigurationElement ext) {
        BackupSettingDescriptor providerDescriptor = new BackupSettingDescriptor(ext);
        this.descriptors.add(providerDescriptor);
    }

    public List<BackupSettingDescriptor> getDescriptors() {
        return new ArrayList<>(descriptors);
    }

    public BackupSettingDescriptor getCurrentDescriptor(@NotNull SQLDialect sqlDialect) {
        for (BackupSettingDescriptor setting : getDescriptors()) {
            if (setting.getDialect().getObjectClass().isInstance(sqlDialect)) {
                return setting;
            }
        }
        return null;
    }

}
