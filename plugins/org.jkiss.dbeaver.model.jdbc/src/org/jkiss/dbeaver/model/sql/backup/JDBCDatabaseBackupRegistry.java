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

public class JDBCDatabaseBackupRegistry {
    public static final String SQL_BACKUP_EXTENSION_ID = "org.jkiss.dbeaver.sqlBackup";
    private static JDBCDatabaseBackupRegistry instance = null;

    private final List<JDBCDatabaseBackupDescriptor> descriptors = new ArrayList<>();

    public synchronized static JDBCDatabaseBackupRegistry getInstance() {
        if (instance == null) {
            instance = new JDBCDatabaseBackupRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private JDBCDatabaseBackupRegistry() {
    }

    private synchronized void loadExtensions(@NotNull IExtensionRegistry registry) {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(SQL_BACKUP_EXTENSION_ID);

        for (IConfigurationElement ext : extConfigs) {
            if (JDBCDatabaseBackupDescriptor.TAG_BACKUP.equals(ext.getName())) {
                parseAttribute(ext);
            }
        }
    }

    private void parseAttribute(@NotNull IConfigurationElement ext) {
        JDBCDatabaseBackupDescriptor providerDescriptor = new JDBCDatabaseBackupDescriptor(ext);
        this.descriptors.add(providerDescriptor);
    }

    public List<JDBCDatabaseBackupDescriptor> getDescriptors() {
        return new ArrayList<>(descriptors);
    }

    public JDBCDatabaseBackupDescriptor getCurrentDescriptor(@NotNull SQLDialect sqlDialect) {
        for (JDBCDatabaseBackupDescriptor descriptor : getDescriptors()) {
            if (sqlDialect.getDialectId().equals(descriptor.getDialect())) {
                return descriptor;
            }
        }
        return null;
    }
}
