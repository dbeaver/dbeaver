/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
public class GroupingPanel implements IResultSetPanel, IResultSetListener {

    private static final Log log = Log.getLog(GroupingPanel.class);

    public static final String PANEL_ID = "results-grouping";

    public static final String SETTINGS_SECTION_GROUPING = "panel-" + PANEL_ID;

    private IResultSetPresentation presentation;
    private IDialogSettings panelSettings;

    private GroupingResultsContainer resultsContainer;

    public GroupingPanel() {
    }

    @Override
    public Control createContents(final IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;
        this.panelSettings = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_GROUPING);

        loadSettings();

        this.resultsContainer = new GroupingResultsContainer(parent, presentation);

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
        this.presentation.getController().addListener(this);
        this.resultsContainer.getResultSetController().getControl().addDisposeListener(e ->
            this.presentation.getController().removeListener(this));

        return this.resultsContainer.getResultSetController().getControl();
    }

    private void loadSettings() {
        IDialogSettings functionsSection = panelSettings.getSection("groups");
    }

    private void saveSettings() {
        IDialogSettings functionsSection = UIUtils.getSettingsSection(panelSettings, "groups");
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

    @Override
    public void handleResultSetLoad() {
        resultsContainer.clearGrouping();
    }

    @Override
    public void handleResultSetChange() {

    }

}
