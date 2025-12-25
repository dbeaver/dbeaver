/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
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

    public record FetchedRowsPolicy(boolean selectedRowsOnly, boolean selectedColumnsOnly) {}

    private static final int DEFAULT_SEGMENT_SIZE = 100000;
    private static final int DEFAULT_FETCH_SIZE = 10000;

    private int segmentSize = DEFAULT_SEGMENT_SIZE;

    private boolean openNewConnections = true;
    private boolean queryRowCount = true;
    private FetchedRowsPolicy fetchedRowsPolicy;
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

    /**
     * Get fetched rows policy.
     * <p>
     * If {@code null}, then all rows should are extracted. Otherwise,
     * only fetched rows those are selected according to the policy are extracted.
     */
    @Nullable
    public FetchedRowsPolicy getFetchedRowsPolicy() {
        return fetchedRowsPolicy;
    }

    public void setFetchedRowsPolicy(@Nullable FetchedRowsPolicy fetchedRowsPolicy) {
        this.fetchedRowsPolicy = fetchedRowsPolicy;
    }

    public boolean isOpenNewConnections() {
        return openNewConnections;
    }

    public void setOpenNewConnections(boolean openNewConnections) {
        this.openNewConnections = openNewConnections;
    }

    @NotNull
    public ExtractType getExtractType() {
        return extractType;
    }

    public void setExtractType(@NotNull ExtractType extractType) {
        this.extractType = extractType;
    }


    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DataTransferSettings dataTransferSettings, Map<String, Object> settings) {
        extractType = CommonUtils.valueOf(ExtractType.class, (String) settings.get("extractType"), extractType);
        segmentSize = CommonUtils.toInt(settings.get("segmentSize"), DEFAULT_SEGMENT_SIZE);
        fetchSize = CommonUtils.toInt(settings.get("fetchSize"), fetchSize);
        openNewConnections = CommonUtils.toBoolean(settings.get("openNewConnections"));
        queryRowCount = CommonUtils.toBoolean(settings.get("queryRowCount"));

        boolean fetchedRowsOnly = CommonUtils.toBoolean(settings.get("fetchedRowsOnly"));
        boolean selectedRowsOnly = CommonUtils.toBoolean(settings.get("selectedRowsOnly"));
        boolean selectedColumnsOnly = CommonUtils.toBoolean(settings.get("selectedColumnsOnly"));
        if (fetchedRowsOnly || selectedRowsOnly || selectedColumnsOnly) {
            fetchedRowsPolicy = new FetchedRowsPolicy(selectedRowsOnly, selectedColumnsOnly);
        } else {
            fetchedRowsPolicy = null;
        }
    }

    @Override
    public void saveSettings(Map<String, Object> settings) {
        settings.put("extractType", extractType.name());
        settings.put("segmentSize", segmentSize);
        settings.put("fetchSize", fetchSize);
        settings.put("openNewConnections", openNewConnections);
        settings.put("queryRowCount", queryRowCount);
        settings.put("fetchedRowsOnly", fetchedRowsPolicy != null);
        settings.put("selectedColumnsOnly", fetchedRowsPolicy != null && fetchedRowsPolicy.selectedColumnsOnly());
        settings.put("selectedRowsOnly", fetchedRowsPolicy != null && fetchedRowsPolicy.selectedRowsOnly());
    }

    @Override
    public String getSettingsSummary() {
        StringBuilder summary = new StringBuilder();

        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_checkbox_new_connection, openNewConnections);
        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_extract_type, extractType.name());
        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_checkbox_select_row_count, queryRowCount);
        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_checkbox_fetched_rows_only, fetchedRowsPolicy != null);
        DTUtils.addSummary(
            summary,
            DTMessages.data_transfer_wizard_output_checkbox_selected_rows_only,
            fetchedRowsPolicy != null && fetchedRowsPolicy.selectedRowsOnly()
        );
        DTUtils.addSummary(
            summary,
            DTMessages.data_transfer_wizard_output_checkbox_selected_columns_only,
            fetchedRowsPolicy != null && fetchedRowsPolicy.selectedColumnsOnly()
        );

        return summary.toString();
    }
}
