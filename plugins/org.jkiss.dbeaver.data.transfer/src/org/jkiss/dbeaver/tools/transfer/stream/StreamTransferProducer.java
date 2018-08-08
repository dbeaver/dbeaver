/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferSettings;

import java.io.File;

/**
 * Data container transfer producer
 */
public class StreamTransferProducer implements IDataTransferProducer<StreamProducerSettings> {

    private static final Log log = Log.getLog(StreamTransferProducer.class);

    @NotNull
    private File inputFile;

    public StreamTransferProducer() {
    }

    public StreamTransferProducer(File file) {
        this.inputFile = file;
    }

    public StreamTransferProducer(@NotNull File inputFile, DataTransferSettings settings)
    {
        this.inputFile = inputFile;
    }

    @Override
    public DBSDataContainer getDatabaseObject()
    {
        return null;
    }

    @Override
    public String getObjectName() {
        return inputFile == null ? null : inputFile.getAbsolutePath();
    }

    @Override
    public void transferData(
        DBRProgressMonitor monitor,
        IDataTransferConsumer consumer,
        StreamProducerSettings settings)
        throws DBException
    {
        throw new DBException("Stream import not supported");
    }

}
