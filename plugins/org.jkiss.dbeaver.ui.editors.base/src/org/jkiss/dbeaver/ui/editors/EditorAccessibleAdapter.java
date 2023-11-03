/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.accessibility.AccessibleListener;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;

public class EditorAccessibleAdapter extends AccessibleControlAdapter implements AccessibleListener {
    private static final String DATA_KEY = EditorAccessibleAdapter.class.getName();

    private final Control composite;
    private boolean active;

    EditorAccessibleAdapter(@NotNull Control composite) {
        this.composite = composite;
    }

//    /**
//     * Checks if any control in the hierarchy has this accessible adapter installed
//     * and it's active.
//     * <p>
//     * In other words, if any method of this adapter was called at least once, it
//     * will be treated as "active".
//     */
//    public static boolean isActive(@NotNull Composite composite) {
//        for (Composite c = composite; c != null; c = c.getParent()) {
//            final Object data = c.getData(DATA_KEY);
//            if (data instanceof EditorAccessibleAdapter && ((EditorAccessibleAdapter) data).active) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    /**
//     * Installs this accessible adapter on the given control.
//     */
//    public static void install(@NotNull Composite composite) {
//        final EditorAccessibleAdapter adapter = new EditorAccessibleAdapter(composite);
//        final Accessible accessible = composite.getAccessible();
//        accessible.addAccessibleListener(adapter);
//        accessible.addAccessibleControlListener(adapter);
//        composite.setData(DATA_KEY, adapter);
//    }

    @Override
    public void getName(AccessibleEvent e) {
        if (this.composite != null) {
            e.result = composeActiveEditorTabName(this.composite);
            active = true;
        }
    }

    @Override
    public void getValue(AccessibleControlEvent e) {
        if (this.composite != null) {
            e.result = composeActiveEditorTabName(this.composite);
            active = true;
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

    @NotNull
    private static String composeActiveEditorTabName(@NotNull Control composite) {
        String msg = "";
        Composite parentTab = composite.getParent();
        if (parentTab instanceof CTabFolder) {
            CTabFolder tabFoler = (CTabFolder) parentTab;
            msg = String.format("active tab %s %s of %s",
                tabFoler.getSelection().getText(),
                tabFoler.getSelectionIndex() + 1,
                tabFoler.getItemCount());
//            Composite parentEditor = parentTab.getParent();
//            if (parentEditor instanceof CTabFolder) {
//                CTabFolder editorFoler = (CTabFolder) parentTab;
//                msg = String.format("active editor %s %s", editorFoler.getSelection().getText(), msg);
//            }
        }
        System.out.println("!--->>>  msg:" + msg);
        return msg;
    }
}
