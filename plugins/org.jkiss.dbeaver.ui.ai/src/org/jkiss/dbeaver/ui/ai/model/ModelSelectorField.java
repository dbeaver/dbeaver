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
package org.jkiss.dbeaver.ui.ai.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIIcon;
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
    private boolean disableModifyListener = false;

    private ModelSelectorField(
        @NotNull Combo combo,
        @NotNull ModelListProvider modelListProvider,
        @Nullable Runnable onModelModify
    ) {
        this.combo = combo;
        if (onModelModify != null) {
            this.combo.addModifyListener(e -> {
                String newText = combo.getText();
                if (!newText.equals(selectedModel) && !disableModifyListener) {
                    selectedModel = newText;
                    onModelModify.run();
                }
            });
        } else {
            this.combo.addModifyListener(e -> selectedModel = combo.getText());
        }

        this.modelListProvider = modelListProvider;
    }

    @NotNull
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
        combo.setText(model);
    }

    public void refreshModelListSilently(boolean refresh) {
        new AbstractJob("Refreshing model list silently") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
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

            disableModifyListener = true;
            combo.setItems(sortedModels.toArray(new String[0]));
            disableModifyListener = false;
            combo.select(sortedModels.indexOf(selectedItem));
        });
    }

    public static class Builder {
        @NotNull
        private Composite parent;

        @NotNull
        private GridData gridData;

        @Nullable
        private Runnable onModify;

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

        public Builder withModifyListener(@NotNull Runnable onModify) {
            this.onModify = onModify;
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

        @NotNull
        public ModelSelectorField build() {
            Combo combo = UIUtils.createLabelCombo(
                parent,
                modelLabel,
                SWT.DROP_DOWN
            );
            combo.setLayoutData(gridData);

            ModelSelectorField modelSelectorField = new ModelSelectorField(combo, modelListSupplier, onModify);

            UIUtils.createPushButton(
                parent,
                null,
                AIUIMessages.gpt_preference_page_refresh_models,
                UIIcon.REFRESH,
                SelectionListener.widgetSelectedAdapter((e) -> {
                    new AbstractJob("Refreshing model list") {
                        @NotNull
                        @Override
                        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
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
        @NotNull
        List<String> getModels(@NotNull DBRProgressMonitor monitor, boolean forceRefresh) throws DBException;
    }
}
