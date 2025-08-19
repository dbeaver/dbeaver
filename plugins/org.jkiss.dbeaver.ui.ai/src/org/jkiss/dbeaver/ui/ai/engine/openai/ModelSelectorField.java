/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.engine.openai;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModelSelectorField {
    private static final Log log = Log.getLog(ModelSelectorField.class);

    @NotNull
    private final Combo combo;
    @NotNull
    private final ModelListProvider modelListProvider;

    private volatile String selectedModel;

    private ModelSelectorField(
        @NotNull Combo combo,
        @NotNull ModelListProvider modelListProvider,
        @NotNull Runnable onModelSelected
    ) {
        this.combo = combo;
        this.combo.addModifyListener(e -> selectedModel = combo.getText());
        this.combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedModel = combo.getText();
                onModelSelected.run();
            }
        });

        this.modelListProvider = modelListProvider;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public String getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(@Nullable String model) {
        if (model == null || model.isBlank()) {
            return;
        }

        for (String o : combo.getItems()) {
            if (o.equals(model)) {
                combo.setText(model);
                return;
            }
        }

        // If the model is not in the list, add it
        combo.add(model);
        combo.select(combo.getItemCount() - 1);
        selectedModel = model;
    }

    public void refreshModelListSilently(boolean refresh) {
        new AbstractJob("Refreshing model list silently") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    refreshModelList(monitor, refresh);
                    return Status.OK_STATUS;
                } catch (DBException e) {
                    log.debug("Error reading model list", e);
                    return Status.CANCEL_STATUS;
                }
            }
        }.schedule();
    }

    public void refreshModelList(DBRProgressMonitor monitor, boolean refresh) throws DBException {
        Set<String> models = new HashSet<>(modelListProvider.getModels(monitor, refresh));

        if (models.isEmpty()) {
            return;
        }

        UIUtils.syncExec(() -> {
            if (combo.isDisposed()) {
                return;
            }
            String selectedItem = combo.getText();
            models.add(selectedItem);

            List<String> sortedModels = new ArrayList<>(models).stream()
                .sorted(String::compareToIgnoreCase)
                .toList();

            combo.setItems(sortedModels.toArray(new String[0]));
            combo.select(sortedModels.indexOf(selectedItem));
        });
    }

    public static class Builder {
        @NotNull
        private Composite parent;

        @NotNull
        private GridData gridData;

        @Nullable
        private SelectionListener selectionListener;

        @NotNull
        private ModelListProvider modelListSupplier;
        private String modelLabel = AIUIMessages.gpt_preference_page_combo_engine;

        public Builder withParent(@NotNull Composite parent) {
            this.parent = parent;
            return this;
        }

        public Builder withGridData(@NotNull GridData gridData) {
            this.gridData = gridData;
            return this;
        }

        public Builder withSelectionListener(@NotNull SelectionListener selectionListener) {
            this.selectionListener = selectionListener;
            return this;
        }

        public Builder withModelListSupplier(@NotNull ModelListProvider modelListProvider) {
            this.modelListSupplier = modelListProvider;
            return this;
        }

        public Builder withModelLabel(@NotNull String modelLabel) {
            this.modelLabel = modelLabel;
            return this;
        }

        public ModelSelectorField build() {
            Combo combo = UIUtils.createLabelCombo(
                parent,
                modelLabel,
                SWT.DROP_DOWN
            );
            combo.setLayoutData(gridData);

            ModelSelectorField modelSelectorField = new ModelSelectorField(
                combo,
                modelListSupplier,
                () -> {
                    if (selectionListener != null) {
                        Event event = new Event();
                        event.widget = combo;
                        event.type = SWT.Selection;

                        selectionListener.widgetSelected(new SelectionEvent(event));
                    }
                }

            );

            UIUtils.createDialogButton(
                parent,
                AIUIMessages.gpt_preference_page_refresh_models,
                SelectionListener.widgetSelectedAdapter((e) -> {
                    new AbstractJob("Refreshing model list") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            try {
                                modelSelectorField.refreshModelList(monitor, true);
                                return Status.OK_STATUS;
                            } catch (DBException exception) {
                                DBWorkbench.getPlatformUI().showError(
                                    "Error reading model list",
                                    null,
                                    exception
                                );

                                return Status.CANCEL_STATUS;
                            }
                        }
                    }.schedule();
                })
            );

            return modelSelectorField;
        }
    }

    public interface ModelListProvider {
        List<String> getModels(DBRProgressMonitor monitor, boolean forceRefresh) throws DBException;
    }
}
