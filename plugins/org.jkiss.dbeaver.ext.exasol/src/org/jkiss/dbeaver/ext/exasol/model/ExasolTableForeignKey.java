/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;


/**
 * @author Karl
 */
public class ExasolTableForeignKey extends JDBCTableConstraint<ExasolTable> implements DBSTableForeignKey,DBPScriptObject, DBPNamedObject2 {

    private static final Log LOG = Log.getLog(ExasolTableForeignKey.class);
    private ExasolTable refTable;
    private String constName;
    private Boolean enabled;
    private List<ExasolTableForeignKeyColumn> columns;
    
    
    private ExasolTableUniqueKey referencedKey;


    // -----------------
    // Constructor
    // -----------------

    public ExasolTableForeignKey(DBRProgressMonitor monitor, ExasolTable exasolTable, ResultSet dbResult) throws DBException {
        super(exasolTable, JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"), null, DBSEntityConstraintType.FOREIGN_KEY, true);

        String refSchemaName = JDBCUtils.safeGetString(dbResult, "REFERENCED_SCHEMA");
        String refTableName = JDBCUtils.safeGetString(dbResult, "REFERENCED_TABLE");
        this.constName = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME");
        
        refTable = ExasolUtils.findTableBySchemaNameAndName(monitor, exasolTable.getDataSource(), refSchemaName, refTableName);

        enabled = JDBCUtils.safeGetBoolean(dbResult, "CONSTRAINT_ENABLED");
        referencedKey = null;
    }

    public ExasolTableForeignKey(ExasolTable exasolTable, ExasolTableUniqueKey referencedKey, Boolean enabled, String name) {
        super(exasolTable, name, "", DBSEntityConstraintType.FOREIGN_KEY, true);
        this.referencedKey = referencedKey;
        this.enabled = enabled;
        this.constName = name;
        setReferencedConstraint(referencedKey);

    }

    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Override
    public ExasolTable getAssociatedEntity() {
        return refTable;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
    }

    @NotNull
    @Override
    public DBSForeignKeyModifyRule getUpdateRule() {
        if (this.enabled) {
            return DBSForeignKeyModifyRule.RESTRICT;
        } else {
            return DBSForeignKeyModifyRule.NO_ACTION;
        }
    }

    @NotNull
    @Override
    public DBSForeignKeyModifyRule getDeleteRule() {
        if (this.enabled) {
            return DBSForeignKeyModifyRule.RESTRICT;
        } else {
            return DBSForeignKeyModifyRule.NO_ACTION;
        }
    }

    // -----------------
    // Columns
    // -----------------
    @Override
    public List<ExasolTableForeignKeyColumn> getAttributeReferences(DBRProgressMonitor monitor) throws DBException {
        return columns;
    }

    public void setColumns(List<ExasolTableForeignKeyColumn> columns) {
        this.columns = columns;
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, order = 3)
    public ExasolTable getReferencedTable() {
        return refTable;
    }

    @Nullable
    @NotNull
    @Override
    @Property(id = "reference", viewable = true)
    public ExasolTableUniqueKey getReferencedConstraint() {
    	if (referencedKey == null) {
    	    if (refTable != null) {
                try {
                    referencedKey = refTable.getPrimaryKey(new VoidProgressMonitor());
                } catch (DBException e) {
                    LOG.error("Error reading pk", e);
                }
            }
    	}
        return referencedKey;
    }

    public void setReferencedConstraint(ExasolTableUniqueKey referencedKey) {
        this.referencedKey = referencedKey;
        this.refTable = referencedKey == null ? null : referencedKey.getTable();
    }

    @Property(viewable = true, editable = true, updatable = true)
    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options)
			throws DBException
	{
		return ExasolUtils.getFKDdl(this, monitor);
	}
	
	@Override
    @Property(viewable = true)
	public String getName()
	{
		return this.constName;
	}
	
	@Override
	public void setName(String name)
	{
		this.constName = name;
	}
		

}
