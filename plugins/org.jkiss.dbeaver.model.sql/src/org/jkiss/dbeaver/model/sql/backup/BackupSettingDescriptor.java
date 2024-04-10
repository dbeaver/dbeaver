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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BackupSettingDescriptor extends AbstractDescriptor {
    private BackupDatabase instance;
    private final ObjectType implType;

    private final ObjectType dialect;

    public static String TAG_BACKUP = "backup";

    public BackupSettingDescriptor(@NotNull IConfigurationElement cfg) {
        super(cfg);
        this.implType = new ObjectType(cfg, "class");
        this.dialect = new ObjectType(cfg, "dialectClass");
    }

    @NotNull
    public BackupDatabase getInstance() {
        if (instance == null) {
            try {
                instance = implType.createInstance(BackupDatabase.class);
            } catch (DBException e) {
                throw new IllegalStateException("Can not instantiate backup '" + implType.getImplName() + "'", e);
            }
        }
        return instance;
    }

    public ObjectType getDialect() {
        return dialect;
    }
}
