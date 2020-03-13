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
package org.jkiss.dbeaver.ui.controls.resultset.panel.references;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * RSV references panel
 */
public class ReferencesPanel implements IResultSetPanel {

    //private static final Log log = Log.getLog(ReferencesPanel.class);

    private static final String PANEL_ID = "results-references";

    private static final String SETTINGS_SECTION_GROUPING = "panel-" + PANEL_ID;

    private IResultSetPresentation presentation;
    private IDialogSettings panelSettings;

    private Composite referencesPlaceholder;
    private ReferencesResultsContainer resultsContainer;

    public ReferencesPanel() {
    }

    @Override
    public Control createContents(final IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;
        this.panelSettings = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_GROUPING);

        loadSettings();

        this.referencesPlaceholder = new Composite(parent, SWT.NONE);
        this.referencesPlaceholder.setLayout(new FillLayout());

        // Data listener
        ResultSetListenerAdapter dataListener = new ResultSetListenerAdapter() {
            @Override
            public void handleResultSetLoad() {
                refresh(true);
            }
        };
        presentation.getController().addListener(dataListener);
        presentation.getControl().addDisposeListener(e -> presentation.getController().removeListener(dataListener));

        if (presentation instanceof ISelectionProvider) {
            ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
                private List<ResultSetRow> prevSelection;
                @Override
                public void selectionChanged(SelectionChangedEvent event) {
                    if (presentation.getController().getVisiblePanel() != ReferencesPanel.this) {
                        return;
                    }
                    if (!(event.getSelection() instanceof IResultSetSelection)) {
                        return;
                    }
                    List<ResultSetRow> selectedItems = ((IResultSetSelection) event.getSelection()).getSelectedRows();
                    if (CommonUtils.equalObjects(prevSelection, selectedItems)) {
                        return;
                    }
                    this.prevSelection = selectedItems;
                    getResultsContainer().refreshReferences();
                }
            };
            ((ISelectionProvider) presentation).addSelectionChangedListener(selectionListener);
            presentation.getControl().addDisposeListener(e -> ((ISelectionProvider) presentation).removeSelectionChangedListener(selectionListener));
        }

        return referencesPlaceholder;
    }

    private ReferencesResultsContainer getResultsContainer() {
        if (this.resultsContainer == null) {
            this.resultsContainer = new ReferencesResultsContainer(referencesPlaceholder, presentation.getController());
            referencesPlaceholder.layout(true, true);
        }

        return this.resultsContainer;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    private void updateControls() {
        // Update panel toolbar
        this.presentation.getController().updatePanelActions();
    }

    private void loadSettings() {
        IDialogSettings functionsSection = panelSettings.getSection("references");
    }

    private void saveSettings() {
        IDialogSettings functionsSection = UIUtils.getSettingsSection(panelSettings, "references");
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
        if (presentation.getController().getVisiblePanel() == this) {
            getResultsContainer().refreshReferences();
        }
    }

    @Override
    public void contributeActions(IContributionManager manager) {

    }

}
