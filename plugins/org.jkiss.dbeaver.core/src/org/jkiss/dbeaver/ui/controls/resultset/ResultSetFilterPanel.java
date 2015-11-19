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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBIcon;
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

    private StyledText filtersText;

    private ToolItem filtersApplyButton;
    private ToolItem filtersClearButton;
    private ToolItem historyBackButton;
    private ToolItem historyForwardButton;

    private ControlEnableState filtersEnableState;
    private final Composite filterComposite;
    private final Color hoverBgColor;

    public ResultSetFilterPanel(ResultSetViewer rsv) {
        super(rsv.getControl(), SWT.NONE);
        this.viewer = rsv;

        this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 3;
        gl.marginWidth = 3;
        this.setLayout(gl);

        hoverBgColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
            //new Color(getDisplay(), 0xe7, 0xe6, 0xe6);

/*
        Button sourceQueryButton = new Button(this, SWT.PUSH | SWT.NO_FOCUS);
        sourceQueryButton.setImage(DBeaverIcons.getImage(UIIcon.SQL_TEXT));
        sourceQueryButton.setText("SQL");
        sourceQueryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                showSourceQuery();
            }
        });
*/

        {
            filterComposite = new Composite(this, SWT.BORDER);
            gl = new GridLayout(4, false);
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            gl.horizontalSpacing = 0;
            gl.verticalSpacing = 0;
            filterComposite.setLayout(gl);
            filterComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            new ActiveObjectPanel(filterComposite);
            //new Label(filterComposite, SWT.SEPARATOR | SWT.VERTICAL);

            this.filtersText = new StyledText(filterComposite, SWT.SINGLE);
            this.filtersText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
/*
            this.filtersText.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setCustomDataFilter();
                }
            });
*/

            new RefreshPanel(filterComposite);

            //addressBar.setBackgroundMode(filtersText.getBackground());
        }

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

        ToolItem filtersCustomButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
        filtersCustomButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER));
        filtersCustomButton.setToolTipText("Custom Filters");
        filtersCustomButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new FilterSettingsDialog(viewer).open();
            }
        });
        filtersCustomButton.setEnabled(true);

        new ToolItem(filterToolbar, SWT.SEPARATOR).setControl(new Label(filterToolbar, SWT.SEPARATOR | SWT.VERTICAL));

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

    private void showSourceQuery() {
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
        filtersText.getParent().setBackground(filtersText.getBackground());
        filterComposite.layout();
        for (Control child : filterComposite.getChildren()) child.redraw();
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
/*
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
*/
        filtersText.setText(whereCondition);
    }

    Control getEditControl() {
        return filtersText;
    }

    void setFilterValue(String whereCondition) {
        filtersText.setText(whereCondition);
    }

    private class FilterPanel extends Canvas {
        public FilterPanel(Composite parent, int style) {
            super(parent, style);

            addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e) {
                    paintPanel(e);
                }
            });
        }

        protected void paintPanel(PaintEvent e) {

        }
    }

    private class ActiveObjectPanel extends FilterPanel {
        private boolean hover = false;

        public ActiveObjectPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setLayoutData(new GridData(GridData.FILL_VERTICAL));

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    filtersText.setFocus();
                }
            });
            addMouseTrackListener(new MouseTrackAdapter() {
                @Override
                public void mouseEnter(MouseEvent e) {
                    hover = true;
                    redraw();
                }

                @Override
                public void mouseExit(MouseEvent e) {
                    hover = false;
                    redraw();
                }
            });
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            GC sizingGC = new GC(this);
            Point textSize = sizingGC.textExtent(viewer.getDataContainer().getName());
            Image image = DBeaverIcons.getImage(DBIcon.TREE_TABLE);
            if (image != null) {
                textSize.x += image.getBounds().width + 4;
            }
            return new Point(
                Math.min(textSize.x + 10, filterComposite.getSize().x / 3),
                Math.min(textSize.y + 4, 20));
        }

        @Override
        protected void paintPanel(PaintEvent e) {
            e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            if (hover) {
                e.gc.setBackground(hoverBgColor);
                e.gc.fillRectangle(e.x, e.y, e.width - 3, e.height);
                e.gc.drawLine(
                    e.x + e.width - 4, e.y,
                    e.x + e.width - 4, e.y + e.height);
            } else {
                e.gc.drawLine(
                    e.x + e.width - 4, e.y + 2,
                    e.x + e.width - 4, e.y + e.height - 4);
            }

            //e.gc.setForeground(filtersText.getForeground());
            e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_DARK_GREEN));
            e.gc.setClipping(e.x, e.y, e.width - 8, e.height);

            int textOffset = 2;
            Image icon = DBeaverIcons.getImage(DBIcon.TREE_TABLE);
            if (icon != null) {
                e.gc.drawImage(icon, 2, 3);
                textOffset += icon.getBounds().width + 2;
            }
            e.gc.drawText(viewer.getDataContainer().getName(), textOffset, 3);
            e.gc.setClipping((Rectangle) null);
        }
    }

    private class RefreshPanel extends FilterPanel {
        public RefreshPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.heightHint = 10;
            setLayoutData(gd);
        }
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
