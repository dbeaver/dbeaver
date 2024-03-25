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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;

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

    void initTransfer(
        @NotNull DBSObject sourceObject,
        @Nullable SETTINGS settings,
        @NotNull TransferParameters parameters,
        @Nullable PROCESSOR processor,
        @Nullable Map<String, Object> processorProperties,
        @Nullable DBPProject project);

    void startTransfer(DBRProgressMonitor monitor) throws DBException;

    /**
     * Finishes this transfer
     * @param monitor monitor
     * @param last called in the very end of all transfers
     */
    void finishTransfer(DBRProgressMonitor monitor, boolean last);

    /**
     * Finishes this transfer
     *
     * @param monitor   monitor
     * @param exception an exception caught during transfer, or {@code null} if transfer was successful
     * @param last      called in the very end of all transfers
     */
    default void finishTransfer(@NotNull DBRProgressMonitor monitor, @Nullable Exception exception, boolean last) {
        finishTransfer(monitor, exception, null, last);
    }

    /**
     * Finishes this transfer
     *
     * @param monitor monitor
     * @param error   an error caught during transfer, or {@code null} if transfer was successful
     * @param task    a task the transfer was started from
     * @param last    called in the very end of all transfers
     */
    default void finishTransfer(@NotNull DBRProgressMonitor monitor, @Nullable Throwable error, @Nullable DBTTask task, boolean last) {
        finishTransfer(monitor, last);
    }

    // Target object. May be null or target database object (table)
    @Nullable
    Object getTargetObject();

    // If not null then this consumer is a fake one which must be replaced by explicit target consumers on configuration stage
    @Nullable
    Object getTargetObjectContainer();

    /**
     * Set non-persistent parameters for data transfer execution which is shared between consumers of the task
     */
    default void setRuntimeParameters(@Nullable Object runtimeParameters) { }
}
