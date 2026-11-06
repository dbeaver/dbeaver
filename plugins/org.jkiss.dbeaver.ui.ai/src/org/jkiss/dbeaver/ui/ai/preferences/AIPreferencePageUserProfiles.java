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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.AIUserProfileCredentials;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AIPreferencePageUserProfiles extends AbstractPrefPage implements IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai.userProfiles";

    private final AISettings settings = AISettingsManager.getStaticSettings();
    private final Map<AIConfigurationProfile, Map<String, String>> changes = new LinkedHashMap<>();
    private AIConfigurationProfile selectedProfile = settings.getDefaultConfigurationOrNull();
    private Composite profilePanel;

    @NotNull
    @Override
    public IAdaptable getElement() {
        return settings;
    }

    @Override
    public void setElement(@NotNull IAdaptable element) {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        Label description = new Label(composite, SWT.WRAP);
        description.setText(AIUIMessages.ai_user_profiles_description);
        GridData descriptionData = new GridData(GridData.FILL_HORIZONTAL);
        descriptionData.widthHint = 450;
        description.setLayoutData(descriptionData);
        Composite selectorPanel = UIUtils.createComposite(composite, 2);
        selectorPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createLabel(selectorPanel, AIUIMessages.ai_user_profiles_default_profile);
        ComboViewer selector = new ComboViewer(selectorPanel, SWT.READ_ONLY | SWT.DROP_DOWN);
        selector.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        selector.setContentProvider(ArrayContentProvider.getInstance());
        selector.setLabelProvider(new LabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((AIConfigurationProfile) element).getProfileName();
            }
        });
        selector.setInput(settings.getConfigurations());
        if (selectedProfile != null) {
            selector.setSelection(new StructuredSelection(selectedProfile));
        }
        profilePanel = UIUtils.createComposite(composite, 2);
        profilePanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        selector.addSelectionChangedListener(event -> {
            selectedProfile = (AIConfigurationProfile) event.getStructuredSelection().getFirstElement();
            showProfile();
        });
        showProfile();
        return composite;
    }

    private void showProfile() {
        UIUtils.disposeChildControls(profilePanel);
        if (selectedProfile == null) {
            UIUtils.createLabel(profilePanel, AIUIMessages.ai_engines_page_no_active_engine);
            profilePanel.getParent().layout(true, true);
            return;
        }
        AIConfigurationProfile profile = selectedProfile;
        try {
            UIUtils.createLabelText(profilePanel, AIUIMessages.ai_engines_page_profile_id_label,
                profile.getProfileId(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(profilePanel, AIUIMessages.ai_engines_page_column_model,
                profile.getEngineDescriptor().getLabel() + " - " + CommonUtils.notEmpty(profile.getConfiguration().getModel()),
                SWT.BORDER | SWT.READ_ONLY);
            if (profile.isGlobal()) {
                Label sharedDescription = new Label(profilePanel, SWT.WRAP);
                sharedDescription.setText(AIUIMessages.ai_user_profiles_shared_description);
                GridData descriptionData = new GridData(GridData.FILL_HORIZONTAL);
                descriptionData.horizontalSpan = 2;
                descriptionData.widthHint = 450;
                sharedDescription.setLayoutData(descriptionData);
            } else {
                Map<String, String> credentials = loadCredentials(profile);
                for (ObjectPropertyDescriptor property : AIUserProfileCredentials.getCredentialProperties(
                    profile.getConfiguration(), true)) {
                    Text editor = UIUtils.createLabelText(profilePanel, property.getDisplayName(),
                        CommonUtils.notEmpty(credentials.get(property.getId())),
                        SWT.BORDER | (property.isPassword() ? SWT.PASSWORD : SWT.NONE));
                    editor.addModifyListener(event -> changes.computeIfAbsent(profile, key -> new LinkedHashMap<>())
                        .put(property.getId(), editor.getText()));
                }
            }
            setErrorMessage(null);
            setValid(true);
        } catch (DBException | InvocationTargetException e) {
            setErrorMessage(e instanceof InvocationTargetException ? e.getCause().getMessage() : e.getMessage());
            setValid(false);
        } catch (InterruptedException e) {
            setValid(false);
        }
        profilePanel.getParent().layout(true, true);
    }

    @NotNull
    private Map<String, String> loadCredentials(@NotNull AIConfigurationProfile profile)
        throws InvocationTargetException, InterruptedException {
        Map<String, String> credentials = new LinkedHashMap<>();
        UIUtils.getDialogRunnableContext().run(true, true, monitor -> {
            try {
                credentials.putAll(AIUserProfileCredentials.load(profile));
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        });
        credentials.putAll(changes.getOrDefault(profile, Map.of()));
        return credentials;
    }

    @Override
    public boolean performOk() {
        if (!isValid()) {
            return false;
        }
        try {
            UIUtils.getDialogRunnableContext().run(true, true, monitor -> {
                try {
                    for (var entry : changes.entrySet()) {
                        AIUserProfileCredentials.save(entry.getKey(), entry.getValue());
                        entry.getKey().resolveSecrets();
                    }
                    if (selectedProfile != null) {
                        AISettingsManager.getInstance().setDefaultConfiguration(selectedProfile);
                        AISettingsManager.getInstance().notifyProfilesChanged();
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
            changes.clear();
            return true;
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(AIUIMessages.ai_settings_user_save_error_title, null, e.getCause());
        } catch (InterruptedException e) {
            return false;
        }
        return false;
    }

    @Override
    protected boolean hasAccessToPage() {
        return DBWorkbench.isDistributed();
    }
}
