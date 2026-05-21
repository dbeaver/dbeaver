/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDocumentContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings.ExtractType;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings.FetchedRowsPolicy;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIObservables;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;
import org.jkiss.dbeaver.ui.forms.UIRowBuilder;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.List;
import java.util.function.Consumer;

public class ConfigureDataExtractionDialog extends BaseDialog {

    private enum Strategy {
        QUERY_DATABASE,
        USE_FETCHED_ROWS
    }

    private static final Log log = Log.getLog(ConfigureDataExtractionDialog.class);

    @NotNull
    private final DataTransferWizard wizard;
    private final DataTransferSettings settings;
    private DatabaseProducerSettings producerSettings;

    private final UIObservable<Strategy> strategy = UIObservable.of(Strategy.QUERY_DATABASE);

    // Query database
    private final UIObservable<Boolean> openNewConnections = UIObservable.of(false);
    private final UIObservable<Boolean> fetchRowCount = UIObservable.of(false);

    // Fetched rows
    private final UIObservable<Boolean> selectedRowsOnly = UIObservable.of(false);
    private final UIObservable<Boolean> selectedColumnsOnly = UIObservable.of(false);

    // Advanced
    private final UIObservable<Integer> fetchSize = UIObservable.of(10000);
    private final UIObservable<Integer> threadCount = UIObservable.of(1);
    private final UIObservable<Integer> segmentSize = UIObservable.of(10000);
    private final UIObservable<Boolean> extractInSegments = UIObservable.of(false);

    public ConfigureDataExtractionDialog(
        @NotNull Shell shell,
        @NotNull DataTransferWizard wizard
    ) {
        super(shell, DTUIMessages.database_producer_page_extract_settings_name_and_title, null);
        setTitle(DTUIMessages.database_producer_page_extract_settings_name_and_title);
        //setDescription(DTUIMessages.database_producer_page_extract_settings_description);
        //setPageComplete(false);

        this.wizard = wizard;
        this.settings = wizard.getSettings();


        wizard.loadNodeSettings();
        DataTransferPipe firstPipe = wizard.getSettings().getDataPipes().getFirst();

        try {
            this.producerSettings = (DatabaseProducerSettings) settings.getNodeSettings(firstPipe.getProducer());
        } catch (Exception e) {
            log.error(e);
        }
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        UIPanelBuilder.build(composite, pb -> pb
            .margins(0, 0)
            .row(rb -> rb
                .titledPanel("Extraction", buildExtractionPanel())));

        if (wizard.getCurrentTask() != null) {
            Composite buttonsPanel = UIUtils.createComposite(composite, 1);
            wizard.createVariablesEditButton(buttonsPanel);
        }

        //var settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);
        // Query database
        openNewConnections.set(producerSettings.isOpenNewConnections());
        fetchRowCount.set(producerSettings.isQueryRowCount());

        // Fetched rows
        var useFetchedRows = producerSettings.getFetchedRowsPolicy();
        strategy.set(useFetchedRows != null ? Strategy.USE_FETCHED_ROWS : Strategy.QUERY_DATABASE);
        selectedRowsOnly.set(useFetchedRows != null && useFetchedRows.selectedRowsOnly());
        selectedColumnsOnly.set(useFetchedRows != null && useFetchedRows.selectedColumnsOnly());

        // Advanced
        fetchSize.set(producerSettings.getFetchSize());
        threadCount.set(settings.getMaxJobCount());
        segmentSize.set(producerSettings.getSegmentSize());
        extractInSegments.set(producerSettings.getExtractType() == ExtractType.SEGMENTS);

        return composite;
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildExtractionPanel() {
        var canExportFetchedOnly = canExportFetchedRows();
        if (canExportFetchedOnly) {
            return buildQueryDatabaseOrUseFetchedRowsPanel();
        } else {
            return buildQueryDatabaseOnlyPanel();
        }
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildQueryDatabaseOnlyPanel() {
        var useFetchedData = UIObservable.of(true);

        return pb -> pb
            .row(rb -> rb.panel(buildQueryDatabasePanel(useFetchedData)))
            .row(buildAdvancedRow(useFetchedData));
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildQueryDatabaseOrUseFetchedRowsPanel() {
        var queryDatabase = UIObservables.equals(strategy, Strategy.QUERY_DATABASE);
        var useFetchedData = UIObservables.equals(strategy, Strategy.USE_FETCHED_ROWS);

        return pb -> pb
            .row(rb -> rb
                .radioButton("Query the database", queryDatabase)
                .radioButton("Use fetched rows", useFetchedData))
            .row(rb -> rb
                .panel(buildQueryDatabasePanel(queryDatabase))
                .panel(buildUseFetchedRowsPanel(useFetchedData)))
            .row(buildAdvancedRow(queryDatabase));
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildQueryDatabasePanel(@NotNull UIObservable<Boolean> enabled) {
        return pb -> pb
            .row(rb -> rb
                .enabled(enabled)
                .checkBox(DTMessages.data_transfer_wizard_output_checkbox_new_connection, bb -> bb
                    .tooltip(DTUIMessages.database_producer_page_extract_settings_new_connection_checkbox_tooltip)
                    .selected(openNewConnections)))
            .row(rb -> rb
                .enabled(enabled)
                .checkBox(DTMessages.data_transfer_wizard_output_checkbox_select_row_count, bb -> bb
                    .tooltip(DTUIMessages.database_producer_page_extract_settings_row_count_checkbox_tooltip)
                    .selected(fetchRowCount)));
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildUseFetchedRowsPanel(@NotNull UIObservable<Boolean> enabled) {
        var canExportSelection = UIObservable.of(canExportColumns());

        return pb -> pb
            .row(rb -> rb
                .enabled(UIObservables.and(enabled, canExportSelection))
                .checkBox("Selected rows only", selectedRowsOnly))
            .row(rb -> rb
                .enabled(UIObservables.and(enabled, canExportSelection))
                .checkBox("Selected columns only", selectedColumnsOnly));
    }

    @NotNull
    private Consumer<UIRowBuilder> buildAdvancedRow(@NotNull UIObservable<Boolean> queryDatabase) {
        return rb -> rb
            .titledPanel("Advanced", buildAdvancedPanel(queryDatabase));
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildAdvancedPanel(@NotNull UIObservable<Boolean> queryDatabase) {
        var canChangeThreads = UIObservables.predicate(() -> settings.getDataPipes().size() > 1);

        return pb -> pb
            .row(DTMessages.data_transfer_wizard_output_label_max_threads, rb -> rb
                .enabled(UIObservables.and(queryDatabase, canChangeThreads))
                .intTextField(threadCount, tb -> tb
                    .tooltip(DTUIMessages.database_producer_page_extract_settings_threads_num_text_tooltip)))
            .row(DTUIMessages.database_producer_page_extract_settings_text_fetch_size_label, rb -> rb
                .enabled(queryDatabase)
                .intTextField(fetchSize, tb -> tb
                    .tooltip(DTUIMessages.database_producer_page_extract_settings_text_fetch_size_tooltip)))
            .row(rb -> rb
                .checkBox(DTMessages.data_transfer_wizard_output_checkbox_extract_in_batches, bb -> bb
                    .tooltip(DTMessages.data_transfer_wizard_output_checkbox_extract_in_batches_tip)
                    .selected(extractInSegments))
                .enabled(queryDatabase)
                .intTextField(segmentSize, tb -> tb.enabled(extractInSegments)));
    }

    @Override
    protected void okPressed() {
        // Query database
        producerSettings.setOpenNewConnections(openNewConnections.get());
        producerSettings.setQueryRowCount(fetchRowCount.get());

        // Fetched rows
        if (strategy.get() == Strategy.USE_FETCHED_ROWS && canExportFetchedRows()) {
            boolean canExportSelection = canExportColumns();
            producerSettings.setFetchedRowsPolicy(new FetchedRowsPolicy(
                canExportSelection && selectedRowsOnly.get(),
                canExportSelection && selectedColumnsOnly.get()
            ));
        } else {
            producerSettings.setFetchedRowsPolicy(null);
        }

        // Advanced
        producerSettings.setFetchSize(fetchSize.get());
        settings.setMaxJobCount(threadCount.get());
        producerSettings.setSegmentSize(segmentSize.get());
        producerSettings.setExtractType(extractInSegments.get() ? ExtractType.SEGMENTS : ExtractType.SINGLE_QUERY);

        super.okPressed();
    }

    private boolean canExportColumns() {
        List<DBSObject> objects = settings.getSourceObjects();
        for (DBSObject object : objects) {
            DBSDataContainer container = GeneralUtils.adapt(object, DBSDataContainer.class);
            if (container instanceof DBSDocumentContainer) {
                return false;
            }
            if (container != null && container.getDataSource().getInfo().isDynamicMetadata()) {
                return false;
            }
        }
        return true;
    }

    private boolean canExportFetchedRows() {
        for (DBSObject object : settings.getSourceObjects()) {
            if (!(object instanceof DBSDataContainer container)) {
                return false;
            }
            if (!container.isFeatureSupported(DBSDataContainer.FEATURE_DATA_READ_FETCHED)) {
                return false;
            }
        }
        return true;
    }
}