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

package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

/*
 * A thread to receive replication target table's data from a peer database.
 */
public class AltibaseReplicationReceiver extends AltibaseReplicationModule {

    private long applyXsn = -1;
    private long insertSuccessCount = 0;
    private long insertFailureCount = 0;
    private long updateSuccessCount = 0;
    private long updateFailureCount = 0;
    private long deleteSuccessCount = 0;
    private long deleteFailureCount = 0;

    public AltibaseReplicationReceiver(AltibaseReplication parent, JDBCResultSet resultSet) {
        super(parent);

        applyXsn = JDBCUtils.safeGetLong(resultSet, "APPLY_XSN");
        insertSuccessCount = JDBCUtils.safeGetLong(resultSet, "INSERT_SUCCESS_COUNT");
        insertFailureCount = JDBCUtils.safeGetLong(resultSet, "INSERT_FAILURE_COUNT");
        updateSuccessCount = JDBCUtils.safeGetLong(resultSet, "UPDATE_SUCCESS_COUNT");
        updateFailureCount = JDBCUtils.safeGetLong(resultSet, "UPDATE_FAILURE_COUNT");
        deleteSuccessCount = JDBCUtils.safeGetLong(resultSet, "DELETE_SUCCESS_COUNT");
        deleteFailureCount = JDBCUtils.safeGetLong(resultSet, "DELETE_FAILURE_COUNT");
    }

    @NotNull
    @Override
    @Property(viewable = false, order = 1, hidden = true)
    public String getName() {
        return "Receiver";
    }

    @Property(viewable = true, order = 3)
    public long getApplyXsn() {
        return applyXsn;
    }

    @Property(viewable = true, order = 4)
    public long getInsertSuccessCount() {
        return insertSuccessCount;
    }

    @Property(viewable = true, order = 5)
    public long getInsertFailureCount() {
        return insertFailureCount;
    }

    @Property(viewable = true, order = 6)
    public long getUpdateSuccessCount() {
        return updateSuccessCount;
    }

    @Property(viewable = true, order = 7)
    public long getUpdateFailureCount() {
        return updateFailureCount;
    }

    @Property(viewable = true, order = 8)
    public long getDeleteSuccessCount() {
        return deleteSuccessCount;
    }

    @Property(viewable = true, order = 9)
    public long getDeleteFailureCount() {
        return deleteFailureCount;
    }
}