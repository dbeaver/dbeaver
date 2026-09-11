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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.List;

public class AIProfileCreateDialog extends BaseDialog {

    private List<AIEngineDescriptor> aiEngines;
    private AIEngineDescriptor selectedEngine;
    private String profileId;
    private String profileName;

    public AIProfileCreateDialog(@Nullable Shell parentShell) {
        super(parentShell, "Choose AI engine", null);
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);
        Composite enginePanel = UIUtils.createTitledComposite(dialogArea, "AI Configuration", 2);

        Combo engineCombo = UIUtils.createLabelCombo(enginePanel, "Engine", SWT.DROP_DOWN | SWT.READ_ONLY);
        aiEngines = AIEngineRegistry.getInstance().getCompletionEngines();
        boolean hasPromotedEngines = false;
        for (AIEngineDescriptor ed : aiEngines) {
            if (hasPromotedEngines && !ed.isPromoted()) {
                engineCombo.add("----------------");
                hasPromotedEngines = false;
            }
            engineCombo.add(ed.getLabel());
            engineCombo.setData(Integer.toString(engineCombo.getItemCount() - 1), ed);
            hasPromotedEngines = ed.isPromoted();
        }
        engineCombo.select(0);
        int[] selectedIndex = {0};
        selectedEngine = aiEngines.getFirst();
        profileId = genProfileId(selectedEngine);
        profileName = selectedEngine.getLabel();

        Text nameText = UIUtils.createLabelText(enginePanel, "Name", genProfileName(selectedEngine.getLabel()));
        nameText.addModifyListener(e ->  profileName = nameText.getText());
        Text idText = UIUtils.createLabelText(enginePanel, "ID", genProfileId(selectedEngine));
        idText.addModifyListener(e ->  profileId = idText.getText());

        engineCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            int selectionIndex = engineCombo.getSelectionIndex();
            Object selection = engineCombo.getData(Integer.toString(selectionIndex));
            if (!(selection instanceof AIEngineDescriptor engine)) {
                engineCombo.select(selectedIndex[0]);
                return;
            }
            String oldAutoId = genProfileId(selectedEngine);
            selectedIndex[0] = selectionIndex;
            selectedEngine = engine;
            if (oldAutoId.equals(profileId)) {
                profileId = genProfileId(selectedEngine);
                profileName = genProfileName(selectedEngine.getLabel());
                idText.setText(profileId);
                nameText.setText(profileName);
            }
        }));

        return dialogArea;
    }

    @NotNull
    static String genProfileId(@NotNull AIEngineDescriptor engineDescriptor) {
        AISettings aiSettings = AISettingsManager.getInstance().getSettings();
        String baseId = engineDescriptor.getId();
        String id = baseId;
        for (int i = 1; ; i++) {
            if (aiSettings.getConfigurationOrNull(id) == null) {
                break;
            }
            id = baseId + "_" + i;
        }
        return id;
    }


    @NotNull
    static String genProfileName(@NotNull String baseName) {
        AISettings aiSettings = AISettingsManager.getInstance().getSettings();
        String name = baseName;
        for (int i = 1; ; i++) {
            if (aiSettings.getConfigurationByNameOrNull(name) == null) {
                break;
            }
            name = baseName + " (" + i + ")";
        }
        return name;
    }

    @Nullable
    public String getProfileId() {
        return profileId;
    }

    @Nullable
    public String getProfileName() {
        return profileName;
    }

    @NotNull
    public AIEngineDescriptor getSelectedEngine() {
        return selectedEngine;
    }

}
