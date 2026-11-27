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
import org.jkiss.dbeaver.ext.generic.model.GenericScriptObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Firebird exception: a named, user-defined error message (CREATE EXCEPTION), stored in RDB$EXCEPTIONS.
 */
public class FireBirdException implements GenericScriptObject, DBPQualifiedObject, DBPSystemObject {

    @NotNull
    private final FireBirdDataSource dataSource;
    @NotNull
    private final String name;
    private final int number;
    @Nullable
    private final String message;
    @Nullable
    private final String ownerName;
    @Nullable
    private final String description;
    private final boolean system;

    public FireBirdException(
        @NotNull FireBirdDataSource dataSource,
        @NotNull String name,
        int number,
        @Nullable String message,
        @Nullable String ownerName,
        @Nullable String description,
        boolean system
    ) {
        this.dataSource = dataSource;
        this.name = name;
        this.number = number;
        this.message = message;
        this.ownerName = ownerName;
        this.description = description;
        this.system = system;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public int getNumber() {
        return number;
    }

    @Nullable
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 3)
    public String getMessage() {
        return message;
    }

    @Nullable
    @Property(viewable = true, order = 4)
    public String getOwnerName() {
        return ownerName;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isSystem() {
        return system;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public FireBirdDataSource getParentObject() {
        return dataSource;
    }

    @NotNull
    @Override
    public FireBirdDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getQuotedIdentifier(this);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) {
        String quotedName = getFullyQualifiedName(DBPEvaluationContext.DDL);
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE EXCEPTION ").append(quotedName)
            .append(' ').append(SQLUtils.quoteString(this, CommonUtils.notEmpty(message))).append(';');
        if (!CommonUtils.isEmpty(description)) {
            ddl.append("\n\nCOMMENT ON EXCEPTION ").append(quotedName)
                .append(" IS ").append(SQLUtils.quoteString(this, description)).append(';');
        }
        return ddl.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}
