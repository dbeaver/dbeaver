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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.confirmation.ConfirmationConstants;
import org.jkiss.dbeaver.registry.confirmation.ConfirmationDescriptor;
import org.jkiss.dbeaver.registry.confirmation.ConfirmationRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DefaultViewerToolTipSupport;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * PrefPageConfirmations
 */
public class PrefPageConfirmations extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.confirmations"; //$NON-NLS-1$
    private static final String DEFAULT_CONFIRM_PREF_KEY_PREFIX = "org.jkiss.dbeaver.core.confirmDefault."; //$NON-NLS-1$

    private TableViewer tableViewer;
    private Table confirmTable;
    private int activeColumn = 1;
    private final List<ConfirmationWithStatus> confirmations = new ArrayList<>();
    private final Map<ConfirmationDescriptor, String> changedConfirmations = new HashMap<>();
    private final Map<ConfirmationDescriptor, Boolean> changedDefaultConfirmations = new HashMap<>();

    @Override
    public void init(IWorkbench workbench) {

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        tableViewer = new TableViewer(
            composite,
            SWT.BORDER | SWT.UNDERLINE_SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

        confirmTable = tableViewer.getTable();
        confirmTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        confirmTable.setHeaderVisible(true);
        confirmTable.setLinesVisible(true);

        TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(
            tableViewer, new FocusCellOwnerDrawHighlighter(tableViewer));
        // Install cell navigation without activating a checkbox editor on clicks or double-clicks.
        TableViewerEditor.create(tableViewer, focusCellManager, new ColumnViewerEditorActivationStrategy(tableViewer) {
            @Override
            protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
                return false;
            }
        }, 0);
        confirmTable.addListener(SWT.MouseUp, event -> {
            if (event.button == 1) {
                ViewerCell cell = tableViewer.getCell(new Point(event.x, event.y));
                if (cell != null) {
                    activeColumn = cell.getColumnIndex() == 2 ? 2 : 1;
                    confirmTable.setFocus();
                    toggleCell(cell.getElement(), cell.getColumnIndex());
                }
            }
        });
        Listener spaceKeyListener = event -> {
            if (event.widget != confirmTable || (event.keyCode != SWT.SPACE && event.character != SWT.SPACE)) {
                return;
            }
            if (confirmTable.getSelection().length != 1) {
                return;
            }
            ViewerCell cell = focusCellManager.getFocusCell();
            // The focus cell can be cleared when the selected row is redrawn after a toggle.
            int column = cell != null && cell.getItem() == confirmTable.getSelection()[0]
                && (cell.getColumnIndex() == 1 || cell.getColumnIndex() == 2)
                ? cell.getColumnIndex() : activeColumn;
            if (toggleCell(confirmTable.getSelection()[0].getData(), column)) {
                event.doit = false;
            }
        };
        Display display = confirmTable.getDisplay();
        display.addFilter(SWT.KeyDown, spaceKeyListener);
        confirmTable.addDisposeListener(event -> display.removeFilter(SWT.KeyDown, spaceKeyListener));

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
                    return ConfirmationDialog.PROMPT.equals(((ConfirmationWithStatus) item).status);
                }
            return false;
        }, null);

        columnsController.addBooleanColumn(
            CoreMessages.pref_page_confirmations_table_column_confirm,
            CoreMessages.pref_page_confirmations_table_column_confirm_tip,
            SWT.CENTER,
            true,
            true,
            item -> {
                if (item instanceof ConfirmationWithStatus) {
                    return ((ConfirmationWithStatus) item).confirm;
                }
                return false;
            }, null);

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
            // We do not want to see confirmation without a toggle message in the preferences
            // because we do not want to add the user's ability to ignore these confirmations
            .filter(item -> CommonUtils.isNotEmpty(item.getToggleMessage()))
            .sorted(Comparator.comparing(ConfirmationDescriptor::getGroup))
            .toList();
        for (ConfirmationDescriptor confirmation : descriptors) {
            String status = getCurrentConfirmValue(confirmation.getId());
            this.confirmations.add(new ConfirmationWithStatus(confirmation, status, getCurrentConfirmDefault(confirmation.getId(), status)));
        }

        tableViewer.setInput(this.confirmations);
        tableViewer.refresh();

        UIUtils.asyncExec(() -> UIUtils.packColumns(confirmTable, true));

        return composite;
    }

    private boolean toggleCell(Object element, int column) {
        if (!(element instanceof ConfirmationWithStatus confirmation)) {
            return false;
        }
        if (column == 1) {
            confirmation.status = ConfirmationDialog.PROMPT.equals(confirmation.status)
                ? (confirmation.confirm ? ConfirmationDialog.ALWAYS : ConfirmationDialog.NEVER)
                : ConfirmationDialog.PROMPT;
        } else if (column == 2) {
            confirmation.confirm = !confirmation.confirm;
            if (!ConfirmationDialog.PROMPT.equals(confirmation.status)) {
                confirmation.status = confirmation.confirm ? ConfirmationDialog.ALWAYS : ConfirmationDialog.NEVER;
            }
        } else {
            return false;
        }
        changedConfirmations.put(confirmation.confirmation, confirmation.status);
        changedDefaultConfirmations.put(confirmation.confirmation, confirmation.confirm);
        tableViewer.update(confirmation, null);
        // Boolean cells are custom painted; their label provider does not redraw them on update.
        confirmTable.redraw();
        return true;
    }

    private String getCurrentConfirmValue(String id) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        String value = store.getString(ConfirmationConstants.CONFIRM_PREF_KEY_PREFIX + id);
        if (CommonUtils.isEmpty(value)) {
            return ConfirmationDialog.PROMPT;
        }

        if (ConfirmationDialog.NEVER.equals(value) || ConfirmationDialog.ALWAYS.equals(value)) {
            return value;
        }

        // Better to ask in other cases
        return ConfirmationDialog.PROMPT;
    }

    private boolean getCurrentConfirmDefault(String id, String status) {
        if (!ConfirmationDialog.PROMPT.equals(status)) {
            return !ConfirmationDialog.NEVER.equals(status);
        }
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        String key = DEFAULT_CONFIRM_PREF_KEY_PREFIX + id;
        // Older preferences have no separate default decision while the dialog is shown.
        return !store.contains(key) || store.getBoolean(key);
    }


    @Override
    public boolean performOk() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        for (Map.Entry<ConfirmationDescriptor, String> entry : changedConfirmations.entrySet()) {
            String id = entry.getKey().getId();
            store.setValue(ConfirmationConstants.CONFIRM_PREF_KEY_PREFIX + id, entry.getValue());
        }
        for (Map.Entry<ConfirmationDescriptor, Boolean> entry : changedDefaultConfirmations.entrySet()) {
            store.setValue(
                DEFAULT_CONFIRM_PREF_KEY_PREFIX + entry.getKey().getId(),
                entry.getValue());
        }
        PrefUtils.savePreferenceStore(store);
        return super.performOk();
    }

    @Override
    protected void performDefaults() {
        // All elements are true by default
        for (ConfirmationWithStatus confirmation : confirmations) {
            if (!ConfirmationDialog.PROMPT.equals(confirmation.status) || !confirmation.confirm) {
                confirmation.status = ConfirmationDialog.PROMPT;
                confirmation.confirm = true;
                changedConfirmations.put(confirmation.confirmation, ConfirmationDialog.PROMPT);
                changedDefaultConfirmations.put(confirmation.confirmation, true);
            }
        }
        tableViewer.refresh();
        UIUtils.asyncExec(() -> UIUtils.packColumns(confirmTable, true));
        super.performDefaults();
    }

    private static class ConfirmationWithStatus {

        private final ConfirmationDescriptor confirmation;
        private String status;
        private boolean confirm;

        ConfirmationWithStatus(ConfirmationDescriptor confirmation, String status, boolean confirm) {
            this.confirmation = confirmation;
            this.status = status;
            this.confirm = confirm;
        }
    }
}