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
import org.jkiss.utils.ByteNumberFormat;

/*
 * A thread to send replication target table's data to a peer database.
 */
public class AltibaseReplicationSender extends AltibaseReplicationModule {

    private long gapSizeInByte;
    private String startFlag;
    private boolean networkError;
    private long xsn;
    private long commitXsn;
    private String status;
    private long readLogCount;
    private long sentLogCount;

    public AltibaseReplicationSender(AltibaseReplication parent, JDBCResultSet resultSet) {
        super(parent);

        startFlag = getStartFlag(JDBCUtils.safeGetLong(resultSet, "START_FLAG"));
        networkError = JDBCUtils.safeGetBoolean(resultSet, "NET_ERROR_FLAG", "1");
        xsn = JDBCUtils.safeGetLong(resultSet, "XSN");
        commitXsn = JDBCUtils.safeGetLong(resultSet, "COMMIT_XSN");
        status = getStatus(JDBCUtils.safeGetLong(resultSet, "STATUS"));
        readLogCount = JDBCUtils.safeGetLong(resultSet, "READ_LOG_COUNT");
        sentLogCount = JDBCUtils.safeGetLong(resultSet, "SEND_LOG_COUNT");
        gapSizeInByte = JDBCUtils.safeGetLong(resultSet, "GAP_SIZE_IN_BYTE");
    }

    @NotNull
    @Override
    @Property(viewable = false, order = 1, hidden = true)
    public String getName() {
        return "Sender";
    }

    @Property(viewable = true, order = 2, formatter = ByteNumberFormat.class)
    public long getGap() {
        return gapSizeInByte;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public String getStartFlag() {
        return startFlag;
    }

    @Property(viewable = true, order = 4)
    public boolean getNetworkError() {
        return networkError;
    }

    @Property(viewable = true, order = 5)
    public long getXsn() {
        return xsn;
    }

    @Property(viewable = true, order = 6)
    public long getCommitXsn() {
        return commitXsn;
    }

    @NotNull
    @Property(viewable = true, order = 7)
    public String getStatus() {
        return status;
    }

    @Property(viewable = true, order = 8)
    public long getReadLogCount() {
        return readLogCount;
    }

    @Property(viewable = true, order = 9)
    public long getSentLogCount() {
        return sentLogCount;
    }

    public long getGapSizeInByte() {
        return gapSizeInByte;
    }

    private String getStartFlag(long value) {
        switch ((int) value) {
            case 0: return "Normal";
            case 1: return "Quick";
            case 2: return "Sync";
            case 3: return "Sync Only";
            case 4: return "Sync Run";
            case 5: return "Sync End";
            case 6: return "Recovery";
            case 7: return "Offline";
            case 8: return "Parallel";
            default: return "Unknown";
        }
    }

    private String getStatus(long value) {
        switch ((int) value) {
            case 0: return "Stop";
            case 1: return "Run";
            case 2: return "Retry";
            case 3: return "Failback Normal";
            case 4: return "Failback Master";
            case 5: return "Failback Slave";
            case 6: return "Sync";
            case 7: return "Failback Eager";
            case 8: return "Failback Flush";
            case 9: return "Idle";
            default: return "Unknown";
        }
    }
}