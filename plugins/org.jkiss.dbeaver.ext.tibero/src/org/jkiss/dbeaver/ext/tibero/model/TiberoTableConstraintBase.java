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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;

import java.util.ArrayList;
import java.util.List;

public abstract class TiberoTableConstraintBase extends JDBCTableConstraint<TiberoTable, TiberoTableConstraintColumn> {

    private final List<TiberoTableConstraintColumn> columns = new ArrayList<>();

    protected TiberoTableConstraintBase(
        @NotNull TiberoTable table,
        @NotNull String name,
        @NotNull DBSEntityConstraintType constraintType,
        boolean persisted
    ) {
        super(table, name, null, constraintType, persisted);
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
    public DBSEntityConstraintType getConstraintType() {
        return constraintType;
    }

    @Nullable
    @Override
    public List<TiberoTableConstraintColumn> getAttributeReferences(@Nullable DBRProgressMonitor monitor) {
        return columns;
    }

    @Override
    public void addAttributeReference(DBSTableColumn column) throws DBException {
        columns.add(new TiberoTableConstraintColumn(this, (TiberoTableColumn) column, columns.size()));
    }

    @Override
    public void setAttributeReferences(List<TiberoTableConstraintColumn> columns) {
        this.columns.clear();
        this.columns.addAll(columns);
    }
}
