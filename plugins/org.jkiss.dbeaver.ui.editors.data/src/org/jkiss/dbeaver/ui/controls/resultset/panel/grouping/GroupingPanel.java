/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.MenuCreator;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;

/**
 * RSV grouping panel
 */
public class GroupingPanel implements IResultSetPanel {

    //private static final Log log = Log.getLog(GroupingPanel.class);

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
                    updateControls();
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
        refresh(false);
    }

    @Override
    public void deactivatePanel() {

    }

    @Override
    public void refresh(boolean force) {
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

    private void fillToolBar(IContributionManager contributionManager)
    {
        contributionManager.add(new DefaultSortingAction());
        contributionManager.add(new DuplicatesOnlyAction());
        contributionManager.add(new Separator());
        contributionManager.add(new EditColumnsAction(getGroupingResultsContainer()));
        contributionManager.add(new DeleteColumnAction(getGroupingResultsContainer()));
        contributionManager.add(new Separator());
        contributionManager.add(new ClearGroupingAction(getGroupingResultsContainer()));
    }

    abstract static class GroupingAction extends Action {
        final GroupingResultsContainer groupingResultsContainer;

        GroupingAction(GroupingResultsContainer groupingResultsContainer, String text, ImageDescriptor image) {
            super(text, image);
            this.groupingResultsContainer = groupingResultsContainer;
        }
    }

    static class EditColumnsAction extends GroupingAction {
        EditColumnsAction(GroupingResultsContainer resultsContainer) {
            super(resultsContainer, ResultSetMessages.controls_resultset_grouping_edit, DBeaverIcons.getImageDescriptor(UIIcon.OBJ_ADD));
        }

        @Override
        public void run() {
            GroupingConfigDialog dialog = new GroupingConfigDialog(groupingResultsContainer.getResultSetController().getControl().getShell(), groupingResultsContainer);
            if (dialog.open() == IDialogConstants.OK_ID) {
                try {
                    groupingResultsContainer.rebuildGrouping();
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Grouping error", "Can't change grouping settings", e);
                }
            }
        }
    }

    static class DeleteColumnAction extends GroupingAction {
        DeleteColumnAction(GroupingResultsContainer resultsContainer) {
            super(resultsContainer, ResultSetMessages.controls_resultset_grouping_remove_column, DBeaverIcons.getImageDescriptor(UIIcon.ACTION_OBJECT_DELETE));
        }

        @Override
        public boolean isEnabled() {
            return !groupingResultsContainer.getResultSetController().getSelection().isEmpty();
        }

        @Override
        public void run() {
            DBDAttributeBinding currentAttribute = groupingResultsContainer.getResultSetController().getActivePresentation().getCurrentAttribute();
            if (currentAttribute != null) {
                List<String> attributes = Collections.singletonList(currentAttribute.getFullyQualifiedName(DBPEvaluationContext.UI));
                if (groupingResultsContainer.removeGroupingAttribute(attributes) || groupingResultsContainer.removeGroupingFunction(attributes)) {
                    try {
                        groupingResultsContainer.rebuildGrouping();
                    } catch (DBException e) {
                        DBWorkbench.getPlatformUI().showError("Grouping error", "Can't change grouping query", e);
                    }
                }
            }
        }
    }

    static class ClearGroupingAction extends GroupingAction {
        ClearGroupingAction(GroupingResultsContainer resultsContainer) {
            super(resultsContainer, ResultSetMessages.controls_resultset_grouping_clear, DBeaverIcons.getImageDescriptor(UIIcon.ERASE));
        }

        @Override
        public boolean isEnabled() {
            return !groupingResultsContainer.getGroupAttributes().isEmpty();
        }

        @Override
        public void run() {
            groupingResultsContainer.clearGrouping();
            groupingResultsContainer.getOwnerPresentation().getController().updatePanelActions();
        }
    }

    class DefaultSortingAction extends Action {

        DefaultSortingAction() {
            super(ResultSetMessages.controls_resultset_grouping_default_sorting, Action.AS_DROP_DOWN_MENU);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.SORT_CONFIG));
        }

        @Override
        public IMenuCreator getMenuCreator() {
            return new MenuCreator(control -> {
                MenuManager menuManager = new MenuManager();
                menuManager.add(new ChangeSortingAction(null));
                menuManager.add(new ChangeSortingAction(Boolean.FALSE));
                menuManager.add(new ChangeSortingAction(Boolean.TRUE));
                return menuManager;
            });
        }
    }

    class ChangeSortingAction extends Action {
        private final Boolean descending;

        ChangeSortingAction(Boolean descending) {
            super(descending == null ? "Unsorted" : (descending ? "Decending" : "Ascending"), Action.AS_CHECK_BOX);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(descending == null ? UIIcon.SORT_UNKNOWN : (descending ? UIIcon.SORT_INCREASE : UIIcon.SORT_DECREASE)));
            this.descending = descending;
        }

        @Override
        public boolean isChecked() {
            DBPDataSource dataSource = getGroupingResultsContainer().getDataContainer().getDataSource();
            if (dataSource == null) {
                return false;
            }
            String defSorting = dataSource.getContainer().getPreferenceStore().getString(ResultSetPreferences.RS_GROUPING_DEFAULT_SORTING);
            if (CommonUtils.isEmpty(defSorting)) {
                return descending == null;
            } else if (defSorting.equals("ASC")) {
                return Boolean.FALSE.equals(descending);
            } else {
                return Boolean.TRUE.equals(descending);
            }
        }

        @Override
        public void run() {
            String newValue = descending == null ? "" : (descending ? "DESC" : "ASC");
            DBPDataSource dataSource = getGroupingResultsContainer().getDataContainer().getDataSource();
            if (dataSource == null) {
                return;
            }
            dataSource.getContainer().getPreferenceStore().setValue(ResultSetPreferences.RS_GROUPING_DEFAULT_SORTING, newValue);
            dataSource.getContainer().getRegistry().flushConfig();
            try {
                getGroupingResultsContainer().rebuildGrouping();
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Grouping error", "Can't change sort order", e);
            }
        }
    }

    class DuplicatesOnlyAction extends Action {
        DuplicatesOnlyAction() {
            super(ResultSetMessages.controls_resultset_grouping_show_duplicates_only, Action.AS_CHECK_BOX);
            updateImage();
        }

        private void updateImage() {
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.DUPS_RESTRICTED));
        }

        @Override
        public boolean isChecked() {
            DBPDataSource dataSource = getGroupingResultsContainer().getDataContainer().getDataSource();
            return dataSource != null && dataSource.getContainer().getPreferenceStore().getBoolean(ResultSetPreferences.RS_GROUPING_SHOW_DUPLICATES_ONLY);
        }

        @Override
        public void run() {
            boolean newValue = !isChecked();
            DBPDataSource dataSource = getGroupingResultsContainer().getDataContainer().getDataSource();
            if (dataSource == null) {
                return;
            }
            dataSource.getContainer().getPreferenceStore().setValue(ResultSetPreferences.RS_GROUPING_SHOW_DUPLICATES_ONLY, newValue);
            try {
                getGroupingResultsContainer().rebuildGrouping();
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Grouping error", "Can't change duplicates presentation", e);
            }
        }
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
