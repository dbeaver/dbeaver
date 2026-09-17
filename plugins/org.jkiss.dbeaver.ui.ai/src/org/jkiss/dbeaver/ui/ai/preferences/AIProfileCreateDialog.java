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

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.ui.CompositeBorderPainter;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
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
        Composite enginePanel = UIUtils.createTitledComposite(
            dialogArea,
            "AI Configuration",
            2,
            GridData.FILL_HORIZONTAL
        );

        aiEngines = AIEngineRegistry.getInstance().getCompletionEngines();
        selectedEngine = aiEngines.getFirst();
        profileId = genProfileId(selectedEngine);
        profileName = selectedEngine.getLabel();

        UIUtils.createControlLabel(enginePanel, "Engine");
        Composite engineSelector = new Composite(enginePanel, SWT.NONE);
        engineSelector.setLayout(GridLayoutFactory.fillDefaults().margins(2, 2).numColumns(3).create());
        engineSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new CompositeBorderPainter(engineSelector);

        Label engineIcon = new Label(engineSelector, SWT.NONE);
        engineIcon.setImage(DBeaverIcons.getImage(selectedEngine.getIcon()));
        Label engineName = new Label(engineSelector, SWT.NONE);
        engineName.setText(selectedEngine.getLabel());
        engineName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label engineArrow = new Label(engineSelector, SWT.NONE);
        engineArrow.setImage(DBeaverIcons.getImage(UIIcon.TREE_COLLAPSE));

        Text nameText = UIUtils.createLabelText(enginePanel, "Name", genProfileName(selectedEngine.getLabel()));
        GridData ngd = new GridData(GridData.FILL_HORIZONTAL);
        ngd.widthHint = UIUtils.getFontHeight(nameText) * 25;
        nameText.setLayoutData(ngd);
        nameText.addModifyListener(e ->  profileName = nameText.getText());
        Text idText = UIUtils.createLabelText(enginePanel, "ID", genProfileId(selectedEngine));
        idText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        idText.addModifyListener(e ->  profileId = idText.getText());

        Menu engineMenu = new Menu(engineSelector);
        boolean hasPromotedEngines = false;
        for (AIEngineDescriptor engine : aiEngines) {
            if (hasPromotedEngines && !engine.isPromoted()) {
                new MenuItem(engineMenu, SWT.SEPARATOR);
                hasPromotedEngines = false;
            }
            MenuItem engineItem = new MenuItem(engineMenu, SWT.RADIO);
            engineItem.setText(engine.getLabel().replace("&", "&&"));
            engineItem.setImage(DBeaverIcons.getImage(engine.getIcon()));
            engineItem.setSelection(engine == selectedEngine);
            engineItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                if (!engineItem.getSelection()) {
                    return;
                }
                for (MenuItem item : engineMenu.getItems()) {
                    if (item != engineItem && (item.getStyle() & SWT.RADIO) != 0) {
                        item.setSelection(false);
                    }
                }
                String oldAutoId = genProfileId(selectedEngine);
                selectedEngine = engine;
                engineIcon.setImage(DBeaverIcons.getImage(engine.getIcon()));
                engineName.setText(engine.getLabel());
                engineSelector.layout(true, true);
                if (oldAutoId.equals(profileId)) {
                    profileId = genProfileId(selectedEngine);
                    profileName = genProfileName(selectedEngine.getLabel());
                    idText.setText(profileId);
                    nameText.setText(profileName);
                }
            }));
            hasPromotedEngines = engine.isPromoted();
        }

        Runnable showEngineMenu = () -> {
            engineMenu.setLocation(engineSelector.toDisplay(0, engineSelector.getSize().y));
            engineMenu.setVisible(true);
        };
        MouseListener mouseListener = MouseListener.mouseDownAdapter(e -> showEngineMenu.run());
        engineSelector.addMouseListener(mouseListener);
        engineIcon.addMouseListener(mouseListener);
        engineName.addMouseListener(mouseListener);
        engineArrow.addMouseListener(mouseListener);
        engineSelector.addKeyListener(KeyListener.keyPressedAdapter(e -> {
            if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR || e.keyCode == SWT.ARROW_DOWN
                || e.character == ' ') {
                showEngineMenu.run();
            } else if (e.keyCode == SWT.ESC) {
                cancelPressed();
            }
        }));
        engineSelector.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                e.doit = true;
            }
        });
        engineSelector.addFocusListener(FocusListener.focusGainedAdapter(e -> engineSelector.redraw()));
        engineSelector.addFocusListener(FocusListener.focusLostAdapter(e -> engineSelector.redraw()));
        engineSelector.addPaintListener(e -> {
            if (engineSelector.isFocusControl()) {
                Rectangle bounds = engineSelector.getBounds();
                e.gc.drawFocus(1, 1, bounds.width - 2, bounds.height - 2);
            }
        });
        engineSelector.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(@NotNull AccessibleEvent e) {
                e.result = "Engine: " + engineName.getText();
            }
        });

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
