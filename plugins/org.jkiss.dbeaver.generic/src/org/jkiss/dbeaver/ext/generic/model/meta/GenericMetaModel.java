/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.generic.model.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generic meta model
 */
public class GenericMetaModel {

    private String id;
    private final Map<String, GenericMetaObject> objects = new HashMap<String, GenericMetaObject>();
    private String driverClass;

    public GenericMetaModel(IConfigurationElement cfg)
    {
        this.id = cfg.getAttribute(RegistryConstants.ATTR_ID);
        IConfigurationElement[] objectList = cfg.getChildren("object");
        if (!ArrayUtils.isEmpty(objectList)) {
            for (IConfigurationElement childConfig : objectList) {
                GenericMetaObject metaObject = new GenericMetaObject(childConfig);
                objects.put(metaObject.getType(), metaObject);
            }
        }
        this.driverClass = cfg.getAttribute("driverClass");
    }

    public GenericMetaModel(String id) {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public GenericMetaObject getObject(String id)
    {
        return objects.get(id);
    }

    public void loadProcedures(DBRProgressMonitor monitor, GenericObjectContainer container)
        throws DBException
    {
        Map<String, GenericPackage> packageMap = null;
        GenericDataSource dataSource = container.getDataSource();
        GenericMetaObject procObject = dataSource.getMetaObject(GenericConstants.OBJECT_PROCEDURE);
        JDBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.META, "Load procedures");
        try {
            JDBCResultSet dbResult = session.getMetaData().getProcedures(
                container.getCatalog() == null ? null : container.getCatalog().getName(),
                container.getSchema() == null ? null : container.getSchema().getName(),
                dataSource.getAllObjectsPattern());
            try {
                while (dbResult.next()) {
                    String procedureCatalog = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_CAT);
                    String procedureName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_NAME);
                    String specificName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.SPECIFIC_NAME);
                    int procTypeNum = GenericUtils.safeGetInt(procObject, dbResult, JDBCConstants.PROCEDURE_TYPE);
                    String remarks = GenericUtils.safeGetString(procObject, dbResult, JDBCConstants.REMARKS);
                    DBSProcedureType procedureType;
                    switch (procTypeNum) {
                        case DatabaseMetaData.procedureNoResult: procedureType = DBSProcedureType.PROCEDURE; break;
                        case DatabaseMetaData.procedureReturnsResult: procedureType = DBSProcedureType.FUNCTION; break;
                        case DatabaseMetaData.procedureResultUnknown: procedureType = DBSProcedureType.PROCEDURE; break;
                        default: procedureType = DBSProcedureType.UNKNOWN; break;
                    }
                    // Check for packages. Oracle (and may be some other databases) uses catalog name as storage for package name
                    String packageName = null;
                    GenericPackage procedurePackage = null;
                    if (!CommonUtils.isEmpty(procedureCatalog) && CommonUtils.isEmpty(dataSource.getCatalogs())) {
                        // Catalog name specified while there are no catalogs in data source
                        packageName = procedureCatalog;
                    }

                    if (!CommonUtils.isEmpty(packageName)) {
                        if (packageMap == null) {
                            packageMap = new TreeMap<String, GenericPackage>();
                        }
                        procedurePackage = packageMap.get(packageName);
                        if (procedurePackage == null) {
                            procedurePackage = new GenericPackage(container, packageName, true);
                            packageMap.put(packageName, procedurePackage);
                            container.addPackage(procedurePackage);
                        }
                    }

                    final GenericProcedure procedure = createProcedureImpl(
                        procedurePackage != null ? procedurePackage : container,
                        procedureName,
                        specificName,
                        remarks,
                        procedureType);
                    if (procedurePackage != null) {
                        procedurePackage.addProcedure(procedure);
                    } else {
                        container.addProcedure(procedure);
                    }
                }
            }
            finally {
                dbResult.close();
            }
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        } finally {
            session.close();
        }
    }

    protected GenericProcedure createProcedureImpl(
        GenericStructContainer container,
        String procedureName,
        String specificName,
        String remarks,
        DBSProcedureType procedureType)
    {
        return new GenericProcedure(
            container,
            procedureName,
            specificName,
            remarks,
            procedureType);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        return "";
    }

    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return "";
    }

}
