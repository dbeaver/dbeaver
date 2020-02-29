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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Date;
import java.util.Map;

/**
 * Data consumer
 */
public interface IDataTransferConsumer<SETTINGS extends IDataTransferSettings, PROCESSOR extends IDataTransferProcessor>
    extends IDataTransferNode<SETTINGS>, DBDDataReceiver
{
    class TransferParameters {
        public int orderNumber;
        public int totalConsumers;
        public boolean isBinary;
        public boolean isHTML;
        public Date startTimestamp;

        public TransferParameters() {
        }

        public TransferParameters(boolean isBinary, boolean isHTML) {
            this.isBinary = isBinary;
            this.isHTML = isHTML;
        }
    }

    void initTransfer(DBSObject sourceObject, SETTINGS settings, TransferParameters parameters, PROCESSOR processor, Map<Object, Object> processorProperties);

    void startTransfer(DBRProgressMonitor monitor) throws DBException;

    /**
     * Finishes this transfer
     * @param monitor monitor
     * @param last called in the very end of all transfers
     */
    void finishTransfer(DBRProgressMonitor monitor, boolean last);

}
