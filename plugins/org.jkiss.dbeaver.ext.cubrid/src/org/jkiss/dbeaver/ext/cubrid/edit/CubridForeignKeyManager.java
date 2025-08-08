/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableForeignKey;
import org.jkiss.dbeaver.ext.generic.edit.GenericForeignKeyManager;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

public class CubridForeignKeyManager extends GenericForeignKeyManager {

    @Override
    protected CubridTableForeignKey createDatabaseObject(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBECommandContext context,
            final Object container,
            Object from,
            @NotNull Map<String, Object> options) {
        CubridTable table = (CubridTable) container;
        CubridTableForeignKey foreignKey = new CubridTableForeignKey(
                table,
                null,
                null,
                null,
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyDeferability.NOT_DEFERRABLE,
                false);
        foreignKey.setName(getNewConstraintName(monitor, foreignKey));
        return foreignKey;
    }
}
