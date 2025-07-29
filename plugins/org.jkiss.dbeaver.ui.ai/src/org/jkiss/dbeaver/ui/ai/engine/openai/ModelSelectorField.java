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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;

import java.util.List;

public class ModelSelectorField {
    @NotNull
    private final Combo combo;
    @NotNull
    private final ModelListProvider modelListProvider;

    private String selectedModel;

    private ModelSelectorField(
        @NotNull Combo combo,
        @NotNull ModelListProvider modelListProvider
    ) {
        this.combo = combo;
        this.combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedModel = combo.getText();
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

    public void refreshModelList(boolean refresh) {
        List<String> models = modelListProvider.getModels(refresh);
        if (models.isEmpty()) {
            return;
        }

        String selectedItem = combo.getText();
        combo.setItems(models.toArray(new String[0]));
        if (models.contains(selectedItem)) {
            combo.setText(selectedItem);
        } else {
            combo.select(0); // Select the first item if the previous selection is not available
        }
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

        public ModelSelectorField build() {
            Combo combo = UIUtils.createLabelCombo(parent, AIUIMessages.gpt_preference_page_combo_engine, SWT.READ_ONLY);
            combo.setLayoutData(gridData);

            if (selectionListener != null) {
                combo.addSelectionListener(selectionListener);
            }

            ModelSelectorField modelSelectorField = new ModelSelectorField(combo, modelListSupplier);

            UIUtils.createDialogButton(
                parent,
                AIUIMessages.gpt_preference_page_refresh_models,
                SelectionListener.widgetSelectedAdapter((e) -> modelSelectorField.refreshModelList(true))
            );

            return modelSelectorField;
        }
    }

    public interface ModelListProvider {
        List<String> getModels(boolean forceRefresh);
    }
}
