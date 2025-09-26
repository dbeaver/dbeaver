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
package org.jkiss.dbeaver.ui.ai.engine;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

public class AIConnectionTestSelectionAdapter extends SelectionAdapter {

    private DBPPlatformUI platformUI;
    private final ModelSelectorField modelSelectorField;
    private final ModelSelectorField.ModelListProvider modelListProvider;

    public AIConnectionTestSelectionAdapter(@NotNull ModelSelectorField modelSelectorField,
                                            @NotNull ModelSelectorField.ModelListProvider modelListProvider) {
        this.modelSelectorField = modelSelectorField;
        this.modelListProvider = modelListProvider;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        platformUI = DBWorkbench.getPlatformUI();
        String selectedModel = modelSelectorField.getSelectedModel();
        if (CommonUtils.isEmpty(selectedModel)) {
            platformUI.showMessageBox(AIUIMessages.gpt_preference_page_ai_connection_test_connection_warning_title,
                AIUIMessages.gpt_preference_page_ai_connection_test_model_not_chosen, true);
            return;
        }

        try {
            Collection<String> knownModels = modelListProvider.getModels(new VoidProgressMonitor(), true);
            if (knownModels.contains(selectedModel)) {
                showSuccessMessageBox(selectedModel);
            } else {
                showWarningMessageBox(selectedModel);
            }
        } catch (Exception exception) {
            showErrorMessageBox(exception);
        }
    }

    private void showWarningMessageBox(String selectedModel) {
        platformUI
            .showWarningMessageBox(AIUIMessages.gpt_preference_page_ai_connection_test_connection_warning_title,
                NLS.bind(AIUIMessages.gpt_preference_page_ai_connection_test_connection_warning_message, selectedModel));
    }

    private void showSuccessMessageBox(String selectedModel) {
        platformUI
            .showMessageBox(AIUIMessages.gpt_preference_page_ai_connection_test_connection_success_title,
                NLS.bind(AIUIMessages.gpt_preference_page_ai_connection_test_connection_success_message, selectedModel),
                false);
    }


    private void showErrorMessageBox(Exception exception) {
        platformUI.showError(
            AIUIMessages.gpt_preference_page_ai_connection_test_connection_error_title,
            AIUIMessages.gpt_preference_page_ai_connection_test_connection_error_message,
            exception
        );
    }


}
