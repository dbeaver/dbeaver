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

import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProjectDatabases;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamMappingAttribute;
import org.jkiss.dbeaver.tools.transfer.stream.StreamMappingContainer;
import org.jkiss.dbeaver.tools.transfer.stream.StreamMappingType;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.stream.StreamConsumerPageSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.UIWidgets;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.navigator.dialogs.ObjectBrowserDialog;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DataTransferPagePipes extends ActiveWizardPage<DataTransferWizard> {

    private static final Log log = Log.getLog(DataTransferPagePipes.class);
    public static final String DATABASE_PRODUCER_ID = "database_producer";
    public static final String DATABASE_CONSUMER_ID = "database_consumer";
    private boolean activated;
    private TableViewer nodesTable;
    private TableViewer migrationTable;
    private TableViewer inputsTable;
    private TransferTarget lastExportTarget;
    private ToolItem removeButton;
    private ToolItem addButton;
    private ToolItem addQueryButton;
    private ToolItem editQueryButton;
    private ToolItem columnsButton;

    private static class TransferTarget {
        DataTransferNodeDescriptor node;
        DataTransferProcessorDescriptor processor;

        private TransferTarget(DataTransferNodeDescriptor node, DataTransferProcessorDescriptor processor) {
            this.node = node;
            this.processor = processor;
        }
    }

    DataTransferPagePipes(@NotNull DataTransferSettings settings) {
        super(DTMessages.data_transfer_wizard_init_title);

        if (settings.isConsumerOptional()) {
            setTitle(DTMessages.data_transfer_wizard_init_title);
            setDescription(DTMessages.data_transfer_wizard_init_description);
        } else {
            setTitle(DTMessages.data_transfer_wizard_producers_title);
            setDescription(DTMessages.data_transfer_wizard_producers_description);
        }
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);
        if (!isDataImport()) {
            GridLayout layout = (GridLayout) composite.getLayout();
            layout.marginWidth = 5;
            layout.marginHeight = 5;
            layout.verticalSpacing = 5;
        }

        Composite sources = createInputsTable(composite);
        Composite targets = createNodesTable(composite);
        if (!isDataImport()) {
            composite.setTabList(new Control[]{sources, targets});
        }

        setControl(composite);

        inputsTable.getTable().addControlListener(ControlListener.controlResizedAdapter(e -> packTablesColumns()));
        nodesTable.getTable().addControlListener(ControlListener.controlResizedAdapter(e -> packTablesColumns()));
        if (migrationTable != null) {
            migrationTable.getTable().addControlListener(ControlListener.controlResizedAdapter(e -> packTablesColumns()));
        }
    }

    private void packTablesColumns() {
        Table sources = inputsTable.getTable();
        if (sources.getColumnCount() == 3) {
            int width = sources.getClientArea().width - 4;
            if (width > 0) {
                TableColumn[] columns = sources.getColumns();
                columns[0].setWidth(width * 43 / 100);
                columns[1].setWidth(width * 42 / 100);
                columns[2].setWidth(width - columns[0].getWidth() - columns[1].getWidth());
            }
        } else {
            UIUtils.packColumns(sources, true);
        }
        nodesTable.getTable().getColumn(0).setWidth(Math.max(1, nodesTable.getTable().getClientArea().width - 4));
        if (migrationTable != null) {
            migrationTable.getTable().getColumn(0).setWidth(Math.max(1, migrationTable.getTable().getClientArea().width - 4));
        }
    }

    @NotNull
    private Composite createNodesTable(@NotNull Composite composite) {
        boolean dataImport = isDataImport();
        if (!dataImport) {
            Composite targets = UIUtils.createComposite(composite, 2);
            GridData targetsData = new GridData(GridData.FILL_BOTH);
            targetsData.widthHint = 0;
            targets.setLayoutData(targetsData);
            ((GridLayout) targets.getLayout()).makeColumnsEqualWidth = true;
            Composite exportPanel = UIUtils.createComposite(targets, 1);
            GridData exportData = new GridData(GridData.FILL_BOTH);
            exportData.widthHint = 0;
            exportPanel.setLayoutData(exportData);
            UIUtils.createControlLabel(exportPanel, DTUIMessages.data_transfer_wizard_format_group);
            nodesTable = createTargetTable(exportPanel, true);
            Composite migrationPanel = UIUtils.createComposite(targets, 1);
            GridData migrationData = new GridData(GridData.FILL_BOTH);
            migrationData.widthHint = 0;
            migrationPanel.setLayoutData(migrationData);
            UIUtils.createControlLabel(migrationPanel, DTUIMessages.data_transfer_wizard_migration_group);
            migrationTable = createTargetTable(migrationPanel, true);
            nodesTable.getTable().addListener(SWT.FocusIn, e -> {
                if (nodesTable.getSelection().isEmpty() && !migrationTable.getSelection().isEmpty() &&
                    nodesTable.getInput() instanceof List<?> options && !options.isEmpty()) {
                    nodesTable.setSelection(new StructuredSelection(getLastExportTarget(options)));
                    nodesTable.getTable().showSelection();
                    migrationTable.setSelection(StructuredSelection.EMPTY);
                    setSelectedSettings(true);
                }
            });
            exportPanel.setTabList(new Control[]{nodesTable.getTable()});
            migrationPanel.setTabList(new Control[]{migrationTable.getTable()});
            targets.setTabList(new Control[]{exportPanel, migrationPanel});
            return targets;
        } else {
            UIUtils.createControlLabel(composite, DTUIMessages.data_transfer_wizard_final_column_source_format);
            nodesTable = createTargetTable(composite, false);
            return composite;
        }
    }

    @NotNull
    private TableViewer createTargetTable(@NotNull Composite panel, boolean fillVertical) {
        TableViewer viewer = new TableViewer(panel, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = viewer.getTable();
        GridData gd = fillVertical ? new GridData(GridData.FILL_BOTH) : new GridData(SWT.FILL, SWT.TOP, true, false);
        if (!isDataImport()) {
            gd.widthHint = 0;
        }
        table.setLayoutData(gd);
        table.setLinesVisible(isDataImport());
        viewer.setContentProvider((IStructuredContentProvider) inputElement -> {
            if (inputElement instanceof Collection<?> collection) {
                return collection.toArray();
            }
            return new Object[0];
        });
        CellLabelProvider labelProvider = new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                TransferTarget element = (TransferTarget) cell.getElement();
                String label;
                if (cell.getColumnIndex() == 0) {
                    if (element.processor != null) {
                        cell.setImage(DBeaverIcons.getImage(element.processor.getIcon()));
                        label = element.processor.getName();
                    } else {
                        cell.setImage(DBeaverIcons.getImage(element.node.getIcon()));
                        label = element.node.getName();
                    }
                } else {
                    if (element.processor != null) {
                        label = element.processor.getDescription();
                    } else {
                        label = element.node.getDescription();
                    }
                    cell.setForeground(table.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                }
                cell.setText(label);
            }

            @Override
            public String getToolTipText(Object element) {
                if (element instanceof TransferTarget tt) {
                    if (tt.processor != null) {
                        return tt.processor.getDescription();
                    }
                }
                return super.getToolTipText(element);
            }
        };
        ColumnViewerToolTipSupport.enableFor(viewer);
        {
            TableViewerColumn columnName = new TableViewerColumn(viewer, SWT.LEFT);
            columnName.setLabelProvider(labelProvider);
            columnName.getColumn().setText(DTMessages.data_transfer_wizard_init_column_exported);

//            TableViewerColumn columnDesc = new TableViewerColumn(nodesTable, SWT.RIGHT);
//            columnDesc.setLabelProvider(labelProvider);
//            columnDesc.getColumn().setText(DTMessages.data_transfer_wizard_init_column_description);
        }

        table.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (viewer == migrationTable) {
                    nodesTable.setSelection(StructuredSelection.EMPTY);
                } else if (migrationTable != null) {
                    lastExportTarget = (TransferTarget) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                    migrationTable.setSelection(StructuredSelection.EMPTY);
                }
                setSelectedSettings(true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
                if (isPageComplete()) {
                    getWizard().getContainer().nextPressed();
                }
            }
        });
        table.addListener(SWT.KeyDown, e -> {
            TableViewer other = e.keyCode == SWT.ARROW_RIGHT && viewer == nodesTable ? migrationTable :
                e.keyCode == SWT.ARROW_LEFT && viewer == migrationTable ? nodesTable : null;
            if (other != null && other.getInput() instanceof List<?> options && !options.isEmpty()) {
                if (viewer == nodesTable) {
                    lastExportTarget = (TransferTarget) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                }
                Object target = other == nodesTable ? getLastExportTarget(options) : options.getFirst();
                other.setSelection(new StructuredSelection(target));
                other.getTable().showSelection();
                viewer.setSelection(StructuredSelection.EMPTY);
                other.getTable().setFocus();
                setSelectedSettings(true);
                e.doit = false;
            }
        });
        return viewer;
    }

    @NotNull
    private Object getLastExportTarget(@NotNull List<?> options) {
        if (lastExportTarget != null) {
            for (Object option : options) {
                if (option instanceof TransferTarget target && target.node == lastExportTarget.node &&
                    target.processor == lastExportTarget.processor) {
                    return target;
                }
            }
        }
        return options.getFirst();
    }

    private void setSelectedSettings(boolean forceUpdate) {
        IStructuredSelection selection = (IStructuredSelection) nodesTable.getSelection();
        if (selection.isEmpty() && migrationTable != null) {
            selection = (IStructuredSelection) migrationTable.getSelection();
        }
        TransferTarget target;
        if (!selection.isEmpty()) {
            target = (TransferTarget) selection.getFirstElement();
        } else {
            target = null;
        }
        DataTransferSettings settings = getWizard().getSettings();
        if (target == null) {
            settings.selectConsumer(null, null, true);
        } else {
            if (settings.isConsumerOptional()) {
                if (forceUpdate || settings.getConsumer() == null || !hasTargetDescriptor(settings.getConsumer())) {
                    settings.selectConsumer(target.node, target.processor, true);
                }
            } else if (settings.isProducerOptional()) {
                if (forceUpdate || settings.getProducer() == null || !hasTargetDescriptor(settings.getProducer())) {
                    settings.selectProducer(target.node, target.processor, true);
                }
            } else {
                // no optional nodes
            }
        }
        updatePageCompletion();
        getWizard().getContainer().updateNavigationTree();
        updateSourceButtons();
    }

    private boolean hasTargetDescriptor(@Nullable DataTransferNodeDescriptor descriptor) {
        if (descriptor != null) {
            return hasTargetDescriptor(nodesTable, descriptor) ||
                migrationTable != null && hasTargetDescriptor(migrationTable, descriptor);
        }
        return false;
    }

    private boolean hasTargetDescriptor(@NotNull TableViewer viewer, @NotNull DataTransferNodeDescriptor descriptor) {
        if (viewer.getInput() instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof TransferTarget target) {
                    if (target.node != null && target.node.getId().equals(descriptor.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @NotNull
    private Composite createInputsTable(Composite composite) {
        Composite panel = UIUtils.createComposite(composite, 1);
        GridData panelData = new GridData(GridData.FILL_HORIZONTAL);
        if (!isDataImport()) {
            panelData.widthHint = 0;
        }
        panel.setLayoutData(panelData);

        boolean dataImport = isDataImport();

        UIUtils.createControlLabel(panel,
            dataImport ?
                DTUIMessages.data_transfer_wizard_final_column_target :
                DTUIMessages.data_transfer_wizard_final_column_source);

        Composite inputTable = UIUtils.createComposite(panel, dataImport ? 1 : 2);
        GridData inputData = new GridData(GridData.FILL_HORIZONTAL);
        if (!dataImport) {
            inputData.widthHint = 0;
        }
        inputTable.setLayoutData(inputData);

        inputsTable = new TableViewer(inputTable, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        Table table = inputsTable.getTable();
        gd.heightHint = 5 * table.getItemHeight();
        if (!dataImport) {
            gd.widthHint = 0;
        }
        table.setLayoutData(gd);
        table.setLinesVisible(dataImport);
        table.setHeaderVisible(true);
        if (!dataImport) {
            table.addPaintListener(e -> {
                if (table.getItemCount() == 0) {
                    String message = DTUIMessages.data_transfer_wizard_empty_sources;
                    var bounds = table.getClientArea();
                    var extent = e.gc.textExtent(message, SWT.DRAW_DELIMITER);
                    e.gc.setForeground(table.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                    e.gc.drawText(message, Math.max(0, (bounds.width - extent.x) / 2),
                        Math.max(table.getHeaderHeight(), (bounds.height - extent.y) / 2),
                        SWT.DRAW_DELIMITER | SWT.DRAW_TRANSPARENT);
                }
            });
        }
        inputsTable.setContentProvider(new ListContentProvider());
        UIWidgets.createTableContextMenu(table, null);
        DBNModel nModel = DBWorkbench.getPlatform().getNavigatorModel();
        CellLabelProvider labelProvider = new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DBSObject element = (DBSObject) cell.getElement();
                if (cell.getColumnIndex() == 0) {
                    DBPImage icon;
                    DBSEntity entity = DBUtils.getAdapter(DBSEntity.class, element);
                    if (entity != null) {
                        DBNDatabaseNode objectNode = nModel.getNodeByObject(entity);
                        icon = objectNode != null ? objectNode.getNodeIconDefault() : DBValueFormatting.getObjectImage(element);
                    } else {
                        icon = UIIcon.SQL_SCRIPT;
                    }
                    cell.setImage(DBeaverIcons.getImage(icon));
                    final SQLQueryContainer queryContainer = DBUtils.getAdapter(SQLQueryContainer.class, element);
                    if (queryContainer != null) {
                        cell.setText(
                            CommonUtils.truncateString(
                                CommonUtils.getSingleLineString(queryContainer.getQuery().getText()), 1024));
                    } else {
                        cell.setText(
                            CommonUtils.truncateString(
                                element.getName(), 1024));
                    }
                } else if (cell.getColumnIndex() == 1) {
                    DBPDataSource dataSource = element.getDataSource();
                    DBPDataSourceContainer container = dataSource == null ? null : dataSource.getContainer();
                    cell.setImage(container == null ? null : DBeaverIcons.getImage(container.getDriver().getIcon()));
                    cell.setText(container == null ? "" : container.getName());
                } else if (cell.getColumnIndex() == 2) {
                    StreamConsumerSettings streamSettings = getStreamConsumerSettings();
                    StreamMappingContainer mapping = streamSettings == null || !(element instanceof DBSDataContainer source)
                        ? null : streamSettings.getDataMapping(source);
                    if (mapping != null && DBUtils.getAdapter(SQLQueryContainer.class, element) == null) {
                        List<StreamMappingAttribute> attributes = mapping.getAttributes(new org.jkiss.dbeaver.model.runtime.VoidProgressMonitor());
                        long selected = attributes.stream()
                            .filter(attribute -> attribute.getMappingType() == StreamMappingType.export).count();
                        cell.setText(selected + " / " + attributes.size());
                    } else {
                        cell.setText("");
                    }
                }
            }

            @Override
            public String getToolTipText(Object element) {
                final SQLQueryContainer queryContainer = DBUtils.getAdapter(SQLQueryContainer.class, element);
                if (queryContainer != null) {
                    return CommonUtils.truncateString(queryContainer.getQuery().getText(), 64000);
                } else if (element instanceof DBSObject object) {
                    return CommonUtils.truncateString(
                        DBUtils.getObjectFullName(object, DBPEvaluationContext.UI), 64000);
                } else {
                    return null;
                }
            }
        };
        ColumnViewerToolTipSupport.enableFor(inputsTable);
        TableViewerColumn sourceColumn = new TableViewerColumn(inputsTable, SWT.LEFT);
        sourceColumn.getColumn().setText(dataImport ? DTUIMessages.data_transfer_wizard_final_column_target :
            DTUIMessages.data_transfer_wizard_source_table);
        sourceColumn.setLabelProvider(labelProvider);
        if (!dataImport) {
            TableViewerColumn dataSourceColumn = new TableViewerColumn(inputsTable, SWT.LEFT);
            dataSourceColumn.getColumn().setText(DTUIMessages.data_transfer_wizard_data_source);
            dataSourceColumn.setLabelProvider(labelProvider);
            TableViewerColumn columnsColumn = new TableViewerColumn(inputsTable, SWT.RIGHT);
            columnsColumn.getColumn().setText(DTMessages.data_transfer_wizard_settings_group_preview_columns);
            columnsColumn.setLabelProvider(labelProvider);
        }
        if (!dataImport) {
            table.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateSourceButtons()));
            table.addListener(SWT.DefaultSelection, e -> {
                if (editQueryButton.isEnabled()) {
                    editQueryButton.notifyListeners(SWT.Selection, new Event());
                } else if (columnsButton.isEnabled()) {
                    columnsButton.notifyListeners(SWT.Selection, new Event());
                }
            });
            ToolBar sourceToolbar = createConfigureColumnsButton(inputTable);
            inputTable.setTabList(new Control[]{table, sourceToolbar});
            panel.setTabList(new Control[]{inputTable});
            updateSourceButtons();
        }
        return panel;
    }

    private ToolBar createConfigureColumnsButton(@NotNull Composite parent) {
        ToolBar buttonsToolbar = new ToolBar(parent, SWT.VERTICAL);
        buttonsToolbar.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        columnsButton = UIUtils.createToolItem(
            buttonsToolbar,
            DTUIMessages.data_transfer_wizard_configure_columns,
            DBIcon.TREE_COLUMNS,
            SelectionListener.widgetSelectedAdapter(selectionEvent -> {
                getWizard().loadNodeSettings();
                final List<StreamMappingContainer> mappings = new ArrayList<>();

                StreamConsumerSettings streamConsumerSettings = getStreamConsumerSettings();
                if (streamConsumerSettings == null) {
                    DBWorkbench.getPlatformUI().showError(
                        DTMessages.stream_transfer_consumer_title_configuration_load_failed,
                        "Current configuration do not support stream settings"
                    );
                    return;
                }
                try {
                    List<?> selectedSources = ((IStructuredSelection) inputsTable.getSelection()).toList();
                    UIUtils.runInProgressDialog(monitor -> refreshMappings(monitor, selectedSources, streamConsumerSettings, mappings));
                } catch (InvocationTargetException e) {
                    DBWorkbench.getPlatformUI().showError(
                        DTMessages.stream_transfer_consumer_title_configuration_load_failed,
                        DTMessages.stream_transfer_consumer_message_cannot_load_configuration,
                        e
                    );
                    return;
                }

                if (new ConfigureColumnsDialog(getShell(), mappings, streamConsumerSettings).open() == org.eclipse.jface.window.Window.OK) {
                    inputsTable.refresh();
                }
            })
        );
        UIUtils.createToolBarSeparator(buttonsToolbar, SWT.HORIZONTAL);
        addButton = UIUtils.createToolItem(buttonsToolbar,
            DTUIMessages.data_transfer_task_configurator_dialog_button_label_add_table, DBIcon.TREE_TABLE_ADD,
            SelectionListener.widgetSelectedAdapter(e -> addTables()));
        addQueryButton = UIUtils.createToolItem(buttonsToolbar,
            DTUIMessages.data_transfer_task_configurator_dialog_button_label_add_query, UIIcon.SQL_SCRIPT_CREATE,
            SelectionListener.widgetSelectedAdapter(e -> addQuery()));
        editQueryButton = UIUtils.createToolItem(buttonsToolbar,
            DTMessages.data_transfer_wizard_settings_button_edit, UIIcon.EDIT,
            SelectionListener.widgetSelectedAdapter(e -> editQuery()));
        removeButton = UIUtils.createToolItem(buttonsToolbar,
            DTUIMessages.data_transfer_task_configurator_dialog_button_label_remove, UIIcon.REMOVE,
            SelectionListener.widgetSelectedAdapter(e -> removeSources()));
        return buttonsToolbar;
    }

    private void setConfigureColumnsButtonVisible(boolean visible) {
        columnsButton.setEnabled(visible && !inputsTable.getSelection().isEmpty() && !getWizard().getSettings().isPipeChangeRestricted());
    }

    private void updateSourceButtons() {
        if (removeButton == null) {
            return;
        }
        boolean editable = !getWizard().getSettings().isPipeChangeRestricted() && !getWizard().isTaskEditor();
        addButton.setEnabled(editable);
        addQueryButton.setEnabled(editable);
        DBSObject selectedSource = (DBSObject) ((IStructuredSelection) inputsTable.getSelection()).getFirstElement();
        boolean querySelected = selectedSource != null && DBUtils.getAdapter(SQLQueryContainer.class, selectedSource) != null;
        editQueryButton.setEnabled(editable && querySelected);
        removeButton.setEnabled(editable && !inputsTable.getSelection().isEmpty());
        addButton.setToolTipText(editable ? DTUIMessages.data_transfer_task_configurator_dialog_button_label_add_table :
            DTUIMessages.data_transfer_wizard_sources_locked);
        addQueryButton.setToolTipText(editable ? DTUIMessages.data_transfer_task_configurator_dialog_button_label_add_query :
            DTUIMessages.data_transfer_wizard_sources_locked);
        editQueryButton.setToolTipText(!editable ? DTUIMessages.data_transfer_wizard_sources_locked :
            querySelected ? DTMessages.data_transfer_wizard_settings_button_edit : DTUIMessages.data_transfer_wizard_select_query);
        removeButton.setToolTipText(!editable ? DTUIMessages.data_transfer_wizard_sources_locked :
            inputsTable.getSelection().isEmpty() ? DTUIMessages.data_transfer_wizard_select_source :
                DTUIMessages.data_transfer_task_configurator_dialog_button_label_remove);
        inputsTable.getTable().redraw();
        setConfigureColumnsButtonVisible((getWizard().getSettings().getConsumer() == null ||
            !DATABASE_CONSUMER_ID.equals(getWizard().getSettings().getConsumer().getId())) &&
            ((IStructuredSelection) inputsTable.getSelection()).toList().stream().noneMatch(source -> DBUtils.getAdapter(SQLQueryContainer.class, source) != null));
        columnsButton.setToolTipText(!columnsButton.isEnabled() ?
            getWizard().getSettings().isPipeChangeRestricted() ? DTUIMessages.data_transfer_wizard_sources_locked :
                inputsTable.getSelection().isEmpty() ? DTUIMessages.data_transfer_wizard_select_source :
                    DTUIMessages.data_transfer_wizard_columns_unavailable : null);
    }

    private void addTables() {
        DataTransferSettings settings = getWizard().getSettings();
        DBPProject project = settings.getProject();
        DBNProjectDatabases root = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(project).getDatabases();
        DBNNode initialNode = null;
        for (DBSObject source : settings.getSourceObjects().reversed()) {
            if (source instanceof DBSDataContainer && DBUtils.getAdapter(SQLQueryContainer.class, source) == null) {
                DBSObject container = source.getParentObject();
                initialNode = container == null ? null : project.getNavigatorModel().getNodeByObject(container);
                if (initialNode == null) {
                    initialNode = getLastDataSourceNode(root, source);
                }
                break;
            }
        }
        List<DBNNode> selected = ObjectBrowserDialog.selectObjects(getShell(),
            DTUIMessages.data_transfer_task_configurator_tables_title_choose_source, root,
            CommonUtils.singletonOrEmpty(initialNode),
            new Class[]{DBSInstance.class, DBSObjectContainer.class, DBSDataContainer.class},
            new Class[]{DBSDataContainer.class}, null);
        if (selected == null || selected.isEmpty()) {
            return;
        }
        List<DataTransferPipe> pipes = new ArrayList<>(settings.getDataPipes());
        for (DBNNode node : selected) {
            if (node instanceof DBNDatabaseNode databaseNode && databaseNode.getObject() instanceof DBSDataContainer source &&
                settings.getSourceObjects().stream().noneMatch(existing -> existing == source)) {
                pipes.add(new DataTransferPipe(new DatabaseTransferProducer(source), null));
            }
        }
        updateSources(pipes);
    }

    @Nullable
    private DBNNode getLastDataSourceNode(@NotNull DBNProjectDatabases root, @NotNull DBSObject source) {
        DBPDataSource dataSource = source.getDataSource();
        return dataSource == null ? null : root.getDataSource(dataSource.getContainer().getId());
    }

    private void addQuery() {
        DataTransferSettings settings = getWizard().getSettings();
        DBPProject project = settings.getProject();
        DBNProjectDatabases root = project.getNavigatorModel().getRoot().getProjectNode(project).getDatabases();
        DBNNode initialNode = settings.getSourceObjects().isEmpty() ? null :
            getLastDataSourceNode(root, settings.getSourceObjects().getLast());
        DBNNode node = ObjectBrowserDialog.selectObject(getShell(),
            DTUIMessages.data_transfer_wizard_choose_data_source, root, initialNode,
            new Class[]{DBPDataSourceContainer.class}, new Class[]{DBPDataSourceContainer.class},
            new Class[]{DBPDataSourceContainer.class});
        DBSObject scope = node instanceof DBNDataSource dataSourceNode ? dataSourceNode.getDataSource() :
            node instanceof DBNDatabaseItem databaseItem ? databaseItem.getObject() : null;
        if (scope == null) {
            return;
        }
        DBPDataSource dataSource = scope.getDataSource();
        DBPDataSourceContainer container = DBUtils.getContainer(dataSource);
        if (container != null && !container.isConnected()) {
            try {
                UIUtils.runInProgressDialog(monitor -> {
                    try {
                        container.connect(monitor, true, true);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError(
                    DTUIMessages.data_transfer_task_configurator_title_error_opening_data_source,
                    DTUIMessages.data_transfer_task_configurator_message_error_while_opening_data_source, e);
                return;
            }
        }

        String catalog = scope instanceof DBSCatalog ? scope.getName() :
            scope.getParentObject() instanceof DBSCatalog parentCatalog ? parentCatalog.getName() : null;
        String schema = scope instanceof DBSSchema ? scope.getName() : null;
        DataSourceContextProvider contextProvider = new DataSourceContextProvider(scope);
        DBCExecutionContext context = contextProvider.getExecutionContext();
        String previousCatalog = null;
        String previousSchema = null;
        if (context instanceof DBCExecutionContextDefaults<?, ?> defaults) {
            previousCatalog = defaults.getDefaultCatalog() == null ? null : defaults.getDefaultCatalog().getName();
            previousSchema = defaults.getDefaultSchema() == null ? null : defaults.getDefaultSchema().getName();
        }
        try {
            DBExecUtils.setExecutionContextDefaults(new VoidProgressMonitor(), dataSource, context, catalog, null, schema);
            UIServiceSQL sqlService = DBWorkbench.getService(UIServiceSQL.class);
            if (sqlService != null) {
                String query = sqlService.openSQLEditor(contextProvider,
                    DTUIMessages.data_transfer_task_configurator_sql_query_title, DBIcon.TREE_SCRIPT, "");
                if (query != null) {
                    SQLScriptContext scriptContext = new SQLScriptContext(null, contextProvider, null,
                        new PrintWriter(System.err, true), null);
                    SQLQueryDataContainer querySource = new SQLQueryDataContainer(contextProvider,
                        new SQLQuery(dataSource, query), scriptContext, log);
                    DatabaseTransferProducer producer = new DatabaseTransferProducer(querySource);
                    producer.setDefaultCatalog(catalog);
                    producer.setDefaultSchema(schema);
                    List<DataTransferPipe> pipes = new ArrayList<>(settings.getDataPipes());
                    pipes.add(new DataTransferPipe(producer, null));
                    updateSources(pipes);
                }
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(
                DTUIMessages.data_transfer_task_configurator_title_error_opening_data_source,
                DTUIMessages.data_transfer_task_configurator_message_error_while_opening_data_source, e);
        } finally {
            try {
                DBExecUtils.setExecutionContextDefaults(new VoidProgressMonitor(), dataSource, context,
                    previousCatalog, null, previousSchema);
            } catch (DBException e) {
                log.warn("Error restoring context defaults", e);
            }
        }
    }

    private void editQuery() {
        Object selected = ((IStructuredSelection) inputsTable.getSelection()).getFirstElement();
        if (!(selected instanceof SQLQueryDataContainer querySource)) {
            return;
        }
        UIServiceSQL sqlService = DBWorkbench.getService(UIServiceSQL.class);
        if (sqlService != null) {
            String query = sqlService.openSQLEditor(new DataSourceContextProvider(querySource),
                DTUIMessages.data_transfer_task_configurator_sql_query_title, DBIcon.TREE_SCRIPT,
                querySource.getQuery().getText());
            if (query != null) {
                querySource.setQuery(new SQLQuery(querySource.getDataSource(), query));
                inputsTable.refresh(querySource);
            }
        }
    }

    private void removeSources() {
        IStructuredSelection selection = (IStructuredSelection) inputsTable.getSelection();
        if (selection.isEmpty()) {
            return;
        }
        String question = selection.size() == 1
            ? NLS.bind(DTUIMessages.data_transfer_task_configurator_confirm_action_question,
                CommonUtils.truncateString(((DBSObject) selection.getFirstElement()).getName(), 255))
            : NLS.bind(DTUIMessages.data_transfer_wizard_confirm_remove_sources, selection.size());
        if (!UIUtils.confirmAction(DTUIMessages.data_transfer_task_configurator_confirm_action_title, question)) {
            return;
        }
        DataTransferSettings settings = getWizard().getSettings();
        List<DataTransferPipe> pipes = new ArrayList<>(settings.getDataPipes());
        for (Object selected : selection.toArray()) {
            pipes.removeIf(pipe -> pipe.getProducer() != null && pipe.getProducer().getDatabaseObject() == selected);
        }
        updateSources(pipes);
    }

    private void updateSources(@NotNull List<DataTransferPipe> pipes) {
        DataTransferSettings settings = getWizard().getSettings();
        if (pipes.size() == settings.getDataPipes().size()) {
            return;
        }
        settings.setDataPipes(pipes, true);
        getWizard().loadSettings();
        inputsTable.setInput(settings.getSourceObjects());
        loadNodeSettings();
        getWizard().getContainer().updateNavigationTree();
        updateSourceButtons();
    }

    private boolean isDataImport() {
        return getWizard().getSettings().isProducerOptional();
    }

    @Nullable
    private StreamConsumerSettings getStreamConsumerSettings() {
        StreamConsumerPageSettings page = getWizard().getPage(StreamConsumerPageSettings.class);
        if (page == null) {
            return null;
        }
        return getWizard().getPageSettings(page, StreamConsumerSettings.class);
    }

    private void refreshMappings(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<?> sources,
        @NotNull StreamConsumerSettings settings,
        @NotNull List<StreamMappingContainer> mappings
    ) {
        try {
            monitor.beginTask("Load mappings", sources.size());
            for (Object object : sources) {
                if (!(object instanceof DBSDataContainer source)) {
                    continue;
                }
                StreamMappingContainer mapping = settings.getDataMapping(source);

                if (mapping == null) {
                    mapping = new StreamMappingContainer(source);

                    for (StreamMappingAttribute attribute : mapping.getAttributes(monitor)) {
                        attribute.setMappingType(StreamMappingType.export);
                    }
                } else {
                    // Create a copy to avoid direct modifications
                    mapping = new StreamMappingContainer(mapping);
                }

                mappings.add(mapping);
                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }
    }

    @Override
    public void activatePage() {
        DataTransferWizard wizard = getWizard();
        DataTransferNodeDescriptor consumer = wizard.getSettings().getConsumer();
        // resolve the last export database on the mapping page, which handles auto reconnect
        if (wizard.isTaskEditor() || isDataImport() || consumer == null ||
            !DatabaseTransferConsumer.class.isAssignableFrom(consumer.getNodeClass())) {
            wizard.loadNodeSettings();
        }

        inputsTable.setInput(getWizard().getSettings().getSourceObjects());
        updateSourceButtons();
        if (!isDataImport()) {
            UIUtils.asyncExec(() -> {
                if (!inputsTable.getTable().isDisposed() && getWizard().getContainer().getCurrentPage() == this) {
                    inputsTable.getTable().setFocus();
                }
            });
        }
        if (!activated) {
            UIUtils.asyncExec(this::loadNodeSettings);
        }
        if (activated && getWizard().getSettings().isPipeChangeRestricted()) {
            // Second activation - we need to disable any selectors
            nodesTable.getTable().setEnabled(false);
            if (migrationTable != null) {
                migrationTable.getTable().setEnabled(false);
            }
            updateSourceButtons();
            return;
        }
        activated = true;
    }

    private void loadNodeSettings() {
        if (getWizard().getSettings().isConsumerOptional()) {
            loadConsumers();
        } else {
            loadProducers();
        }

        DataTransferNodeDescriptor consumer = getWizard().getSettings().getConsumer();
        DataTransferNodeDescriptor producer = getWizard().getSettings().getProducer();
        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();
        List<TransferTarget> targets = new ArrayList<>((List<TransferTarget>) nodesTable.getInput());
        if (migrationTable != null) {
            targets.addAll((List<TransferTarget>) migrationTable.getInput());
        }
        TransferTarget currentTarget = null;
        if (consumer != null || producer != null) {
            for (TransferTarget target : targets) {
                if ((target.node == consumer || target.node == producer) &&
                    (target.processor == null || target.processor == processor)
                ) {
                    currentTarget = target;
                    break;
                }
            }
        }
        if (currentTarget == null && !targets.isEmpty()) {
            currentTarget = targets.getFirst();
        }

        inputsTable.setInput(getWizard().getSettings().getSourceObjects());

        if (currentTarget != null) {
            StructuredSelection selection = new StructuredSelection(currentTarget);
            if (migrationTable != null && ((List<?>) migrationTable.getInput()).contains(currentTarget)) {
                nodesTable.setSelection(StructuredSelection.EMPTY);
                migrationTable.setSelection(selection);
                migrationTable.getTable().showSelection();
            } else {
                if (migrationTable != null) {
                    migrationTable.setSelection(StructuredSelection.EMPTY);
                }
                nodesTable.setSelection(selection);
                nodesTable.getTable().showSelection();
                lastExportTarget = currentTarget;
            }
            setSelectedSettings(false);
        }

        packTablesColumns();

        updatePageCompletion();
    }

    private void loadConsumers() {
        final DataTransferWizard wizard = getWizard();
        DataTransferSettings settings = wizard.getSettings();
        Collection<DBSObject> objects = settings.getSourceObjects();

        List<TransferTarget> transferTargets = new ArrayList<>();
        for (DataTransferNodeDescriptor consumer : DataTransferRegistry.getInstance().getAvailableConsumers(objects)) {
            if (consumer.isAdvancedNode() && !DBWorkbench.hasFeature(DTConstants.PRODUCT_FEATURE_ADVANCED_DATA_TRANSFER)) {
                continue;
            }
            if (DATABASE_CONSUMER_ID.equals(consumer.getId())
                && !DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_DATABASE_DEVELOPER)) {
                continue;
            }
            if (wizard.isTaskEditor() && settings.getConsumer() != null && !settings.getConsumer().getId().equals(consumer.getId())) {
                continue;
            }
            Collection<DataTransferProcessorDescriptor> processors = consumer.getAvailableProcessors(objects);
            if (CommonUtils.isEmpty(processors)) {
                transferTargets.add(new TransferTarget(consumer, null));
            } else {
                for (DataTransferProcessorDescriptor processor : processors) {
                    transferTargets.add(new TransferTarget(consumer, processor));
                }
            }
        }
        if (migrationTable != null) {
            nodesTable.setInput(transferTargets.stream().filter(target -> !DATABASE_CONSUMER_ID.equals(target.node.getId())).toList());
            migrationTable.setInput(transferTargets.stream().filter(target -> DATABASE_CONSUMER_ID.equals(target.node.getId())).toList());
        } else {
            nodesTable.setInput(transferTargets);
        }
    }

    private void loadProducers() {
        final DataTransferWizard wizard = getWizard();
        DataTransferSettings settings = wizard.getSettings();
        Collection<DBSObject> objects = settings.getSourceObjects();

        List<TransferTarget> transferTargets = new ArrayList<>();
        for (DataTransferNodeDescriptor producer : DataTransferRegistry.getInstance().getAvailableProducers(objects)) {
            if (producer.isAdvancedNode() && !DBWorkbench.hasFeature(DTConstants.PRODUCT_FEATURE_ADVANCED_DATA_TRANSFER)) {
                continue;
            }
            if (DATABASE_PRODUCER_ID.equals(producer.getId())
                && !DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_DATABASE_DEVELOPER)) {
                continue;
            }
            if (wizard.isTaskEditor() && settings.getProducer() != null && !settings.getProducer().getId().equals(producer.getId())) {
                continue;
            }

            Collection<DataTransferProcessorDescriptor> processors = producer.getAvailableProcessors(objects);
            if (CommonUtils.isEmpty(processors)) {
                transferTargets.add(new TransferTarget(producer, null));
            } else {
                for (DataTransferProcessorDescriptor processor : processors) {
                    transferTargets.add(new TransferTarget(producer, processor));
                }
            }
        }
        nodesTable.setInput(transferTargets);
    }

    @Override
    protected boolean determinePageCompletion() {
        DataTransferSettings settings = getWizard().getSettings();
        if (settings.getDataPipes().isEmpty()) {
            setErrorMessage(DTUIMessages.data_transfer_error_no_objects_selected);
            return false;
        }
        if (settings.getConsumer() == null || settings.getProducer() == null) {
            return false;
        }
//        if (settings.isProducerOptional()) {
//            settings.setProcessorProperties();
//        }

        return true;
    }

}
