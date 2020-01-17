/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.hive.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.hive.model.jdbc.HiveJdbcFactory;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCFactory;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLState;

public class HiveDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(HiveDataSource.class);

    public HiveDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new HiveSQLDialect());
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        return new HiveDataSourceInfo(monitor, this, metaData);
    }

    @NotNull
    @Override
    protected JDBCFactory createJdbcFactory() {
        return new HiveJdbcFactory();
    }

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        String sqlState = SQLState.getStateFromException(error);
        if (SQLState.SQL_08S01.getCode().equals(sqlState)) {
            // By some reason many Hive errors have this SQL state
            return ErrorType.NORMAL;
        }
        return super.discoverErrorType(error);
    }
}
