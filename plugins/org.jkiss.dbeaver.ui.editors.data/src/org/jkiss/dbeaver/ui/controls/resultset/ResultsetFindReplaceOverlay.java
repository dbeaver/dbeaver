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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.findandreplace.SearchOptions;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.findandreplace.AccessibleToolItemBuilder;
import org.jkiss.dbeaver.ui.controls.findandreplace.FindReplaceOverlay;
import org.jkiss.dbeaver.ui.controls.findandreplace.SearchQuickFilterInfo;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.utils.CommonUtils;

public class ResultsetFindReplaceOverlay extends FindReplaceOverlay {

    // delay between the search field modification and an actual filter application to swallow more upcoming keystrokes in one go
    private static final long filterSchedulingTimeoutMilliseconds = 250;

    // we want default to be ENABLE, but initial when not set is FALSE, so storing inverted value
    private static final String INTERACTIVE_FILTER_DISABLED_DIALOG_SETTING = "interactiveFilterDisabled"; //$NON-NLS-1$

    @NotNull
    private final AbstractPresentation resultsetPresentation;

    private boolean findAllActionApplied = false;
    private boolean isInteractiveFilterEnabled = true;

    private IAction enableInteractiveFilterAction;
    private IAction disableInteractiveFilterAction;

    private Composite extraContentContainer;

    private Composite placeholder;
    private GridData placeholderLayoutData;
    private GridData extraContentLayoutData;

    @NotNull
    private final AbstractUIJob interactiveFilterJob = new AbstractUIJob("Resultset filter job") {

        @NotNull
        @Override
        protected IStatus runInUIThread(@NotNull DBRProgressMonitor monitor) {
            try {
                ResultsetFindReplaceOverlay.this.applyInteractiveFilterJob();
                return Status.OK_STATUS;
            } catch (Throwable e) {
                log.debug(e);
                return Status.CANCEL_STATUS;
            }
        }
    };

    public ResultsetFindReplaceOverlay(
        @NotNull IWorkbenchPart part,
        @NotNull Composite targetControl,
        @NotNull IFindReplaceTarget target,
        @NotNull AbstractPresentation resultsetPresentation
    ) {
        super(part, targetControl, target, resultsetPresentation);
        this.resultsetPresentation = resultsetPresentation;
    }

    @Override
    protected void configureSelectAllButton(@NotNull AccessibleToolItemBuilder buttonBuilder) {
        buttonBuilder.withStyleBits(SWT.CHECK)
                     .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_FIND_ALL_FILTER))
                     .withToolTipText(ResultSetMessages.find_and_replace_find_all_tooltip)
                     .withOperation(this::performSelectAll).withShortcuts(KeyboardShortcuts.SEARCH_ALL);

        MenuManager contextMenuManager = new MenuManager();
        this.enableInteractiveFilterAction = new Action("Check 'Show matching' on to filter while typing") {
            @Override
            public void run() {
                if (!isInteractiveFilterEnabled) {
                    setEnableInteractiveFilter(true);
                }
            }
        };
        this.disableInteractiveFilterAction =  new Action("Use 'Show matching' explicitly to apply filter") {
            @Override
            public void run() {
                if (isInteractiveFilterEnabled) {
                    setEnableInteractiveFilter(false);
                }
            }
        };

        contextMenuManager.add(enableInteractiveFilterAction);
        contextMenuManager.add(disableInteractiveFilterAction);
        buttonBuilder.withContextMenu(contextMenuManager);
    }

    @Override
    protected void restoreOverlaySettings() {
        super.restoreOverlaySettings();

        IDialogSettings dialogSettings = this.getDialogSettings(ResultsetFindReplaceOverlay.class);

        boolean shouldEnableInteractiveFilter = !dialogSettings.getBoolean(INTERACTIVE_FILTER_DISABLED_DIALOG_SETTING);
        this.setEnableInteractiveFilter(shouldEnableInteractiveFilter);
    }

    private void setEnableInteractiveFilter(boolean enabled) {
        if (!enabled) {
            this.selectAllButton.setSelection(false);
        }
        this.isInteractiveFilterEnabled = enabled;
        this.enableInteractiveFilterAction.setChecked(enabled);
        this.disableInteractiveFilterAction.setChecked(!enabled);

        IDialogSettings dialogSettings = this.getDialogSettings(ResultsetFindReplaceOverlay.class);
        dialogSettings.put(INTERACTIVE_FILTER_DISABLED_DIALOG_SETTING, !enabled);
    }

    @Override
    protected void onSearchFieldModified(@NotNull ModifyEvent event) {
        if (this.isInteractiveFilterEnabled) {
            this.wholeWordSearchButton.setEnabled(this.findReplaceLogic.isAvailable(SearchOptions.WHOLE_WORD));
            if (this.selectAllButton.getSelection()) {
                this.scheduleFilter();
            } else {
                super.onSearchFieldModified(event);
            }
        } else {
            super.onSearchFieldModified(event);
        }
    }

    @Override
    public void setFilterState(@Nullable SearchQuickFilterInfo quickFilter) {
        if (quickFilter != null) {
            this.findAllActionApplied = true;
            this.open();
            super.setFilterState(quickFilter);
        }
        this.setExtrasVisibility(quickFilter != null);
    }

    private void applyFilterAction(@Nullable SearchQuickFilterInfo quickFilter) {
        this.resultsetPresentation.getController().getModel().setQuickFilter(quickFilter);
        this.resultsetPresentation.refreshData(false, false, false);
        this.setExtrasVisibility(quickFilter != null);
    }

    private void applyInteractiveFilterJob() {
        this.applyQuickFilter();
        this.updateIncrementalSearch();
        this.decorate();
    }

    @Override
    protected void performSelectAll() {
        if (this.isInteractiveFilterEnabled) {
            if (this.selectAllButton.getSelection()) {
                this.scheduleFilter();
            } else {
                this.applyFilterAction(null);
            }
        } else {
            this.selectAllButton.setSelection(false);
            this.applyQuickFilter();
            this.searchBar.storeHistory();
        }
    }

    private void applyQuickFilter() {
        BusyIndicator.showWhile(
            this.containerControl.getShell() != null ? this.containerControl.getShell().getDisplay() : Display.getCurrent(),
            () -> {
                this.findAllActionApplied = true;
                if (CommonUtils.isEmpty(this.searchBar.getText())) {
                    this.applyFilterAction(null);
                } else {
                    this.applyFilterAction(new SearchQuickFilterInfo(
                        this.searchBar.getText(),
                        this.caseSensitiveSearchButton.getSelection(),
                        this.regexSearchButton.getSelection(),
                        this.wholeWordSearchButton.getSelection()
                    ));
                }
            }
        );
    }

    @Override
    protected void onClose() {
        if (this.isInteractiveFilterEnabled) {
            this.interactiveFilterJob.cancel();
            this.selectAllButton.setSelection(false);
        }

        if (this.findAllActionApplied) {
            this.findAllActionApplied = false;
            BusyIndicator.showWhile(
                this.containerControl.getShell() != null ? this.containerControl.getShell().getDisplay() : Display.getCurrent(),
                () -> this.applyFilterAction(null)
            );
        }
    }

    @Override
    protected void createExtraContent(@NotNull Composite realContainerControl) {
        this.placeholder = new FixedColorComposite(realContainerControl, SWT.NONE, this.overlayBackgroundColor);
        this.placeholderLayoutData = GridDataFactory.fillDefaults().hint(0, 0).create();
        this.placeholder.setLayoutData(this.placeholderLayoutData);

        this.extraContentContainer = new FixedColorComposite(realContainerControl, SWT.NONE, this.overlayBackgroundColor);
        GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).spacing(0, 2).applyTo(this.extraContentContainer);
        this.extraContentLayoutData = GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).create();
        this.extraContentContainer.setLayoutData(this.extraContentLayoutData);

        UIUtils.createInfoLabel(this.extraContentContainer, ResultSetMessages.find_and_replace_overlay_label)
            .setToolTipText(ResultSetMessages.find_and_replace_overlay_tooltip);
    }

    private void setExtrasVisibility(boolean visible) {
        this.placeholderLayoutData.exclude = !visible;
        this.extraContentLayoutData.exclude = !visible;
        this.placeholder.setVisible(visible);
        this.extraContentContainer.setVisible(visible);
        for (Control c = extraContentContainer; c != null && c != this.targetControl; c = c.getParent()) {
            this.containerControl.layout(true);
        }
        this.updatePlacementAndVisibility(true);
    }

    private void scheduleFilter() {
        long delay;
        if (this.interactiveFilterJob.getState() == Job.RUNNING) {
            delay = filterSchedulingTimeoutMilliseconds * 2;
        } else {
            delay = filterSchedulingTimeoutMilliseconds;
            this.interactiveFilterJob.cancel();
        }
        this.interactiveFilterJob.schedule(delay);
    }
}
