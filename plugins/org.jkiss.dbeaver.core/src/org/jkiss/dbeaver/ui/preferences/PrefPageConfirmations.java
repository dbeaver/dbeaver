/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DefaultViewerToolTipSupport;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomCheckboxCellEditor;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.registry.ConfirmationDescriptor;
import org.jkiss.dbeaver.ui.registry.ConfirmationRegistry;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PrefPageConfirmations
 */
public class PrefPageConfirmations extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.confirmations"; //$NON-NLS-1$

    private Table confirmTable;
    private List<ConfirmationWithStatus> confirmations = new ArrayList<>();
    private Map<ConfirmationDescriptor, Object> changedConfirmations = new HashMap<>();

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        TableViewer tableViewer = new TableViewer(
            composite,
            SWT.BORDER | SWT.UNDERLINE_SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

        confirmTable = tableViewer.getTable();
        confirmTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        confirmTable.setHeaderVisible(true);
        confirmTable.setLinesVisible(true);

        ViewerColumnController<Object, Object> columnsController = new ViewerColumnController<>(
            "PrefPageConfirmationsEditor", //$NON-NLS-1$
            tableViewer);

        columnsController.addColumn(
            CoreMessages.pref_page_confirmations_table_column_confirmation,
            CoreMessages.pref_page_confirmations_table_column_confirmation_tip,
            SWT.LEFT,
            true,
            true,
            new ColumnLabelProvider() {

            @Override
            public String getToolTipText(Object element) {
                if (element instanceof ConfirmationWithStatus) {
                    return ((ConfirmationWithStatus) element).confirmation.getDescription();
                }
                return null;
            }

            @Override
            public String getText(Object element) {
                if (element instanceof ConfirmationWithStatus) {
                    return ((ConfirmationWithStatus) element).confirmation.getTitle();
                }
                return super.getText(element);
            }

        });

        columnsController.addBooleanColumn(
            CoreMessages.pref_page_confirmations_table_column_value,
            CoreMessages.pref_page_confirmations_table_column_value_tip,
            SWT.CENTER,
            true,
            true,
            item -> {
                if (item instanceof ConfirmationWithStatus) {
                return  ((ConfirmationWithStatus) item).enabled;
            }
            return false;
        }, new EditingSupport(tableViewer) {

                @Override
                protected CellEditor getCellEditor(Object element) {
                    return new CustomCheckboxCellEditor(tableViewer.getTable());
                }

                @Override
                protected boolean canEdit(Object element) {
                    return true;
                }

                @Override
                protected Object getValue(Object element) {
                    if (element instanceof ConfirmationWithStatus) {
                        return ((ConfirmationWithStatus) element).enabled;
                    }
                    return false;
                }

                @Override
                protected void setValue(Object element, Object value) {
                    if (element instanceof ConfirmationWithStatus) {
                        ConfirmationWithStatus confirmation = (ConfirmationWithStatus) element;
                        confirmation.enabled = CommonUtils.getBoolean(value, true);
                        changedConfirmations.put((confirmation).confirmation, value);
                    }

                }
            });

        columnsController.addColumn(
            CoreMessages.pref_page_confirmations_table_column_group,
            CoreMessages.pref_page_confirmations_table_column_group,
            SWT.RIGHT,
            true,
            true,
            new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof ConfirmationWithStatus) {
                    return ((ConfirmationWithStatus) element).confirmation.getGroup();
                }
                return super.getText(element);
            }
        });

        columnsController.createColumns(false);
        tableViewer.setContentProvider(new ListContentProvider());
        new DefaultViewerToolTipSupport(tableViewer);

        Collection<ConfirmationDescriptor> descriptors = ConfirmationRegistry.getInstance().getConfirmations().stream()
            .sorted(Comparator.comparing(ConfirmationDescriptor::getGroup))
            .collect(Collectors.toList());
        for (ConfirmationDescriptor confirmation : descriptors) {
            String toggleMessage = confirmation.getToggleMessage();
            if (CommonUtils.isNotEmpty(toggleMessage)) {
                // We do not want to see confirmation without a toggle message in the preferences
                // because we do not want to add the user's ability to ignore these confirmations
                this.confirmations.add(new ConfirmationWithStatus(confirmation, getCurrentConfirmValue(confirmation.getId())));
            }
        }

        tableViewer.setInput(this.confirmations);
        tableViewer.refresh();

        UIUtils.asyncExec(() -> UIUtils.packColumns(confirmTable, true));

        return composite;
    }

    private Boolean getCurrentConfirmValue(String id) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        String value = store.getString(ConfirmationDialog.PREF_KEY_PREFIX + id);
        if (CommonUtils.isEmpty(value)) {
            return Boolean.TRUE;
        }

        switch (value) {
            case ConfirmationDialog.NEVER: // backward compatibility
            case "false":
                return Boolean.FALSE;
            case ConfirmationDialog.ALWAYS: // backward compatibility
            default: return Boolean.TRUE;
        }
    }


    @Override
    public boolean performOk() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        for (Map.Entry<ConfirmationDescriptor, Object> entry : changedConfirmations.entrySet()) {
            String id = entry.getKey().getId();
            // Store values as String and not as boolean because of the backward compatibility problems
            store.setValue(ConfirmationDialog.PREF_KEY_PREFIX + id, entry.getValue().toString());
        }
        PrefUtils.savePreferenceStore(store);
        return super.performOk();
    }

    @Override
    protected void performDefaults() {
        // All elements true by default
        for (ConfirmationWithStatus confirmation : confirmations) {
            if (!confirmation.enabled) {
                confirmation.enabled = true;
                changedConfirmations.put((confirmation).confirmation, Boolean.TRUE);
            }
        }
        super.performDefaults();
    }

    private class ConfirmationWithStatus {

        private ConfirmationDescriptor confirmation;
        private boolean enabled;

        ConfirmationWithStatus(ConfirmationDescriptor confirmation, boolean enabled) {
            this.confirmation = confirmation;
            this.enabled = enabled;
        }
    }
}