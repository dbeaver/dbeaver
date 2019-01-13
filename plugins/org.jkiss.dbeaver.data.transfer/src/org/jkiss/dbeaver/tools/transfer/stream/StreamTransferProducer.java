/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Data container transfer producer
 */
public class StreamTransferProducer implements IDataTransferProducer<StreamProducerSettings> {

    private static final Log log = Log.getLog(StreamTransferProducer.class);

    public static final String NODE_ID = "stream_producer";

    @NotNull
    private File inputFile;
    private DataTransferProcessorDescriptor defaultProcessor;
    private StreamSourceObject sourceObject;

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
    public String getObjectContainerName() {
        return inputFile == null ? null : inputFile.getParentFile().getAbsolutePath();
    }

    @Override
    public Color getObjectColor() {
        return null;
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

        StreamProducerSettings.EntityMapping entityMapping = settings.getEntityMapping(sourceObject);
        if (entityMapping != null) {
            loadObjectDefinition(entityMapping);
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

    public void loadObjectDefinition(StreamProducerSettings.EntityMapping entityMapping) throws DBException {
        if (defaultProcessor == null) {
            return;
        }
        this.sourceObject.entityMapping = entityMapping;
    }

    private class StreamSourceObject implements DBSEntity, DBSDataContainer {

        private StreamProducerSettings.EntityMapping entityMapping;

        public StreamSourceObject(StreamProducerSettings.EntityMapping entityMapping) {
            this.entityMapping = entityMapping;
        }

        @Override
        public DBSEntityType getEntityType() {
            return DBSEntityType.TABLE;
        }

        @Override
        public List<StreamSourceAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException {
            List<StreamProducerSettings.AttributeMapping> attrMappings = entityMapping.getValuableAttributeMappings();
            List<StreamSourceAttribute> result = new ArrayList<>(attrMappings.size());
            for (StreamProducerSettings.AttributeMapping sa : attrMappings) {
                result.add(new StreamSourceAttribute(this, sa));
            }
            return result;
        }

        @Override
        public DBSEntityAttribute getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException {
            for (StreamProducerSettings.AttributeMapping sa : entityMapping.getAttributeMappings()) {
                if (sa.isValuable() && attributeName.equals(sa.getSourceAttributeName())) {
                    return new StreamSourceAttribute(this, sa);
                }
            }
            return null;
        }

        @Override
        public Collection<? extends DBSEntityConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @Override
        public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @Override
        public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
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

        @Override
        public DBPDataSource getDataSource() {
            return null;
        }

        @Override
        public int getSupportedFeatures() {
            return DATA_SELECT;
        }

        @Override
        public DBCStatistics readData(DBCExecutionSource source, DBCSession session, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags) throws DBCException {
            throw new DBCException("Not implemented");
        }

        @Override
        public long countData(DBCExecutionSource source, DBCSession session, DBDDataFilter dataFilter, long flags) throws DBCException {
            return -1;
        }

        @Override
        public String getName() {
            return StreamTransferProducer.this.getObjectName();
        }

        @Override
        public boolean isPersisted() {
            return true;
        }
    }

    private static class StreamSourceAttribute extends AbstractAttribute implements DBSEntityAttribute {

        private final StreamSourceObject sourceObject;
        private final StreamProducerSettings.AttributeMapping attributeMapping;

        public StreamSourceAttribute(StreamSourceObject sourceObject, StreamProducerSettings.AttributeMapping attributeMapping) {
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

        @Override
        public DBSEntity getParentObject() {
            return sourceObject;
        }

        @Override
        public DBPDataSource getDataSource() {
            return sourceObject.getDataSource();
        }
    }

}
