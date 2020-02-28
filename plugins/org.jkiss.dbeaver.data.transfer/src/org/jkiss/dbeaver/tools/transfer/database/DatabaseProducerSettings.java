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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * DatabaseProducerSettings
 */
public class DatabaseProducerSettings implements IDataTransferSettings {

    public enum ExtractType {
        SINGLE_QUERY,
        SEGMENTS
    }

    private static final int DEFAULT_SEGMENT_SIZE = 100000;
    private static final int DEFAULT_FETCH_SIZE = 10000;

    private int segmentSize = DEFAULT_SEGMENT_SIZE;

    private boolean openNewConnections = true;
    private boolean queryRowCount = true;
    private boolean selectedRowsOnly = false;
    private boolean selectedColumnsOnly = false;
    private ExtractType extractType = ExtractType.SINGLE_QUERY;
    private int fetchSize = DEFAULT_FETCH_SIZE;

    public DatabaseProducerSettings() {
    }

    public int getSegmentSize() {
        return segmentSize;
    }

    public void setSegmentSize(int segmentSize) {
        if (segmentSize > 0) {
            this.segmentSize = segmentSize;
        }
    }

    public boolean isQueryRowCount() {
        return queryRowCount;
    }

    public void setQueryRowCount(boolean queryRowCount) {
        this.queryRowCount = queryRowCount;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public boolean isSelectedRowsOnly() {
        return selectedRowsOnly;
    }

    public void setSelectedRowsOnly(boolean selectedRowsOnly) {
        this.selectedRowsOnly = selectedRowsOnly;
    }

    public boolean isSelectedColumnsOnly() {
        return selectedColumnsOnly;
    }

    public void setSelectedColumnsOnly(boolean selectedColumnsOnly) {
        this.selectedColumnsOnly = selectedColumnsOnly;
    }

    public boolean isOpenNewConnections() {
        return openNewConnections;
    }

    public void setOpenNewConnections(boolean openNewConnections) {
        this.openNewConnections = openNewConnections;
    }

    public ExtractType getExtractType() {
        return extractType;
    }

    public void setExtractType(ExtractType extractType) {
        this.extractType = extractType;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DataTransferSettings dataTransferSettings, Map<String, Object> settings) {
        extractType = CommonUtils.valueOf(ExtractType.class, (String) settings.get("extractType"), extractType);
        segmentSize = CommonUtils.toInt(settings.get("segmentSize"), DEFAULT_SEGMENT_SIZE);
        fetchSize = CommonUtils.toInt(settings.get("fetchSize"), fetchSize);
        openNewConnections = CommonUtils.toBoolean(settings.get("openNewConnections"));
        queryRowCount = CommonUtils.toBoolean(settings.get("queryRowCount"));
        selectedColumnsOnly = CommonUtils.toBoolean(settings.get("selectedColumnsOnly"));
        selectedRowsOnly = CommonUtils.toBoolean(settings.get("selectedRowsOnly"));
    }

    @Override
    public void saveSettings(Map<String, Object> settings) {
        settings.put("extractType", extractType.name());
        settings.put("segmentSize", segmentSize);
        settings.put("fetchSize", fetchSize);
        settings.put("openNewConnections", openNewConnections);
        settings.put("queryRowCount", queryRowCount);
        settings.put("selectedColumnsOnly", selectedColumnsOnly);
        settings.put("selectedRowsOnly", selectedRowsOnly);
    }

    @Override
    public String getSettingsSummary() {
        StringBuilder summary = new StringBuilder();

        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_checkbox_new_connection, openNewConnections);
        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_extract_type, extractType.name());
        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_checkbox_select_row_count, queryRowCount);
        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_checkbox_selected_rows_only, selectedRowsOnly);
        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_checkbox_selected_columns_only, selectedColumnsOnly);

        return summary.toString();
    }
}
