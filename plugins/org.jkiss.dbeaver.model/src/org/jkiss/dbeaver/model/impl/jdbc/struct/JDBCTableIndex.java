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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

/**
 * JDBC abstract index
 */
public abstract class JDBCTableIndex<CONTAINER extends DBSObjectContainer, TABLE extends JDBCTable>
    extends AbstractTableIndex
    implements DBPSaveableObject {
    private final CONTAINER container;
    private final TABLE table;
    protected String name;
    protected DBSIndexType indexType;
    private boolean persisted;

    protected JDBCTableIndex(CONTAINER container, TABLE table, String name, @Nullable DBSIndexType indexType, boolean persisted) {
        this.container = container;
        this.table = table;
        this.name = name;
        this.indexType = indexType;
        this.persisted = persisted;
    }

    // Copy constructor
    protected JDBCTableIndex(CONTAINER container, TABLE table, DBSTableIndex source, boolean persisted) {
        this.container = container;
        this.table = table;
        this.name = source.getName();
        this.indexType = source.getIndexType();
        this.persisted = persisted;
    }

    protected JDBCTableIndex(JDBCTableIndex<CONTAINER, TABLE> source) {
        this.container = source.container;
        this.table = source.table;
        this.name = source.name;
        this.indexType = source.indexType;
        this.persisted = source.persisted;
    }

    @Override
    public CONTAINER getContainer() {
        return container;
    }

    @NotNull
    @Override
    public TABLE getParentObject() {
        return table;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return name;
    }

    public void setName(String indexName) {
        this.name = indexName;
    }

    @Override
    @Property(viewable = true, order = 2)
    public TABLE getTable() {
        return table;
    }

    @Override
    @Property(viewable = true, order = 3)
    public DBSIndexType getIndexType() {
        return this.indexType;
    }

    public void setIndexType(DBSIndexType indexType) {
        this.indexType = indexType;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

}
