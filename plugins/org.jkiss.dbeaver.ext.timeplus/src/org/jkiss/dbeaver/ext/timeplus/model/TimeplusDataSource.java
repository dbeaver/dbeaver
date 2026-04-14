/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.timeplus.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.timeplus.model.jdbc.TimeplusJdbcFactory;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCFactory;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class TimeplusDataSource extends GenericDataSource {

    public TimeplusDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull GenericMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel, new GenericSQLDialect());
    }

    @NotNull
    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        String message = error.getMessage();
        if (CommonUtils.isNotEmpty(message)) {
            if (message.contains("Connection is closed") || message.contains("Connection refused")) {
                return ErrorType.CONNECTION_LOST;
            }
            if (message.contains("timeout") || message.contains("Timeout")) {
                return ErrorType.CONNECTION_LOST;
            }
        }
        return super.discoverErrorType(error);
    }

    @NotNull
    @Override
    public JDBCFactory getJdbcFactory() {
        return new TimeplusJdbcFactory();
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false, false);
        }
        return null;
    }
}
