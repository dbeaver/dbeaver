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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.driver.DataSourceTypeViewer;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

/**
 * Data source selection page
 * step1
 */
class ConnectionPageDataSource extends ActiveWizardPage<NewConnectionWizard> implements ISelectionChangedListener, IDoubleClickListener {

    private DBPDataSourceType selectedDataSourceType;
    private DataSourceTypeViewer dataSourceTypeViewer;
    private ProjectSelectorPanel projectSelector;
    private Control filterIndentLabel;

    ConnectionPageDataSource(NewConnectionWizard wizard) {
        super("newConnectionDrivers");
        setTitle(UIConnectionMessages.dialog_new_connection_wizard_start_title);
        setDescription(UIConnectionMessages.dialog_new_connection_wizard_start_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite placeholder = UIUtils.createComposite(parent, 1);

        setControl(placeholder);

        Composite controlsGroup = UIUtils.createComposite(placeholder, 4);
        controlsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        {
            dataSourceTypeViewer = new DataSourceTypeViewer(
                placeholder,
                this,
                getWizard().getAvailableProvides()
            ) {
                @Override
                protected void createExtraFilterControlsBefore(@NotNull Composite filterGroup) {
                    ((GridLayout) filterGroup.getLayout()).numColumns++;
                    filterIndentLabel = UIUtils.createEmptyLabel(filterGroup, 1, 1);
                    GridData gd = new GridData();
                    gd.widthHint = 100;
                    filterIndentLabel.setLayoutData(gd);
                }

                @Override
                protected void createExtraFilterControlsAfter(@NotNull Composite filterGroup) {
                    ((GridLayout) filterGroup.getLayout()).numColumns++;
                    Composite extraControlsComposite = UIUtils.createComposite(filterGroup, 1);
                    extraControlsComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

                    createSorterControl(extraControlsComposite);
                }
            };
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            gd.widthHint = 400;
            dataSourceTypeViewer.getControl().setLayoutData(gd);

            ((GridData) filterIndentLabel.getLayoutData()).widthHint =
                dataSourceTypeViewer.getFolderComposite().getTabsWidth() -
                    ((GridLayout) filterIndentLabel.getParent().getLayout()).horizontalSpacing - 1;
        }

        {
            Composite bottomPanel = new Composite(placeholder, SWT.NONE);
            bottomPanel.setLayout(new GridLayout(1, false));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            bottomPanel.setLayoutData(gd);
            projectSelector = new ProjectSelectorPanel(bottomPanel, NavigatorUtils.getSelectedProject(), SWT.NONE, true);
            if (projectSelector.getSelectedProject() == null) {
                setErrorMessage("You need to create a project first");
            }
        }

        Dialog.applyDialogFont(placeholder);
        UIUtils.setHelp(placeholder, IHelpContextIds.CTX_CON_WIZARD_DRIVER);
        UIUtils.asyncExec(() -> dataSourceTypeViewer.getControl().setFocus());
    }

    public void createSorterControl(Composite controlsGroup) {
        // Sorter
        Composite orderGroup = new Composite(controlsGroup, SWT.NONE);
        orderGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        orderGroup.setLayout(new RowLayout());
        new Label(orderGroup, SWT.NONE).setText(UIConnectionMessages.driver_connection_sort_by + " ");
        DataSourceTypeViewer.OrderBy defaultOrderBy = DataSourceTypeViewer.getDefaultOrderBy();

        for (DataSourceTypeViewer.OrderBy ob : DataSourceTypeViewer.OrderBy.values()) {
            Button obScoreButton = new Button(orderGroup, SWT.RADIO);
            obScoreButton.setText(ob.getLabel());
            obScoreButton.setToolTipText(ob.getDescription());
            obScoreButton.setData(ob);
            if (ob == defaultOrderBy) {
                obScoreButton.setSelection(true);
            }
            obScoreButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> dataSourceTypeViewer.setOrderBy(
                        (DataSourceTypeViewer.OrderBy) obScoreButton.getData())));
        }
    }

    public DBPDataSourceType getSelectedDataSourceType() {
        return selectedDataSourceType;
    }

    public void setSelectedDataSourceType(DBPDataSourceType selectedDataSourceType) {
        this.selectedDataSourceType = selectedDataSourceType;
    }

    public DBPProject getConnectionProject() {
        return projectSelector.getSelectedProject();
    }

    @NotNull
    public DBNBrowseSettings getNavigatorSettings() {
        return DataSourceNavigatorSettings.getDefaultSettings();
    }

    @Override
    public boolean canFlipToNextPage() {
        return this.projectSelector.getSelectedProject() != null && this.selectedDataSourceType != null;
    }

    @Override
    public boolean isPageComplete() {
        if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_DATABASE_DEVELOPER)) {
            setErrorMessage("The user needs more permissions to create a new connection.");
            return false;
        }
        return canFlipToNextPage();
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        this.selectedDataSourceType = null;
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection ss) {
            Object selectedObject = ss.getFirstElement();
            if (selectedObject instanceof DBPDataSourceType dataSourceType) {
                selectedDataSourceType = dataSourceType;
            }
        }
        getWizard().getContainer().updateButtons();
    }

    @Override
    public void doubleClick(DoubleClickEvent event) {
        if (selectedDataSourceType != null && projectSelector.getSelectedProject() != null) {
            NewConnectionWizard wizard = getWizard();
            wizard.getContainer().showPage(wizard.getNextPage(this));
        }
    }

}
