/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTableCache;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTableForeignKeyCache;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTableUniqueKeyCache;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolViewCache;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;


public class ExasolSchema extends ExasolGlobalObject implements DBSSchema, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer, DBPScriptObject {

    private static final List<String> SYSTEM_SCHEMA = Arrays.asList("SYS","EXA_STATISTICS");
    private String name;
    private String owner;
    private Timestamp createTime;
    private String remarks;


    // ExasolSchema's children
    private DBSObjectCache<ExasolSchema, ExasolScript> scriptCache;
    private ExasolViewCache viewCache = new ExasolViewCache();
    private ExasolTableCache tableCache = new ExasolTableCache();

    // ExasolTable's children
    private final ExasolTableUniqueKeyCache constraintCache = new ExasolTableUniqueKeyCache(tableCache);
    private final ExasolTableForeignKeyCache associationCache = new ExasolTableForeignKeyCache(tableCache);

    public ExasolSchema(ExasolDataSource exasolDataSource, String name) {
        super(exasolDataSource, true);
        this.name = name;
        this.scriptCache = new JDBCObjectSimpleCache<>(
        		ExasolScript.class,
        		"select "
        		+ "script_name,script_owner,script_language,script_type,script_result_type,script_text,script_comment,b.created "
        		+ "from EXA_ALL_SCRIPTS a inner join EXA_ALL_OBJECTS b "
        		+ "on a.script_name = b.object_name and a.script_schema = b.root_name where a.script_schema = ? order by script_name",
        		name);

    }

    public ExasolSchema(ExasolDataSource exasolDataSource, ResultSet dbResult) throws DBException {

        this(exasolDataSource, JDBCUtils.safeGetStringTrimmed(dbResult, "OBJECT_NAME"));

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
        this.remarks = JDBCUtils.safeGetString(dbResult, "OBJECT_COMMENT");
        this.name = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");


    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName() {
        return this.name;
    }

    @Override
    public Collection<ExasolTableBase> getChildren(DBRProgressMonitor monitor) throws DBException {
        List<ExasolTableBase> allChildren = new ArrayList<>();
        allChildren.addAll(tableCache.getAllObjects(monitor, this));
        allChildren.addAll(viewCache.getAllObjects(monitor, this));
        return allChildren;
    }

    @Override
    public ExasolTableBase getChild(DBRProgressMonitor monitor, String childName) throws DBException {

        ExasolTableBase child = tableCache.getObject(monitor, this, childName);
        if (child == null) {
            child = viewCache.getObject(monitor, this, childName);
        }
        return child;
    }

    @Override
    public Class<ExasolTableBase> getChildType(DBRProgressMonitor monitor) throws DBException {
    	return ExasolTableBase.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
        if (((scope & STRUCT_ENTITIES) != 0)) {
            monitor.subTask("Cache tables");
            tableCache.getAllObjects(monitor, this);
            monitor.subTask("Cache Views");
            viewCache.getAllObjects(monitor, this);

        }
        if (((scope & STRUCT_ATTRIBUTES) != 0)) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, this, null);
            monitor.subTask("Cache Views");
            viewCache.loadChildren(monitor, this, null);
        }

        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table unique keys");
            constraintCache.getObjects(monitor, this, null);
            monitor.subTask("Cache table foreign keys");
            associationCache.getObjects(monitor, this, null);

        }


    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<ExasolTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getTypedObjects(monitor, this, ExasolTable.class);
    }

    public ExasolTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
        return tableCache.getObject(monitor, this, name, ExasolTable.class);
    }

    @Association
    public Collection<ExasolView> getViews(DBRProgressMonitor monitor) throws DBException {
        return viewCache.getTypedObjects(monitor, this, ExasolView.class);
    }

    public ExasolView getView(DBRProgressMonitor monitor, String name) throws DBException {
        return viewCache.getObject(monitor, this, name, ExasolView.class);
    }


    @Override
    public boolean isSystem() {
        // TODO Auto-generated method stub
        return SYSTEM_SCHEMA.contains(name);
    }

    @Override
    public Collection<ExasolScript> getProcedures(DBRProgressMonitor monitor) throws DBException {

        return scriptCache.getAllObjects(monitor, this);
    }

    @Override
    public ExasolScript getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {

        return scriptCache.getObject(monitor, this, uniqueName);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {

        scriptCache.clearCache();
        tableCache.clearCache();
        viewCache.clearCache();


        constraintCache.clearCache();
        associationCache.clearCache();
        return this;
    }

    @Override
    public String toString() {
        return "Schema " + name;
    }

    @Property(viewable = true, editable = false, order = 2)
    public Timestamp getCreateTime() {
        return createTime;
    }

    @Property(viewable = true, editable = true, order = 3)
    public String getDescription() {
        return remarks;
    }
    
    public void setDescription(String newRemarks)
    {
    	remarks = newRemarks;
    }

    @Property(viewable = true, editable = false, order = 4)
    public String getOwner() {
        return owner;
    }

    public ExasolTableCache getTableCache() {
        return tableCache;
    }

    public ExasolViewCache getViewCache() {
        return viewCache;
    }


    public ExasolTableUniqueKeyCache getConstraintCache() {
        return constraintCache;
    }

    public ExasolTableForeignKeyCache getAssociationCache() {
        return associationCache;
    }

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor)
			throws DBException
	{
		return ExasolUtils.generateDDLforSchema(monitor, this);
	}


}
