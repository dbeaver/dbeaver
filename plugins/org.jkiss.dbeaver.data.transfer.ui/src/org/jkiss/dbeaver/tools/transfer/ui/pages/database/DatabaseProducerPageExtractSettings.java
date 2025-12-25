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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
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
import org.jkiss.dbeaver.ui.forms.AlignX;
import org.jkiss.dbeaver.ui.forms.PanelBuilder;
import org.jkiss.dbeaver.ui.forms.util.Bindings;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.List;
import java.util.function.Consumer;

public class DatabaseProducerPageExtractSettings extends DataTransferPageNodeSettings {

    private enum Strategy {
        QUERY_DATABASE,
        USE_FETCHED_ROWS
    }

    private final WritableValue<Strategy> strategy = Bindings.of(Strategy.QUERY_DATABASE);

    // Query database
    private final WritableValue<Boolean> openNewConnections = Bindings.of(false);
    private final WritableValue<Boolean> fetchRowCount = Bindings.of(false);

    // Fetched rows
    private final WritableValue<Boolean> selectedRowsOnly = Bindings.of(false);
    private final WritableValue<Boolean> selectedColumnsOnly = Bindings.of(false);

    // Advanced
    private final WritableValue<Integer> fetchSize = Bindings.of(10000);
    private final WritableValue<Integer> threadCount = Bindings.of(1);
    private final WritableValue<Integer> segmentSize = Bindings.of(10000);
    private final WritableValue<ExtractType> extractType = Bindings.of(ExtractType.SINGLE_QUERY);

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

        PanelBuilder.build(composite, pb -> pb
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
    private Consumer<PanelBuilder> buildExtractionPanel() {
        var queryDatabase = Bindings.select(strategy, Strategy.QUERY_DATABASE);
        var useFetchedData = Bindings.select(strategy, Strategy.USE_FETCHED_ROWS);

        return pb -> pb
            .row(rb -> rb
                .radioButton("Query the database", bb -> bb.selected(queryDatabase))
                .radioButton("Use fetched rows", bb -> bb.selected(useFetchedData)))
            .row(rb -> rb
                .panel(buildQueryDatabasePanel(queryDatabase))
                .panel(buildUseFetchedRowsPanel(useFetchedData)))
            .row(rb -> rb
                .expandableGroup("Advanced", false, pb1 -> pb1
                    .align(AlignX.FILL).grow()
                    .accept(buildAdvancedPanel(queryDatabase))));
    }

    @NotNull
    private Consumer<PanelBuilder> buildQueryDatabasePanel(@NotNull IObservableValue<Boolean> enabled) {
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
    private Consumer<PanelBuilder> buildUseFetchedRowsPanel(@NotNull IObservableValue<Boolean> enabled) {
        var canExportSelection = Bindings.of(hasCellSelection() && canExportColumns());

        return pb -> pb
            .row(rb -> rb
                .enabled(Bindings.and(enabled, canExportSelection))
                .checkBox("Selected rows only", bb -> bb.selected(selectedRowsOnly)))
            .row(rb -> rb
                .enabled(Bindings.and(enabled, canExportSelection))
                .checkBox("Selected columns only", bb -> bb.selected(selectedColumnsOnly)));
    }

    @NotNull
    private Consumer<PanelBuilder> buildAdvancedPanel(@NotNull IObservableValue<Boolean> queryDatabase) {
        var canChangeThreads = Bindings.of(getWizard().getSettings().getDataPipes().size() > 2);
        var canChangeSegment = ComputedValue.create(() -> extractType.getValue() == ExtractType.SEGMENTS);

        return pb -> pb
            .row(rb -> rb
                .enabled(Bindings.and(queryDatabase, canChangeThreads))
                .controlLabel(DTMessages.data_transfer_wizard_output_label_max_threads)
                .intTextField(threadCount, tb -> tb
                    .tooltip(DTUIMessages.database_producer_page_extract_settings_threads_num_text_tooltip)))
            .row(rb -> rb
                .enabled(queryDatabase)
                .controlLabel(DTUIMessages.database_producer_page_extract_settings_text_fetch_size_label)
                .intTextField(fetchSize, tb -> tb
                    .tooltip(DTUIMessages.database_producer_page_extract_settings_text_fetch_size_tooltip)))
            .row(rb -> rb
                .enabled(queryDatabase)
                .controlLabel(DTMessages.data_transfer_wizard_output_label_extract_type)
                .comboBox(extractType, IConverter.create(DatabaseProducerPageExtractSettings::getExtractTypeLabel)))
            .row(rb -> rb
                .enabled(Bindings.and(queryDatabase, canChangeSegment))
                .controlLabel(DTMessages.data_transfer_wizard_output_label_segment_size)
                .intTextField(segmentSize));
    }

    @Override
    public void activatePage() {
        getWizard().loadNodeSettings();

        var settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);

        // Query database
        openNewConnections.setValue(settings.isOpenNewConnections());
        fetchRowCount.setValue(settings.isQueryRowCount());

        // Fetched rows
        var useFetchedRows = settings.getFetchedRowsPolicy();
        strategy.setValue(useFetchedRows != null ? Strategy.USE_FETCHED_ROWS : Strategy.QUERY_DATABASE);
        selectedRowsOnly.setValue(useFetchedRows != null && useFetchedRows.selectedRowsOnly());
        selectedColumnsOnly.setValue(useFetchedRows != null && useFetchedRows.selectedColumnsOnly());

        // Advanced
        fetchSize.setValue(settings.getFetchSize());
        threadCount.setValue(getWizard().getSettings().getMaxJobCount());
        segmentSize.setValue(settings.getSegmentSize());
        extractType.setValue(settings.getExtractType());

        updatePageCompletion();
    }

    @Override
    public void deactivatePage() {
        var settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);

        // Query database
        settings.setOpenNewConnections(openNewConnections.getValue());
        settings.setQueryRowCount(fetchRowCount.getValue());

        // Fetched rows
        if (strategy.getValue() == Strategy.USE_FETCHED_ROWS) {
            boolean canExportSelection = hasCellSelection() && canExportColumns();
            settings.setFetchedRowsPolicy(new FetchedRowsPolicy(
                canExportSelection && selectedRowsOnly.getValue(),
                canExportSelection && selectedColumnsOnly.getValue()
            ));
        } else {
            settings.setFetchedRowsPolicy(null);
        }

        // Advanced
        settings.setFetchSize(fetchSize.getValue());
        getWizard().getSettings().setMaxJobCount(threadCount.getValue());
        settings.setSegmentSize(segmentSize.getValue());
        settings.setExtractType(extractType.getValue());
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

    private boolean hasCellSelection() {
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
}