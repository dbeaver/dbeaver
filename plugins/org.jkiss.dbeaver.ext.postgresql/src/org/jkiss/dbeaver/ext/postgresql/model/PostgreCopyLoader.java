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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataBulkLoader;

import java.util.Map;

/**
 * Bulk loader based on CopyManager
 */
public class PostgreCopyLoader implements DBSDataBulkLoader {

    private static final Log log = Log.getLog(PostgreCopyLoader.class);

    @NotNull
    @Override
    public BulkLoadManager createBulkLoad(@NotNull DBCSession session, @NotNull DBSAttributeBase[] attributes, @NotNull DBCExecutionSource source, int batchSize, Map<String, Object> options) throws DBCException {
        throw new DBCException("Not implemented");
    }
}
