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
package org.jkiss.dbeaver.ext.dynamodb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;

import java.util.ArrayList;
import java.util.List;

public class DynamoDBKeyConstraint implements DBSEntityReferrer {

    private final DynamoDBTable table;
    private final List<DBSEntityAttributeRef> keyAttributes;

    public DynamoDBKeyConstraint(@NotNull DynamoDBTable table, @NotNull List<DynamoDBAttribute> keys) {
        this.table = table;
        this.keyAttributes = new ArrayList<>();
        for (DynamoDBAttribute attr : keys) {
            keyAttributes.add(new DynamoDBKeyAttributeRef(attr));
        }
    }

    @NotNull
    @Override
    public String getName() {
        return "PRIMARY_KEY";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "DynamoDB primary key";
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public DynamoDBTable getParentObject() {
        return table;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return table.getDataSource();
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType() {
        return DBSEntityConstraintType.PRIMARY_KEY;
    }

    @Nullable
    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(@Nullable DBRProgressMonitor monitor) throws DBException {
        return keyAttributes;
    }

    private static class DynamoDBKeyAttributeRef implements DBSEntityAttributeRef {
        private final DynamoDBAttribute attribute;

        DynamoDBKeyAttributeRef(DynamoDBAttribute attribute) {
            this.attribute = attribute;
        }

        @NotNull
        @Override
        public DBSEntityAttribute getAttribute() {
            return attribute;
        }
    }
}
