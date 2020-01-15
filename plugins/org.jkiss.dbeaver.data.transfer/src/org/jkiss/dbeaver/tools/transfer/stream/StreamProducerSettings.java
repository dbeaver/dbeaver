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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Stream transfer settings
 */
public class StreamProducerSettings implements IDataTransferSettings {

    private static final Log log = Log.getLog(StreamProducerSettings.class);

    public static class EntityMapping {
        private String entityName;
        private DBSEntity entity;
        private List<AttributeMapping> attributeMappings = new ArrayList<>();
        private List<StreamDataImporterColumnInfo> streamColumns = new ArrayList<>();

        EntityMapping(DBSEntity entity) {
            this.entity = entity;
            this.entityName = DBUtils.getObjectFullName(entity, DBPEvaluationContext.DML);
        }

        EntityMapping(String entityName) {
            this.entityName = entityName;
        }

        EntityMapping(DBRRunnableContext runnableContext, Map<String, Object> config) throws DBCException, InvocationTargetException, InterruptedException {
            String projectName = CommonUtils.toString(config.get("project"));
            DBPProject project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
            if (project == null) {
                throw new DBCException("Error loading stream producer mappings - project not found");
            }

            String entityId = CommonUtils.toString(config.get("entityId"));
            if (!CommonUtils.isEmpty(entityId)) {
                runnableContext.run(true, true, monitor -> {
                    try {
                        entity = (DBSEntity) DBUtils.findObjectById(monitor, project, entityId);
                        if (entity == null) {
                            log.error("Entity '" + entityId + "' not found");
                            return;
                        }
                        Map<String, Object> attrsConfig = JSONUtils.getObject(config, "attributes");
                        for (String sourceAttrName : attrsConfig.keySet()) {
                            Map<String, Object> attrMap = (Map<String, Object>) attrsConfig.get(sourceAttrName);
                            AttributeMapping am = new AttributeMapping(monitor, EntityMapping.this, sourceAttrName, attrMap);
                            attributeMappings.add(am);
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            }
        }

        public String getEntityName() {
            return entityName;
        }

        public DBSEntity getEntity() {
            return entity;
        }

        public List<AttributeMapping> getAttributeMappings() {
            return attributeMappings;
        }

        public List<AttributeMapping> getValuableAttributeMappings() {
            List<AttributeMapping> result = new ArrayList<>();
            for (AttributeMapping am : attributeMappings) {
                if (am.isValuable()) {
                    result.add(am);
                }
            }
            return result;
        }

        public AttributeMapping getAttributeMapping(DBSEntityAttribute attr) {
            for (AttributeMapping am : attributeMappings) {
                if (attr.getName().equals(am.getTargetAttributeName())) {
                    return am;
                }
            }
            AttributeMapping am = new AttributeMapping(attr);
            attributeMappings.add(am);
            return am;
        }

        public boolean isComplete() {
            for (AttributeMapping am : attributeMappings) {
                if (am.getMappingType() == AttributeMapping.MappingType.NONE) {
                    return false;
                }
            }
            return true;
        }

        public List<StreamDataImporterColumnInfo> getStreamColumns() {
            return streamColumns;
        }

        public void setStreamColumns(List<StreamDataImporterColumnInfo> streamColumns) {
            this.streamColumns.clear();
            this.streamColumns.addAll(streamColumns);
        }

        public StreamDataImporterColumnInfo getStreamColumn(String name) {
            for (StreamDataImporterColumnInfo col : streamColumns) {
                if (name.equals(col.getColumnName())) {
                    return col;
                }
            }
            return null;
        }

        Map<String, Object>saveSettings() {
            if (entity == null || entity.getParentObject() == null) {
                return null;
            }
            Map<String, Object> mappings = new LinkedHashMap<>();

            mappings.put("project", entity.getDataSource().getContainer().getProject().getName());
            mappings.put("entityId", DBUtils.getObjectFullId(entity));
            Map<String, Object> attrConfig = new LinkedHashMap<>();
            mappings.put("attributes", attrConfig);
            for (AttributeMapping attr : attributeMappings) {
                attr.saveSettings(attrConfig);
            }
            return mappings;
        }
    }

    public static class AttributeMapping {

        public enum MappingType {
            NONE("none"),
            IMPORT("import"),
            DEFAULT_VALUE("custom value"),
            SKIP("skip");

            private final String title;

            MappingType(String title) {
                this.title = title;
            }

            public String getTitle() {
                return title;
            }
        }

        private DBSEntityAttribute targetAttribute;
        private DBDValueHandler targetValueHandler;

        private String targetAttributeName;
        private String sourceAttributeName;
        private int sourceAttributeIndex = -1;
        private boolean skip;
        private String defaultValue;
        private MappingType mappingType = MappingType.NONE;
        private StreamDataImporterColumnInfo sourceColumn;

        public AttributeMapping(DBSEntityAttribute attr) {
            this.sourceAttributeName = this.targetAttributeName = attr.getName();
            this.targetAttribute = attr;
            this.targetValueHandler = DBUtils.findValueHandler(attr.getDataSource(), attr);
        }

        AttributeMapping(DBRProgressMonitor monitor, EntityMapping em, String sourceAttrName, Map<String, Object> config) throws DBException {
            this.targetAttributeName = JSONUtils.getString(config, "targetName");
            this.sourceAttributeName = sourceAttrName;
            this.mappingType = CommonUtils.valueOf(MappingType.class, CommonUtils.toString(config.get("mapping")), MappingType.SKIP);
            this.skip = JSONUtils.getBoolean(config, "skip");
            this.defaultValue = JSONUtils.getString(config, "default");

            this.targetAttribute = em.entity.getAttribute(monitor, this.targetAttributeName);
            if (this.targetAttribute != null) {
                this.targetValueHandler = DBUtils.findValueHandler(this.targetAttribute.getDataSource(), this.targetAttribute);
            }
        }

        public MappingType getMappingType() {
            return mappingType;
        }

        public void setMappingType(MappingType mappingType) {
            this.mappingType = mappingType;
        }

        public DBSEntityAttribute getTargetAttribute() {
            return targetAttribute;
        }

        public DBDValueHandler getTargetValueHandler() {
            return targetValueHandler;
        }

        public String getSourceAttributeName() {
            return CommonUtils.isEmpty(sourceAttributeName) ? String.valueOf(sourceAttributeIndex) : sourceAttributeName;
        }

        public void setSourceAttributeName(String sourceAttributeName) {
            this.sourceAttributeName = sourceAttributeName;
        }

        public int getSourceAttributeIndex() {
            return sourceAttributeIndex;
        }

        public void setSourceAttributeIndex(int sourceAttributeIndex) {
            this.sourceAttributeIndex = sourceAttributeIndex;
        }

        public String getTargetAttributeName() {
            return targetAttributeName;
        }

        public void setTargetAttributeName(String targetAttributeName) {
            this.targetAttributeName = targetAttributeName;
        }

        public boolean isSkip() {
            return skip;
        }

        public void setSkip(boolean skip) {
            this.skip = skip;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public StreamDataImporterColumnInfo getSourceColumn() {
            return sourceColumn;
        }

        public void setSourceColumn(StreamDataImporterColumnInfo sourceColumn) {
            this.sourceColumn = sourceColumn;
        }

        public boolean isValuable() {
            return mappingType == MappingType.IMPORT || mappingType == MappingType.DEFAULT_VALUE;
        }

        void saveSettings(Map<String, Object> config) {
            Map<String, Object> attrMap = new LinkedHashMap<>();
            config.put(getSourceAttributeName(), attrMap);
            attrMap.put("targetName", getTargetAttributeName());
            attrMap.put("mapping", mappingType.name());
            if (skip) {
                attrMap.put("skip", true);
            }
            if (!CommonUtils.isEmpty(defaultValue)) {
                attrMap.put("default", defaultValue);
            }
        }

        @Override
        public String toString() {
            return sourceAttributeName + " " + mappingType + " (" + targetAttributeName + ")";
        }
    }

    private Map<String, EntityMapping> entityMapping = new HashMap<>();
    private Map<Object, Object> processorProperties;
    private int maxRows;

    public StreamProducerSettings() {
    }

    public EntityMapping getEntityMapping(DBSEntity entity) {
        String fullName = DBUtils.getObjectFullName(entity, DBPEvaluationContext.DML);
        EntityMapping mapping = this.entityMapping.get(fullName);
        if (mapping == null) {
            mapping = new EntityMapping(entity);
            entityMapping.put(fullName, mapping);
        }
        return mapping;
    }

    public Map<Object, Object> getProcessorProperties() {
        return processorProperties;
    }

    public void setProcessorProperties(Map<Object, Object> processorProperties) {
        this.processorProperties = processorProperties;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DataTransferSettings dataTransferSettings, Map<String, Object> settings) {
        setProcessorProperties(dataTransferSettings.getProcessorProperties());

        try {
            for (Map<String, Object> mapping : JSONUtils.getObjectList(settings, "mappings")) {
                EntityMapping em = new EntityMapping(runnableContext, mapping);
                DBSEntity entity = em.getEntity();
                if (entity != null) {
                    entityMapping.put(DBUtils.getObjectFullName(entity, DBPEvaluationContext.DML), em);
                }
            }
            updateMappingsFromStream(dataTransferSettings);
        } catch (Exception e) {
            log.error("Error loading stream producer settings", e);
        }
    }

    public void updateMappingsFromStream(DataTransferSettings dataTransferSettings) throws DBException {
        final Map<Object, Object> processorProperties = dataTransferSettings.getProcessorProperties();
        for (DataTransferPipe pipe : dataTransferSettings.getDataPipes()) {
            StreamTransferProducer producer = (StreamTransferProducer) pipe.getProducer();
            if (producer != null && pipe.getConsumer() != null) {
                DBSObject targetObject = pipe.getConsumer().getDatabaseObject();
                if (targetObject instanceof DBSEntity) {
                    DBSEntity entity = (DBSEntity) targetObject;

                    StreamProducerSettings.EntityMapping entityMapping = getEntityMapping(entity);

                    List<StreamDataImporterColumnInfo> columnInfos = null;
                    File inputFile = producer.getInputFile();

                    if (inputFile != null && inputFile.exists()) {
                        IDataTransferProcessor importer = dataTransferSettings.getProcessor().getInstance();

                        if (importer instanceof IStreamDataImporter) {
                            IStreamDataImporter sdi = (IStreamDataImporter) importer;
                            try (InputStream is = new FileInputStream(inputFile)) {
                                sdi.init(new StreamDataImporterSite(this, entity, processorProperties));
                                try {
                                    columnInfos = sdi.readColumnsInfo(is);
                                } finally {
                                    sdi.dispose();
                                }
                            } catch (IOException e) {
                                throw new DBException("IO error", e);
                            }
                        }
                        entityMapping.setStreamColumns(columnInfos);
                    }

                    // Map source columns
                    if (columnInfos != null) {
                        List<StreamProducerSettings.AttributeMapping> attributeMappings = entityMapping.getAttributeMappings();
                        for (StreamDataImporterColumnInfo columnInfo : columnInfos) {
                            boolean mappingFound = false;
                            if (columnInfo.getColumnName() != null) {
                                for (StreamProducerSettings.AttributeMapping attr : attributeMappings) {
                                    if (CommonUtils.equalObjects(attr.getSourceAttributeName(), columnInfo.getColumnName())) {
                                        if (attr.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.NONE || attr.getSourceAttributeIndex() < 0) {
                                            // Set source name only if it wasn't set
                                            attr.setSourceAttributeName(columnInfo.getColumnName());
                                            attr.setSourceAttributeIndex(columnInfo.getColumnIndex());
                                            if (attr.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.NONE) {
                                                attr.setMappingType(StreamProducerSettings.AttributeMapping.MappingType.IMPORT);
                                            }
                                            attr.setSourceColumn(columnInfo);
                                        }
                                        mappingFound = true;
                                        break;
                                    }
                                }
                            }
                            if (!mappingFound) {
                                if (columnInfo.getColumnIndex() >= 0 && columnInfo.getColumnIndex() < attributeMappings.size()) {
                                    StreamProducerSettings.AttributeMapping attr = attributeMappings.get(columnInfo.getColumnIndex());
                                    if (attr.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.NONE) {
                                        if (!CommonUtils.isEmpty(columnInfo.getColumnName())) {
                                            attr.setSourceAttributeName(columnInfo.getColumnName());
                                        }
                                        attr.setSourceAttributeIndex(columnInfo.getColumnIndex());
                                        attr.setMappingType(StreamProducerSettings.AttributeMapping.MappingType.IMPORT);
                                        attr.setSourceColumn(columnInfo);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void saveSettings(Map<String, Object> settings) {
        List<Map<String, Object>> mappings = new ArrayList<>();
        settings.put("mappings", mappings);

        for (EntityMapping emc : entityMapping.values()) {
            Map<String, Object> emSettings = emc.saveSettings();
            if (emSettings != null) {
                mappings.add(emSettings);
            }
        }
    }

    @Override
    public String getSettingsSummary() {
        StringBuilder summary = new StringBuilder();

        return summary.toString();
    }

}
