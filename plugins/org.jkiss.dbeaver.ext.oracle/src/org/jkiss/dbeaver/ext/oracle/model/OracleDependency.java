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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

public class OracleDependency extends OracleObject<DBSObject> implements DBPUniqueObject, DBPImageProvider {
    private final String objectOwnerName;
    private final String objectName;
    private final OracleObjectType objectType;
    private final OracleDependencyType dependencyType;

    public OracleDependency(DBSObject parent, String objectOwnerName, String objectName, String objectType, String dependencyType) {
        super(parent, null, parent.isPersisted());
        this.objectOwnerName = objectOwnerName;
        this.objectName = objectName;
        this.objectType = OracleObjectType.getByType(objectType);
        this.dependencyType = OracleDependencyType.getByType(dependencyType);
    }

    @NotNull
    public static List<OracleDependency> readDependencies(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object, boolean dependents) throws DBException {
        List<OracleDependency> dependencies = new ArrayList<>();

        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Load object dependencies")) {
            OracleDataSource dataSource = (OracleDataSource) object.getDataSource();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(dataSource) + " *" +
                "\nFROM " + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), dataSource, "DEPENDENCIES") +
                "\nWHERE " + (dependents ? "REFERENCED_OWNER=? AND REFERENCED_NAME=?" : "OWNER=? AND NAME=? AND REFERENCED_TYPE <> 'NON-EXISTENT'") +
                "\nORDER BY " + (dependents ? "NAME" : "REFERENCED_NAME")
            )) {
                dbStat.setString(1, object.getParentObject().getName());
                dbStat.setString(2, object.getName());

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        dependencies.add(new OracleDependency(
                            object,
                            JDBCUtils.safeGetString(dbResult, dependents ? "OWNER" : "REFERENCED_OWNER"),
                            JDBCUtils.safeGetString(dbResult, dependents ? "NAME" : "REFERENCED_NAME"),
                            JDBCUtils.safeGetString(dbResult, dependents ? "TYPE" : "REFERENCED_TYPE"),
                            JDBCUtils.safeGetString(dbResult, "DEPENDENCY_TYPE")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            throw new DBCException("Error reading dependencies", e);
        }

        return dependencies;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return objectName;
    }

    @Property(viewable = true, order = 2)
    public OracleObjectType getObjectType() {
        return objectType;
    }

    @Property(viewable = true, order = 3)
    public OracleSchema getObjectOwner(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().getSchema(monitor, objectOwnerName);
    }

    @Property(viewable = true, order = 4)
    public DBSObject getObject(DBRProgressMonitor monitor) throws DBException {
        return objectType.findObject(monitor, getObjectOwner(monitor), objectName);
    }

    @Property(viewable = true, order = 5)
    public OracleDependencyType getDependencyType() {
        return dependencyType;
    }

    @Override
    public DBPImage getObjectImage() {
        return objectType.getImage();
    }

    @NotNull
    @Override
    public String getUniqueName() {
        return objectOwnerName + '.' + objectName + '(' + dependencyType + ')';
    }
}
