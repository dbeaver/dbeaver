/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolStatefulObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Karl Griesser
 */
public abstract class ExasolTableBase extends JDBCTable<ExasolDataSource, ExasolSchema> implements DBPNamedObject2, DBPRefreshableObject, ExasolStatefulObject {


    private String remarks;
    private String objectType;
    private BigDecimal objectId;
    


    public ExasolTableBase(ExasolSchema schema, String name, boolean persisted) {
        super(schema, name, persisted);
    }

    public ExasolTableBase(DBRProgressMonitor monitor, ExasolSchema schema, ResultSet dbResult) {
        super(schema, true);
        setName(JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");
        this.objectType = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE");
        this.objectId =JDBCUtils.safeGetBigDecimal(dbResult, "OBJECT_ID");

    }


    @Nullable
    @Override
    @Property(viewable = false, order = 99, editable = true, updatable = true, multiline = true)
    public String getDescription()
    {
        return remarks;
    }

    public void setDescription(String description)
    {
        this.remarks = description;
    }

    @Override
    public boolean isView() {

        if (objectType.equals("VIEW"))
            return true;
        else
            return false;
    }
    // -----------------
    // Business Contract
    // -----------------

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return this;
    }


    // -----------------
    // Columns
    // -----------------
    @Override
    public List<ExasolTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (this instanceof ExasolTable)
            return getContainer().getTableCache().getChildren(monitor, getContainer(), (ExasolTable) this);

        if (this instanceof ExasolView)
            return getContainer().getViewCache().getChildren(monitor, getContainer(), (ExasolView) this);

        throw new DBException("Unknow object with columns encountered");
    }

    @Override
    public ExasolTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        if (this instanceof ExasolTable) {
            return getContainer().getTableCache().getChild(monitor, getContainer(), (ExasolTable) this, attributeName);
        } else if (this instanceof ExasolView) {
            return getContainer().getViewCache().getChild(monitor, getSchema(), (ExasolView) this, attributeName); 
        } 
        // Other kinds don't have columns..
        throw new DBException("Unknown object with columns encountered");
    }


    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return super.getName();
    }

    @Override
    @Property(viewable = true, editable = false, order = 2)
    public ExasolSchema getSchema() {
        return super.getContainer();
    }

    // -----------------
    // Associations (Imposed from DBSTable). In Exasol, Most of objects "derived"
    // from Tables don't have those..
    // -----------------
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<ExasolTableUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    @Override
    public Collection<ExasolTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }


    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        // No Indexes in Exasol
        return Collections.emptyList();
    }
    
    public BigDecimal getObjectId()
    {
    	return this.objectId;
    }


}
