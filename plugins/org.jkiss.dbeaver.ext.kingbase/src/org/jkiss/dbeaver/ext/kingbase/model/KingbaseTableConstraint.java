/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;

/**
 * KingbaseTableConstraint
 */
public class KingbaseTableConstraint extends KingbaseTableConstraintBase<KingbaseTableConstraintColumn> {
    private static final Log log = Log.getLog(KingbaseTableConstraint.class);

    private String source;
    private final List<KingbaseTableConstraintColumn> columns = new ArrayList<>();

    public KingbaseTableConstraint(KingbaseTableBase table, String name, DBSEntityConstraintType constraintType, JDBCResultSet resultSet) throws DBException {
        super(table, name, constraintType, resultSet);
        String sourceCopy = JDBCUtils.safeGetString(resultSet, "consrc_copy");
        if (sourceCopy == null ) {
            
            this.source = null;
            
        } else {
            this.source = sourceCopy;
        }
    }

    public KingbaseTableConstraint(KingbaseTableBase table, String constraintName, DBSEntityConstraintType constraintType) {
        super(table, constraintName, constraintType);
    }

    public KingbaseTableConstraint(DBRProgressMonitor monitor, KingbaseTableReal owner, KingbaseTableConstraint srcConstr) throws DBException {
        super(monitor, owner, srcConstr);
        this.source = srcConstr.source;
        for (KingbaseTableConstraintColumn srcCol : srcConstr.columns) {
            KingbaseTableColumn ownAttr = owner.getAttribute(monitor, srcCol.getAttribute().getName());
            if (ownAttr != null) {
                this.columns.add(new KingbaseTableConstraintColumn(this, ownAttr, this.columns.size()));
            }
        }
    }

    @Override
    void cacheAttributes(DBRProgressMonitor monitor, List<? extends KingbaseTableConstraintColumn> children, boolean secondPass) {
        if (secondPass) {
            return;
        }
        columns.clear();
        columns.addAll(children);
    }

    @Override
    public List<KingbaseTableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    @Override
    public void addAttributeReference(DBSTableColumn column) throws DBException {
        columns.add(new KingbaseTableConstraintColumn(this, (KingbaseAttribute<?>) column, columns.size()));
    }

    public void addColumn(KingbaseTableConstraintColumn column) {
        this.columns.add(column);
    }

    public List<KingbaseTableConstraintColumn> getColumns() {
        return columns;
    }

    @Override
    public void setAttributeReferences(List<KingbaseTableConstraintColumn> columns) throws DBException {
        this.columns.clear();
        this.columns.addAll(columns);
    }

    @Property(viewable = true, editable = true, order = 10)
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
