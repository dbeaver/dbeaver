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
package org.jkiss.dbeaver.ui.ai.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
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
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ModelSelectorField {
    private static final Log log = Log.getLog(ModelSelectorField.class);

    @NotNull
    private final Combo combo;
    @NotNull
    private final Button refreshButton;
    @NotNull
    private final ModelListProvider modelListProvider;
    @NotNull
    private final List<RequiredSetting> requiredSettings;

    private volatile String selectedModel;
    private boolean disableModifyListener = false;

    private ModelSelectorField(@NotNull Builder builder) {
        this.modelListProvider = builder.modelListSupplier;
        this.requiredSettings = List.copyOf(builder.requiredSettings);

        this.combo = UIUtils.createLabelCombo(builder.parent, builder.modelLabel, SWT.DROP_DOWN);
        this.combo.setLayoutData(builder.gridData);
        this.combo.addModifyListener(e -> {
            String newText = combo.getText();
            if (disableModifyListener || newText.equals(selectedModel)) {
                return;
            }
            selectedModel = newText;
            if (builder.onModify != null) {
                builder.onModify.run();
            }
        });

        this.refreshButton = UIUtils.createPushButton(
            builder.parent,
            null,
            AIUIMessages.gpt_preference_page_refresh_models,
            UIIcon.REFRESH,
            SelectionListener.widgetSelectedAdapter(e -> refreshModelListWithProgress())
        );
        for (RequiredSetting setting : requiredSettings) {
            setting.control().addModifyListener(e -> updateRefreshButtonState());
        }
        updateRefreshButtonState();
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
        if (findMissingSetting() != null) {
            return;
        }
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

    public int refreshModelList(@NotNull DBRProgressMonitor monitor, boolean refresh) throws DBException {
        List<String> loadedModels = modelListProvider.getModels(monitor, refresh);

        if (loadedModels.isEmpty()) {
            return 0;
        }

        UIUtils.syncExec(() -> {
            if (combo.isDisposed()) {
                return;
            }
            String selectedItem = combo.getText();
            Set<String> models = new LinkedHashSet<>(loadedModels);
            if (!selectedItem.isEmpty()) {
                models.add(selectedItem);
            }

            List<String> sortedModels = models.stream()
                .sorted(String::compareToIgnoreCase)
                .toList();

            disableModifyListener = true;
            combo.setItems(sortedModels.toArray(new String[0]));
            disableModifyListener = false;
            combo.select(sortedModels.indexOf(selectedItem));
        });

        return loadedModels.size();
    }

    private void refreshModelListWithProgress() {
        RefreshModelListJob job = new RefreshModelListJob();
        if (!runJobWithProgressDialog(job)) {
            return;
        }
        if (job.error != null) {
            DBWorkbench.getPlatformUI().showError(AIUIMessages.model_selector_refresh_error_title, null, job.error);
            return;
        }
        DBWorkbench.getPlatformUI().showMessageBox(
            AIUIMessages.model_selector_refresh_title,
            job.modelCount > 0
                ? NLS.bind(AIUIMessages.model_selector_refresh_success_message, job.modelCount)
                : AIUIMessages.model_selector_refresh_empty_message,
            job.modelCount == 0
        );
    }

    private static boolean runJobWithProgressDialog(@NotNull AbstractJob job) {
        boolean[] completed = new boolean[1];
        job.schedule();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                monitor.beginTask(job.getName(), IProgressMonitor.UNKNOWN);
                try {
                    completed[0] = job.join(0, RuntimeUtils.getNestedMonitor(monitor));
                } catch (OperationCanceledException e) {
                    // the user pressed сancel
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            log.error("Error waiting for " + job.getName(), e.getTargetException());
        }
        if (!completed[0]) {
            job.cancel();
        }
        return completed[0];
    }

    private class RefreshModelListJob extends AbstractJob {
        @Nullable
        private volatile DBException error;
        private volatile int modelCount;

        private RefreshModelListJob() {
            super(AIUIMessages.model_selector_refresh_title);
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            try {
                modelCount = refreshModelList(monitor, true);
            } catch (DBException e) {
                error = e;
            }
            return Status.OK_STATUS;
        }
    }

    private void updateRefreshButtonState() {
        if (refreshButton.isDisposed()) {
            return;
        }
        String missingSetting = findMissingSetting();
        refreshButton.setEnabled(missingSetting == null);
        refreshButton.setToolTipText(
            missingSetting == null ? AIUIMessages.gpt_preference_page_refresh_models : missingSetting);
    }

    @Nullable
    private String findMissingSetting() {
        for (RequiredSetting setting : requiredSettings) {
            if (!setting.control().isDisposed() && setting.control().getText().isEmpty()) {
                return setting.messageWhenEmpty();
            }
        }
        return null;
    }

    private record RequiredSetting(@NotNull Text control, @NotNull String messageWhenEmpty) {
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

        private final List<RequiredSetting> requiredSettings = new ArrayList<>();

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

        public Builder withRequiredSetting(@NotNull Text control, @NotNull String messageWhenEmpty) {
            this.requiredSettings.add(new RequiredSetting(control, messageWhenEmpty));
            return this;
        }

        @NotNull
        public ModelSelectorField build() {
            return new ModelSelectorField(this);
        }
    }

    public interface ModelListProvider {
        @NotNull
        List<String> getModels(@NotNull DBRProgressMonitor monitor, boolean forceRefresh) throws DBException;
    }
}
