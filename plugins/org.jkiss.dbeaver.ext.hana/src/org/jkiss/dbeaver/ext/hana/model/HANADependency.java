/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HANADependency implements DBSObject {

    class DummyObject implements DBSObject {
        // TODO: dummy object should be shown as plain text without non-working hyperlink
        private String name;
        
        DummyObject(String name) { this.name = name; }
        
        @Override public String getName() { return name; }
        @Override public String getDescription() { return null; }
        @Override public boolean isPersisted() { return false; }
        @Override public DBSObject getParentObject() { return null; }
        @Override public DBPDataSource getDataSource() { return null; }
    };
    
    private DBSObject dependentObject;
    private String baseObjectType, baseObjectSchema, baseObjectName;

    public HANADependency(DBSObject dependentObject, String baseObjectType, String baseObjectSchema, String baseObjectName) {
        this.dependentObject = dependentObject;
        this.baseObjectType = baseObjectType;
        this.baseObjectSchema = baseObjectSchema;
        this.baseObjectName = baseObjectName;
    }
    

    @Override
    public String getName() {
        return DBUtils.getFullyQualifiedName(dependentObject.getDataSource(), baseObjectSchema, baseObjectName);
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return dependentObject.getParentObject();
    }

    @Override
    public DBPDataSource getDataSource() {
        return dependentObject.getDataSource();
    }
    
    
    @Property(viewable = true, order = 20)
    public String getBaseObjectType() {
        return baseObjectType;
    }
    
    @Property(viewable = true, order = 21)
    public DBSObject getBaseObjectSchema(DBRProgressMonitor monitor) throws DBException {
        DBSObject schema = ((GenericDataSource)dependentObject.getDataSource()).getSchema(baseObjectSchema);
        if(schema == null) 
            schema = new DummyObject(baseObjectSchema);
        return schema;
    }

    @Property(viewable = true, order = 22)
    public DBSObject getBaseObject(DBRProgressMonitor monitor) throws DBException {
        GenericSchema schema = ((GenericDataSource)dependentObject.getDataSource()).getSchema(baseObjectSchema);
        DBSObject object = null;
        if(schema != null) {
            switch(baseObjectType) {
            case "TABLE":
            case "VIEW":
                object = schema.getTable(monitor, baseObjectName);
                break;
            case "SYNONYM":
                object = DBUtils.findObject(schema.getSynonyms(monitor), baseObjectName);
                break;
            case "PROCEDURE":
            case "FUNCTION":
                object = schema.getProcedure(monitor, baseObjectName);
                break;
            default:
                object = schema.getChild(monitor, baseObjectName);
            }
        }
        if(object == null)
            object = new DummyObject(baseObjectName);
        return object;
    }


    public static List<HANADependency> readDependencies(DBRProgressMonitor monitor, DBSObject object) throws DBException {
        List<HANADependency> dependencies = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object.getDataSource(), "Read dependencies")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT BASE_OBJECT_TYPE, BASE_SCHEMA_NAME, BASE_OBJECT_NAME" +
                    " FROM SYS.OBJECT_DEPENDENCIES" +
                    " WHERE DEPENDENT_SCHEMA_NAME = ? AND DEPENDENT_OBJECT_NAME = ?" +
                    " ORDER BY BASE_SCHEMA_NAME, BASE_OBJECT_NAME")) {
                dbStat.setString(1, object.getParentObject().getName());
                dbStat.setString(2, object.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String baseObjectType   = dbResult.getString(1);
                        String baseObjectSchema = dbResult.getString(2);
                        String baseObjectName   = dbResult.getString(3);
                        HANADependency dependency = new HANADependency(object, baseObjectType, baseObjectSchema, baseObjectName);
                        dependencies.add(dependency);
                    }
                }
                return dependencies;
            }
        } catch (SQLException e) {
            throw new DBException(e, object.getDataSource());
        }
    }

}
