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
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AIPreferencePagePrompts extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    private final AISettingsManager settingsManager;
    private final AISettings settings;

    private final Map<String, String> customInstructions = new LinkedHashMap<>();

    private AIPromptGeneratorDescriptor activeGenerator;
    private Viewer promptsViewer;
    private Text instructionsText;

    public AIPreferencePagePrompts() {
        this.settingsManager = AISettingsManager.getInstance();
        this.settings = settingsManager.getSettings();
        this.customInstructions.putAll(settingsManager.getSettings().getCustomInstructions());
    }

    @Override
    public void init(@NotNull IWorkbench workbench) {
        // do nothing
    }

    @NotNull
    @Override
    public IAdaptable getElement() {
        return settings;
    }

    @Override
    public void setElement(@NotNull IAdaptable element) {
        // do nothing
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        var generators = AIPromptGeneratorRegistry.getInstance().getAllPromptGenerator().stream()
            .sorted(Comparator.comparing(AIPromptGeneratorDescriptor::getLabel))
            .toList();

        var sash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);
        promptsViewer = createViewer(sash, generators);
        instructionsText = createEditor(sash);

        sash.setSashWidth(6);
        sash.setWeights(30, 70);

        UIUtils.createInfoLabel(parent, AIUIMessages.gpt_preference_page_prompts_hint);

        promptsViewer.setSelection(new StructuredSelection(generators.getFirst()));

        return sash;
    }

    @Override
    public boolean performOk() {
        var instructions = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : customInstructions.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                instructions.put(entry.getKey(), entry.getValue());
            }
        }

        settings.setCustomInstructions(instructions);
        settingsManager.saveSettings(settings);

        return super.performOk();
    }

    @Override
    protected void performDefaults() {
        customInstructions.clear();
        activeGenerator = null;
        instructionsText.setText("");
        promptsViewer.refresh();
    }

    @NotNull
    private Viewer createViewer(@NotNull Composite parent, @NotNull List<AIPromptGeneratorDescriptor> generators) {
        var viewer = new TableViewer(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new ListContentProvider());
        viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new ViewerLabelProvider()));
        viewer.setInput(generators);
        viewer.addSelectionChangedListener(event -> {
            var descriptor = (AIPromptGeneratorDescriptor) event.getStructuredSelection().getFirstElement();
            onGeneratorChanged(descriptor);
        });

        return viewer;
    }

    @NotNull
    private Text createEditor(@NotNull Composite parent) {
        var contents = new Composite(parent, SWT.NONE);
        contents.setLayout(GridLayoutFactory.fillDefaults().create());

        var editor = new Text(contents, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        editor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        editor.addModifyListener(e -> onInstructionsChanged(editor.getText().strip()));

        return editor;
    }

    private void onGeneratorChanged(@NotNull AIPromptGeneratorDescriptor generator) {
        activeGenerator = generator;
        instructionsText.setText(getCustomInstructions(generator));
    }

    private void onInstructionsChanged(@NotNull String instructions) {
        if (activeGenerator != null) {
            var previous = customInstructions.put(activeGenerator.getId(), instructions);
            if (previous == null || !previous.equals(instructions)) {
                promptsViewer.refresh();
            }
        }
    }

    private boolean isModified(@NotNull AIPromptGeneratorDescriptor generator) {
        return !getCustomInstructions(generator).isEmpty();
    }

    @NotNull
    private String getCustomInstructions(@NotNull AIPromptGeneratorDescriptor generator) {
        return customInstructions.getOrDefault(generator.getId(), "");
    }

    private final class ViewerLabelProvider extends ColumnLabelProvider implements DelegatingStyledCellLabelProvider.IStyledLabelProvider {
        private static final StyledString.Styler ITALIC_STYLER = new StyledString.Styler() {
            @Override
            public void applyStyles(@NotNull TextStyle style) {
                style.font = BaseThemeSettings.instance.treeAndTableFontItalic;
            }
        };

        @NotNull
        @Override
        public StyledString getStyledText(Object element) {
            var descriptor = (AIPromptGeneratorDescriptor) element;
            var modified = isModified(descriptor);
            return new StyledString(descriptor.getLabel(), modified ? ITALIC_STYLER : null);
        }

        @Nullable
        @Override
        public Image getImage(@NotNull Object element) {
            var descriptor = (AIPromptGeneratorDescriptor) element;
            return descriptor.getIcon() != null ? DBeaverIcons.getImage(descriptor.getIcon()) : null;
        }
    }
}
