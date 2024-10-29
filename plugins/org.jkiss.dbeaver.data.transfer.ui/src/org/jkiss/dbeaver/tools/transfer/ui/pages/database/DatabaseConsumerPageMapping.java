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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.*;
import org.jkiss.dbeaver.tools.transfer.internal.DTActivator;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferAttributeTransformerDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.DataTransferPageNodeSettings;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.ObjectContainerSelectorPanel;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseConsumerPageMapping extends DataTransferPageNodeSettings {
    private static final Log log = Log.getLog(DatabaseConsumerPageMapping.class);

    private static final String TARGET_NAME_BROWSE = "[browse]";
    private final List<DatabaseMappingContainer> model = new ArrayList<>();
    private TreeViewer mappingViewer;
    private Button loadMappingsButton;
    private Button autoAssignButton;
    private Button upButton;
    private Button downButton;
    private ObjectContainerSelectorPanel containerPanel;
    private boolean firstInit = true;
    private String mappingErrorMessage;

    private static abstract class MappingLabelProvider extends CellLabelProvider {
        @Override
        public void update(ViewerCell cell)
        {
        }
    }

    public DatabaseConsumerPageMapping() {
        super(DTUIMessages.database_consumer_page_mapping_name_and_title);
        setTitle(DTUIMessages.database_consumer_page_mapping_name_and_title);
        setDescription(DTUIMessages.database_consumer_page_mapping_description);
        setPageComplete(false);
    }

    private DatabaseConsumerSettings getDatabaseConsumerSettings() {
        return getWizard().getPageSettings(this, DatabaseConsumerSettings.class);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();

        Composite composite = UIUtils.createComposite(parent, 1);

        {
            // Target container
            // Use first source object as cur selection (it's better than nothing)
            containerPanel = new ObjectContainerSelectorPanel(composite,
                getWizard().getProject(),
                "container.data-transfer.database-consumer",
                DTMessages.data_transfer_db_consumer_target_container,
                DTMessages.data_transfer_db_consumer_choose_container)
            {
                @Nullable
                @Override
                protected DBNNode getSelectedNode() {
                    DBSObjectContainer container = settings.getContainer();
                    DBNNode selectedNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(container);
                    if (selectedNode == null && !settings.getDataMappings().isEmpty()) {
                        // Use first source object as cur selection (it's better than nothing)
                        DBSDataContainer firstSource = settings.getDataMappings().keySet().iterator().next();
                        selectedNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(firstSource);
                        while (selectedNode != null) {
                            if (selectedNode instanceof DBSWrapper && ((DBSWrapper) selectedNode).getObject() instanceof DBSObjectContainer) {
                                break;
                            } else {
                                selectedNode = selectedNode.getParentNode();
                            }
                        }
                    }
                    return selectedNode;
                }

                @Override
                protected void setSelectedNode(DBNDatabaseNode node) {
                    settings.setContainer(DBUtils.getAdapter(DBSObjectContainer.class, node.getObject()));
                    loadSettings(false);
                    setContainerInfo(node);
                    getWizard().runWithProgress(monitor -> {
                        // Reset mappings
                        for (DatabaseMappingContainer mappingContainer : settings.getDataMappings().values()) {
                            setMappingTarget(
                                monitor,
                                mappingContainer,
                                mappingContainer.getTargetName(),
                                true,
                                true);
                        }
                    });
                    mappingViewer.refresh();
                    updatePageCompletion();
                    setMessage(null);
                }

            };
        }

        Composite mappingsGroup = UIUtils.createComposite(composite, 2);
        mappingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        createMappingsTree(mappingsGroup);

        {
            // Control buttons
            Composite buttonsPanel = UIUtils.createComposite(mappingsGroup, 1);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_VERTICAL));

            final Button mapTableButton = UIUtils.createDialogButton(buttonsPanel,
                DTMessages.data_transfer_db_consumer_existing_table,
                DBIcon.TREE_TABLE,
                DTMessages.data_transfer_db_consumer_existing_table_description,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        mapExistingTables(getSelectedMappingContainers());
                    }
                });
            mapTableButton.setEnabled(false);

            UIUtils.createLabelSeparator(buttonsPanel, SWT.HORIZONTAL);

            final Button configureButton = UIUtils.createDialogButton(buttonsPanel,
                DTMessages.data_transfer_db_consumer_button_configure,
                DBIcon.TREE_COLUMNS,
                DTMessages.data_transfer_db_consumer_button_configure_description,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        DatabaseMappingObject selectedMapping = getSelectedMapping();
                        mapColumnsAndTable(
                            selectedMapping instanceof DatabaseMappingContainer
                                ? (DatabaseMappingContainer) selectedMapping : ((DatabaseMappingAttribute) selectedMapping).getParent());
                    }
                });
            configureButton.setEnabled(false);

            final Button previewButton = UIUtils.createDialogButton(buttonsPanel,
                DTMessages.data_transfer_wizard_page_preview_name,
                UIIcon.SQL_PREVIEW,
                DTMessages.data_transfer_wizard_page_preview_description,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        DBPDataSourceContainer dataSourceContainer = getDatabaseConsumerSettings().getContainer().getDataSource().getContainer();
                        if (!dataSourceContainer.hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_METADATA)) {
                            UIUtils.showMessageBox(getShell(), DTMessages.data_transfer_wizard_restricted_title, NLS.bind(DTMessages.data_transfer_wizard_restricted_description, dataSourceContainer.getName()), SWT.ICON_WARNING);
                            return;
                        }
                        DatabaseMappingObject selectedMapping = getSelectedMapping();
                        showPreview(selectedMapping instanceof DatabaseMappingContainer ?
                            (DatabaseMappingContainer) selectedMapping :
                            ((DatabaseMappingAttribute)selectedMapping).getParent());
                    }
                });
            previewButton.setEnabled(false);

            if (getWizard().getSettings().getDataPipes().size() > 1) {
                UIUtils.createLabelSeparator(buttonsPanel, SWT.HORIZONTAL);

                upButton = UIUtils.createDialogButton(buttonsPanel, DTMessages.data_transfer_db_consumer_up_label, UIIcon.ARROW_UP, DTMessages.data_transfer_db_consumer_up_tooltip, new SelectionAdapter() { //FIXME i18ze + tooltip
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DataTransferPipe pipe = getPipeFromCurrentSelection();
                        DatabaseMappingContainer mappingContainer = getMappingContainerFromCurrentSelection();
                        if (pipe == null || mappingContainer == null) {
                            return;
                        }
                        getWizard().getSettings().processPipeEarlier(pipe);
                        mappingViewer.getTree().setVisible(false);
                        CommonUtils.shiftLeft(model, mappingContainer);
                        mappingViewer.refresh();
                        mappingViewer.getTree().setVisible(true);
                        updateUpAndDownButtons(pipe);
                    }
                });
                upButton.setEnabled(false);

                downButton = UIUtils.createDialogButton(buttonsPanel, DTMessages.data_transfer_db_consumer_down_label, UIIcon.ARROW_DOWN, DTMessages.data_transfer_db_consumer_down_tooltip, new SelectionAdapter() { //FIXME i18ze + tooltip
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DataTransferPipe pipe = getPipeFromCurrentSelection();
                        DatabaseMappingContainer mappingContainer = getMappingContainerFromCurrentSelection();
                        if (pipe == null || mappingContainer == null) {
                            return;
                        }
                        getWizard().getSettings().processPipeLater(pipe);
                        mappingViewer.getTree().setVisible(false);
                        CommonUtils.shiftRight(model, mappingContainer);
                        mappingViewer.refresh();
                        mappingViewer.getTree().setVisible(true);
                        updateUpAndDownButtons(pipe);
                    }
                });
                downButton.setEnabled(false);
            }

            UIUtils.createLabelSeparator(buttonsPanel, SWT.HORIZONTAL);

            UIUtils.createEmptyLabel(buttonsPanel, 1, 1).setLayoutData(new GridData(GridData.FILL_VERTICAL));

            final Button mappingRules = UIUtils.createDialogButton(buttonsPanel,
                DTMessages.data_transfer_db_consumer_mapping_rules_button,
                null,
                DTMessages.data_transfer_db_consumer_mapping_rules_button_tip,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        updateMappingRules();
                    }
                });
            mappingRules.setEnabled(false);

            autoAssignButton = UIUtils.createDialogButton(buttonsPanel,
                DTMessages.data_transfer_db_consumer_auto_assign,
                UIIcon.ASTERISK,
                DTMessages.data_transfer_db_consumer_auto_assign_description,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        autoAssignMappings();
                    }
                });

            mappingViewer.getTree().addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    try {
                        boolean updated = false;
                        Object element = null;
                        if (e.character == SWT.CR) {
                            //Object element = mappingViewer.getStructuredSelection().getFirstElement();
                            //mappingViewer.editElement(element, 1);
                        } else if (e.character == SWT.DEL) {
                            for (TreeItem item : mappingViewer.getTree().getSelection()) {
                                element = item.getData();
                                if (element instanceof DatabaseMappingAttribute) {
                                    DatabaseMappingAttribute attribute = (DatabaseMappingAttribute) element;
                                    attribute.setMappingType(DatabaseMappingType.skip);
                                } else if (element instanceof DatabaseMappingContainer) {
                                    DatabaseMappingContainer container = (DatabaseMappingContainer) element;
                                    container.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.skip, false);
                                }
                                selectNextColumn(item);
                            }
                            updated = true;
                        } else if (e.character == SWT.SPACE) {
                            for (TreeItem item : mappingViewer.getTree().getSelection()) {
                                element = item.getData();
                                if (element instanceof DatabaseMappingAttribute) {
                                    DatabaseMappingAttribute attribute = (DatabaseMappingAttribute) item.getData();
                                    attribute.setMappingType(DatabaseMappingType.existing);
                                    attribute.updateMappingType(new LoggingProgressMonitor(log), false, false);
                                } else if (element instanceof DatabaseMappingContainer) {
                                    DatabaseMappingContainer container = (DatabaseMappingContainer) element;
                                    getWizard().runWithProgress(monitor ->
                                        setMappingTarget(
                                            monitor,
                                            container,
                                            container.getTargetName(),
                                            false,
                                            false));
                                }
                                selectNextColumn(item);
                            }
                            updated = true;
                        } else if (e.keyCode == SWT.INSERT) {
                            TreeItem[] selection = mappingViewer.getTree().getSelection();
                            if (selection.length > 0) {
                                mappingViewer.editElement(selection[0].getData(), 1);
                            }
                        }
                        if (updated) {
                            updateMappingsAndButtons();
                            updatePageCompletion();
                            if (element instanceof DatabaseMappingContainer) {
                                // Select next container
                                @SuppressWarnings("unchecked")
                                List<DatabaseMappingContainer> model = (List<DatabaseMappingContainer>) mappingViewer.getInput();
                                int curIndex = model.indexOf(element);
                                if (curIndex < model.size() - 1) {
                                    mappingViewer.setSelection(new StructuredSelection(model.get(curIndex + 1)), true);
                                }
                            }
                        }
                    } catch (DBException e1) {
                        DBWorkbench.getPlatformUI().showError(DTUIMessages.database_consumer_page_mapping_title_error_mapping_table,
                                DTUIMessages.database_consumer_page_mapping_message_error_mapping_target_table, e1);
                    }
                }
            });
            mappingViewer.addSelectionChangedListener(event -> {
                DatabaseMappingObject mapping = getSelectedMapping();
                mapTableButton.setEnabled(mapping instanceof DatabaseMappingContainer);
                //createNewButton.setEnabled(mapping instanceof DatabaseMappingContainer && settings.getContainerNode() != null);
                final boolean hasMappings = settings.getContainer() != null &&
                    ((mapping instanceof DatabaseMappingContainer && mapping.getMappingType() != DatabaseMappingType.unspecified) ||
                    (mapping instanceof DatabaseMappingAttribute && ((DatabaseMappingAttribute) mapping).getParent().getMappingType() != DatabaseMappingType.unspecified));
                configureButton.setEnabled(hasMappings);
                previewButton.setEnabled(hasMappings);
                mappingRules.setEnabled(hasMappings);
                updateUpAndDownButtons();
            });
            mappingViewer.addDoubleClickListener(event -> {
                DatabaseMappingObject selectedMapping = getSelectedMapping();
                if (selectedMapping != null) {
                    if (selectedMapping instanceof DatabaseMappingContainer){
/*
                        if (selectedMapping.getMappingType() == DatabaseMappingType.unspecified) {
                            mapExistingTable((DatabaseMappingContainer) selectedMapping);
                        } else {
                            mapColumnsAndTable((DatabaseMappingContainer) selectedMapping);
                        }
*/
                    }
                }
            });
        }

        {
            Composite hintPanel = new Composite(composite, SWT.NONE);
            hintPanel.setLayout(new GridLayout(3, false));
            hintPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            new Label(hintPanel, SWT.NONE).setText(DTUIMessages.database_consumer_page_mapping_label_hint);
        }
        composite.pack(true);
        setControl(composite);
    }

    private void updateUpAndDownButtons() {
        DataTransferPipe pipe = getPipeFromCurrentSelection();
        if (pipe != null) {
            updateUpAndDownButtons(pipe);
            return;
        }
        if (upButton != null) upButton.setEnabled(false);
        if (downButton != null) downButton.setEnabled(false);
    }

    private void updateUpAndDownButtons(@NotNull DataTransferPipe pipeFromCurrentSelection) {
        List<DataTransferPipe> pipes = getWizard().getSettings().getDataPipes();
        int idx = pipes.indexOf(pipeFromCurrentSelection);
        if (upButton != null) upButton.setEnabled(idx > 0);
        if (downButton != null) downButton.setEnabled(idx > -1 && idx < pipes.size() - 1);
    }

    private void selectNextColumn(TreeItem item) {
        TreeItem parentItem = item.getParentItem();
        if (parentItem != null) {
            TreeItem[] childItems = parentItem.getItems();
            int index = ArrayUtils.indexOf(childItems, item);
            if (index >= 0 && index < childItems.length - 1) {
                mappingViewer.setSelection(new StructuredSelection(childItems[index + 1].getData()));
            }
        }
    }

    private void createMappingsTree(Composite composite)
    {
        // Mapping table
        mappingViewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        mappingViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        mappingViewer.getTree().setLinesVisible(true);
        mappingViewer.getTree().setHeaderVisible(true);

        final DBPDataSourceContainer container = DatabaseConsumerSettings.getDataSourceContainer(getWizard().getSettings());
        if (container != null) {
            loadMappingsButton = new Button(mappingViewer.getTree(), SWT.PUSH);
            loadMappingsButton.setText(NLS.bind(DTUIMessages.columns_mapping_dialog_composite_button_reconnect, container.getName()));
            loadMappingsButton.setImage(DBeaverIcons.getImage(UIIcon.SQL_CONNECT));
            loadMappingsButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> loadSettings(true)));

            final ControlEditor overlay = new ControlEditor(mappingViewer.getTree());
            final Point buttonSize = loadMappingsButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            overlay.minimumWidth = buttonSize.x;
            overlay.minimumHeight = buttonSize.y;
            overlay.setEditor(loadMappingsButton);

            // Lines look weird on an empty table and visually clash with the button
            mappingViewer.getTree().setLinesVisible(false);
        }

        UIWidgets.setControlContextMenu(mappingViewer.getTree(), manager -> {
            IStructuredSelection selection = (IStructuredSelection) mappingViewer.getSelection();
            if (!selection.isEmpty()) {
                Object element = selection.getFirstElement();
                if (element instanceof DatabaseMappingAttribute) {
                    DatabaseMappingAttribute mapping= (DatabaseMappingAttribute) element;
                    if (mapping.getTransformer() != null && !mapping.getTransformer().getProperties().isEmpty()) {
                        manager.add(new Action("Transformer settings ...") {
                            @Override
                            public void run() {
                                AttributeTransformerSettingsDialog settingsDialog = new AttributeTransformerSettingsDialog(
                                    getShell(),
                                    (DatabaseMappingAttribute) element,
                                    mapping.getTransformer());
                                if (settingsDialog.open() != IDialogConstants.OK_ID) {
                                    return;
                                }
                            }
                        });
                    }
                }
            }
            UIWidgets.fillDefaultTreeContextMenu(manager, mappingViewer.getTree());
        });

        {
            TreeViewerColumn columnSource = new TreeViewerColumn(mappingViewer, SWT.LEFT);
            columnSource.setLabelProvider(new MappingLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    DatabaseMappingObject mapping = (DatabaseMappingObject) cell.getElement();
                    cell.setText(DBUtils.getObjectFullName(mapping.getSource(), DBPEvaluationContext.UI));
                    if (mapping.getIcon() != null) {
                        cell.setImage(DBeaverIcons.getImage(mapping.getIcon()));
                    }
                    super.update(cell);
                }
            });
            columnSource.getColumn().setText(DTUIMessages.database_consumer_page_mapping_column_source_text);
        }

        {
            TreeViewerColumn columnTarget = new TreeViewerColumn(mappingViewer, SWT.LEFT);
            columnTarget.setLabelProvider(new MappingLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    DatabaseMappingObject mapping = (DatabaseMappingObject) cell.getElement();
                    cell.setText(mapping.getTargetName());
                    if (mapping.getMappingType() == DatabaseMappingType.unspecified) {
                        cell.setBackground(UIUtils.getSharedTextColors().getColor(SharedTextColors.COLOR_WARNING));
                    } else {
                        cell.setBackground(null);
                    }
                    super.update(cell);
                }
            });
            columnTarget.getColumn().setText(DTUIMessages.database_consumer_page_mapping_column_target_text);
            columnTarget.setEditingSupport(new EditingSupport(mappingViewer) {
                @Override
                protected CellEditor getCellEditor(Object element) {
                    try {
                        return createTargetEditor(element);
                    } catch (DBException e) {
                        setErrorMessage(e.getMessage());
                        return null;
                    }
                }

                @Override
                protected boolean canEdit(Object element) {
                    return true;
                }

                @Override
                protected Object getValue(Object element) {
                    DatabaseMappingObject mapping = (DatabaseMappingObject) element;
                    if (mapping.getMappingType() == DatabaseMappingType.unspecified) {
                        String targetName = mapping.getTargetName();
                        if (!CommonUtils.isEmpty(targetName)) {
                            return targetName;
                        }
                        return transformTargetName(DBUtils.getQuotedIdentifier(mapping.getSource()), DatabaseMappingType.unspecified);
                    }
                    if (mapping instanceof DatabaseMappingContainer) {
                        DBSDataManipulator target = ((DatabaseMappingContainer) mapping).getTarget();
                        return target != null ? target : mapping.getTargetName();
                    } else {
                        if (mapping.getMappingType() == DatabaseMappingType.existing) {
                            return ((DatabaseMappingAttribute) mapping).getTarget();
                        }
                        return mapping.getTargetName();
                    }
                }

                @Override
                protected void setValue(final Object element, Object value) {
                    final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
                    String name = CommonUtils.toString(value);
                    DBPDataSource dataSource = settings.getTargetDataSource((DatabaseMappingObject) element);
                    if (!name.equals(DatabaseMappingAttribute.TARGET_NAME_SKIP) && !name.equals(TARGET_NAME_BROWSE)
                        && dataSource != null && !DBUtils.isQuotedIdentifier(dataSource, name)
                    ) {
                        name = DBObjectNameCaseTransformer.transformName(dataSource, name);
                    }
                    String finalName = name;
                    //getWizard().runWithProgress(monitor ->
                    setMappingTarget(new LoggingProgressMonitor(log), (DatabaseMappingObject) element, finalName, false, false);
                    mappingViewer.update(element, null);
                    mappingViewer.setSelection(mappingViewer.getSelection());
                    updatePageCompletion();
                }
            });
        }
        //TreeViewerEditor.create(mappingViewer, new TreeViewerFocusCellManager(), ColumnViewerEditor.TABBING_CYCLE_IN_ROW);

        {
            TreeViewerColumn columnMapping = new TreeViewerColumn(mappingViewer, SWT.LEFT);
            columnMapping.setLabelProvider(new MappingLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    DatabaseMappingObject mapping = (DatabaseMappingObject) cell.getElement();
                    cell.setText(mapping.getMappingType().name());
                    super.update(cell);
                }
            });
            columnMapping.getColumn().setText(DTUIMessages.database_consumer_page_mapping_column_mapping_text);
            columnMapping.setEditingSupport(new EditingSupport(mappingViewer) {
                @Override
                protected CellEditor getCellEditor(Object element) {
                    return createMappingTypeEditor((DatabaseMappingObject) element);
                }

                @Override
                protected boolean canEdit(Object element) {
                    return true;
                }

                @Override
                protected Object getValue(Object element) {
                    DatabaseMappingObject mapping = (DatabaseMappingObject) element;
                    return mapping.getMappingType().name();
                }

                @Override
                protected void setValue(Object element, Object value) {
                    try {
                        DatabaseMappingObject mapping = (DatabaseMappingObject) element;
                        DatabaseMappingType mappingType = DatabaseMappingType.valueOf(value.toString());
                        if (mapping.getMappingType() != DatabaseMappingType.recreate && mappingType == DatabaseMappingType.recreate) {
                            // Show this confirmation if mapping is not recreate at this moment
                            boolean confirmed = UIUtils.confirmAction(
                                getShell(),
                                DTUIMessages.database_consumer_page_mapping_recreate_confirm_title,
                                DTUIMessages.database_consumer_page_mapping_recreate_confirm_tip,
                                DBIcon.STATUS_WARNING
                            );
                            if (!confirmed) {
                                return;
                            }
                        }
                        if (mapping instanceof DatabaseMappingAttribute) {
                            ((DatabaseMappingAttribute) mapping).setMappingType(mappingType);
                        } else {
                            ((DatabaseMappingContainer) mapping).refreshMappingType(getWizard().getRunnableContext(), mappingType, false);
                        }
                        mappingViewer.refresh();
                        setErrorMessage(null);
                    } catch (DBException e) {
                        setErrorMessage(e.getMessage());
                    }
                }
            });
        }

        {
            TreeViewerColumn columnTransformer = new TreeViewerColumn(mappingViewer, SWT.LEFT);
            columnTransformer.setLabelProvider(new MappingLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    DataTransferAttributeTransformerDescriptor transformer = getTransformer (cell.getElement());
                    if (transformer != null) {
                        cell.setText(transformer.getName());
                    } else {
                        cell.setText("");
                    }
                    super.update(cell);
                }

                @Override
                public String getToolTipText(Object element) {
                    DataTransferAttributeTransformerDescriptor transformer = getTransformer (element);
                    if (transformer != null) {
                        Map<String, Object> props = ((DatabaseMappingAttribute) element).getTransformerProperties();
                        if (!CommonUtils.isEmpty(props)) {
                            return props.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining(GeneralUtils.getDefaultLineSeparator()));
                        }
                    }
                    return null;
                }
            });
            columnTransformer.getColumn().setText(DTUIMessages.database_consumer_page_mapping_column_transformer_text);
            columnTransformer.getColumn().setToolTipText(DTUIMessages.database_consumer_page_mapping_column_transformer_tip);
            columnTransformer.setEditingSupport(new EditingSupport(mappingViewer) {
                @Override
                protected CellEditor getCellEditor(Object element) {
                    if (element instanceof DatabaseMappingAttribute) {
                        List<DataTransferAttributeTransformerDescriptor> transformers = DataTransferRegistry.getInstance().getAttributeTransformers();
                        transformers.add(0, null);

                        List<String> tsfNames = transformers.stream().map(t->t == null ? "" : t.getName()).collect(Collectors.toList());

                        return new CustomComboBoxCellEditor(
                            mappingViewer,
                            mappingViewer.getTree(),
                            tsfNames.toArray(new String[0]),
                            SWT.DROP_DOWN | SWT.READ_ONLY);
                    } else {
                        return null;
                    }
                }

                @Override
                protected boolean canEdit(Object element) {
                    return element instanceof DatabaseMappingAttribute &&
                        ((DatabaseMappingAttribute) element).getMappingType().isValid();
                }

                @Override
                protected Object getValue(Object element) {
                    DataTransferAttributeTransformerDescriptor transformer = getTransformer(element);
                    return transformer == null ? "" : transformer.getName();
                }

                @Override
                protected void setValue(Object element, Object value) {
                    String tName = (String) value;
                    DataTransferAttributeTransformerDescriptor newTransformer;
                    if (CommonUtils.isEmpty(tName)) {
                        newTransformer = null;
                    } else {
                        newTransformer = DataTransferRegistry.getInstance().getAttributeTransformerByName(tName);
                    }
                    if (element instanceof DatabaseMappingAttribute) {
                        if (newTransformer == ((DatabaseMappingAttribute) element).getTransformer()) {
                            return;
                        }
                        if (newTransformer != null && !newTransformer.getProperties().isEmpty()) {
                            AttributeTransformerSettingsDialog settingsDialog = new AttributeTransformerSettingsDialog(
                                getShell(),
                                (DatabaseMappingAttribute) element,
                                newTransformer);
                            if (settingsDialog.open() != IDialogConstants.OK_ID) {
                                return;
                            }
                        }
                        ((DatabaseMappingAttribute) element).setTransformer(newTransformer);
                        mappingViewer.refresh();
                    }
                    setErrorMessage(null);
                }
            });
        }

        new DefaultViewerToolTipSupport(mappingViewer);
        mappingViewer.setContentProvider(new TreeContentProvider() {
            @Override
            public boolean hasChildren(Object element)
            {
                return element instanceof DatabaseMappingContainer;
            }

            @Override
            public Object[] getChildren(Object parentElement)
            {
                if (parentElement instanceof DatabaseMappingContainer) {
                    return ((DatabaseMappingContainer) parentElement).getAttributeMappings().toArray();
                }
                return null;
            }
        });
        mappingViewer.addDoubleClickListener(event -> {
            DatabaseMappingObject selectedMapping = getSelectedMapping();
            if (selectedMapping instanceof DatabaseMappingContainer) {
                mapColumnsAndTable((DatabaseMappingContainer) selectedMapping);
            } else if (selectedMapping instanceof DatabaseMappingAttribute) {
                mapColumnsAndTable(((DatabaseMappingAttribute) selectedMapping).getParent());
            }
        });
    }

    @NotNull
    private CustomComboBoxCellEditor createMappingTypeEditor(DatabaseMappingObject mapping) {
        List<String> mappingTypes = new ArrayList<>();
        DatabaseMappingType mappingType = mapping.getMappingType();
        if (mappingType != DatabaseMappingType.skip) {
            mappingTypes.add(mappingType.name());
        }
        if (mapping instanceof DatabaseMappingContainer) {
            if (mappingType == DatabaseMappingType.existing || mappingType == DatabaseMappingType.create) {
                // Recreate can be used for not-existing at this moment tables if user will save this mapping in the task
                mappingTypes.add(DatabaseMappingType.recreate.name());
            } else if (mappingType == DatabaseMappingType.recreate) {
                // Depends on the existence of the target table
                if (mapping.getTarget() != null) {
                    mappingTypes.add(DatabaseMappingType.existing.name());
                } else {
                    mappingTypes.add(DatabaseMappingType.create.name());
                }
            }
        }
        if (mapping instanceof DatabaseMappingAttribute) {
            DatabaseMappingType parentMapping = ((DatabaseMappingAttribute) mapping).getParent().getMappingType();
            if (mappingType != parentMapping && parentMapping == DatabaseMappingType.create) {
                mappingTypes.add(DatabaseMappingType.create.name());
            }
        }
        mappingTypes.add(DatabaseMappingType.skip.name());
        return new CustomComboBoxCellEditor(
            mappingViewer,
            mappingViewer.getTree(),
            mappingTypes.toArray(new String[0]),
            SWT.DROP_DOWN | SWT.READ_ONLY);
    }

    private CellEditor createTargetEditor(Object element) throws DBException
    {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        boolean allowsCreate = true;
        List<String> items = new ArrayList<>();
        if (element instanceof DatabaseMappingContainer) {
            if (settings.getContainer() == null) {
                allowsCreate = false;
            }
            if (settings.getContainer() != null) {
                // container's tables
                DBSObjectContainer container = settings.getContainer();
                for (DBSObject child : container.getChildren(new LoggingProgressMonitor(log))) {
                    if (child instanceof DBSDataManipulator) {
                        items.add(transformTargetName(
                            DBUtils.getQuotedIdentifier(child),
                            ((DatabaseMappingContainer) element).getMappingType()));
                    }
                }

            }
            items.add(TARGET_NAME_BROWSE);
        } else {
            DatabaseMappingAttribute mapping = (DatabaseMappingAttribute) element;
            switch (mapping.getParent().getMappingType()) {
                case skip:
                case unspecified:
                    allowsCreate = false;
                    break;
            }
            DBSDataManipulator target = mapping.getParent().getTarget();
            if (target instanceof DBSEntity) {
                DBSEntity parentEntity = (DBSEntity) target;
                for (DBSEntityAttribute attr : parentEntity.getAttributes(new LoggingProgressMonitor(log))) {
                    items.add(transformTargetName(DBUtils.getQuotedIdentifier(attr), mapping.getMappingType()));
                }
            } else if (target == null) {
                // New table?
                items.add(transformTargetName(DBUtils.getQuotedIdentifier(mapping.getSource()), mapping.getMappingType()));
            }

        }
        items.add(DatabaseMappingAttribute.TARGET_NAME_SKIP);
        CustomComboBoxCellEditor editor = new CustomComboBoxCellEditor(
            mappingViewer,
            mappingViewer.getTree(),
            items.toArray(new String[0]),
            SWT.DROP_DOWN | (allowsCreate ? SWT.NONE : SWT.READ_ONLY));
        return editor;
    }

    private void setMappingTarget(DBRProgressMonitor monitor, DatabaseMappingObject mapping, String name, boolean forceRefresh, boolean updateAttributesNames) {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        try {
            if (name.equals(DatabaseMappingAttribute.TARGET_NAME_SKIP)) {
                if (mapping instanceof DatabaseMappingAttribute) {
                    ((DatabaseMappingAttribute) mapping).setMappingType(DatabaseMappingType.skip);
                } else {
                    ((DatabaseMappingContainer) mapping).refreshMappingType(
                        monitor,
                        DatabaseMappingType.skip,
                        false,
                        false);
                }
            } else if (name.equals(TARGET_NAME_BROWSE)) {
                mapExistingTable((DatabaseMappingContainer) mapping);
            } else {
                name = transformTargetName(name, mapping.getMappingType());
                if (mapping instanceof DatabaseMappingContainer) {
                    DatabaseMappingContainer containerMapping = (DatabaseMappingContainer) mapping;
                    if (settings.getContainer() != null) {
                        // container's tables
                        DBSObjectContainer container = settings.getContainer();
                        String unQuotedNameForSearch = DBUtils.getUnQuotedIdentifier(container.getDataSource(), name);

                        DBSDataManipulator targetDataContainer = null;

                        // Check name conflict in namespace
                        DBSNamespaceContainer namespaceContainer = DBUtils.getAdapter(DBSNamespaceContainer.class, container);
                        if (namespaceContainer != null) {
                            DBSNamespace ns = namespaceContainer.getNamespaceForObjectType(RelationalObjectType.TYPE_TABLE);
                            if (ns != null) {
                                DBSObject existingObject = ns.getObjectByName(monitor, unQuotedNameForSearch);
                                if (existingObject != null) {
                                    if (existingObject instanceof DBSDataManipulator) {
                                        targetDataContainer = (DBSDataManipulator) existingObject;
                                    } else {
                                        containerMapping.setTargetName(name);
                                        containerMapping.refreshMappingType(
                                            monitor,
                                            DatabaseMappingType.unspecified,
                                            false,
                                            false);
                                        mappingErrorMessage =
                                            "Name '" + unQuotedNameForSearch + "' is already used by " + DBUtils.getObjectTypeName(existingObject);

                                        UIUtils.asyncExec(() -> {
                                            mappingViewer.refresh();
                                        });
                                        return;
                                    }
                                }
                            }
                        }

                        if (targetDataContainer == null) {
                            // Search for existing data manipulator (writable table)
                            for (DBSObject child : container.getChildren(monitor)) {
                                if (child instanceof DBSDataManipulator && unQuotedNameForSearch.equalsIgnoreCase(child.getName())) {
                                    targetDataContainer = (DBSDataManipulator) child;
                                    break;
                                }
                            }
                        }
                        if (targetDataContainer != null) {
                            containerMapping.setTarget(targetDataContainer);
                            if (forceRefresh && mapping.getMappingType() == DatabaseMappingType.recreate) {
                                // Keep container mapping type, refresh only attributes
                                containerMapping.refreshAttributesMappingTypes(monitor, false, false);
                            } else {
                                containerMapping.refreshMappingType(monitor, DatabaseMappingType.existing, false, false);
                            }

                            DBSDataManipulator finalTargetDataContainer = targetDataContainer;
                            UIUtils.asyncExec(() -> {
                                DataTransferPipe pipeFromCurrentSelection = getPipeFromCurrentSelection();
                                if (pipeFromCurrentSelection != null) {
                                    IDataTransferConsumer<?, ?> consumer = pipeFromCurrentSelection.getConsumer();
                                    if (consumer instanceof DatabaseTransferConsumer) {
                                        ((DatabaseTransferConsumer) consumer).setTargetObject(finalTargetDataContainer);
                                    }
                                }
                                mappingViewer.refresh();
                            });
                            return;
                        }
                    }
                    if (forceRefresh && mapping.getMappingType() == DatabaseMappingType.recreate) {
                        // Keep container mapping type, refresh only attributes
                        containerMapping.refreshAttributesMappingTypes(monitor, false, false);
                    } else {
                        containerMapping.setTarget(null);
                        containerMapping.setTargetName(name);
                        containerMapping.refreshMappingType(monitor, DatabaseMappingType.create, forceRefresh, updateAttributesNames);
                    }
                } else {
                    DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) mapping;
                    DBPDataSource targetDataSource = settings.getTargetDataSource(mapping);
                    if (attrMapping.getParent().getTarget() instanceof DBSEntity) {
                        DBSEntity parentEntity = (DBSEntity) attrMapping.getParent().getTarget();
                        Iterable<? extends DBSEntityAttribute> attributes = parentEntity.getAttributes(new LoggingProgressMonitor(log));
                        if (attributes != null) {
                            DBSEntityAttribute matchingAttribute = CommonUtils.findBestCaseAwareMatch(
                                attributes,
                                targetDataSource != null ? DBUtils.getUnQuotedIdentifier(targetDataSource, name) : name, // unquote for better search
                                DBSEntityAttribute::getName);
                            if (matchingAttribute != null) {
                                attrMapping.setMappingType(DatabaseMappingType.existing);
                                attrMapping.setTarget(matchingAttribute);
                                attrMapping.setTargetName(name);
                                return;
                            }
                        }
                    }
                    attrMapping.setMappingType(DatabaseMappingType.create);
                    attrMapping.setTargetName(updateAttributesNames && targetDataSource != null
                        ? DBUtils.getUnQuotedIdentifier(targetDataSource, name) : name);
                }
                UIUtils.asyncExec(this::updateMappingsAndButtons);
            }
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError(DTUIMessages.database_consumer_page_mapping_title_mapping_error,
                NLS.bind(DTUIMessages.database_consumer_page_mapping_message_error_auto_mapping_source_table, name),
                e);
        }
    }

    private void updateMappingRules() {
        DBSObjectContainer container = getDatabaseConsumerSettings().getContainer();
        if (container == null) {
            return;
        }
        DBPDataSource dataSource = container.getDataSource();
        if (dataSource == null) {
            return;
        }
        List<Object> elementList = Arrays.stream(mappingViewer.getTree().getItems())
            .map(Widget::getData).collect(Collectors.toList());
        MappingRulesDialog dialog = new MappingRulesDialog(getShell(), dataSource, elementList);
        if (dialog.open() == IDialogConstants.OK_ID) {
            mappingViewer.refresh();
            updateMappingsAndButtons();
            updatePageCompletion();
        }
    }

    void autoAssignMappings() {
        List<Object> elementList = Arrays.stream(mappingViewer.getTree().getItems())
            .map(Widget::getData).collect(Collectors.toList());

        getWizard().runWithProgress(monitor -> {
            if (getWizard().getSettings().getDataPipes().size() > 1) {
                getWizard().getSettings().sortDataPipes(monitor);
            }
            //loadAndUpdateColumnsModel();
            for (Object element : elementList) {
                if (element instanceof DatabaseMappingContainer) {
                    DatabaseMappingContainer container = (DatabaseMappingContainer) element;
                    setMappingTarget(monitor, container, container.getTargetName(), true, false);
                }
            }
        });
        updateMappingsAndButtons();
        updatePageCompletion();
    }

    private void updateAutoAssign() {
        boolean hasUnassigned = false;
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        for (DatabaseMappingContainer mapping : settings.getDataMappings().values()) {
            if (mapping.getMappingType() == DatabaseMappingType.unspecified || mapping.getMappingType() == DatabaseMappingType.skip) {
                hasUnassigned = true;
                break;
            }
        }
        autoAssignButton.setEnabled(hasUnassigned);
    }

    private void mapExistingTable(@NotNull DatabaseMappingContainer mapping) {
        mapExistingTables(new DatabaseMappingContainer[]{mapping});
    }

    private void mapExistingTables(@NotNull DatabaseMappingContainer[] mappings)
    {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject != null) {
            DBNNode rootNode = DBNUtils.getNodeByObject(settings.getContainer());
            if (rootNode == null) {
                rootNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(
                    activeProject).getDatabases();
            }
            DBNNode selectedNode = rootNode;
            if (mappings.length == 1 && mappings[0].getTarget() != null) {
                selectedNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(mappings[0].getTarget());
            }
            DBNNode node = DBWorkbench.getPlatformUI().selectObject(
                getShell(),
                DTUIMessages.database_consumer_page_mapping_node_title,
                rootNode,
                selectedNode,
                new Class[] {DBSObjectContainer.class, DBSDataManipulator.class},
                new Class[] {DBSDataManipulator.class},
                null);
            if (node != null && node instanceof DBSWrapper) {
                DBSObject object = ((DBSWrapper) node).getObject();
                try {
                    boolean needsUpdate = false;
                    for (final DatabaseMappingContainer mapping : mappings) {
                        if (object instanceof DBSDataManipulator) {
                            mapping.setTarget((DBSDataManipulator) object);
                            mapping.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.existing, false);
                            if (mappings.length == 1) {
                                // Call to this method also shows up a dialog.
                                // It could be very noisy in case of a large amount of mappings
                                mapColumnsAndTable(mapping);
                            } else {
                                needsUpdate = true;
                            }
                        } else {
                            mapping.setTarget(null);
                            mapping.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.unspecified, false);
                        }
                    }
                    if (needsUpdate) {
                        mappingViewer.refresh();
                        updatePageCompletion();
                    }
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError(DTUIMessages.database_consumer_page_mapping_title_error_mapping_table,
                            DTUIMessages.database_consumer_page_mapping_message_error_mapping_existing_table, e);
                }
                updateMappingsAndButtons();
                updatePageCompletion();
            }
        }
    }

    private void updateMappingsAndButtons() {
        mappingViewer.refresh();
        mappingViewer.setSelection(mappingViewer.getSelection());
    }

    private void mapNewTable(DatabaseMappingContainer mapping)
    {
        String tableName = EnterNameDialog.chooseName(
            getShell(),
            DTUIMessages.database_consumer_page_mapping_table_name,
            transformTargetName(
                mapping.getMappingType() == DatabaseMappingType.create ? mapping.getTargetName() : "",
                mapping.getMappingType()));
        if (!CommonUtils.isEmpty(tableName)) {
            try {
                mapping.setTargetName(tableName);
                mapping.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.create, false);
                updateMappingsAndButtons();
                updatePageCompletion();
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(DTUIMessages.database_consumer_page_mapping_title_mapping_error,
                        DTUIMessages.database_consumer_page_mapping_message_error_mapping_new_table, e);
            }
        }
    }

    private String transformTargetName(String name, @NotNull DatabaseMappingType mappingType) {
        DBSObjectContainer container = getDatabaseConsumerSettings().getContainer();
        if (container == null || container.getDataSource() == null) {
            return name;
        }
        if (mappingType == DatabaseMappingType.create) {
            return DatabaseTransferUtils.getTransformedName(container.getDataSource(), name, false);
        }
        return DBObjectNameCaseTransformer.transformName(container.getDataSource(), name);
    }

    private void mapColumnsAndTable(DatabaseMappingContainer mapping) {
        ConfigureMetadataStructureDialog dialog = new ConfigureMetadataStructureDialog(
            getWizard(),
            getDatabaseConsumerSettings(),
            mapping,
            this);
        if (dialog.open() == IDialogConstants.OK_ID) {
            mappingViewer.refresh();
            updatePageCompletion();
        }

    }

    private void showPreview(DatabaseMappingContainer mappingContainer) {
        DataTransferPipe pipe = getPipe(mappingContainer);
        DataTransferSettings dtSettings = getWizard().getSettings();

        PreviewMappingDialog previewDialog = new PreviewMappingDialog(
            getShell(),
            pipe,
            mappingContainer,
            dtSettings);
        previewDialog.open();
    }

    @Nullable
    private DatabaseMappingContainer getMappingContainerFromCurrentSelection() {
        for (Object o: mappingViewer.getStructuredSelection()) {
            if (o instanceof DatabaseMappingContainer) {
                return (DatabaseMappingContainer) o;
            }
        }
        return null;
    }

    @Nullable
    private DataTransferPipe getPipeFromCurrentSelection() {
        DatabaseMappingContainer mappingContainer = getMappingContainerFromCurrentSelection();
        if (mappingContainer == null) {
            return null;
        }
        return getPipe(mappingContainer);
    }

    @Nullable
    private DataTransferPipe getPipe(DatabaseMappingContainer mappingContainer) {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            if (pipe.getProducer() == null) {
                continue;
            }
            DBSDataContainer sourceObject = (DBSDataContainer)pipe.getProducer().getDatabaseObject();
            DatabaseMappingContainer mapping = settings.getDataMapping(sourceObject);
            if (mapping == mappingContainer) {
                return pipe;
            }
        }
        return null;
    }

    private DatabaseMappingObject getSelectedMapping()
    {
        IStructuredSelection selection = (IStructuredSelection) mappingViewer.getSelection();
        return selection.isEmpty() ? null : (DatabaseMappingObject) selection.getFirstElement();
    }

    @NotNull
    private DatabaseMappingContainer[] getSelectedMappingContainers() {
        final IStructuredSelection selection = (IStructuredSelection) mappingViewer.getSelection();
        final List<DatabaseMappingContainer> objects = new ArrayList<>();
        for (final Object o : selection) {
            if (o instanceof DatabaseMappingContainer) {
                objects.add((DatabaseMappingContainer) o);
            }
        }
        return objects.toArray(DatabaseMappingContainer[]::new);
    }

    @Override
    public void activatePage() {
        final DBPDataSourceContainer container = DatabaseConsumerSettings.getDataSourceContainer(getWizard().getSettings());
        final DBPPreferenceStore preferences = DTActivator.getDefault().getPreferences();

        if (getDatabaseConsumerSettings().getContainer() != null ||
            container != null && container.isConnected() ||
            preferences.getBoolean(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE)
        ) {
            loadSettings(true);
        }
    }

    private void loadSettings(boolean loadContainerFromSettings) {
        if (loadMappingsButton != null && !loadMappingsButton.isDisposed()) {
            loadMappingsButton.dispose();
            mappingViewer.getTree().setLinesVisible(true);
        }

        getWizard().loadNodeSettings();

        // Detect producer container (e.g. schema)
        DBSObjectContainer producerContainer = null;
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            if (pipe.getProducer() != null) {
                DBSObject producerObject = pipe.getProducer().getDatabaseObject();
                if (producerObject instanceof DBSDataContainer) {
                    DBSObject container = producerObject.getParentObject();
                    if (container instanceof DBSObjectContainer) {
                        producerContainer = (DBSObjectContainer) container;
                    }
                }
            }
        }

        if (loadContainerFromSettings) {
            DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
            settings.loadObjectContainer(getWizard().getRunnableContext(), getWizard().getSettings(), producerContainer);
            if (settings.getContainer() != null) {
                final DBNDatabaseNode[] containerNode = new DBNDatabaseNode[1];
                try {
                    getWizard().getRunnableContext().run(
                        true,
                        true,
                        monitor -> containerNode[0] = DBNUtils.getNodeByObject(monitor, settings.getContainer(), false)
                    );
                } catch (InvocationTargetException | InterruptedException ignored) {
                }
                if (containerNode[0] != null) {
                    try {
                        containerPanel.checkValidContainerNode(containerNode[0]);
                        containerPanel.setContainerInfo(containerNode[0]);
                    } catch (DBException e) {
                        setErrorMessage(e.getMessage());
                    }
                }
            }
        }

        loadAndUpdateColumnsModel();
        updatePageCompletion();

        if (firstInit) {
            firstInit = false;
            UIUtils.asyncExec(() -> {
                Tree table = mappingViewer.getTree();
                int totalWidth = table.getClientArea().width;
                TreeColumn[] columns = table.getColumns();
                columns[0].setWidth(totalWidth * 35 / 100);
                columns[1].setWidth(totalWidth * 35 / 100);
                columns[2].setWidth(totalWidth * 15 / 100);
                columns[3].setWidth(totalWidth * 15 / 100);
                this.autoAssignMappings();
            });
        }
    }

    private void loadAndUpdateColumnsModel() {
        // Load columns model. Update it only if mapping have different set of source columns
        // Otherwise we keep current mappings (to allow wizard page navigation without loosing mappings)
        DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        model.clear();

        List<Throwable> errors = new ArrayList<>();
        try {
            getWizard().getRunnableContext().run(true, true, monitor -> {
                for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
                    if (pipe.getProducer() == null || !(pipe.getProducer().getDatabaseObject() instanceof DBSDataContainer)) {
                        continue;
                    }
                    DBSDataContainer sourceDataContainer = (DBSDataContainer) pipe.getProducer().getDatabaseObject();
                    DatabaseMappingContainer mapping = settings.getDataMapping(sourceDataContainer);
                    // Create new mapping for source object
                    DatabaseMappingContainer newMapping;
                    IDataTransferConsumer<?, ?> pipeConsumer = pipe.getConsumer();
                    if (pipeConsumer instanceof DatabaseTransferConsumer && ((DatabaseTransferConsumer) pipeConsumer).getTargetObject() != null) {
                        try {
                            newMapping = new DatabaseMappingContainer(
                                monitor,
                                getDatabaseConsumerSettings(),
                                sourceDataContainer,
                                ((DatabaseTransferConsumer) pipe.getConsumer()).getTargetObject());
                        } catch (DBException e) {
                            errors.add(e);
                            newMapping = new DatabaseMappingContainer(getDatabaseConsumerSettings(), sourceDataContainer);
                        }
                    } else {
                        newMapping = new DatabaseMappingContainer(getDatabaseConsumerSettings(), sourceDataContainer);
                    }
                    newMapping.getAttributeMappings();
                    // Update current mapping if it differs from new one
                    if (mapping == null || !mapping.isSameMapping(newMapping)) {
                        mapping = newMapping;
                        settings.addDataMappings(getWizard().getRunnableContext(), sourceDataContainer, mapping);
                    }
                    model.add(mapping);
                }
            });
        } catch (InvocationTargetException e) {
            errors.add(e.getTargetException());
        } catch (InterruptedException e) {
            errors.add(e);
        }

        if (!errors.isEmpty()) {
            Throwable lastError = errors.get(errors.size() - 1);
            log.error(lastError);
            setMessage(lastError.getMessage(), IMessageProvider.ERROR);
        }


        mappingViewer.getTree().setVisible(false);
        Object[] expandedElements = mappingViewer.getExpandedElements();
        mappingViewer.setInput(model);
        mappingViewer.setExpandedElements(expandedElements);
        mappingViewer.getTree().setVisible(true);

        if (!model.isEmpty()) {
            // Select first element
            mappingViewer.setSelection(new StructuredSelection(model.get(0)));
        }
    }

    @Override
    protected boolean determinePageCompletion()
    {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        if (settings.getContainer() == null) {
            setErrorMessage(DTUIMessages.database_consumer_page_mapping_error_message_set_target_container);
            return false;
        }
        if (!settings.isCompleted(getWizard().getSettings().getDataPipes())) {
            String errorMessage = DTUIMessages.database_consumer_page_mapping_error_message_set_all_tables_mappings;
            if (mappingErrorMessage != null) {
                errorMessage += ": " + mappingErrorMessage;
            }
            setErrorMessage(errorMessage);
            return false;
        } else {
            setErrorMessage(null);
            return true;
        }
    }

    @Override
    protected void updatePageCompletion() {
        super.updatePageCompletion();
        updateAutoAssign();
    }

    private DataTransferAttributeTransformerDescriptor getTransformer(Object element) {
        if (element instanceof DatabaseMappingAttribute) {
            return ((DatabaseMappingAttribute) element).getTransformer();
        }
        return null;
    }

    @Override
    public boolean isPageApplicable() {
        return isConsumerOfType(DatabaseTransferConsumer.class);
    }


}
