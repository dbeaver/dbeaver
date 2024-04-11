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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

public class JDBCDatabaseBackupDescriptor extends AbstractDescriptor {
    @Nullable
    private JDBCDatabaseBackupHandler instance;
    @NotNull
    private final ObjectType classType;

    @NotNull
    private final String dialect;

    public static String TAG_BACKUP = "backup";

    public JDBCDatabaseBackupDescriptor(@NotNull IConfigurationElement cfg) {
        super(cfg);
        this.classType = new ObjectType(cfg, "class");
        this.dialect = cfg.getAttribute("dialect");
    }

    @NotNull
    public JDBCDatabaseBackupHandler getInstance() {
        if (instance == null) {
            try {
                instance = classType.createInstance(JDBCDatabaseBackupHandler.class);
            } catch (DBException e) {
                throw new IllegalStateException("Can not instantiate backup '" + classType.getImplName() + "'", e);
            }
        }
        return instance;
    }

    @Nullable
    public String getDialect() {
        return dialect;
    }
}
