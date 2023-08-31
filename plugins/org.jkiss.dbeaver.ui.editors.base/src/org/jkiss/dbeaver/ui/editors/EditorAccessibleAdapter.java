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

import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.jkiss.code.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;

public class EditorAccessibleAdapter extends AccessibleControlAdapter implements AccessibleListener {
    private static final String DATA_KEY = EditorAccessibleAdapter.class.getName();

    private final WeakReference<Composite> composite;
    private boolean active;

    private EditorAccessibleAdapter(@NotNull Composite composite) {
        this.composite = new WeakReference<>(composite);
    }

    /**
     * Checks if any control in the hierarchy has this accessible adapter installed and it's active.
     * <p>
     * In other words, if any method of this adapter was called at least once, it will be treated as "active".
     */
    public static boolean isActive(@NotNull Composite composite) {
        for (Composite c = composite; c != null; c = c.getParent()) {
            final Object data = c.getData(DATA_KEY);
            if (data instanceof EditorAccessibleAdapter && ((EditorAccessibleAdapter) data).active) {
                return true;
            }
        }

        return false;
    }

    /**
     * Installs this accessible adapter on the given control.
     */
    public static void install(@NotNull Composite composite) {
        final EditorAccessibleAdapter adapter = new EditorAccessibleAdapter(composite);

        final Accessible accessible = composite.getAccessible();
        accessible.addAccessibleListener(adapter);
        accessible.addAccessibleControlListener(adapter);

        composite.setData(DATA_KEY, adapter);
    }

    @Override
    public void getName(AccessibleEvent e) {
        final Composite composite = this.composite.get();

        if (composite != null) {
            e.result = getWorkbenchLocation(composite);
            active = true;
        }
    }

    @Override
    public void getValue(AccessibleControlEvent e) {
        final Composite composite = this.composite.get();

        if (composite != null) {
            e.result = getWorkbenchLocation(composite);
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
    private static String getWorkbenchLocation(@NotNull Composite composite) {
        final Deque<String> path = new ArrayDeque<>();

        for (Composite c = composite; c != null; c = c.getParent()) {
            if (c instanceof TabFolder) {
                final TabFolder folder = (TabFolder) c;
                final int index = folder.getSelectionIndex();

                if (index >= 0) {
                    path.offerFirst(getTabName(folder.getItem(index).getText(), index, folder.getItemCount()));
                }
            } else if (c instanceof CTabFolder) {
                final CTabFolder folder = (CTabFolder) c;
                final int index = folder.getSelectionIndex();

                if (index >= 0) {
                    path.offerFirst(getTabName(folder.getItem(index).getText(), index, folder.getItemCount()));
                }
            }
        }

        return String.join(", ", path);
    }

    @NotNull
    private static String getTabName(@NotNull String text, int index, int count) {
        return String.format("%s tab, %d out of %d", text, index + 1, count);
    }
}
