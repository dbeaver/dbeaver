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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

/**
 * Firebird foreign key.
 * Firebird supports the rules NO ACTION (the default), CASCADE, SET NULL and SET DEFAULT, but not RESTRICT,
 * and its foreign keys are never deferrable.
 */
public class FireBirdTableForeignKey extends GenericTableForeignKey {

    public FireBirdTableForeignKey(
        @NotNull GenericTableBase table,
        @Nullable String name,
        @Nullable String remarks,
        @Nullable DBSEntityReferrer referencedKey,
        @Nullable DBSForeignKeyModifyRule deleteRule,
        @Nullable DBSForeignKeyModifyRule updateRule,
        @Nullable DBSForeignKeyDeferability deferability,
        boolean persisted
    ) {
        super(table, name, remarks, referencedKey, deleteRule, updateRule, deferability, persisted);
    }

    /**
     * Firebird foreign keys are never deferrable, so the deferability is not shown.
     */
    @Override
    @Property(hidden = true)
    public DBSForeignKeyDeferability getDeferability() {
        return super.getDeferability();
    }
}
