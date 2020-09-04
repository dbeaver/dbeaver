/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;

import java.util.List;
import java.util.Map;

/**
 * Data manipulator.
 * Extends data container and provides additional methods to manipulate underlying data.
 */
public interface DBSDataManipulator extends DBSDataContainer {

    int DATA_INSERT         = 1 << 16;
    int DATA_UPDATE         = 1 << 17;
    int DATA_DELETE         = 1 << 18;
    int DATA_TRUNCATE       = 1 << 19;

    String OPTION_DISABLE_BATCHES = "data.manipulate.disableBatches";//$NON-NLS-1$

    interface ExecuteBatch extends AutoCloseable {
        void add(@NotNull Object[] attributeValues) throws DBCException;

        @NotNull
        DBCStatistics execute(@NotNull DBCSession session, Map<String, Object> options) throws DBCException;

        void generatePersistActions(@NotNull DBCSession session, @NotNull List<DBEPersistAction> actions, Map<String, Object> options) throws DBCException;

        void close();
    }

    @NotNull
    ExecuteBatch insertData(
        @NotNull DBCSession session,
        @NotNull DBSAttributeBase[] attributes,
        @Nullable DBDDataReceiver keysReceiver,
        @NotNull DBCExecutionSource source)
        throws DBCException;

    @NotNull
    ExecuteBatch updateData(
        @NotNull DBCSession session,
        @NotNull DBSAttributeBase[] updateAttributes,
        @NotNull DBSAttributeBase[] keyAttributes,
        @Nullable DBDDataReceiver keysReceiver,
        @NotNull DBCExecutionSource source)
        throws DBCException;

    @NotNull
    ExecuteBatch deleteData(
        @NotNull DBCSession session,
        @NotNull DBSAttributeBase[] keyAttributes,
        @NotNull DBCExecutionSource source)
        throws DBCException;

    @NotNull
    DBCStatistics truncateData(
        @NotNull DBCSession session,
        @NotNull DBCExecutionSource source)
        throws DBCException;

}
