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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream transfer settings
 */
public class StreamProducerSettings implements IDataTransferSettings {

    private static final Log log = Log.getLog(StreamProducerSettings.class);

    private Map<String, StreamEntityMapping> entityMapping = new LinkedHashMap<>();
    private Map<String, Object> processorProperties;
    private int maxRows;

    public StreamProducerSettings() {
    }

    public Map<String, Object> getProcessorProperties() {
        return processorProperties;
    }

    public void setProcessorProperties(Map<String, Object> processorProperties) {
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
                StreamEntityMapping em = new StreamEntityMapping(mapping);
                entityMapping.put(em.getEntityName(), em);
            }
            runnableContext.run(true, true, monitor ->
                updateMappingsFromStream(monitor, dataTransferSettings));
        } catch (Exception e) {
            log.error("Error loading stream producer settings", e);
        }
    }

    public void updateMappingsFromStream(DBRProgressMonitor monitor, DataTransferSettings dataTransferSettings) {
        for (DataTransferPipe pipe : dataTransferSettings.getDataPipes()) {
            StreamTransferProducer producer = (StreamTransferProducer) pipe.getProducer();
            if (producer.getEntityMapping() != null) {
                updateProducerSettingsFromStream(monitor, producer, dataTransferSettings);
            }
        }
    }

    public void updateProducerSettingsFromStream(DBRProgressMonitor monitor, @NotNull StreamTransferProducer producer, DataTransferSettings dataTransferSettings) {
        monitor.beginTask("Update data produces settings from import stream", 1);
        final Map<String, Object> processorProperties = dataTransferSettings.getProcessorProperties();

        List<StreamDataImporterColumnInfo> columnInfos = null;
        StreamEntityMapping entityMapping = producer.getEntityMapping();

        IDataTransferProcessor importer = dataTransferSettings.getProcessor().getInstance();

        if (importer instanceof IStreamDataImporter) {
            IStreamDataImporter sdi = (IStreamDataImporter) importer;
            try (InputStream is = new FileInputStream(entityMapping.getInputFile())) {
                sdi.init(new StreamDataImporterSite(this, entityMapping, processorProperties));
                try {
                    columnInfos = sdi.readColumnsInfo(entityMapping, is);
                } finally {
                    sdi.dispose();
                }
            } catch (Exception e) {
                log.error("IO error while reading columns from stream", e);
            }
        }
        entityMapping.setStreamColumns(columnInfos);

        monitor.done();
    }

    @Override
    public void saveSettings(Map<String, Object> settings) {
        List<Map<String, Object>> mappings = new ArrayList<>();
        settings.put("mappings", mappings);

        for (StreamEntityMapping emc : entityMapping.values()) {
            Map<String, Object> emSettings = emc.saveSettings();
            if (emSettings != null) {
                mappings.add(emSettings);
            }
        }
    }

    @Override
    public String getSettingsSummary() {
        return "";
    }

}
