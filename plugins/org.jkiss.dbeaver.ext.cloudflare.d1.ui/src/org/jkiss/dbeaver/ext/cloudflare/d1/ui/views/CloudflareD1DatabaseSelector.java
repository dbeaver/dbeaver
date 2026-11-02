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
package org.jkiss.dbeaver.ext.cloudflare.d1.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cloudflare.d1.model.CloudflareD1ConnectionAdapter.DatabaseChoice;

import java.util.List;

final class CloudflareD1DatabaseSelector extends Composite {
    private final Combo combo;
    private List<DatabaseChoice> databases = List.of();
    private final Runnable onChange;
    private boolean updating;

    CloudflareD1DatabaseSelector(@NotNull Composite parent, @NotNull Runnable onChange) {
        super(parent, SWT.NONE);
        this.onChange = onChange;
        setLayout(new FillLayout());
        combo = new Combo(this, SWT.DROP_DOWN);
        combo.setVisibleItemCount(15);
        combo.addListener(SWT.Selection, event -> notifyChanged());
        combo.addModifyListener(event -> notifyChanged());
    }

    @NotNull
    String getSelectedId() {
        int index = combo.getSelectionIndex();
        if (index >= 0 && index < databases.size()) {
            return databases.get(index).id();
        }
        String text = combo.getText().trim();
        // Editable combos can emit Modify before their selection index is updated.
        for (DatabaseChoice database : databases) {
            if (displayName(database).equals(text)) {
                return database.id();
            }
        }
        return text;
    }

    void setSelectedId(@NotNull String id) {
        for (int index = 0; index < databases.size(); index++) {
            if (databases.get(index).id().equals(id)) {
                combo.select(index);
                return;
            }
        }
        combo.setText(id);
    }

    void setDatabases(@NotNull List<DatabaseChoice> databases) {
        String previous = getSelectedId();
        updating = true;
        try {
            this.databases = List.copyOf(databases);
            combo.setItems(databases.stream().map(CloudflareD1DatabaseSelector::displayName).toArray(String[]::new));
            setSelectedId(previous);
            if (combo.getSelectionIndex() < 0 && previous.isBlank() && !databases.isEmpty()) {
                combo.select(0);
            }
        } finally {
            updating = false;
        }
        notifyChanged();
    }

    @NotNull
    private static String displayName(@NotNull DatabaseChoice database) {
        return database.name() + " (" + database.id() + ")";
    }

    private void notifyChanged() {
        if (!updating) {
            onChange.run();
        }
    }
}
