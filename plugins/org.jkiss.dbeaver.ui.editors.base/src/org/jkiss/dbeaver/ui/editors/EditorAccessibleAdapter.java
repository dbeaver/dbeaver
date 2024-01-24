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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.accessibility.AccessibleListener;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;

public class EditorAccessibleAdapter extends AccessibleControlAdapter implements AccessibleListener {

    private static final String ACTIVE_NAME_EDITOR = "editor %s %s"; // $NON-NLS-0$
    private static final String ACTIVE_NAME_TAB = "active tab %s %s of %s"; // $NON-NLS-0$
    private static final String ACTIVE_NAME_TAB_NO_TITLE = "active tab %s of %s"; // $NON-NLS-0$
    private final Control composite;

    EditorAccessibleAdapter(@NotNull Control composite) {
        this.composite = composite;
    }

    @Override
    public void getName(AccessibleEvent e) {
        if (this.composite != null) {
            e.result = composeActiveEditorTabName(this.composite);
        }
    }

    @Override
    public void getValue(AccessibleControlEvent e) {
        if (this.composite != null) {
            e.result = composeActiveEditorTabName(this.composite);
        }
    }

    @Override
    public void getHelp(AccessibleEvent e) {
        // not implemented
    }

    @Override
    public void getKeyboardShortcut(AccessibleEvent e) {
        // not implemented
    }

    @Override
    public void getDescription(AccessibleEvent e) {
        // not implemented
    }

    /**
     * The method designed to combine name of editor-tab by selection context
     *
     * @param context - initial selection
     * @return - string message
     */
    @NotNull
    private static String composeActiveEditorTabName(@NotNull Control context) {
        String msg = "";
        Composite parentTab = context.getParent();
        if (parentTab instanceof CTabFolder) {
            CTabFolder tabFoler = (CTabFolder) parentTab;
            if (tabFoler.getSelection() == null) {
                return msg;
            }
            String text = tabFoler.getSelection().getText();
            if (text != null) {
                msg = String.format(ACTIVE_NAME_TAB,
                    text,
                    tabFoler.getSelectionIndex() + 1,
                    tabFoler.getItemCount());
            } else {
                msg = String.format(ACTIVE_NAME_TAB_NO_TITLE,
                    tabFoler.getSelectionIndex() + 1,
                    tabFoler.getItemCount());
            }
            // level 3
            Composite parentEditor = parentTab.getParent();
            if (parentEditor != null && !parentEditor.isDisposed()) {
                // level 2
                parentEditor = parentEditor.getParent();
                if (parentEditor != null && !parentEditor.isDisposed()) {
                    // level 1
                    parentEditor = parentEditor.getParent();
                    if (parentEditor != null && !parentEditor.isDisposed() && parentEditor instanceof CTabFolder) {
                        CTabFolder parentEditorFolder = (CTabFolder) parentEditor;
                        msg = String.format(ACTIVE_NAME_EDITOR, parentEditorFolder.getSelection().getText(), msg);
                    }
                }
            }
        }
        return msg;
    }
}
