/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;

/**
 * RSV grouping panel
 */
public class GroupingPanel implements IResultSetPanel {

    private static final Log log = Log.getLog(GroupingPanel.class);

    public static final String PANEL_ID = "results-grouping";

    public static final String SETTINGS_SECTION_GROUPING = "panel-" + PANEL_ID;

    private IResultSetPresentation presentation;
    private IDialogSettings panelSettings;

    public GroupingPanel() {
    }

    @Override
    public Control createContents(final IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;
        this.panelSettings = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_GROUPING);
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        loadSettings();

        if (this.presentation instanceof ISelectionProvider) {
            ((ISelectionProvider) this.presentation).addSelectionChangedListener(event -> {
                if (presentation.getController().getVisiblePanel() == GroupingPanel.this) {
                    refresh(false);
                }
            });
        }

/*
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(manager -> {
            manager.add(new CopyAction());
            manager.add(new CopyAllAction());
            manager.add(new Separator());
            fillToolBar(manager);
        });

        menuMgr.setRemoveAllWhenShown(true);
        this.aggregateTable.setMenu(menuMgr.createContextMenu(this.aggregateTable));
*/

        return composite;
    }

    private void loadSettings() {
/*
        IDialogSettings functionsSection = panelSettings.getSection("functions");
        if (functionsSection != null) {
            final Map<AggregateFunctionDescriptor, Integer> funcIndexes = new HashMap<>();

            for (IDialogSettings funcSection : functionsSection.getSections()) {
                String funcId = funcSection.getName();
                if (!funcSection.getBoolean("enabled")) {
                    continue;
                }
                AggregateFunctionDescriptor func = FunctionsRegistry.getInstance().getFunction(funcId);
                if (func == null) {
                    log.debug("Function '" + funcId + "' not found");
                } else {
                    funcIndexes.put(func, funcSection.getInt("index"));
                    enabledFunctions.add(func);
                }
            }
            enabledFunctions.sort(Comparator.comparingInt(funcIndexes::get));
        }

        if (enabledFunctions.isEmpty()) {
            loadDefaultFunctions();
        }
*/
    }

    private void saveSettings() {
        IDialogSettings functionsSection = UIUtils.getSettingsSection(panelSettings, "functions");

/*
        for (AggregateFunctionDescriptor func : FunctionsRegistry.getInstance().getFunctions()) {
            IDialogSettings funcSection = UIUtils.getSettingsSection(functionsSection, func.getId());
            boolean enabled = enabledFunctions.contains(func);
            funcSection.put("enabled", enabled);
            if (enabled) {
                funcSection.put("index", enabledFunctions.indexOf(func));
            } else {
                funcSection.put("index", -1);
            }
        }
*/
    }

    @Override
    public void activatePanel() {
        refresh(false);
    }

    @Override
    public void deactivatePanel() {

    }

    @Override
    public void refresh(boolean force) {
/*
        aggregateTable.setRedraw(false);
        try {
            aggregateTable.removeAll();
            if (this.presentation instanceof ISelectionProvider) {
                ISelection selection = ((ISelectionProvider) presentation).getSelection();
                if (selection instanceof IResultSetSelection) {
                    aggregateSelection((IResultSetSelection)selection);
                }
            }
            UIUtils.packColumns(aggregateTable, true, null);
        } finally {
            aggregateTable.setRedraw(true);
        }
*/
        saveSettings();
    }

    @Override
    public void contributeActions(ToolBarManager manager) {
        fillToolBar(manager);
    }

    private void fillToolBar(IContributionManager contributionManager)
    {
/*
        contributionManager.add(new AddFunctionAction());
        contributionManager.add(new RemoveFunctionAction());
        contributionManager.add(new ResetFunctionsAction());
        contributionManager.add(new Separator());
        contributionManager.add(new GroupByColumnsAction());
*/
    }

}
