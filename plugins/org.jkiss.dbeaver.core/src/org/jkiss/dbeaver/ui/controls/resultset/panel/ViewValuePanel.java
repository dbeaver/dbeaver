/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.panel;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;
import org.jkiss.utils.CommonUtils;

/**
 * RSV value view panel
 */
public class ViewValuePanel implements IResultSetPanel, IAdaptable {

    private static final Log log = Log.getLog(ViewValuePanel.class);

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

    public ViewValuePanel() {
    }

    @Override
    public String getPanelTitle() {
        return "Value";
    }

    @Override
    public DBPImage getPanelImage() {
        return UIIcon.PANEL_VALUE;
    }

    @Override
    public String getPanelDescription() {
        return "Value view/edit";
    }

    @Override
    public Control createContents(IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;

        viewPlaceholder = new Composite(parent, SWT.NONE);
        viewPlaceholder.setLayout(new FillLayout());
        viewPlaceholder.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                if (viewPlaceholder.getChildren().length == 0) {
                    String hidePanelCmd = ActionUtils.findCommandDescription(
                        ResultSetCommandHandler.CMD_TOGGLE_PANELS,
                        ViewValuePanel.this.presentation.getController().getSite(),
                        true);

                    UIUtils.drawMessageOverControl(viewPlaceholder, e, "Select a cell to view/edit value", 0);
                    UIUtils.drawMessageOverControl(viewPlaceholder, e, "Press " + hidePanelCmd + " to hide this panel", 20);
                }
            }
        });

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
            final ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
                @Override
                public void selectionChanged(SelectionChangedEvent event) {
                    if (ViewValuePanel.this.presentation.getController().getVisiblePanel() == ViewValuePanel.this) {
                        refreshValue(false);
                    }
                }
            };
            selectionProvider.addSelectionChangedListener(selectionListener);
            viewPlaceholder.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    selectionProvider.removeSelectionChangedListener(selectionListener);
                }
            });
        }

        return viewPlaceholder;
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
    public void refresh(boolean force) {
        refreshValue(force);
    }

    @Override
    public void contributeActions(ToolBarManager manager) {
        fillToolBar(manager);
    }

    private void refreshValue(boolean force) {
        DBDAttributeBinding attr = presentation.getCurrentAttribute();
        ResultSetRow row = presentation.getController().getCurrentRow();
        if (attr == null || row == null) {
            clearValue();
            return;
        }
        boolean updateActions;
        if (previewController == null) {
            previewController = new ResultSetValueController(
                presentation.getController(),
                attr,
                row,
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
            updateActions = force = (force || previewController.getBinding() != attr);
            previewController.setCurRow(row);
            previewController.setBinding(attr);
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
        if (valueManager == null || valueEditor == null) {
            forceRefresh = true;
        }
        if (forceRefresh) {
            cleanupPanel();
            // Create a new one
            valueManager = previewController.getValueManager();
            try {
                valueEditor = valueManager.createEditor(previewController);
            } catch (Throwable e) {
                DBUserInterface.getInstance().showError("Value preview", "Can't create value viewer", e);
                return;
            }
            if (valueEditor != null) {
                try {
                    valueEditor.createControl();
                } catch (Exception e) {
                    log.error(e);
                }
                Control control = valueEditor.getControl();
                if (control != null) {
                    UIUtils.addFocusTracker(presentation.getController().getSite(), VALUE_VIEW_CONTROL_ID, control);
                    presentation.getController().lockActionsByFocus(control);
                }

                referenceValueEditor = new ReferenceValueEditor(previewController, valueEditor);
                if (referenceValueEditor.isReferenceValue()) {
                    GridLayout gl = new GridLayout(1, false);
                    viewPlaceholder.setLayout(gl);
                    valueEditor.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    referenceValueEditor.createEditorSelector(viewPlaceholder);
                } else {
                    viewPlaceholder.setLayout(new FillLayout());
                }

            } else {
                final Composite placeholder = UIUtils.createPlaceholder(viewPlaceholder, 1);
                placeholder.setBackground(placeholder.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
                placeholder.addPaintListener(new PaintListener() {
                    @Override
                    public void paintControl(PaintEvent e)
                    {
                        Rectangle bounds = placeholder.getBounds();
                        String message = "No editor for [" + previewController.getValueType().getTypeName() + "]";
                        Point ext = e.gc.textExtent(message);
                        e.gc.drawText(message, (bounds.width - ext.x) / 2, bounds.height / 3 + 20);
                    }
                });
                referenceValueEditor = null;
            }

            viewPlaceholder.layout();
        }
        if (valueEditor instanceof BaseValueEditor) {
            ((BaseValueEditor) valueEditor).setAutoSaveEnabled(false);
        }
        if (valueEditor != null) {
            try {
                Object newValue = previewController.getValue();
                if (newValue instanceof DBDValue) {
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
                    if (!CommonUtils.equalObjects(oldValue, newValue)) {
                        valueEditor.primeEditorValue(newValue);
                    }
                }
            } catch (DBException e) {
                log.error(e);
            }
            valueEditor.setDirty(false);
        }
        if (valueEditor instanceof BaseValueEditor) {
            ((BaseValueEditor) valueEditor).setAutoSaveEnabled(true);
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
            DBUserInterface.getInstance().showError("Value apply", "Can't apply edited value", e);
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
        // Cleanup previous viewer
        for (Control child : viewPlaceholder.getChildren()) {
            child.dispose();
        }
    }

    private void fillToolBar(final IContributionManager contributionManager)
    {
        contributionManager.add(new Separator());
        //contributionManager.add(new Separator());
        if (valueManager != null) {
            try {
                valueManager.contributeActions(contributionManager, previewController, valueEditor);
            } catch (Exception e) {
                log.error("Can't contribute value manager actions", e);
            }
        }

        contributionManager.add(
            ActionUtils.makeCommandContribution(presentation.getController().getSite(), ValueViewCommandHandler.CMD_SAVE_VALUE));

        contributionManager.add(
            new Action("Auto-apply value", Action.AS_CHECK_BOX) {
                {
                    setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.LINK_TO_EDITOR));
                }
                @Override
                public boolean isChecked() {
                    return DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.RS_EDIT_AUTO_UPDATE_VALUE);
                }

                @Override
                public void run() {
                    boolean newValue = !isChecked();
                    DBeaverCore.getGlobalPreferenceStore().setValue(DBeaverPreferences.RS_EDIT_AUTO_UPDATE_VALUE, newValue);
                    presentation.getController().updatePanelActions();
                }
            });
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
