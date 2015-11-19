/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.sql.ViewSQLDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * ResultSetFilterPanel
 */
class ResultSetFilterPanel extends Composite
{
    private final ResultSetViewer viewer;

    private Combo filtersText;

    private ToolItem filtersApplyButton;
    private ToolItem filtersClearButton;
    private ToolItem filtersCustomButton;
    private ToolItem historyBackButton;
    private ToolItem historyForwardButton;

    private ControlEnableState filtersEnableState;

    public ResultSetFilterPanel(ResultSetViewer rsv) {
        super(rsv.getControl(), SWT.NONE);
        this.viewer = rsv;

        this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 3;
        gl.marginWidth = 3;
        this.setLayout(gl);

        Button sourceQueryButton = new Button(this, SWT.PUSH | SWT.NO_FOCUS);
        sourceQueryButton.setImage(DBeaverIcons.getImage(UIIcon.SQL_TEXT));
        sourceQueryButton.setText("SQL");
        sourceQueryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                DBCStatistics statistics = viewer.getModel().getStatistics();
                String queryText = statistics == null ? null : statistics.getQueryText();
                if (queryText == null || queryText.isEmpty()) {
                    queryText = "<empty>";
                }
                ViewSQLDialog dialog = new ViewSQLDialog(viewer.getSite(), viewer.getExecutionContext(), "Query Text", DBeaverIcons.getImage(UIIcon.SQL_TEXT), queryText);
                dialog.setEnlargeViewPanel(false);
                dialog.setWordWrap(true);
                dialog.open();
            }
        });

/*
        Button customizeButton = new Button(this, SWT.PUSH | SWT.NO_FOCUS);
        customizeButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER));
        customizeButton.setText("Filters");
        customizeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new FilterSettingsDialog(viewer).open();
            }
        });
*/

        this.filtersText = new Combo(this, SWT.BORDER | SWT.DROP_DOWN);
        this.filtersText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.filtersText.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                setCustomDataFilter();
            }
        });

        {
            // Register filters text in focus service
            UIUtils.addFocusTracker(viewer.getSite(), UIUtils.INLINE_WIDGET_EDITOR_ID, this.filtersText);

            this.filtersText.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e)
                {
                    // Unregister from focus service
                    UIUtils.removeFocusTracker(viewer.getSite(), filtersText);
                    dispose();
                }
            });
        }

        // Handle all shortcuts by filters editor, not by host editor
        UIUtils.enableHostEditorKeyBindingsSupport(viewer.getSite(), this.filtersText);

        ToolBar filterToolbar = new ToolBar(this, SWT.HORIZONTAL | SWT.RIGHT);

        filtersApplyButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
        filtersApplyButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER_APPLY));
        //filtersApplyButton.setText("Apply");
        filtersApplyButton.setToolTipText("Apply filter criteria");
        filtersApplyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setCustomDataFilter();
            }
        });
        filtersApplyButton.setEnabled(false);

        filtersClearButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
        filtersClearButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER_RESET));
        filtersClearButton.setToolTipText("Remove all filters");
        filtersClearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                viewer.resetDataFilter(true);
            }
        });
        filtersClearButton.setEnabled(false);

        filtersCustomButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
        filtersCustomButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER));
        filtersCustomButton.setToolTipText("Custom Filters");
        filtersCustomButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new FilterSettingsDialog(viewer).open();
            }
        });
        filtersCustomButton.setEnabled(true);

        historyBackButton = new ToolItem(filterToolbar, SWT.DROP_DOWN | SWT.NO_FOCUS);
        historyBackButton.setImage(DBeaverIcons.getImage(UIIcon.RS_BACK));
        historyBackButton.setEnabled(false);
        historyBackButton.addSelectionListener(new HistoryMenuListener(historyBackButton, true));

        historyForwardButton = new ToolItem(filterToolbar, SWT.DROP_DOWN | SWT.NO_FOCUS);
        historyForwardButton.setImage(DBeaverIcons.getImage(UIIcon.RS_FORWARD));
        historyForwardButton.setEnabled(false);
        historyForwardButton.addSelectionListener(new HistoryMenuListener(historyForwardButton, false));

        this.filtersText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                if (filtersEnableState == null) {
                    String filterText = filtersText.getText();
                    filtersApplyButton.setEnabled(true);
                    filtersClearButton.setEnabled(!CommonUtils.isEmpty(filterText));
                }
            }
        });

        this.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e)
            {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    setCustomDataFilter();
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            }
        });

        filtersEnableState = ControlEnableState.disable(this);

    }

    void enableFilters(boolean enableFilters) {
        if (enableFilters) {
            if (filtersEnableState != null) {
                filtersEnableState.restore();
                filtersEnableState = null;
            }
            int historyPosition = viewer.getHistoryPosition();
            List<ResultSetViewer.StateItem> stateHistory = viewer.getStateHistory();

            String filterText = filtersText.getText();
            filtersApplyButton.setEnabled(true);
            filtersClearButton.setEnabled(!CommonUtils.isEmpty(filterText));
            // Update history buttons
            if (historyPosition > 0) {
                historyBackButton.setEnabled(true);
                historyBackButton.setToolTipText(stateHistory.get(historyPosition - 1).describeState());
            } else {
                historyBackButton.setEnabled(false);
            }
            if (historyPosition < stateHistory.size() - 1) {
                historyForwardButton.setEnabled(true);
                historyForwardButton.setToolTipText(stateHistory.get(historyPosition + 1).describeState());
            } else {
                historyForwardButton.setEnabled(false);
            }
        } else if (filtersEnableState == null) {
            filtersEnableState = ControlEnableState.disable(this);
        }
    }

    private void setCustomDataFilter()
    {
        DBCExecutionContext context = viewer.getExecutionContext();
        if (context == null) {
            return;
        }
        String condition = filtersText.getText();
        StringBuilder currentCondition = new StringBuilder();
        SQLUtils.appendConditionString(viewer.getModel().getDataFilter(), context.getDataSource(), null, currentCondition, true);
        if (currentCondition.toString().trim().equals(condition.trim())) {
            // The same
            return;
        }
        DBDDataFilter newFilter = viewer.getModel().createDataFilter();
        newFilter.setWhere(condition);
        viewer.setDataFilter(newFilter, true);
        viewer.getControl().setFocus();
    }

    void addFiltersHistory(String whereCondition)
    {
        int historyCount = filtersText.getItemCount();
        for (int i = 0; i < historyCount; i++) {
            if (filtersText.getItem(i).equals(whereCondition)) {
                if (i > 0) {
                    // Move to beginning
                    filtersText.remove(i);
                    break;
                } else {
                    return;
                }
            }
        }
        filtersText.add(whereCondition, 0);
        filtersText.setText(whereCondition);
    }

    Control getEditControl() {
        return filtersText;
    }

    void setFilterValue(String whereCondition) {
        filtersText.setText(whereCondition);
    }

    private class HistoryMenuListener extends SelectionAdapter {
        private final ToolItem dropdown;
        private final boolean back;
        public HistoryMenuListener(ToolItem item, boolean back) {
            this.dropdown = item;
            this.back = back;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            int historyPosition = viewer.getHistoryPosition();
            List<ResultSetViewer.StateItem> stateHistory = viewer.getStateHistory();
            if (e.detail == SWT.ARROW) {
                ToolItem item = (ToolItem) e.widget;
                Rectangle rect = item.getBounds();
                Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));

                Menu menu = new Menu(dropdown.getParent().getShell());
                menu.setLocation(pt.x, pt.y + rect.height);
                menu.setVisible(true);
                for (int i = historyPosition + (back ? -1 : 1); i >= 0 && i < stateHistory.size(); i += back ? -1 : 1) {
                    MenuItem mi = new MenuItem(menu, SWT.NONE);
                    ResultSetViewer.StateItem state = stateHistory.get(i);
                    mi.setText(state.describeState());
                    final int statePosition = i;
                    mi.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            viewer.navigateHistory(statePosition);
                        }
                    });
                }
            } else {
                int newPosition = back ? historyPosition - 1 : historyPosition + 1;
                viewer.navigateHistory(newPosition);
            }
        }
    }

}
