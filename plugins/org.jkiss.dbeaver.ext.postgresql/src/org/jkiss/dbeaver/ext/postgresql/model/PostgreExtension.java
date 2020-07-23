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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBPSystemInfoObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * PostgreExtension
 */
public class PostgreExtension implements PostgreObject, PostgreScriptObject, DBPSystemInfoObject {

    private static final Log log = Log.getLog(PostgreExtension.class);
    
    private PostgreDatabase database;

    private long oid;
    private String name;
    private String owner;
    private String tables;
    private String conditions;
    private boolean relocatable;
    private String version;
    private Map<Long, String> tableConditions;
    
    public PostgreExtension(PostgreDatabase database) {
        this.database = database;
        this.owner = PostgreConstants.PUBLIC_SCHEMA_NAME;
    }

    public PostgreExtension(PostgreDatabase database, ResultSet dbResult)
        throws SQLException
    {
        this.database = database;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "extname");
        this.version = JDBCUtils.safeGetString(dbResult, "extversion");
        this.owner = JDBCUtils.safeGetString(dbResult, "schema_name");
        this.tables = JDBCUtils.safeGetString(dbResult, "tbls");
        this.relocatable = JDBCUtils.safeGetBoolean(dbResult, "extrelocatable");
        this.conditions = JDBCUtils.safeGetString(dbResult, "extcondition");
    }

    @NotNull
    @Override
    @Property(viewable = true,editable = true, order = 1)
    public String getName()
    {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;        
    }

    @Property(viewable = true, order = 5)
    public String getTables() {
        return tables;
    }
    
    @Property(viewable = true, order = 6)
    public String getConditions() {
        return conditions;
    }
    
    @NotNull
    @Property(viewable = true,editable = true,updatable = true, order = 4, listProvider = SchemaListProvider.class)
    public String getSchema() {
        return owner;
    }
    
    public void setSchema(String schema) {
        this.owner = schema;        
    }
    
    @NotNull
    @Property(viewable = true, order = 3)
    public boolean getRelocatable() {
        return relocatable;
    }

    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = true, order = 2)
    public String getVersion() {
        return version;
    }

    public boolean isExtensionTable(PostgreTableBase table) {
        return tableConditions != null && tableConditions.containsKey(table.getObjectId());
    }

    public String getExternalTableCondition(long tableOid) {
        return tableConditions  == null ? null : tableConditions.get(tableOid);
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return database;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return
            "-- Extension: " + getName() + "\n\n" +
            "-- DROP EXTENSION " + getName() + ";\n\n" +
            "CREATE EXTENSION " + getName() + "\n\t" +
            "SCHEMA \"" + getSchema() + "\"\n\t" +
            "VERSION " + version;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }
    
    public static class SchemaListProvider implements IPropertyValueListProvider<PostgreExtension> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(PostgreExtension object)
        {
            try {
                Collection<PostgreSchema> schemas = object.getDatabase().getSchemas(new VoidProgressMonitor());
                return schemas.toArray(new Object[schemas.size()]);
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }
}
