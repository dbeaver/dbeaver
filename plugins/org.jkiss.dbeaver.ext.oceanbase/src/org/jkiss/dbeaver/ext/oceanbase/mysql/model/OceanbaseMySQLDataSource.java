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
package org.jkiss.dbeaver.ext.oceanbase.mysql.model;

import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLEngine;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.oceanbase.model.plan.OceanbasePlanAnalyzer;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStandardValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

public class OceanbaseMySQLDataSource extends MySQLDataSource {
	private final JDBCBasicDataTypeCache<MySQLDataSource, JDBCDataType> dataTypeCache;

    public OceanbaseMySQLDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
    	super(monitor, container);
        dataTypeCache = new JDBCBasicDataTypeCache<>(this);
    }
    
    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.initialize(monitor);
        dataTypeCache.getAllObjects(monitor, this);
    }
    
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new OceanbasePlanAnalyzer(this));
        }
        else if (adapter == DBDValueHandlerProvider.class) {
        	return adapter.cast(new JDBCStandardValueHandlerProvider());
        }
        else {
        	return super.getAdapter(adapter);
        }
    }
    
    @Override
    public MySQLEngine getEngine(String name) {
    	return DBUtils.findObject(getEngines(), name, true);
    }
    
    @Override
    public MySQLEngine getDefaultEngine() {
    	return this.getEngine("oceanbase");
    }
    
    @Override
    public MySQLPrivilege getPrivilege(DBRProgressMonitor monitor, String name)
            throws DBException {
    		if (name.equalsIgnoreCase("SHOW DB")) {
    			return DBUtils.findObject(getPrivileges(monitor), "Show databases", true);
    		};
            return DBUtils.findObject(getPrivileges(monitor), name, true);
        }
    
    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public DBSDataType getLocalDataType(int typeID) {
        return dataTypeCache.getCachedObject(typeID);
    }
}
