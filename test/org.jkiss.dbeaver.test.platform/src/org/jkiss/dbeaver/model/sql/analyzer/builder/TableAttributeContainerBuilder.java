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
package org.jkiss.dbeaver.model.sql.analyzer.builder;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItem;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TableAttributeContainerBuilder extends Builder<DBSTable, DBSEntityAttribute> {
    private final DBSTable entity;
    private final List<DBSTableForeignKey> associations = new ArrayList<>();

    public TableAttributeContainerBuilder(@NotNull DBPDataSource dataSource, @NotNull DBSObject parent, @NotNull String name) throws DBException {
        super(dataSource, parent);
        this.entity = mock(DBSTable.class);
        when(entity.getDataSource()).thenReturn(dataSource);
        when(entity.getParentObject()).thenReturn(parent);
        when(entity.getName()).thenReturn(name);
        when(entity.getEntityType()).thenReturn(DBSEntityType.TABLE);
        when(entity.getAttributes(any())).then(x -> children);
        when(entity.getAttribute(any(), any())).then(x -> DBUtils.findObject(children, x.getArgument(1, String.class)));
        when(entity.getAssociations(any())).then(x -> associations);
        when(entity.getFullyQualifiedName(any()))
            .then(x -> DBUtils.getFullyQualifiedName(
                dataSource,
                SQLQueryCompletionItem.prepareQualifiedNameParts(entity, null).toArray(String[]::new)
            ));
    }

    public TableAttributeContainerBuilder(@NotNull DBPDataSource dataSource, @NotNull String name) throws DBException {
        this(dataSource, dataSource, name);
    }

    @NotNull
    public TableAttributeContainerBuilder attribute(@NotNull String name) {
        final DBSEntityAttribute attribute = mock(DBSEntityAttribute.class);
        when(attribute.getDataSource()).thenReturn(dataSource);
        when(attribute.getParentObject()).thenReturn(entity);
        when(attribute.getName()).thenReturn(name);
        when(attribute.getTypeName()).thenReturn("Unknown");
        when(attribute.getDataKind()).thenReturn(DBPDataKind.UNKNOWN);
        children.add(attribute);
        return this;
    }

    /**
     * Registers a foreign key constraint from this table's attribute to the target table's attribute,
     * the same way a real database exposes it on the owning table only.
     */
    @NotNull
    public TableAttributeContainerBuilder foreignKey(
        @NotNull String attributeName,
        @NotNull TableAttributeContainerBuilder targetTable,
        @NotNull String targetAttributeName
    ) throws DBException {
        final DBSEntityAttribute sourceAttr = DBUtils.findObject(children, attributeName);
        final DBSEntityAttribute targetAttr = DBUtils.findObject(targetTable.children, targetAttributeName);
        if (sourceAttr == null || targetAttr == null) {
            throw new IllegalArgumentException(
                "Can't resolve foreign key columns " + attributeName + " -> " + targetAttributeName);
        }

        final DBSTableForeignKeyColumn fkColumn = mock(DBSTableForeignKeyColumn.class);
        when(fkColumn.getAttribute()).thenReturn(sourceAttr);
        when(fkColumn.getReferencedColumn()).thenReturn(targetAttr);

        final DBSTableForeignKey foreignKey = mock(DBSTableForeignKey.class);
        when(foreignKey.getName()).thenReturn("<unnamed>");
        when(foreignKey.getAttributeReferences(any())).then(x -> List.of(fkColumn));

        this.associations.add(foreignKey);
        return this;
    }

    @NotNull
    @Override
    public DBSTable build() {
        return entity;
    }
}
