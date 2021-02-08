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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DataSourceContextProvider
 */
public class DataSourceContextProvider implements DBPContextProvider
{
    private static final Log log = Log.getLog(DataSourceContextProvider.class);

    private final DBSObject object;

    public DataSourceContextProvider(DBSObject object) {
        this.object = object;
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        try {
            return DBUtils.getOrOpenDefaultContext(object, false);
        } catch (DBCException e) {
            log.error("Error obtaining context", e);
            return null;
        }
    }
}
