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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.meta.DBSerializable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.model.StreamDataSource;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Data container transfer producer
 */
@DBSerializable("streamTransferProducer")
public class StreamTransferProducer implements IDataTransferProducer<StreamProducerSettings> {

    private static final Log log = Log.getLog(StreamTransferProducer.class);

    public static final String NODE_ID = "stream_producer";

    private File inputFile;
    private DataTransferProcessorDescriptor defaultProcessor;
    private StreamSourceObject sourceObject;
    private StreamDataSource streamDataSource;

    public StreamTransferProducer() {
    }

    public StreamTransferProducer(File file) {
        this(file, null);
    }

    public StreamTransferProducer(File file, DataTransferProcessorDescriptor defaultProcessor) {
        this.inputFile = file;
        this.defaultProcessor = defaultProcessor;
        this.sourceObject = new StreamSourceObject(new StreamProducerSettings.EntityMapping(file.getName()));
    }

    public StreamDataSource getStreamDataSource() {
        if (streamDataSource == null) {
            this.streamDataSource = new StreamDataSource(inputFile.getName());
        }
        return streamDataSource;
    }

    @Override
    public DBSEntity getDatabaseObject()
    {
        return sourceObject;
    }

    @Override
    public String getObjectName() {
        return inputFile == null ? null : inputFile.getName();
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
        return inputFile == null ? null : inputFile.getParentFile().getAbsolutePath();
    }

    @Override
    public DBPImage getObjectContainerIcon() {
        return DBIcon.TREE_FOLDER;
    }

    public File getInputFile() {
        return inputFile;
    }

    @Override
    public void transferData(
        @NotNull DBRProgressMonitor monitor,
        @NotNull IDataTransferConsumer consumer,
        @Nullable IDataTransferProcessor processor,
        @NotNull StreamProducerSettings settings, DBTTask task)
        throws DBException
    {
        // Initialize importer
        DBSObject databaseObject = consumer.getDatabaseObject();
        if (!(databaseObject instanceof DBSEntity)) {
            throw new DBException("Wrong consumer object for stream producer: " + databaseObject);
        }
        if (processor == null) {
            throw new DBException("Stream data producer requires data processor");
        }

        StreamProducerSettings.EntityMapping entityMapping = settings.getEntityMapping(sourceObject);
        if (entityMapping != null) {
            loadObjectDefinition(entityMapping);
        }

        Map<String, Object> processorProperties = settings.getProcessorProperties();
        StreamDataImporterSite site = new StreamDataImporterSite(settings, (DBSEntity) databaseObject, processorProperties);
        IStreamDataImporter importer = (IStreamDataImporter) processor;
        importer.init(site);

        // Perform transfer
        try (InputStream is = new FileInputStream(inputFile)) {
            importer.runImport(monitor, getStreamDataSource(), is, consumer);
        } catch (IOException e) {
            throw new DBException("IO error", e);
        } finally {
            importer.dispose();
        }
    }

    private void loadObjectDefinition(StreamProducerSettings.EntityMapping entityMapping) throws DBException {
        if (defaultProcessor == null) {
            return;
        }
        this.sourceObject.entityMapping = entityMapping;
    }

    private class StreamSourceObject implements DBSEntity, DBSDataContainer, DBPQualifiedObject {

        private StreamProducerSettings.EntityMapping entityMapping;

        StreamSourceObject(StreamProducerSettings.EntityMapping entityMapping) {
            this.entityMapping = entityMapping;
        }

        @NotNull
        @Override
        public DBSEntityType getEntityType() {
            return DBSEntityType.TABLE;
        }

        @Override
        public List<StreamSourceAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
            List<StreamProducerSettings.AttributeMapping> attrMappings = entityMapping.getValuableAttributeMappings();
            List<StreamSourceAttribute> result = new ArrayList<>(attrMappings.size());
            for (StreamProducerSettings.AttributeMapping sa : attrMappings) {
                result.add(new StreamSourceAttribute(this, sa));
            }
            return result;
        }

        @Override
        public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
            for (StreamProducerSettings.AttributeMapping sa : entityMapping.getAttributeMappings()) {
                if (sa.isValuable() && attributeName.equals(sa.getSourceAttributeName())) {
                    return new StreamSourceAttribute(this, sa);
                }
            }
            return null;
        }

        @Override
        public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @Override
        public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @Override
        public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public DBSObject getParentObject() {
            return null;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource() {
            return getStreamDataSource();
        }

        @Override
        public int getSupportedFeatures() {
            return DATA_SELECT;
        }

        @NotNull
        @Override
        public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags, int fetchSize) throws DBCException {
            throw new DBCException("Not implemented");
        }

        @Override
        public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, DBDDataFilter dataFilter, long flags) throws DBCException {
            return -1;
        }

        @NotNull
        @Override
        public String getName() {
            return StreamTransferProducer.this.getObjectName();
        }

        @Override
        public boolean isPersisted() {
            return true;
        }

        @NotNull
        @Override
        public String getFullyQualifiedName(DBPEvaluationContext context) {
            return getName();
        }
    }

    private static class StreamSourceAttribute extends AbstractAttribute implements DBSEntityAttribute {

        private final StreamSourceObject sourceObject;
        private final StreamProducerSettings.AttributeMapping attributeMapping;

        StreamSourceAttribute(StreamSourceObject sourceObject, StreamProducerSettings.AttributeMapping attributeMapping) {
            super(attributeMapping.getSourceAttributeName(), "String", 1, attributeMapping.getSourceAttributeIndex(), Integer.MAX_VALUE, null, null, false, false);
            this.sourceObject = sourceObject;
            this.attributeMapping = attributeMapping;
        }

        @Override
        public DBPDataKind getDataKind() {
            return DBPDataKind.STRING;
        }

        @Override
        public String getDefaultValue() {
            return attributeMapping.getDefaultValue();
        }

        @NotNull
        @Override
        public DBSEntity getParentObject() {
            return sourceObject;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource() {
            return sourceObject.getDataSource();
        }
    }

    public static class ObjectSerializer implements DBPObjectSerializer<DBTTask, StreamTransferProducer> {

        @Override
        public void serializeObject(DBRRunnableContext runnableContext, DBTTask context, StreamTransferProducer object, Map<String, Object> state) {
            state.put("file", object.inputFile.getAbsolutePath());
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
            return new StreamTransferProducer(inputFile, processor);
        }
    }

}
