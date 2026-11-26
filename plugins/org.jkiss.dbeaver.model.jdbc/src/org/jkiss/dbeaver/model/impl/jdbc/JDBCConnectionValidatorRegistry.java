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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.util.HashMap;
import java.util.Map;

class JDBCConnectionValidatorRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.connectionValidator";
    private static JDBCConnectionValidatorRegistry instance;

    private final Map<String, JDBCConnectionValidatorDescriptor> validators = new HashMap<>();

    private JDBCConnectionValidatorRegistry() {
        for (IConfigurationElement config : Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID)) {
            if ("validator".equals(config.getName())) {
                validators.put(config.getAttribute("id"), new JDBCConnectionValidatorDescriptor(config));
            }
        }
    }

    @NotNull
    static synchronized JDBCConnectionValidatorRegistry getInstance() {
        if (instance == null) {
            instance = new JDBCConnectionValidatorRegistry();
        }
        return instance;
    }

    @NotNull
    JDBCConnectionValidator createValidator(@NotNull String id) throws DBCException {
        JDBCConnectionValidatorDescriptor descriptor = validators.get(id);
        if (descriptor == null) {
            throw new DBCException("Connection validator '" + id + "' not found");
        }
        try {
            return descriptor.createValidator();
        } catch (DBException e) {
            // A declared validator must never be silently skipped if its contribution is broken.
            throw new DBCException("Cannot create connection validator '" + id + "'", e);
        }
    }
}
