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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.*;

/**
 * Stream transfer settings
 */
public class StreamProducerSettings implements IDataTransferSettings {

    private static final Log log = Log.getLog(StreamProducerSettings.class);

    private final Map<String, StreamEntityMapping> entityMapping = new LinkedHashMap<>();
    private Map<String, Object> processorProperties;
    private int maxRows;

    private transient Map<String, Object> lastProcessorProperties;
    private transient StreamTransferProducer lastProducer;

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
            runnableContext.run(true, true, monitor -> {
                for (Map<String, Object> mapping : JSONUtils.getObjectList(settings, "mappings")) {
                    try {
                        StreamEntityMapping em = new StreamEntityMapping(monitor, dataTransferSettings.getProject(), mapping);
                        entityMapping.put(em.getEntityName(), em);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
                updateMappingsFromStream(monitor, dataTransferSettings);
            });
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

    public boolean extractExtraEntities(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StreamEntityMapping entityMapping,
        @NotNull DataTransferSettings settings,
        @NotNull Collection<StreamEntityMapping> pendingEntityMappings
    ) {
        if (!entityMapping.isChild() && settings.getProcessor().isMulti()) {
            final IMultiStreamDataImporter importer = (IMultiStreamDataImporter) settings.getProcessor().getInstance();

            monitor.beginTask("Extract extra entities from stream", 1);

            try (InputStream is = Files.newInputStream(entityMapping.getInputFile())) {
                return pendingEntityMappings.addAll(importer.readEntitiesInfo(entityMapping, is));
            } catch (Exception e) {
                settings.getState().addError(e);
                log.error("IO error while reading entities from stream", e);
            } finally {
                monitor.done();
            }
        }

        return false;
    }

    public void updateProducerSettingsFromStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StreamTransferProducer producer,
        @NotNull DataTransferSettings dataTransferSettings
    ) {
        try {
            updateProducerSettingsFromStream(
                monitor,
                producer,
                dataTransferSettings.getProcessor().getInstance(),
                dataTransferSettings.getProcessorProperties()
            );
        } catch (DBException e) {
            log.warn("Exception during of updating producer settings", e);
            dataTransferSettings.getState().addError(e);
        }
    }

    public void updateProducerSettingsFromStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StreamTransferProducer producer,
        @NotNull IDataTransferProcessor processor,
        @NotNull Map<String, Object> processorProperties
    ) throws DBException {
        monitor.beginTask("Update data produces settings from import stream", 1);

        if (CommonUtils.equalObjects(lastProcessorProperties, processorProperties) && CommonUtils.equalObjects(lastProducer, producer)) {
            // Nothing has changed
            return;
        }

        lastProcessorProperties = new LinkedHashMap<>(processorProperties);
        lastProducer = producer;

        List<StreamDataImporterColumnInfo> columnInfos;
        StreamEntityMapping entityMapping = producer.getEntityMapping();

        if (entityMapping != null && processor instanceof IStreamDataImporter sdi) {
            try (InputStream is = Files.newInputStream(entityMapping.getInputFile())) {
                sdi.init(new StreamDataImporterSite(this, entityMapping, processorProperties));
                try {
                    columnInfos = sdi.readColumnsInfo(entityMapping, is);
                    entityMapping.setStreamColumns(columnInfos);
                } finally {
                    sdi.dispose();
                }
            } catch (Exception e) {
                if (e instanceof FileNotFoundException &&
                    DBWorkbench.getPlatform().getApplication().isMultiuser() &&
                    IOUtils.isLocalPath(entityMapping.getInputFile())
                ) {
                    throw new DBException(
                        "Local file '" + entityMapping.getInputFile() + "' doesn't exist or not accessible. " +
                            "Use Cloud Storage to import/export data." +
                            "\nLearn more at " + HelpUtils.getHelpExternalReference("Cloud-Storage"), e);
                } else {
                    throw new DBException("Error reading columns from stream", e);
                }
            }
        }

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
        StringBuilder summary = new StringBuilder();

        return summary.toString();
    }

}
