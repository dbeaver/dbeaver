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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IMultiController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;

/**
* BaseValueEditor
*/
public abstract class BaseValueEditor<T extends Control> implements IValueEditor {

    private static final String RESULTS_EDIT_CONTEXT_ID = "org.jkiss.dbeaver.ui.context.resultset.edit";

    protected final IValueController valueController;
    protected T control;
    protected boolean dirty;
    protected boolean autoSaveEnabled;

    protected BaseValueEditor(final IValueController valueController)
    {
        this.valueController = valueController;
    }

    public void createControl() {
        this.control = createControl(valueController.getEditPlaceholder());
        if (this.control != null) {
            initInlineControl(this.control);
        }
    }

    @Override
    public Control getControl()
    {
        return control;
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {

    }

    protected abstract T createControl(Composite editPlaceholder);

    protected void initInlineControl(final Control inlineControl)
    {
        boolean isInline = (valueController.getEditType() == IValueController.EditType.INLINE);
        if (isInline && UIUtils.isInDialog(inlineControl)) {
            //isInline = false;
        }
        UIUtils.enableHostEditorKeyBindingsSupport(valueController.getValueSite(), inlineControl);

//            if (!isInline) {
//                inlineControl.setBackground(valueController.getEditPlaceholder().getBackground());
//            }

        final IContextService contextService = valueController.getValueSite().getService(IContextService.class);
        inlineControl.addFocusListener(new FocusListener() {
            private IContextActivation activationEditor;
            @Override
            public void focusGained(FocusEvent e) {
                if (contextService != null) {
                    activationEditor = contextService.activateContext(RESULTS_EDIT_CONTEXT_ID);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                contextService.deactivateContext(activationEditor);
                activationEditor = null;
            }
        });

        if (isInline) {
            inlineControl.setFont(valueController.getEditPlaceholder().getFont());
            inlineControl.setFocus();

            if (valueController instanceof IMultiController) { // In dialog it also should handle all standard stuff because we have params dialog
                 inlineControl.addTraverseListener(new TraverseListener() {
                    @Override
                    public void keyTraversed(TraverseEvent e) {
                        if (e.detail == SWT.TRAVERSE_RETURN) {
                            saveValue();
                            ((IMultiController) valueController).closeInlineEditor();
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                        } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                            ((IMultiController) valueController).closeInlineEditor();
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                        } else if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                            saveValue();
                            ((IMultiController) valueController).nextInlineEditor(e.detail == SWT.TRAVERSE_TAB_NEXT);
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                        }
                      }
                 });
                 if (!UIUtils.isInDialog(inlineControl)) {
                     addAutoSaveSupport(inlineControl);
                 }
            }
        }
        final ControlModifyListener modifyListener = new ControlModifyListener();
        control.addListener(SWT.Modify, modifyListener);
        control.addListener(SWT.Selection, modifyListener);
    }

    private void addAutoSaveSupport(final Control inlineControl) {
        // Do not use focus listener in dialogs (because dialog has controls like Ok/Cancel buttons)
        inlineControl.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Check new focus control in async mode
                // (because right now focus is still on edit control)
                if (!valueController.isReadOnly()) {
                    saveValue();
                }
                if (valueController instanceof IMultiController) {
                    ((IMultiController) valueController).closeInlineEditor();
                }
            }
        });
    }

    private void saveValue()
    {
        try {
            Object newValue = extractEditorValue();
            if (dirty || control instanceof Combo || control instanceof CCombo || control instanceof List) {
                // Combos are always dirty (because drop-down menu sets a selection)
                valueController.updateSelectionValue(newValue);
            }
        } catch (DBException e) {
            ((IMultiController) valueController).closeInlineEditor();
            DBUserInterface.getInstance().showError("Value save", "Can't save edited value", e);
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    public void setAutoSaveEnabled(boolean autoSaveEnabled) {
        this.autoSaveEnabled = autoSaveEnabled;
    }

    private class ControlModifyListener implements Listener {
        @Override
        public void handleEvent(Event event) {
            setDirty(true);
            if (autoSaveEnabled && DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.RS_EDIT_AUTO_UPDATE_VALUE)) {
                saveValue();
            }
        }
    }
}
