/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * @author Karl Griesser
 */
public class ExasolTableUniqueKey extends JDBCTableConstraint<ExasolTable> implements DBSEntityReferrer,DBPScriptObject, DBPNamedObject2 {

    private String owner;
    private Boolean enabled;

    private List<ExasolTableKeyColumn> columns;


    // CONSTRUCTOR

    public ExasolTableUniqueKey(DBRProgressMonitor monitor, ExasolTable table, ResultSet dbResult, DBSEntityConstraintType type)
        throws DBException {
        super(table, JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"), null, type, true);
        this.owner = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_OWNER");
        this.enabled = JDBCUtils.safeGetBoolean(dbResult, "CONSTRAINT_ENABLED");

    }

    public ExasolTableUniqueKey(
    		ExasolTable exasolTable, 
    		DBSEntityConstraintType constraintType,
    		Boolean enabled,
    		String name
    		) {
        super(exasolTable, name, "", constraintType, false);
        this.enabled = enabled;
    }

    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return getTable().getDataSource();
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor) throws DBException {
        return columns;
    }

    public void setColumns(List<ExasolTableKeyColumn> columns) {
        this.columns = columns;
    }

    // -----------------
    // Properties
    // -----------------
    @Override
    @Property(viewable = true, editable = false, order = 2)
    public ExasolTable getTable() {
        return super.getTable();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 3)
    public DBSEntityConstraintType getConstraintType() {
        return super.getConstraintType();
    }

    @Nullable
    @Override
    @Property(viewable = false, editable = false, order = 4)
    public String getDescription() {
        return null;
    }

    @Property(viewable = false, editable = false, category = ExasolConstants.CAT_OWNER)
    public String getOwner() {
        return owner;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public Boolean getEnabled() {
        return enabled;
    }
    
    

	public boolean hasColumn(ExasolTableColumn column)
	{
        if (this.columns != null) {
            for (ExasolTableKeyColumn constColumn : columns) {
                if (constColumn.getAttribute() == column) {
                    return true;
                }
            }
        }
        return false;
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options)
			throws DBException
	{
		return ExasolUtils.getPKDdl(this, monitor);
	}
	
	public void setEnabled(Boolean enable)
	{
		this.enabled = enable;
	}

    
}
