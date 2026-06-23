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
package org.jkiss.dbeaver.ui.controls.findandreplace;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Derived from <a href="https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/bundles/org.eclipse.ui.workbench.texteditor/src/org/eclipse/ui/internal/findandreplace/overlay/FindReplaceOverlayAction.java">eclipse.platform.ui</a>
 */
class FindReplaceOverlayAction {
    @NotNull
    private final Runnable operation;

    @NotNull
    private final List<KeyStroke> shortcuts = new ArrayList<>();

    FindReplaceOverlayAction(@NotNull Runnable operation) {
        this.operation = operation;
    }

    void addShortcuts(@NotNull List<KeyStroke> shortcutsToAdd) {
        this.shortcuts.addAll(shortcutsToAdd);
    }

    void execute() {
        this.operation.run();
    }

    boolean executeIfMatching(@NotNull KeyStroke keystroke) {
        if (this.shortcuts.stream().anyMatch(keystroke::equals)) {
            execute();
            return true;
        }
        return false;
    }

    @NotNull
    String addShortcutHintToTooltipText(@NotNull String originalTooltipText) {
        if (this.shortcuts.isEmpty()) {
            return originalTooltipText;
        }
        String shortcutText = "(" + this.shortcuts.getFirst().format() + ")";
        String boundText = NLS.bind(originalTooltipText, shortcutText);
        if (boundText.equals(originalTooltipText)) {
            return originalTooltipText + " " + shortcutText; //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            return boundText;
        }
    }

}
