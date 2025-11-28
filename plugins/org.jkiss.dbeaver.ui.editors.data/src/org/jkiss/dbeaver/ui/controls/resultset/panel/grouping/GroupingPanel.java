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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ToolbarSeparatorContribution;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.panel.ResultSetPanelBase;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.action.*;

/**
 * RSV grouping panel
 */
public class GroupingPanel extends ResultSetPanelBase {

    private static final String PANEL_ID = "results-grouping";

    private static final String SETTINGS_SECTION_GROUPING = "panel-" + PANEL_ID;

    private IResultSetPresentation presentation;
    private IDialogSettings panelSettings;

    private GroupingResultsContainer resultsContainer;
    private Composite groupingPlaceholder;
    private IResultSetListener ownerListener;

    public GroupingPanel() {
    }

    @Override
    public Control createContents(final IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;
        this.panelSettings = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_GROUPING);

        loadSettings();

        this.groupingPlaceholder = new Composite(parent, SWT.NONE);
        this.groupingPlaceholder.setLayout(new FillLayout());

        this.ownerListener = new ResultSetListenerAdapter() {
            @Override
            public void handleResultSetLoad() {
                refresh(true);
            }
        };
        //this.presentation.getController().addListener(ownerListener);

        return groupingPlaceholder;
    }

    private GroupingResultsContainer getGroupingResultsContainer() {
        if (resultsContainer == null) {
            this.resultsContainer = new GroupingResultsContainer(groupingPlaceholder, presentation);

            IResultSetController groupingViewer = this.resultsContainer.getResultSetController();

            groupingViewer.getControl().addDisposeListener(e ->
                this.presentation.getController().removeListener(ownerListener));

            IResultSetListener groupingResultsListener = new ResultSetListenerAdapter() {
                @Override
                public void handleResultSetLoad() {
                    updateControls();
                }

                @Override
                public void handleResultSetSelectionChange(SelectionChangedEvent event) {
                    //updateControls();
                }
            };
            groupingViewer.addListener(groupingResultsListener);
        }

        return resultsContainer;
    }
    @Override
    public boolean isDirty() {
        return !getGroupingResultsContainer().getGroupAttributes().isEmpty();
    }

    private void updateControls() {
        // Update panel toolbar
        this.presentation.getController().updatePanelActions();
    }

    private void loadSettings() {
        IDialogSettings functionsSection = panelSettings.getSection("groups");
    }

    private void saveSettings() {
        IDialogSettings functionsSection = UIUtils.getSettingsSection(panelSettings, "groups");
    }

    @Override
    public void activatePanel() {
        getGroupingResultsContainer();
        refresh(false);
        groupingPlaceholder.layout(true, true);
    }

    @Override
    public void deactivatePanel() {

    }

    @Override
    public void setFocus() {
        //resultsContainer.getResultSetController().getControl().setFocus();
    }

    @Override
    public void refresh(boolean force) {
        if (!force) {
            return;
        }
        // Here we can refresh grouping (makes sense if source query was modified with some conditions)
        // Or just clear it (if brand new query was executed)
        GroupingResultsContainer groupingResultsContainer = getGroupingResultsContainer();
        if (presentation.getController().getModel().isMetadataChanged()) {
            groupingResultsContainer.clearGrouping();
        } else {
            try {
                groupingResultsContainer.rebuildGrouping();
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Grouping error", "Can't refresh grouping query", e);
            }
        }
        groupingPlaceholder.layout(true, true);
    }

    @Override
    public void contributeActions(IContributionManager manager) {
        fillToolBar(manager);
    }

    private void fillToolBar(IContributionManager contributionManager) {
        ActionContributionItem sortAction = new ActionContributionItem(new DefaultSortingAction(getGroupingResultsContainer()));
        sortAction.setMode(ActionContributionItem.MODE_FORCE_TEXT);
        contributionManager.add(sortAction);
        contributionManager.add(new DuplicatesOnlyAction(getGroupingResultsContainer()));
        contributionManager.add(new PercentFromTotalAction(getGroupingResultsContainer()));
        contributionManager.add(new ToolbarSeparatorContribution(true));
        contributionManager.add(new EditColumnsAction(getGroupingResultsContainer()));
        contributionManager.add(new DeleteColumnAction(getGroupingResultsContainer()));
        contributionManager.add(new ToolbarSeparatorContribution(true));
        contributionManager.add(new ClearGroupingAction(getGroupingResultsContainer()));
    }

    private class PresentationToggleAction extends Action {
        private final ResultSetPresentationDescriptor presentationDescriptor;

        public PresentationToggleAction(ResultSetPresentationDescriptor presentationDescriptor) {
            super(presentationDescriptor.getLabel(), Action.AS_RADIO_BUTTON);
            this.presentationDescriptor = presentationDescriptor;
            setImageDescriptor(DBeaverIcons.getImageDescriptor(presentationDescriptor.getIcon()));
            setToolTipText(presentationDescriptor.getDescription());
            // Icons turns menu into mess - checkboxes are much better
            //setImageDescriptor(DBeaverIcons.getImageDescriptor(panel.getIcon()));
        }

        @Override
        public boolean isChecked() {
            return presentationDescriptor.matches(
                getGroupingResultsContainer().getResultSetController().getActivePresentation().getClass());
        }

        @Override
        public void run() {
            ((ResultSetViewer)getGroupingResultsContainer().getResultSetController()).switchPresentation(presentationDescriptor);
        }
    }

}
