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
package org.jkiss.dbeaver.ui.controls.resultset.panel.valueviewer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;
import org.jkiss.utils.CommonUtils;

/**
 * RSV value view panel
 */
public class ValueViewerPanel implements IResultSetPanel, DBPAdaptable {

    private static final Log log = Log.getLog(ValueViewerPanel.class);

    public static final String PANEL_ID = "value-view";
    public static final String SETTINGS_SECTION = "panel-" + PANEL_ID;

    private static final String VALUE_VIEW_CONTROL_ID = "org.jkiss.dbeaver.ui.resultset.panel.valueView";

    private IResultSetPresentation presentation;
    private Composite viewPlaceholder;

    private ResultSetValueController previewController;
    private IValueEditor valueEditor;
    private ReferenceValueEditor referenceValueEditor;

    private volatile boolean valueSaving;
    private IValueManager valueManager;

    public static IDialogSettings getPanelSettings() {
        return ResultSetUtils.getViewerSettings(SETTINGS_SECTION);
    }

    public ValueViewerPanel() {
    }

    @Override
    public Control createContents(IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;

        viewPlaceholder = new Composite(parent, SWT.NONE);
        viewPlaceholder.setLayout(new FillLayout());
        viewPlaceholder.addPaintListener(e -> {
            if (previewController == null && viewPlaceholder.getChildren().length == 0) {
                e.gc.setForeground(UIStyles.getDefaultTextForeground());
                String hidePanelCmd = ActionUtils.findCommandDescription(
                    ResultSetHandlerMain.CMD_TOGGLE_PANELS,
                    ValueViewerPanel.this.presentation.getController().getSite(),
                    true);

                UIUtils.drawMessageOverControl(viewPlaceholder, e, ResultSetMessages.value_viewer_select_view_message, 0);
                UIUtils.drawMessageOverControl(viewPlaceholder, e, NLS.bind(ResultSetMessages.value_viewer_hide_panel_message, hidePanelCmd), 20);
            }
        });

        viewPlaceholder.addDisposeListener(e -> disposeValueEditor());
        viewPlaceholder.addTraverseListener(this::handleTraverseEvent);
/*
        addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    hidePanel();
                    e.doit = false;
                }
            }
        });
*/

        if (this.presentation instanceof ISelectionProvider) {
            final ISelectionProvider selectionProvider = (ISelectionProvider) this.presentation;
            final ISelectionChangedListener selectionListener = event -> {
                if (ValueViewerPanel.this.presentation.getController().getVisiblePanel() == ValueViewerPanel.this) {
                    refreshValue(false);
                }
            };
            selectionProvider.addSelectionChangedListener(selectionListener);
            viewPlaceholder.addDisposeListener(e -> selectionProvider.removeSelectionChangedListener(selectionListener));
        }

        return viewPlaceholder;
    }

    @Override
    public boolean isDirty() {
        return valueEditor != null && valueEditor.isDirty();
    }

    @Override
    public void activatePanel() {
        refreshValue(false);
    }

    @Override
    public void deactivatePanel() {
        // Dispose panel control
        if (viewPlaceholder != null && !viewPlaceholder.isDisposed()) {
            viewPlaceholder.dispose();
            viewPlaceholder = null;
        }
    }

    @Override
    public void setFocus() {
        viewPlaceholder.setFocus();
    }

    @Override
    public void refresh(boolean force) {
        refreshValue(force);
    }

    @Override
    public void contributeActions(IContributionManager manager) {
        fillToolBar(manager);
    }

    private void refreshValue(boolean force) {
        DBDAttributeBinding attr = presentation.getCurrentAttribute();
        ResultSetRow row = presentation.getController().getCurrentRow();

        if (attr == null || row == null) {
            clearValue();
            return;
        }
        int[] rowIndexes = presentation.getCurrentRowIndexes();
        boolean updateActions;
        if (previewController == null) {
            previewController = new ResultSetValueController(
                presentation.getController(),
                new ResultSetCellLocation(attr, row, rowIndexes),
                IValueController.EditType.PANEL,
                viewPlaceholder)
            {
                @Override
                public void updateValue(@Nullable Object value, boolean updatePresentation) {
                    valueSaving = true;
                    try {
                        super.updateValue(value, updatePresentation);
                    } finally {
                        valueSaving = false;
                    }
                    presentation.updateValueView();
                }
            };
            updateActions = true;
            force = true;
        } else {
            updateActions = force = (
                force ||
                previewController.getBinding() != attr ||
                !CommonUtils.equalObjects(rowIndexes, previewController.getRowIndexes()));
            previewController.setCellLocation(new ResultSetCellLocation(attr, row, rowIndexes));
        }
        if (!force && (valueManager == null || valueEditor == null)) {
            force = true;
            updateActions = true;
        }
        viewValue(force);
        if (updateActions) {
            presentation.getController().updatePanelActions();
        }
    }

    private void viewValue(boolean forceRefresh)
    {
        if (valueSaving) {
            return;
        }
        if (forceRefresh) {
            cleanupPanel();

            referenceValueEditor = new ReferenceValueEditor(presentation.getController(), previewController, valueEditor);
            final boolean referenceValue = referenceValueEditor.isReferenceValue();
            if (referenceValue) {
                previewController.setEditType(IValueController.EditType.INLINE);
            } else {
                previewController.setEditType(IValueController.EditType.PANEL);
            }

            // Create a new one
            valueManager = previewController.getValueManager();
            try {
                valueEditor = valueManager.createEditor(previewController);
            } catch (Throwable e) {
                DBWorkbench.getPlatformUI().showError(ResultSetMessages.value_viewer_preview_error_title, ResultSetMessages.value_viewer_preview_error_message, e);
                return;
            }
            if (valueEditor != null) {
                try {
                    if (referenceValue) {
                        Label valueLabel = new Label(viewPlaceholder, SWT.NONE);
                        valueLabel.setText(ResultSetMessages.reference_value_editor_value_label);
                        valueLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    }
                    valueEditor.createControl();
                } catch (Exception e) {
                    log.error(e);
                }
                boolean singleLineEditor = false;
                Control control = valueEditor.getControl();
                if (control != null) {
                    singleLineEditor =
                        control instanceof Combo ||
                        control instanceof CCombo ||
                        control instanceof Button ||
                        (control instanceof Text && (control.getStyle() & SWT.MULTI) == 0);
                    UIUtils.addFocusTracker(presentation.getController().getSite(), VALUE_VIEW_CONTROL_ID, control);
                    presentation.getController().lockActionsByFocus(control);

                    control.addTraverseListener(this::handleTraverseEvent);
                }

                if (referenceValue || singleLineEditor) {
                    GridLayout gl = new GridLayout(1, false);
                    viewPlaceholder.setLayout(gl);
                    valueEditor.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    referenceValueEditor.setValueEditor(valueEditor);
                    referenceValueEditor.createEditorSelector(viewPlaceholder);
                } else {
                    viewPlaceholder.setLayout(new FillLayout());
                }
            }

            if (valueEditor == null || valueEditor.getControl() == null) {
                final Composite placeholder = UIUtils.createPlaceholder(viewPlaceholder, 1);
                placeholder.setBackground(placeholder.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
                placeholder.addPaintListener(e -> {
                    Rectangle bounds = placeholder.getBounds();
                    String message = "No editor for [" + previewController.getValueType().getTypeName() + "]";
                    Point ext = e.gc.textExtent(message);
                    e.gc.drawText(message, (bounds.width - ext.x) / 2, bounds.height / 3 + 20);
                });
                referenceValueEditor = null;
            }

            viewPlaceholder.layout();
        }
        if (valueEditor instanceof BaseValueEditor<?> baseValueEditor) {
            baseValueEditor.setAutoSaveEnabled(false);
        }
        if (valueEditor != null) {
            try {
                Object newValue = previewController.getValue();
                if (newValue instanceof DBDValueError) {
                    // Error value. Do not populate it in value viewer
                } else if (newValue instanceof DBDValue) {
                    // Do not check for difference
                    valueEditor.primeEditorValue(newValue);
                } else {
                    Object oldValue = null;
                    try {
                        if (previewController.getExecutionContext() != null) {
                            oldValue = valueEditor.extractEditorValue();
                        }
                    } catch (Throwable e) {
                        // Some error extracting current value
                        // This may happen if we were disconnected
                    }
                    if (forceRefresh || !CommonUtils.equalObjects(oldValue, newValue)) {
                        valueEditor.primeEditorValue(newValue);
                    }
                }
            } catch (DBException e) {
                log.error(e);
            }
            valueEditor.setDirty(false);
        }
        if (valueEditor instanceof BaseValueEditor<?> baseValueEditor) {
            baseValueEditor.setAutoSaveEnabled(true);
        }
    }

    private void handleTraverseEvent(TraverseEvent e) {
        if (e.detail == SWT.TRAVERSE_TAB_NEXT) {
            e.doit = false;
            UIUtils.asyncExec(() -> presentation.getControl().setFocus());
        }
    }

    public void saveValue()
    {
        if (valueEditor == null) {
            return;
        }
        try {
            valueSaving = true;
            Object newValue = valueEditor.extractEditorValue();
            previewController.updateValue(newValue, true);
            presentation.updateValueView();
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError(ResultSetMessages.value_viewer_apply_error_title, ResultSetMessages.value_viewer_apply_error_message, e);
        } finally {
            valueSaving = false;
        }
    }

    public void clearValue()
    {
        cleanupPanel();
        valueManager = null;
        valueEditor = null;

        presentation.getController().updateEditControls();
        viewPlaceholder.layout();
    }

    private void cleanupPanel()
    {
        disposeValueEditor();
        // Cleanup previous viewer
        UIUtils.disposeChildControls(viewPlaceholder);
    }

    private void disposeValueEditor() {
        if (valueEditor != null) {
            valueEditor.dispose();
            valueEditor = null;
        }
    }

    private void fillToolBar(final IContributionManager contributionManager)
    {
        contributionManager.add(new Separator());
        if (valueManager != null) {
            try {
                valueManager.contributeActions(contributionManager, previewController, valueEditor);
            } catch (Exception e) {
                log.error("Can't contribute value manager actions", e);
            }
        }
        contributionManager.add(new GroupMarker(IValueManager.GROUP_ACTIONS_ADDITIONAL));
        if (referenceValueEditor != null && referenceValueEditor.isReferenceValue()) {
            for (ContributionItem contributionItem : referenceValueEditor.getContributionItems()) {
                contributionManager.add(contributionItem);
            }
        }
        if (valueEditor != null && !valueEditor.isReadOnly()) {
            contributionManager.add(
                ActionUtils.makeCommandContribution(presentation.getController().getSite(), ValueViewCommandHandler.CMD_SAVE_VALUE));

            contributionManager.add(
                new Action(ResultSetMessages.value_viewer_auto_apply_action_text, Action.AS_CHECK_BOX) {
                    {
                        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.AUTO_SAVE));
                    }

                    @Override
                    public boolean isChecked() {
                        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ResultSetPreferences.RS_EDIT_AUTO_UPDATE_VALUE);
                    }

                    @Override
                    public void run() {
                        boolean newValue = !isChecked();
                        DBWorkbench.getPlatform().getPreferenceStore().setValue(ResultSetPreferences.RS_EDIT_AUTO_UPDATE_VALUE, newValue);
                        presentation.getController().updatePanelActions();
                    }
                });
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (valueEditor != null) {
            if (adapter.isAssignableFrom(valueEditor.getClass())) {
                return adapter.cast(valueEditor);
            }
            if (valueEditor instanceof IAdaptable) {
                return ((IAdaptable) valueEditor).getAdapter(adapter);
            }
        }

        return null;
    }


}
