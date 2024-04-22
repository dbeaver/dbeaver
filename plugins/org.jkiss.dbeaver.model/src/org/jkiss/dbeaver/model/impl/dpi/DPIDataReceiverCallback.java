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
package org.jkiss.dbeaver.model.impl.dpi;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.dpi.DPISmartCallback;
import org.jkiss.dbeaver.model.exec.DBCSession;

public class DPIDataReceiverCallback implements DPISmartCallback {
    private final DBCSession session;
    private final DPIResultSet dpiResultSet;
    private final long offset;
    private final long maxRows;

    public DPIDataReceiverCallback(DBCSession session, DPIResultSet dpiResultSet, long offset, long maxRows) {
        this.session = session;
        this.dpiResultSet = dpiResultSet;
        this.offset = offset;
        this.maxRows = maxRows;
    }


    @Override
    public void callback(@Nullable Object realObject) throws DBException {
        if (realObject instanceof DBDDataReceiver dataReceiver) {
            dataReceiver.fetchStart(
                session, dpiResultSet, offset, maxRows
            );
            while (dpiResultSet.nextRow()) {
                dataReceiver.fetchRow(session, dpiResultSet);
            }
            dataReceiver.fetchEnd(session, dpiResultSet);
        }
    }
}
