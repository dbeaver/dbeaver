/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.handler;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetDataContainerOptions;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class CopyAsConfigurationDialog extends BaseDialog {
    private static final Log log = Log.getLog(CopyAsConfigurationDialog.class);

    private final Map<DataTransferProcessorDescriptor, Map<String, Object>> propertiesMap = CopyAsConfigurationStorage.getProcessorProperties();
    private final IResultSetController resultSetController;

    private TableViewer processorsTable;
    private PropertyTreeViewer propertyEditor;
    private PropertySourceCustom propertySource;
    private DataTransferProcessorDescriptor selectedProcessor;

    CopyAsConfigurationDialog(IResultSetController resultSetController) {
        super(UIUtils.getActiveShell(), ResultSetMessages.dialog_copy_as_configuration_name, null);
        this.resultSetController = resultSetController;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.PROCEED_LABEL, false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
    }

    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        SashForm sash = new SashForm(dialogArea, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        createNodesTable(sash);
        propertyEditor = new PropertyTreeViewer(sash, SWT.BORDER);

        ResultSetDataContainerOptions options = new ResultSetDataContainerOptions();
        ResultSetDataContainer dataContainer = new ResultSetDataContainer(resultSetController, options);
        List<DataTransferProcessorDescriptor> model = DataTransferRegistry.getInstance().getAvailableConsumers(Collections.singleton(dataContainer)).stream()
            .flatMap(node -> Arrays.stream(node.getProcessors()))
            .filter(processor -> !processor.isBinaryFormat())
            .sorted(Comparator.comparing(DataTransferProcessorDescriptor::getName))
            .collect(Collectors.toList());
        if (!model.isEmpty()) {
            processorsTable.setInput(model);
            selectedProcessor = model.get(0);
            processorsTable.setSelection(new StructuredSelection(selectedProcessor));
            showPropertiesForSelectedProcessor();
        } else {
            log.debug("No appropriate descriptor found, nothing to add to the configure page");
        }

        UIUtils.maxTableColumnsWidth(processorsTable.getTable());
        sash.setWeights(50, 50);

        return dialogArea;
    }

    private void createNodesTable(@NotNull Composite composite) {
        Composite panel = UIUtils.createComposite(composite, 1);

        processorsTable = new TableViewer(panel, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        processorsTable.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        processorsTable.getTable().setLinesVisible(true);
        processorsTable.setContentProvider(new IStructuredContentProvider() {
            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            @Override
            public Object[] getElements(Object inputElement) {
                if (inputElement instanceof Collection) {
                    return ((Collection<?>) inputElement).toArray();
                }
                return new Object[0];
            }
        });

        CellLabelProvider labelProvider = new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DataTransferProcessorDescriptor processorDescriptor = (DataTransferProcessorDescriptor) cell.getElement();
                if (processorDescriptor == null) {
                    log.debug("Unable to provide label for cell: cell's element is null");
                    return;
                }

                if (cell.getColumnIndex() == 0) {
                    cell.setImage(DBeaverIcons.getImage(processorDescriptor.getIcon()));
                    cell.setText(processorDescriptor.getName());
                } else {
                    cell.setText(processorDescriptor.getDescription());
                }
            }
        };

        TableViewerColumn columnName = new TableViewerColumn(processorsTable, SWT.LEFT);
        columnName.setLabelProvider(labelProvider);
        columnName.getColumn().setText("");
        TableViewerColumn columnDesc = new TableViewerColumn(processorsTable, SWT.LEFT);
        columnDesc.setLabelProvider(labelProvider);
        columnDesc.getColumn().setText("");

        processorsTable.getTable().addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                propertiesMap.put(selectedProcessor, propertySource.getPropertiesWithDefaults());
                TableItem tableItem = (TableItem) e.item;
                selectedProcessor = (DataTransferProcessorDescriptor) tableItem.getData();
                showPropertiesForSelectedProcessor();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
    }

    @Override
    protected void buttonPressed(int buttonId) {
        propertyEditor.saveEditorValues();
        super.buttonPressed(buttonId);
    }

    @Override
    protected void okPressed() {
        propertiesMap.put(selectedProcessor, propertySource.getPropertiesWithDefaults());
        try {
            CopyAsConfigurationStorage.saveProcessorProperties(propertiesMap);
        } catch (IOException e) {
            DBWorkbench.getPlatformUI().showError(
                ResultSetMessages.dialog_copy_as_configuration_error_saving_processor_properties_title,
                ResultSetMessages.dialog_copy_as_configuration_error_saving_processor_properties_message,
                e
            );
        }
        super.okPressed();
    }

    private void showPropertiesForSelectedProcessor() {
        Map<String, Object> properties = propertiesMap.computeIfAbsent(selectedProcessor, proc -> new HashMap<>());
        propertySource = new PropertySourceCustom(selectedProcessor.getProperties(), properties);
        propertyEditor.loadProperties(propertySource);
    }
}
