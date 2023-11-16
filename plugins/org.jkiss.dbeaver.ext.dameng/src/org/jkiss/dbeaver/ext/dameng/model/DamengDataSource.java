/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dameng.DamengConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Types;

/**
 * @author Shengkai Bai
 */
public class DamengDataSource extends GenericDataSource {
    public DamengDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel) throws DBException {
        super(monitor, container, metaModel, new DamengSQLDialect());
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new DamengExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(DBRProgressMonitor monitor, JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        DamengExecutionContext executionContext = (DamengExecutionContext) context;
        if (initFrom == null) {
            executionContext.refreshDefaults(monitor, true);
            return;
        }
        DamengExecutionContext executionMetaContext = (DamengExecutionContext) initFrom;
        DamengSchema defaultSchema = executionMetaContext.getDefaultSchema();
        if (defaultSchema != null) {
            executionContext.setDefaultSchema(monitor, defaultSchema);
        }
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, JDBCDatabaseMetaData metaData) {
        return new JDBCDataSourceInfo(metaData);
    }

    @Override
    public DBPDataKind resolveDataKind(String typeName, int valueType) {
        return getDataKind(typeName, valueType);
    }

    @NotNull
    public static DBPDataKind getDataKind(@NotNull String typeName, int valueType) {
        if (valueType == Types.JAVA_OBJECT && DamengConstants.INTERVAL_TYPES.contains(typeName)) {
            return DBPDataKind.DATETIME;
        }
        return GenericDataSource.getDataKind(typeName, valueType);

    }
}
