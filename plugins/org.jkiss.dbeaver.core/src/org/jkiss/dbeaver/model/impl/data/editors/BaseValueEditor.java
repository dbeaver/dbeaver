/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.data.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

/**
* BaseValueEditor
*/
public abstract class BaseValueEditor<T extends Control> implements DBDValueEditor {
    protected final DBDValueController valueController;
    protected T control;
    private boolean activated;
    protected BaseValueEditor(final DBDValueController valueController)
    {
        this.valueController = valueController;
    }

    public void createControl() {
        this.control = createControl(valueController.getEditPlaceholder());
        if (this.control != null) {
            initInlineControl(this.control);
        }
        IContributionManager editToolBar = valueController.getEditBar();
        if (editToolBar != null) {
            editToolBar.add(new Separator());
            if (!valueController.isReadOnly()) {
                editToolBar.add(new Action("Apply changes", DBIcon.CONFIRM.getImageDescriptor()) {
                    @Override
                    public void run() {
                        saveValue();
                    }
                });
            }
        }
    }

    @Override
    public Control getControl()
    {
        return control;
    }

    protected abstract T createControl(Composite editPlaceholder);

    protected void initInlineControl(final Control inlineControl)
    {
        boolean isInline = (valueController.getEditType() == DBDValueController.EditType.INLINE);
        if (isInline && UIUtils.isInDialog(inlineControl)) {
            //isInline = false;
        }
        UIUtils.enableHostEditorKeyBindingsSupport(valueController.getValueSite(), inlineControl);

//            if (!isInline) {
//                inlineControl.setBackground(valueController.getEditPlaceholder().getBackground());
//            }

        if (isInline) {
            inlineControl.setFont(valueController.getEditPlaceholder().getFont());
            // There is a bug in windows. First time date control gain focus it renders cell editor incorrectly.
            // Let's focus on it in async mode
            inlineControl.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run()
                {
                    if (!inlineControl.isDisposed()) {
                        inlineControl.setFocus();
                    }
                }
            });

            inlineControl.addTraverseListener(new TraverseListener() {
                @Override
                public void keyTraversed(TraverseEvent e)
                {
                    if (e.detail == SWT.TRAVERSE_RETURN) {
                        saveValue();
                        e.doit = false;
                        e.detail = SWT.TRAVERSE_NONE;
                    } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                        valueController.closeInlineEditor();
                        e.doit = false;
                        e.detail = SWT.TRAVERSE_NONE;
                    } else if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                        saveValue();
                        valueController.nextInlineEditor(e.detail == SWT.TRAVERSE_TAB_NEXT);
                        e.doit = false;
                        e.detail = SWT.TRAVERSE_NONE;
                    }
                }
            });
            inlineControl.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e)
                {
                    // Check new focus control in async mode
                    // (because right now focus is still on edit control)
                    inlineControl.getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run()
                        {
                            if (inlineControl.isDisposed()) {
                                return;
                            }
                            Control newFocus = inlineControl.getDisplay().getFocusControl();
                            if (newFocus != null) {
                                for (Control fc = newFocus.getParent(); fc != null; fc = fc.getParent()) {
                                    if (fc == valueController.getEditPlaceholder()) {
                                        // New focus is still a child of inline placeholder - do not close it
                                        return;
                                    }
                                }
                            }
                            saveValue();
                        }
                    });
                }
            });
        }
    }

    private void saveValue()
    {
        try {
            Object newValue = extractEditorValue();
            valueController.closeInlineEditor();
            valueController.updateValue(newValue);
        } catch (DBException e) {
            UIUtils.showErrorDialog(getControl().getShell(), "Value save", "Can't save edited value", e);
        }
    }
}
