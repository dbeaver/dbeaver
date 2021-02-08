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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.meta.DBSerializable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Data container transfer producer
 */
@DBSerializable("streamTransferProducer")
public class StreamTransferProducer implements IDataTransferProducer<StreamProducerSettings> {

    private static final Log log = Log.getLog(StreamTransferProducer.class);

    public static final String NODE_ID = "stream_producer";

    private StreamEntityMapping entityMapping;
    private DataTransferProcessorDescriptor defaultProcessor;

    public StreamTransferProducer() {
    }

    public StreamTransferProducer(StreamEntityMapping entityMapping) {
        this(entityMapping, null);
    }

    public StreamTransferProducer(StreamEntityMapping entityMapping, DataTransferProcessorDescriptor defaultProcessor) {
        this.entityMapping = entityMapping;
        this.defaultProcessor = defaultProcessor;
    }

    public StreamEntityMapping getEntityMapping() {
        return entityMapping;
    }

    @Override
    public StreamEntityMapping getDatabaseObject()
    {
        return entityMapping;
    }

    @Override
    public String getObjectName() {
        return entityMapping == null ? null : entityMapping.getName();
    }

    @Override
    public DBPImage getObjectIcon() {
        if (defaultProcessor != null) {
            return defaultProcessor.getIcon();
        }
        return null;
    }

    @Override
    public String getObjectContainerName() {
        File inputFile = entityMapping.getInputFile();
        return inputFile == null ? null : inputFile.getParentFile().getAbsolutePath();
    }

    @Override
    public DBPImage getObjectContainerIcon() {
        return DBIcon.TREE_FOLDER;
    }

    public File getInputFile() {
        return entityMapping == null ? null : entityMapping.getInputFile();
    }

    @Override
    public void transferData(
        @NotNull DBRProgressMonitor monitor,
        @NotNull IDataTransferConsumer consumer,
        @Nullable IDataTransferProcessor processor,
        @NotNull StreamProducerSettings settings,
        @Nullable DBTTask task)
        throws DBException
    {
        // Initialize importer
        DBSObject databaseObject = consumer.getDatabaseObject();
        if (!(databaseObject instanceof DBSEntity)) {
            //throw new DBException("Wrong consumer object for stream producer: " + databaseObject);
        }
        if (processor == null) {
            throw new DBException("Stream data producer requires data processor");
        }

        Map<String, Object> processorProperties = settings.getProcessorProperties();
        StreamDataImporterSite site = new StreamDataImporterSite(settings, entityMapping, processorProperties);
        IStreamDataImporter importer = (IStreamDataImporter) processor;
        importer.init(site);

        // Perform transfer
        try (InputStream is = new FileInputStream(entityMapping.getInputFile())) {
            importer.runImport(monitor, entityMapping.getDataSource(), is, consumer);
        } catch (IOException e) {
            throw new DBException("IO error", e);
        } finally {
            importer.dispose();
        }
    }

    public static class ObjectSerializer implements DBPObjectSerializer<DBTTask, StreamTransferProducer> {

        @Override
        public void serializeObject(DBRRunnableContext runnableContext, DBTTask context, StreamTransferProducer object, Map<String, Object> state) {
            state.put("file", object.getInputFile().getAbsolutePath());
            if (object.defaultProcessor != null) {
                state.put("node", object.defaultProcessor.getNode().getId());
                state.put("processor", object.defaultProcessor.getId());
            }
        }

        @Override
        public StreamTransferProducer deserializeObject(DBRRunnableContext runnableContext, DBTTask objectContext, Map<String, Object> state) {
            File inputFile = new File(CommonUtils.toString(state.get("file")));
            String nodeId = CommonUtils.toString(state.get("node"));
            String processorId = CommonUtils.toString(state.get("processor"));
            DataTransferProcessorDescriptor processor = null;
            if (!CommonUtils.isEmpty(nodeId) && !CommonUtils.isEmpty(processorId)) {
                DataTransferNodeDescriptor nodeDesc = DataTransferRegistry.getInstance().getNodeById(nodeId);
                if (nodeDesc == null) {
                    log.warn("Stream producer node " + nodeId + " not found");
                } else {
                    processor = nodeDesc.getProcessor(processorId);
                    if (processor == null) {
                        log.warn("Stream processor " + processorId + " not found");
                    }
                }
            }
            return new StreamTransferProducer(new StreamEntityMapping(inputFile), processor);
        }
    }

}
