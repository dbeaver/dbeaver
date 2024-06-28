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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.meta.DBSerializable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.serialize.DTObjectSerializer;
import org.jkiss.dbeaver.tools.transfer.serialize.SerializerContext;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Data container transfer producer
 */
@DBSerializable("streamTransferProducer")
public class StreamTransferProducer implements IDataTransferProducer<StreamProducerSettings> {

    private static final Log log = Log.getLog(StreamTransferProducer.class);

    public static final String NODE_ID = "stream_producer";

    private final StreamEntityMapping entityMapping;
    private final DataTransferProcessorDescriptor defaultProcessor;

    public StreamTransferProducer() {
        this(null, null);
    }

    public StreamTransferProducer(@Nullable StreamEntityMapping entityMapping) {
        this(entityMapping, null);
    }

    public StreamTransferProducer(@Nullable StreamEntityMapping entityMapping, @Nullable DataTransferProcessorDescriptor defaultProcessor) {
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

    @Nullable
    @Override
    public DBPProject getProject() {
        DBPDataSourceContainer dsContainer = getDataSourceContainer();
        return dsContainer == null ? null : dsContainer.getProject();
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return entityMapping == null ? null : entityMapping.getDataSource().getContainer();
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
        if (entityMapping == null) {
            return "";
        }
        Path inputFile = entityMapping.getInputFile();
        Path parent = inputFile.getParent();
        return parent == null ? inputFile.toAbsolutePath().toString() : parent.toAbsolutePath().toString();
    }

    @Override
    public DBPImage getObjectContainerIcon() {
        return DBIcon.TREE_FOLDER;
    }

    @Override
    public boolean isConfigurationComplete() {
        return entityMapping != null;
    }

    public Path getInputFile() {
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
        try (InputStream is = Files.newInputStream(entityMapping.getInputFile())) {
            importer.runImport(monitor, entityMapping.getDataSource(), is, consumer);
        } catch (Exception e) {
            if (e instanceof DBException dbe) {
                throw dbe;
            }
            throw new DBException("IO error", e);
        } finally {
            importer.dispose();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StreamTransferProducer) {
            return CommonUtils.equalObjects(entityMapping, ((StreamTransferProducer) obj).entityMapping);
        }
        return super.equals(obj);
    }

    public static class ObjectSerializer implements DTObjectSerializer<DBTTask, StreamTransferProducer> {

        @Override
        public void serializeObject(@NotNull DBRRunnableContext runnableContext, @NotNull DBTTask context, @NotNull StreamTransferProducer object, @NotNull Map<String, Object> state) throws DBException {
            final StreamEntityMapping mapping = object.getEntityMapping();
            if (mapping == null) {
                throw new DBException("Task configuration incomplete: source file not specified");
            }
            state.put("file", DBFUtils.getUriFromPath(mapping.getInputFile()));
            state.put("name", mapping.getEntityName());
            state.put("child", mapping.isChild());
            if (object.defaultProcessor != null) {
                state.put("node", object.defaultProcessor.getNode().getId());
                state.put("processor", object.defaultProcessor.getId());
            }
        }

        @Override
        public StreamTransferProducer deserializeObject(
            @NotNull DBRRunnableContext runnableContext,
            @NotNull SerializerContext serializeContext,
            @NotNull DBTTask objectContext,
            @NotNull Map<String, Object> state
        ) throws DBException {
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
            return new StreamTransferProducer(
                new StreamEntityMapping(
                    DBFUtils.resolvePathFromString(runnableContext, objectContext.getProject(), CommonUtils.toString(state.get("file"))),
                    CommonUtils.toString(state.get("name")),
                    CommonUtils.toBoolean(state.get("child"))
                ),
                processor
            );
        }
    }

}
