/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreTableConstraint
 */
public class PostgreTableConstraint extends PostgreTableConstraintBase {

    private String source;
    private List<PostgreTableConstraintColumn> columns = new ArrayList<>();

    public PostgreTableConstraint(PostgreTableBase table, String name, DBSEntityConstraintType constraintType, JDBCResultSet resultSet) throws DBException {
        super(table, name, constraintType, resultSet);
        this.source = JDBCUtils.safeGetString(resultSet, "consrc");
    }

    public PostgreTableConstraint(PostgreTableBase table, String constraintName, DBSEntityConstraintType constraintType) {
        super(table, constraintName, constraintType);
    }

    @Override
    void cacheAttributes(DBRProgressMonitor monitor, List<? extends PostgreTableConstraintColumn> children, boolean secondPass) {
        if (secondPass) {
            return;
        }
        columns.clear();
        columns.addAll(children);
    }

    @Override
    public List<PostgreTableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public List<PostgreTableConstraintColumn> getColumns() {
        return columns;
    }

    public void addColumn(PostgreTableConstraintColumn column) {
        this.columns.add(column);
    }

    @Property(viewable = true, editable = true, order = 10)
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
