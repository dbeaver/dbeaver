/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.gbase8s.model;

import java.lang.reflect.Field;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.gbase8s.GBase8sUtils;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * @author Chao Tian
 */
public class GBase8sDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(GBase8sDataType.class);

    public GBase8sDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
            throws DBException {
        super(monitor, container, metaModel, new GenericSQLDialect());
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        // replace the connector of the database/table in queryGetActiveDB
        replaceConnector4GetActiveDB(container);
        return new GenericExecutionContext(instance, type);
    }

    void replaceConnector4GetActiveDB(DBPDataSourceContainer container) {
        final DBPDriver driver = container.getDriver();
        String getActiveDBQuery = CommonUtils
                .toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_GET_ACTIVE_DB));
        if (GBase8sUtils.isOracleSqlMode(container)) {
            getActiveDBQuery = getActiveDBQuery.replaceFirst("\\?", ".");
        } else {
            getActiveDBQuery = getActiveDBQuery.replaceFirst("\\?", ":");
        }
        Field field;
        try {
            field = this.getClass().getSuperclass().getDeclaredField("queryGetActiveDB");
            field.setAccessible(true);
            field.set(this, getActiveDBQuery);
        } catch (Exception e) {
            log.error("Failed to replace the connector of the database/table in queryGetActiveDB", e);
        }
    }
}
