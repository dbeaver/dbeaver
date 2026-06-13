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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

public class TiberoTableConstraint extends TiberoTableConstraintBase {

    private static final Log log = Log.getLog(TiberoTableConstraint.class);

    private String searchCondition;

    public TiberoTableConstraint(
        @NotNull TiberoTable table,
        @NotNull String name,
        @NotNull DBSEntityConstraintType type,
        @Nullable String searchCondition,
        boolean persisted
    ) {
        super(table, name, type, persisted);
        this.searchCondition = searchCondition;
    }

    @Property(viewable = true, editable = true, order = 4)
    @Nullable
    public String getSearchCondition() {
        return searchCondition;
    }

    public void setSearchCondition(String searchCondition) {
        this.searchCondition = searchCondition;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(
            getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    @NotNull
    public static DBSEntityConstraintType getConstraintType(@Nullable String code) {
        if (code == null) {
            return DBSEntityConstraintType.CHECK;
        }
        return switch (code) {
            case "P" -> DBSEntityConstraintType.PRIMARY_KEY;
            case "U" -> DBSEntityConstraintType.UNIQUE_KEY;
            case "C" -> DBSEntityConstraintType.CHECK;
            default -> {
                log.debug("Unsupported Tibero constraint type: " + code);
                yield DBSEntityConstraintType.CHECK;
            }
        };
    }
}
