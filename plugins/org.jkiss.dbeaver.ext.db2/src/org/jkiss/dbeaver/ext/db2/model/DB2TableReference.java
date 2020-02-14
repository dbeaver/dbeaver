/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2DeleteUpdateRule;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.List;

/**
 * DB2 Table Foreign Key
 * 
 * @author Denis Forveille
 */
public class DB2TableReference extends JDBCTableConstraint<DB2Table> implements DBSTableForeignKey {
    private static final Log log = Log.getLog(DB2TableReference.class);

    private DB2Table refTable;

    private DB2DeleteUpdateRule deleteRule;
    private DB2DeleteUpdateRule updateRule;

    private List<DB2TableKeyColumn> columns;

    private DB2TableForeignKey referencedKey;

    // -----------------
    // Constructors
    // -----------------

    public DB2TableReference(DBRProgressMonitor monitor, DB2Table db2Table, ResultSet dbResult) throws DBException
    {
        super(db2Table, JDBCUtils.safeGetString(dbResult, "CONSTNAME"), null, DBSEntityConstraintType.FOREIGN_KEY, true);

        String refSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
        String refTableName = JDBCUtils.safeGetString(dbResult, "TABNAME");
        String constName = JDBCUtils.safeGetString(dbResult, "CONSTNAME");
        refTable = DB2Utils.findTableBySchemaNameAndName(monitor, db2Table.getDataSource(), refSchemaName, refTableName);
        if (refTable == null) {
            log.debug("Reference table '" + refTableName + "' not found in schema '" + refSchemaName + "'");
        } else {
            referencedKey = refTable.getAssociation(monitor, constName);
            if (referencedKey == null) {
                log.debug("Foreign key '" + constName + "' not found in table '" + refTable.getName() + "'");
            }
        }

        deleteRule = CommonUtils.valueOf(DB2DeleteUpdateRule.class, JDBCUtils.safeGetString(dbResult, "DELETERULE"));
        updateRule = CommonUtils.valueOf(DB2DeleteUpdateRule.class, JDBCUtils.safeGetString(dbResult, "UPDATERULE"));
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    public DB2Table getAssociatedEntity()
    {
        return refTable;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
    }

    // -----------------
    // Columns
    // -----------------
    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor) throws DBException
    {
        return columns;
    }

    public void setColumns(List<DB2TableKeyColumn> columns)
    {
        this.columns = columns;
    }

    // -----------------
    // Direct Attributes
    // -----------------

    @Property(viewable = true, order = 3)
    public DB2Table getReferencedTable()
    {
        return refTable;
    }

    @Nullable
    @Override
    @Property(id = "reference", viewable = false)
    public DB2TableForeignKey getReferencedConstraint()
    {
        return referencedKey;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false)
    public DBSForeignKeyModifyRule getUpdateRule()
    {
        return updateRule.getRule();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false)
    public DBSForeignKeyModifyRule getDeleteRule()
    {
        return deleteRule.getRule();
    }

}
