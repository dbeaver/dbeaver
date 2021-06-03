/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TableContainerBuilder extends Builder<DBSObjectContainer, DBSEntity> {
    private final DBSObjectContainer container;

    public TableContainerBuilder(@NotNull DBPDataSource dataSource, @NotNull DBSObject parent, @NotNull String name) throws DBException {
        super(dataSource, parent);
        this.container = mock(DBSObjectContainer.class);
        when(container.getDataSource()).thenReturn(dataSource);
        when(container.getParentObject()).thenReturn(parent);
        when(container.getName()).thenReturn(name);
        when(container.getPrimaryChildType(any())).thenReturn(null);
        when(container.getChildren(any())).then(x -> children);
        when(container.getChild(any(), any())).then(x -> DBUtils.findObject(children, x.getArgumentAt(1, String.class)));
    }

    public TableContainerBuilder(@NotNull DBPDataSource dataSource, @NotNull String name) throws DBException {
        this(dataSource, dataSource, name);
    }

    @NotNull
    public TableContainerBuilder table(@NotNull String name, @NotNull Consumer<TableAttributeContainerBuilder> applier) throws DBException {
        final TableAttributeContainerBuilder builder = new TableAttributeContainerBuilder(dataSource, container, name);
        applier.apply(builder);
        children.add(builder.build());
        return this;
    }

    @NotNull
    @Override
    public DBSObjectContainer build() {
        return container;
    }
}
