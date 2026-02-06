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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDCellValue;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDocumentContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings.ExtractType;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings.FetchedRowsPolicy;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.DataTransferPageNodeSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.forms.*;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.List;
import java.util.function.Consumer;

public class DatabaseProducerPageExtractSettings extends DataTransferPageNodeSettings {

    private enum Strategy {
        QUERY_DATABASE,
        USE_FETCHED_ROWS
    }

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
    private final UIObservable<ExtractType> extractType = UIObservable.of(ExtractType.SINGLE_QUERY);

    public DatabaseProducerPageExtractSettings() {
        super(DTUIMessages.database_producer_page_extract_settings_name_and_title);
        setTitle(DTUIMessages.database_producer_page_extract_settings_name_and_title);
        setDescription(DTUIMessages.database_producer_page_extract_settings_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);

        UIPanelBuilder.build(composite, pb -> pb
            .margins(0, 0)
            .row(rb -> rb
                .group("Extraction", buildExtractionPanel())));

        if (getWizard().getCurrentTask() != null) {
            Composite buttonsPanel = UIUtils.createComposite(composite, 1);
            getWizard().createVariablesEditButton(buttonsPanel);
        }

        setControl(composite);
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
                .radioButton("Query the database", bb -> bb.selected(queryDatabase))
                .radioButton("Use fetched rows", bb -> bb.selected(useFetchedData)))
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
        var canExportSelection = UIObservable.of(hasSelection() && canExportColumns());

        return pb -> pb
            .row(rb -> rb
                .enabled(UIObservables.and(enabled, canExportSelection))
                .checkBox("Selected rows only", bb -> bb.selected(selectedRowsOnly)))
            .row(rb -> rb
                .enabled(UIObservables.and(enabled, canExportSelection))
                .checkBox("Selected columns only", bb -> bb.selected(selectedColumnsOnly)));
    }

    @NotNull
    private Consumer<UIRowBuilder> buildAdvancedRow(@NotNull UIObservable<Boolean> queryDatabase) {
        return rb -> rb
            .expandableGroup("Advanced", false, pb -> pb
                .align(UIAlignX.FILL).grow()
                .accept(buildAdvancedPanel(queryDatabase)));
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildAdvancedPanel(@NotNull UIObservable<Boolean> queryDatabase) {
        var canChangeThreads = UIObservable.predicate(() -> getWizard().getSettings().getDataPipes().size() > 2);
        var canChangeSegment = UIObservable.predicate(() -> extractType.get() == ExtractType.SEGMENTS);

        return pb -> pb
            .row(DTMessages.data_transfer_wizard_output_label_max_threads, rb -> rb
                .enabled(UIObservables.and(queryDatabase, canChangeThreads))
                .intTextField(threadCount, tb -> tb
                    .tooltip(DTUIMessages.database_producer_page_extract_settings_threads_num_text_tooltip)))
            .row(DTUIMessages.database_producer_page_extract_settings_text_fetch_size_label, rb -> rb
                .enabled(queryDatabase)
                .intTextField(fetchSize, tb -> tb
                    .tooltip(DTUIMessages.database_producer_page_extract_settings_text_fetch_size_tooltip)))
            .row(DTMessages.data_transfer_wizard_output_label_extract_type, rb -> rb
                .enabled(queryDatabase)
                .comboBox(extractType, DatabaseProducerPageExtractSettings::getExtractTypeLabel))
            .row(DTMessages.data_transfer_wizard_output_label_segment_size, rb -> rb
                .enabled(UIObservables.and(queryDatabase, canChangeSegment))
                .intTextField(segmentSize));
    }

    @Override
    public void activatePage() {
        getWizard().loadNodeSettings();

        var settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);

        // Query database
        openNewConnections.set(settings.isOpenNewConnections());
        fetchRowCount.set(settings.isQueryRowCount());

        // Fetched rows
        var useFetchedRows = settings.getFetchedRowsPolicy();
        strategy.set(useFetchedRows != null ? Strategy.USE_FETCHED_ROWS : Strategy.QUERY_DATABASE);
        selectedRowsOnly.set(useFetchedRows != null && useFetchedRows.selectedRowsOnly());
        selectedColumnsOnly.set(useFetchedRows != null && useFetchedRows.selectedColumnsOnly());

        // Advanced
        fetchSize.set(settings.getFetchSize());
        threadCount.set(getWizard().getSettings().getMaxJobCount());
        segmentSize.set(settings.getSegmentSize());
        extractType.set(settings.getExtractType());

        updatePageCompletion();
    }

    @Override
    public void deactivatePage() {
        var settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);

        // Query database
        settings.setOpenNewConnections(openNewConnections.get());
        settings.setQueryRowCount(fetchRowCount.get());

        // Fetched rows
        if (strategy.get() == Strategy.USE_FETCHED_ROWS && canExportFetchedRows()) {
            boolean canExportSelection = hasSelection() && canExportColumns();
            settings.setFetchedRowsPolicy(new FetchedRowsPolicy(
                canExportSelection && selectedRowsOnly.get(),
                canExportSelection && selectedColumnsOnly.get()
            ));
        } else {
            settings.setFetchedRowsPolicy(null);
        }

        // Advanced
        settings.setFetchSize(fetchSize.get());
        getWizard().getSettings().setMaxJobCount(threadCount.get());
        settings.setSegmentSize(segmentSize.get());
        settings.setExtractType(extractType.get());
    }

    @Override
    public boolean isPageApplicable() {
        return isProducerOfType(DatabaseTransferProducer.class);
    }

    @NotNull
    private static String getExtractTypeLabel(@NotNull ExtractType type) {
        return switch (type) {
            case SINGLE_QUERY -> DTMessages.data_transfer_wizard_output_combo_extract_type_item_single_query;
            case SEGMENTS -> DTMessages.data_transfer_wizard_output_combo_extract_type_item_by_segments;
        };
    }

    private boolean hasSelection() {
        var selection = getWizard().getCurrentSelection();
        return selection != null && !selection.isEmpty() && selection.getFirstElement() instanceof DBDCellValue;
    }

    private boolean canExportColumns() {
        List<DBSObject> objects = getWizard().getSettings().getSourceObjects();
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
        for (DBSObject object : getWizard().getSettings().getSourceObjects()) {
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