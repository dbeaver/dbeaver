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
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

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

    @Override
    public DBSDataContainer getDatabaseObject()
    {
        return null;
    }

    @Override
    public String getObjectName() {
        return inputFile == null ? null : inputFile.getAbsolutePath();
    }

    public File getInputFile() {
        return inputFile;
    }

    @Override
    public void transferData(
        DBRProgressMonitor monitor,
        IDataTransferConsumer consumer,
        IDataTransferProcessor processor,
        StreamProducerSettings settings)
        throws DBException
    {
        // Initialize importer
        DBSObject databaseObject = consumer.getDatabaseObject();
        if (!(databaseObject instanceof DBSEntity)) {
            throw new DBException("Wrong consumer object for stream producer: " + databaseObject);
        }
        Map<Object, Object> processorProperties = settings.getProcessorProperties();
        StreamDataImporterSite site = new StreamDataImporterSite(settings, (DBSEntity) databaseObject, processorProperties);
        IStreamDataImporter importer = (IStreamDataImporter) processor;
        importer.init(site);

        // Perform transfer
        try (InputStream is = new FileInputStream(inputFile)) {
            importer.runImport(monitor, is, consumer);
        } catch (IOException e) {
            throw new DBException("IO error", e);
        } finally {
            importer.dispose();
        }
    }

}
