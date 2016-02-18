/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2DeleteUpdateRule;
import org.jkiss.dbeaver.model.DBPDataSource;
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

    private DB2Table refTable;

    private DB2DeleteUpdateRule deleteRule;
    private DB2DeleteUpdateRule updateRule;

    private List<DB2TableKeyColumn> columns;

    private DB2TableUniqueKey referencedKey;

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
        referencedKey = refTable.getConstraint(monitor, constName);

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
    public String getFullQualifiedName()
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

    @NotNull
    @Override
    @Property(id = "reference", viewable = false)
    public DB2TableUniqueKey getReferencedConstraint()
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
