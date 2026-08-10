/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sync;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Components declared as synchronizable. Contributions are read on first use.
 */
public class DBPSyncRegistry {

    private static final Log log = Log.getLog(DBPSyncRegistry.class);

    private static final String EXTENSION_ID = "org.jkiss.dbeaver.syncUnit";
    private static final String TAG_UNIT = "unit";
    private static final String ATTR_CLASS = "class";

    private static DBPSyncRegistry instance;

    public static synchronized DBPSyncRegistry getInstance() {
        if (instance == null) {
            instance = new DBPSyncRegistry();
        }
        return instance;
    }

    private final List<DBPSyncUnit> units = new ArrayList<>();

    private DBPSyncRegistry() {
        IConfigurationElement[] elements = Platform.getExtensionRegistry()
            .getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement element : elements) {
            if (!TAG_UNIT.equals(element.getName())) {
                continue;
            }
            try {
                units.add((DBPSyncUnit) element.createExecutableExtension(ATTR_CLASS));
            } catch (CoreException e) {
                log.error("Error creating synchronization unit " + element.getAttribute(ATTR_CLASS), e);
            }
        }
    }

    @NotNull
    public List<DBPSyncUnit> getUnits() {
        return units;
    }

    @Nullable
    public DBPSyncUnit findById(@NotNull String id) {
        for (DBPSyncUnit unit : units) {
            if (unit.getId().equals(id)) {
                return unit;
            }
        }
        return null;
    }
}
