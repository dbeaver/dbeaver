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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * ResultSetFilterPanel
 */
class ResultSetFilterPanel extends Composite
{
    public static final int MIN_FILTER_TEXT_WIDTH = 50;
    public static final int MIN_FILTER_TEXT_HEIGHT = 20;
    private final ResultSetViewer viewer;
    private final ActiveObjectPanel activeObjectPanel;
    private final RefreshPanel refreshPanel;

    private StyledText filtersText;

    private ToolItem filtersApplyButton;
    private ToolItem filtersClearButton;
    private ToolItem historyBackButton;
    private ToolItem historyForwardButton;

    private ControlEnableState filtersEnableState;
    private final Composite filterComposite;
    private final Color hoverBgColor;
    private final GC sizingGC;

    public ResultSetFilterPanel(ResultSetViewer rsv) {
        super(rsv.getControl(), SWT.NONE);
        this.viewer = rsv;

        this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        sizingGC = new GC(this);

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

            activeObjectPanel = new ActiveObjectPanel(filterComposite);
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

            refreshPanel = new RefreshPanel(filterComposite);

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

        this.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                UIUtils.dispose(sizingGC);
            }
        });

    }

    @NotNull
    private String getActiveQueryText() {
        DBCStatistics statistics = viewer.getModel().getStatistics();
        String queryText = statistics == null ? null : statistics.getQueryText();
        if (queryText == null || queryText.isEmpty()) {
            DBSDataContainer dataContainer = viewer.getDataContainer();
            if (dataContainer != null) {
                return dataContainer.getName();
            }
            queryText = "<empty>";
        }
        return queryText;
    }

    @Nullable
    private DBPImage getActiveObjectImage() {
        DBSDataContainer dataContainer = viewer.getDataContainer();
        if (dataContainer instanceof DBSEntity) {
            DBNDatabaseNode dcNode = viewer.getDataContainer().getDataSource().getContainer().getApplication().getNavigatorModel().findNode(dataContainer);
            if (dcNode != null) {
                return dcNode.getNodeIcon();
            }
        }
        if (dataContainer instanceof DBPImageProvider) {
            return ((DBPImageProvider) dataContainer).getObjectImage();
        } else if (dataContainer instanceof DBSEntity) {
            return DBIcon.TREE_TABLE;
        } else {
            return UIIcon.SQL_TEXT;
        }
    }

    @NotNull
    private String getActiveObjectDisplayString() {
        DBSDataContainer dataContainer = viewer.getDataContainer();
        if (dataContainer instanceof DBSEntity) {
            return dataContainer.getName();
        }
        String name = getActiveQueryText();
        name = name.replaceAll("\\s+", " ");
        return name;
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
        activeObjectPanel.redraw();
        refreshPanel.redraw();
        //activeObjectPanel.setToolTipText(getActiveQueryText());
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

    @NotNull
    private Control createObjectPanel(Shell popup) throws PartInitException {
        Composite panel = new Composite(popup, SWT.BORDER);
        GridLayout gl = new GridLayout(2, false);
//        gl.marginWidth = 0;
//        gl.marginHeight = 0;
//        gl.horizontalSpacing = 0;
        panel.setLayout(gl);

        Label iconLabel = new Label(panel, SWT.NONE);
        iconLabel.setImage(DBeaverIcons.getImage(getActiveObjectImage()));
        iconLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite editorPH = new Composite(panel, SWT.NONE);
        editorPH.setLayoutData(new GridData(GridData.FILL_BOTH));
        editorPH.setLayout(new FillLayout());

        final SQLEditorBase editor = new SQLEditorBase() {
            @Nullable
            @Override
            public DBCExecutionContext getExecutionContext() {
                return viewer.getExecutionContext();
            }

        };
        editor.setHasVerticalRuler(false);
        editor.init(new SubEditorSite(viewer.getSite()), new StringEditorInput("SQL", getActiveQueryText(), true, ContentUtils.DEFAULT_CHARSET));
        editor.createPartControl(editorPH);
        editor.reloadSyntaxRules();
        StyledText textWidget = editor.getTextViewer().getTextWidget();
        textWidget.setAlwaysShowScrollBars(false);

/*
        new Label(panel, SWT.SEPARATOR | SWT.VERTICAL).setLayoutData(new GridData(GridData.FILL_VERTICAL));
        Button button = new Button(panel, SWT.PUSH | SWT.FLAT);
        button.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        button.setText(">");
*/

        panel.setBackground(textWidget.getBackground());
        panel.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                editor.dispose();
            }
        });

        return textWidget;
    }

    private class FilterPanel extends Canvas {
        protected boolean hover = false;
        public FilterPanel(Composite parent, int style) {
            super(parent, style);

            addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e) {
                    paintPanel(e);
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

        protected void paintPanel(PaintEvent e) {

        }
    }

    private class ActiveObjectPanel extends FilterPanel {
        public static final int MIN_INFO_PANEL_WIDTH = 300;
        public static final int MIN_INFO_PANEL_HEIGHT = 100;
        public static final int MAX_INFO_PANEL_HEIGHT = 400;

        private String activeDisplayName;

        public ActiveObjectPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setLayoutData(new GridData(GridData.FILL_VERTICAL));

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    showObjectInfoPopup(e);
                }
            });
        }

        private void showObjectInfoPopup(MouseEvent e) {
            final Shell popup = new Shell(getShell(), SWT.NO_TRIM | SWT.ON_TOP | SWT.RESIZE);
            popup.setLayout(new FillLayout());
            Control editControl;
            try {
                editControl = createObjectPanel(popup);
            } catch (PartInitException e1) {
                UIUtils.showErrorDialog(getShell(), "Object info", "Error opening object info", e1);
                popup.dispose();
                return;
            }

            Point controlRect = editControl.computeSize(-1, -1);

            Rectangle parentRect = getDisplay().map(activeObjectPanel, null, getBounds());
            Rectangle displayRect = getMonitor().getClientArea();
            int width = Math.min(filterComposite.getSize().x, Math.max(MIN_INFO_PANEL_WIDTH, controlRect.x + 30));
            int height = Math.min(MAX_INFO_PANEL_HEIGHT, Math.max(MIN_INFO_PANEL_HEIGHT, controlRect.y + 30));
            int x = parentRect.x + e.x + 1;
            int y = parentRect.y + e.y + 1;
            if (y + height > displayRect.y + displayRect.height) {
                y = parentRect.y - height;
            }
            popup.setBounds(x, y, width, height);
            popup.setVisible(true);
            editControl.setFocus();

            editControl.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    popup.dispose();
                }
            });
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            int maxWidth = viewer.getControl().getParent().getSize().x / 3;
            activeDisplayName = CommonUtils.notEmpty(CommonUtils.truncateString(getActiveObjectDisplayString(), 200));
            //TextUtils.getShortText(sizingGC, getActiveObjectDisplayString(), maxWidth);
            Point textSize = sizingGC.textExtent(activeDisplayName);
            Image image = DBeaverIcons.getImage(getActiveObjectImage());
            if (image != null) {
                textSize.x += image.getBounds().width + 4;
            }
            return new Point(
                Math.max(MIN_FILTER_TEXT_WIDTH, Math.min(textSize.x + 10, maxWidth)),
                Math.min(textSize.y + 4, MIN_FILTER_TEXT_HEIGHT));
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
            Image icon = DBeaverIcons.getImage(getActiveObjectImage());
            if (icon != null) {
                e.gc.drawImage(icon, 2, 3);
                textOffset += icon.getBounds().width + 2;
            }
            e.gc.drawText(activeDisplayName, textOffset, 3);
            e.gc.setClipping((Rectangle) null);
        }
    }

    private class RefreshPanel extends FilterPanel {

        private final Image enabledImage, disabledImage;

        public RefreshPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setToolTipText(CoreMessages.controls_resultset_viewer_action_refresh);
            enabledImage = DBeaverIcons.getImage(UIIcon.RS_REFRESH);
            disabledImage = new Image(enabledImage.getDevice(), enabledImage, SWT.IMAGE_GRAY);
            addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    UIUtils.dispose(disabledImage);
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    if (!viewer.isRefreshInProgress() && e.x > 8) {
                        viewer.refresh();
                        redraw();
                    }
                }
            });

            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.heightHint = MIN_FILTER_TEXT_HEIGHT;
            gd.widthHint = 10 + enabledImage.getBounds().width + 6;
            setLayoutData(gd);
        }

        @Override
        protected void paintPanel(PaintEvent e) {
            e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            e.gc.drawLine(
                e.x + 4, e.y + 2,
                e.x + 4, e.y + e.height - 4);
            if (viewer.isRefreshInProgress()) {
                e.gc.drawImage(DBeaverIcons.getImage(UIIcon.CLOSE), e.x + 10, e.y + 2);
            } else if (hover) {
                e.gc.drawImage(enabledImage, e.x + 10, e.y + 2);
            } else {
                e.gc.drawImage(disabledImage, e.x + 10, e.y + 2);
            }
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
