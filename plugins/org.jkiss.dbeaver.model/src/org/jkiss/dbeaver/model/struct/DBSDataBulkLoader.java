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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;

import java.util.Map;

/**
 * Data bulk loader
 */
public interface DBSDataBulkLoader {

    interface BulkLoadManager extends AutoCloseable {
        void addRow(@NotNull DBCSession session, @NotNull Object[] attributeValues) throws DBCException;

        void flushRows(@NotNull DBCSession session) throws DBCException;

        void finishBulkLoad(@NotNull DBCSession session) throws DBCException;

        void close();
    }

    @NotNull
    BulkLoadManager createBulkLoad(
        @NotNull DBCSession session,
        @NotNull DBSDataContainer dataContainer,
        @NotNull DBSAttributeBase[] attributes,
        @NotNull DBCExecutionSource source,
        int batchSize,
        Map<String, Object> options)
        throws DBCException;

}
