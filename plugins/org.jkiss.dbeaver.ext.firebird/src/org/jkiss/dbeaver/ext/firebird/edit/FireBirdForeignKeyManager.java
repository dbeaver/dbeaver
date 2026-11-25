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
package org.jkiss.dbeaver.ext.firebird.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.edit.GenericForeignKeyManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

/**
 * Firebird foreign key manager.
 */
public class FireBirdForeignKeyManager extends GenericForeignKeyManager {

    /**
     * Firebird has no RESTRICT rule (it is a syntax error). Its default NO ACTION checks the reference
     * immediately, which is the behavior of RESTRICT, so RESTRICT is written as NO ACTION by omitting the clause.
     * This also applies to foreign keys copied from other databases.
     */
    @Override
    protected void appendUpdateDeleteRule(@NotNull GenericTableForeignKey foreignKey, @NotNull StringBuilder decl) {
        appendRule(decl, " ON DELETE ", foreignKey.getDeleteRule());
        appendRule(decl, " ON UPDATE ", foreignKey.getUpdateRule());
    }

    private static void appendRule(@NotNull StringBuilder decl, @NotNull String prefix, @Nullable DBSForeignKeyModifyRule rule) {
        if (rule == null || rule == DBSForeignKeyModifyRule.RESTRICT || CommonUtils.isEmpty(rule.getClause())) {
            return;
        }
        decl.append(prefix).append(rule.getClause());
    }
}
