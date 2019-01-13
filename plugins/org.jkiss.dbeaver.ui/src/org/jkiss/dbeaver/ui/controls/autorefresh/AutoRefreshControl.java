/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.autorefresh;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

import java.util.ArrayList;
import java.util.List;

public class AutoRefreshControl {

    private Control parent;
    private String controlId;
    private DBRRunnableWithProgress runnable;
    private AutoRefreshJob autoRefreshJob;
    private RefreshSettings refreshSettings;
    private volatile boolean autoRefreshEnabled = false;

    private ToolItem autoRefreshButton;
    private Menu schedulerMenu;

    public AutoRefreshControl(Control parent, String controlId, DBRRunnableWithProgress runnable) {
        this.parent = parent;
        this.controlId = controlId;
        this.runnable = runnable;

        parent.addDisposeListener(e -> {
            if (schedulerMenu != null) {
                schedulerMenu.dispose();
                schedulerMenu = null;
            }
        });
    }

    String getControlId() {
        return controlId;
    }

    public DBRRunnableWithProgress getRunnable() {
        return runnable;
    }

    private synchronized RefreshSettings getRefreshSettings() {
        if (refreshSettings == null) {
            refreshSettings = new RefreshSettings(controlId);
            refreshSettings.loadSettings();
        }
        return refreshSettings;
    }

    private synchronized void setRefreshSettings(RefreshSettings refreshSettings) {
        this.refreshSettings = refreshSettings;
        this.refreshSettings.saveSettings();
    }

    synchronized boolean isAutoRefreshEnabled() {
        return autoRefreshEnabled;
    }

    public synchronized void enableAutoRefresh(boolean enable) {
        this.autoRefreshEnabled = enable;
        scheduleAutoRefresh(false);
        updateAutoRefreshToolbar();
    }

    public synchronized void scheduleAutoRefresh(boolean afterError) {
        if (autoRefreshJob != null) {
            autoRefreshJob.cancel();
            autoRefreshJob = null;
        }
        if (!this.autoRefreshEnabled || parent.isDisposed()) {
            return;
        }
        RefreshSettings settings = getRefreshSettings();
        if (afterError && settings.isStopOnError()) {
            return;
        }
        autoRefreshJob = new AutoRefreshJob(this);
        autoRefreshJob.schedule((long)settings.getRefreshInterval() * 1000);
    }

    public void cancelRefresh() {
        // Cancel any auto-refresh activities
        final AutoRefreshJob refreshJob = this.autoRefreshJob;
        if (refreshJob != null) {
            refreshJob.cancel();
            this.autoRefreshJob = null;
        }
    }

    public void populateRefreshButton(ToolBar toolbar) {
        if (autoRefreshButton != null && !autoRefreshButton.isDisposed()) {
            autoRefreshButton.dispose();
        }
        autoRefreshButton = new ToolItem(toolbar, SWT.DROP_DOWN | SWT.NO_FOCUS);
        autoRefreshButton.addSelectionListener(new AutoRefreshMenuListener(autoRefreshButton));
        updateAutoRefreshToolbar();
    }

    public void populateRefreshButton(IContributionManager contributionManager) {
        contributionManager.add(new ContributionItem() {
            @Override
            public void fill(ToolBar parent, int index) {
                populateRefreshButton(parent);
            }
        });
    }

    private void updateAutoRefreshToolbar() {
        if (autoRefreshButton != null && !autoRefreshButton.isDisposed()) {
            if (isAutoRefreshEnabled()) {
                autoRefreshButton.setImage(DBeaverIcons.getImage(UIIcon.RS_SCHED_STOP));
                autoRefreshButton.setToolTipText(UIMessages.sql_editor_resultset_filter_panel_btn_stop_refresh);
            } else {
                autoRefreshButton.setImage(DBeaverIcons.getImage(UIIcon.RS_SCHED_START));
                autoRefreshButton.setToolTipText(UIMessages.sql_editor_resultset_filter_panel_btn_config_refresh);
            }
        }
    }

    private static final int[] AUTO_REFRESH_DEFAULTS = new int[]{1, 5, 10, 15, 30, 60};

    private class AutoRefreshMenuListener extends SelectionAdapter {
        private final ToolItem dropdown;

        AutoRefreshMenuListener(ToolItem item) {
            this.dropdown = item;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            if (e.detail == SWT.ARROW) {
                ToolItem item = (ToolItem) e.widget;
                Rectangle rect = item.getBounds();
                Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));

                if (schedulerMenu != null) {
                    schedulerMenu.dispose();
                }
                schedulerMenu = new Menu(dropdown.getParent().getShell());
                {
                    MenuItem mi = new MenuItem(schedulerMenu, SWT.NONE);
                    mi.setText(UIMessages.sql_editor_resultset_filter_panel_menu_customize);
                    mi.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            runCustomized();
                        }
                    });

                    mi = new MenuItem(schedulerMenu, SWT.NONE);
                    mi.setText(UIMessages.sql_editor_resultset_filter_panel_menu_stop);
                    mi.setEnabled(isAutoRefreshEnabled());
                    mi.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            enableAutoRefresh(false);
                        }
                    });
                    new MenuItem(schedulerMenu, SWT.SEPARATOR);

                    List<Integer> presetList = new ArrayList<>();
                    for (int t : AUTO_REFRESH_DEFAULTS) presetList.add(t);

                    int defaultInterval = getRefreshSettings().getRefreshInterval();
                    if (defaultInterval > 0 && !presetList.contains(defaultInterval)) {
                        presetList.add(0, defaultInterval);
                    }

                    for (int i = 0; i < presetList.size(); i++) {
                        final Integer timeout = presetList.get(i);
                        mi = new MenuItem(schedulerMenu, SWT.PUSH);
                        String text = i == 0 ?
                            NLS.bind(UIMessages.sql_editor_resultset_filter_panel_menu_refresh_interval, timeout) :
                            NLS.bind(UIMessages.sql_editor_resultset_filter_panel_menu_refresh_interval_1, timeout);
                        mi.setText(text);
                        if (isAutoRefreshEnabled() && timeout == defaultInterval) {
                            schedulerMenu.setDefaultItem(mi);
                        }
                        mi.addSelectionListener(new SelectionAdapter() {
                            @Override
                            public void widgetSelected(SelectionEvent e) {
                                runPreset(timeout);
                            }
                        });
                    }
                }

                schedulerMenu.setLocation(pt.x, pt.y + rect.height);
                schedulerMenu.setVisible(true);
            } else {
                if (isAutoRefreshEnabled()) {
                    enableAutoRefresh(false);
                } else {
                    runCustomized();
                }
            }
        }

        private void runCustomized() {
            AutoRefreshConfigDialog dialog = new AutoRefreshConfigDialog(parent.getShell(), getRefreshSettings());
            if (dialog.open() == IDialogConstants.OK_ID) {
                setRefreshSettings(dialog.getRefreshSettings());
                enableAutoRefresh(true);
            }
        }

        private void runPreset(int interval) {
            RefreshSettings settings = new RefreshSettings(getRefreshSettings());
            settings.setRefreshInterval(interval);
            setRefreshSettings(settings);
            enableAutoRefresh(true);
        }
    }


}
