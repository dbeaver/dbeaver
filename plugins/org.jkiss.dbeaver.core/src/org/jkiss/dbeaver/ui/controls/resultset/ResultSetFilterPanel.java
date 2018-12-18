/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.SystemJob;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.StyledTextContentAdapter;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ResultSetFilterPanel
 */
class ResultSetFilterPanel extends Composite implements IContentProposalProvider, IAdaptable
{
    private static final Log log = Log.getLog(ResultSetFilterPanel.class);

    private static final int MIN_FILTER_TEXT_WIDTH = 50;
    private static final int MIN_FILTER_TEXT_HEIGHT = 20;
    private static final int MAX_HISTORY_PANEL_HEIGHT = 200;

    private static final String DEFAULT_QUERY_TEXT = "SQL";

    private final ResultSetViewer viewer;
    private final ActiveObjectPanel activeObjectPanel;
    private final EditFilterPanel editFilterPanel;
    private final RefreshPanel refreshPanel;
    private final HistoryPanel historyPanel;

    private final TextViewer filtersTextViewer;
    private final StyledText filtersText;
    private final ContentProposalAdapter filtersProposalAdapter;

    private final ToolBar filterToolbar;
    private final ToolItem filtersApplyButton;
    private final ToolItem filtersClearButton;
    private final ToolItem filtersSaveButton;
    private final ToolItem historyBackButton;
    private final ToolItem historyForwardButton;

    private final Composite filterComposite;

    private final Color hoverBgColor;
    private final Color shadowColor;
    private final GC sizingGC;
    private final Font hintFont;

    private String activeDisplayName = DEFAULT_QUERY_TEXT;

    private String prevQuery = null;
    private final List<String> filtersHistory = new ArrayList<>();
    private Menu historyMenu;
    private boolean filterExpanded = false;

    ResultSetFilterPanel(ResultSetViewer rsv) {
        super(rsv.getControl(), SWT.NONE);
        this.viewer = rsv;

        this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.sizingGC = new GC(this);

        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 3;
        gl.marginWidth = 3;
        this.setLayout(gl);

        this.hoverBgColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
        this.shadowColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
        this.hintFont = UIUtils.modifyFont(getFont(), SWT.ITALIC);

        {
            this.filterComposite = new Composite(this, SWT.BORDER);
            gl = new GridLayout(5, false);
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            gl.horizontalSpacing = 0;
            gl.verticalSpacing = 0;
            this.filterComposite.setLayout(gl);
            this.filterComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            this.activeObjectPanel = new ActiveObjectPanel(filterComposite);

            this.filtersTextViewer = new TextViewer(filterComposite, SWT.MULTI);
            this.filtersTextViewer.setDocument(new Document());
            TextViewerUndoManager undoManager = new TextViewerUndoManager(200);
            undoManager.connect(filtersTextViewer);
            this.filtersTextViewer.setUndoManager(undoManager);
            this.filtersText = this.filtersTextViewer.getTextWidget();

            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.verticalIndent = 1;
            this.filtersText.setLayoutData(gd);
            StyledTextUtils.fillDefaultStyledTextContextMenu(filtersText);
            StyledTextUtils.enableDND(this.filtersText);

            this.editFilterPanel = new EditFilterPanel(filterComposite);
            this.historyPanel = new HistoryPanel(filterComposite);
            this.refreshPanel = new RefreshPanel(filterComposite);

            // Register filters text in focus service
            UIUtils.addDefaultEditActionsSupport(viewer.getSite(), this.filtersText);

            this.filtersText.addPaintListener(e -> {
                /*if (viewer.getModel().hasData())*/ {
                    final boolean supportsDataFilter = viewer.supportsDataFilter();
                    if (!supportsDataFilter || (filtersText.isEnabled() && filtersText.getCharCount() == 0 && !filtersText.isFocusControl())) {
                        e.gc.setForeground(shadowColor);
                        e.gc.setFont(hintFont);
                        e.gc.drawText(supportsDataFilter ?
                                CoreMessages.sql_editor_resultset_filter_panel_text_enter_sql_to_filter:
                                CoreMessages.sql_editor_resultset_filter_panel_text_enter_filter_not_support,
                            2, 0, true);
                        e.gc.setFont(null);
                    }
                }
            });
            this.filtersText.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    filtersText.redraw();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    filtersText.redraw();
                }
            });
            this.filtersText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    String filterText = filtersText.getText();
                    filtersApplyButton.setEnabled(true);
                    filtersClearButton.setEnabled(!CommonUtils.isEmpty(filterText));
                }
            });
            this.filtersText.addTraverseListener(e -> {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    if (filterExpanded) {
                        e.doit = true;
                        return;
                    }
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            });

            this.filtersText.addVerifyKeyListener(e -> {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.LF || e.character == SWT.CR) {
                    if (filterExpanded && (e.stateMask & SWT.CTRL) == 0) {
                        return;
                    }
                    // Suppress Enter handling if filter is not expanded
                    e.doit = false;
                }
            });
            this.filtersText.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.keyCode == SWT.ARROW_DOWN) {
                        if (filterExpanded && (e.stateMask & SWT.CTRL) ==0) {
                            return;
                        }
                        historyPanel.showFilterHistoryPopup();
                    } else if (e.keyCode == SWT.CR || e.keyCode == SWT.LF || e.character == SWT.CR) {
                        if (filtersProposalAdapter != null && filtersProposalAdapter.isProposalPopupOpen()) {
                            return;
                        }
                        if (filterExpanded && (e.stateMask & SWT.CTRL) == 0) {
                            return;
                        }
                        e.doit = false;
                        setCustomDataFilter();
                    }
                }
            });

            StyledTextContentAdapter contentAdapter = new StyledTextContentAdapter(filtersText) {
                @Override
                public void setControlContents(Control control, String text, int cursorPosition) {
                    // We need to set selection in the beginning of current word
                    Point selection = filtersText.getSelection();
                    String curText = filtersText.getText();
                    int insertPosition = selection.x;
                    for (int i = selection.x - 1; i >= 0; i--) {
                        if (Character.isUnicodeIdentifierPart(curText.charAt(i))) {
                            insertPosition = i;
                        } else {
                            break;
                        }
                    }
                    filtersText.setSelection(insertPosition, selection.y);
                    insertControlContents(control, text, cursorPosition);
                }
            };
            filtersProposalAdapter = UIUtils.installContentProposal(
                filtersText, contentAdapter, this, false, false);
        }

        // Handle all shortcuts by filters editor, not by host editor
        TextEditorUtils.enableHostEditorKeyBindingsSupport(viewer.getSite(), this.filtersText);

        {
            filterToolbar = new ToolBar(this, SWT.HORIZONTAL | SWT.RIGHT);
            filterToolbar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));

            filtersApplyButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
            filtersApplyButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER_APPLY));
            //filtersApplyButton.setText("Apply");
            filtersApplyButton.setToolTipText(CoreMessages.sql_editor_resultset_filter_panel_btn_apply);
            filtersApplyButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setCustomDataFilter();
                }
            });
            filtersApplyButton.setEnabled(false);

            filtersClearButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
            filtersClearButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER_RESET));
            filtersClearButton.setToolTipText(CoreMessages.sql_editor_resultset_filter_panel_btn_remove);
            filtersClearButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    viewer.resetDataFilter(true);
                }
            });
            filtersClearButton.setEnabled(false);

            filtersSaveButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
            filtersSaveButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER_SAVE));
            filtersSaveButton.setToolTipText(CoreMessages.sql_editor_resultset_filter_panel_btn_save);
            filtersSaveButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    viewer.saveDataFilter();
                }
            });
            filtersSaveButton.setEnabled(false);

            ToolItem filtersCustomButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
            filtersCustomButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER));
            filtersCustomButton.setToolTipText(CoreMessages.sql_editor_resultset_filter_panel_btn_custom);
            filtersCustomButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    new FilterSettingsDialog(viewer).open();
                }
            });
            filtersCustomButton.setEnabled(true);

            UIUtils.createToolBarSeparator(filterToolbar, SWT.VERTICAL);

            rsv.getAutoRefresh().populateRefreshButton(filterToolbar);

            UIUtils.createToolBarSeparator(filterToolbar, SWT.VERTICAL);

            historyBackButton = new ToolItem(filterToolbar, SWT.DROP_DOWN | SWT.NO_FOCUS);
            historyBackButton.setImage(DBeaverIcons.getImage(UIIcon.RS_BACK));
            historyBackButton.setEnabled(false);
            historyBackButton.addSelectionListener(new HistoryMenuListener(historyBackButton, true));

            historyForwardButton = new ToolItem(filterToolbar, SWT.DROP_DOWN | SWT.NO_FOCUS);
            historyForwardButton.setImage(DBeaverIcons.getImage(UIIcon.RS_FORWARD));
            historyForwardButton.setEnabled(false);
            historyForwardButton.addSelectionListener(new HistoryMenuListener(historyForwardButton, false));
        }

        this.addControlListener(new ControlListener() {
            @Override
            public void controlMoved(ControlEvent e) {
                redrawPanels();
            }

            @Override
            public void controlResized(ControlEvent e) {
                redrawPanels();
            }
        });

        enablePanelControls(false);

        this.addDisposeListener(e -> {
            if (historyMenu != null) {
                historyMenu.dispose();
                historyMenu = null;
            }
            UIUtils.dispose(sizingGC);
            UIUtils.dispose(hintFont);
        });

    }

    void enableFilters(boolean enableFilters) {
        if (isDisposed()) {
            return;
        }
        enablePanelControls(enableFilters);
        if (enableFilters) {
            final boolean supportsDataFilter = viewer.supportsDataFilter();
            int historyPosition = viewer.getHistoryPosition();
            List<ResultSetViewer.HistoryStateItem> stateHistory = viewer.getStateHistory();

            String filterText = filtersText.getText();
            filtersText.setEnabled(supportsDataFilter);
            filtersApplyButton.setEnabled(supportsDataFilter);
            filtersClearButton.setEnabled(viewer.getModel().getDataFilter().hasFilters() || !CommonUtils.isEmpty(filterText));
            filtersSaveButton.setEnabled(viewer.getDataContainer() instanceof DBSEntity);
            // Update history buttons
            if (historyPosition > 0) {
                historyBackButton.setEnabled(true);
                historyBackButton.setToolTipText(
                        stateHistory.get(historyPosition - 1).describeState() +
                                " (" + ActionUtils.findCommandDescription(IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY, viewer.getSite(), true) + ")");
            } else {
                historyBackButton.setEnabled(false);
            }
            if (historyPosition < stateHistory.size() - 1) {
                historyForwardButton.setEnabled(true);
                historyForwardButton.setToolTipText(
                        stateHistory.get(historyPosition + 1).describeState() +
                                " (" + ActionUtils.findCommandDescription(IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY, viewer.getSite(), true) + ")");
            } else {
                historyForwardButton.setEnabled(false);
            }
        }
        filterComposite.setBackground(filtersText.getBackground());

        {
            String displayName = getActiveSourceQuery();
            if (prevQuery == null || !prevQuery.equals(displayName)) {
                loadFiltersHistory(displayName);
                prevQuery = displayName;
            }
            Pattern mlCommentsPattern = Pattern.compile("/\\*.*\\*/", Pattern.DOTALL);
            Matcher m = mlCommentsPattern.matcher(displayName);
            if (m.find()) {
                displayName = m.replaceAll("");
            }

            displayName = displayName.replaceAll("--.+", "");
            displayName = TextUtils.compactWhiteSpaces(displayName);
            activeDisplayName = CommonUtils.notEmpty(CommonUtils.truncateString(displayName, 200));
            if (CommonUtils.isEmpty(activeDisplayName)) {
                activeDisplayName = DEFAULT_QUERY_TEXT;
            }
        }

        filterComposite.layout();
        redrawPanels();
    }

    private void enablePanelControls(boolean enable) {
        setRedraw(false);
        try {
            filterToolbar.setEnabled(enable);
            this.editFilterPanel.setEnabled(enable);
            refreshPanel.setEnabled(enable);
            historyPanel.setEnabled(enable);
            filtersText.setEditable(enable && viewer.supportsDataFilter());
        } finally {
            setRedraw(true);
        }
    }

    private boolean isFiltersAvailable() {
        DBSDataContainer dataContainer = viewer.getDataContainer();
        return dataContainer != null && (dataContainer.getSupportedFeatures() & DBSDataContainer.DATA_FILTER) != 0;
    }

    private void redrawPanels() {
        if (activeObjectPanel != null && !activeObjectPanel.isDisposed()) {
            activeObjectPanel.redraw();
        }
        if (historyPanel != null && !historyPanel.isDisposed()) {
            historyPanel.redraw();
        }
        if (editFilterPanel != null && !editFilterPanel.isDisposed() ){
            editFilterPanel.redraw();
        }
        if (refreshPanel != null && !refreshPanel.isDisposed()) {
            refreshPanel.redraw();
        }
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
            queryText = DEFAULT_QUERY_TEXT;
        }
        return queryText;
    }

    @Nullable
    private DBPImage getActiveObjectImage() {
        DBSDataContainer dataContainer = viewer.getDataContainer();
        if (dataContainer instanceof DBSEntity) {
            DBPDataSource dataSource = viewer.getDataContainer().getDataSource();
            if (dataSource != null) {
                DBNDatabaseNode dcNode = dataSource.getContainer().getPlatform().getNavigatorModel().findNode(dataContainer);
                if (dcNode != null) {
                    return dcNode.getNodeIcon();
                }
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
    private String getActiveSourceQuery() {
        String displayName;
        DBSDataContainer dataContainer = viewer.getDataContainer();
        if (dataContainer != null) {
            displayName = dataContainer.getName();
        } else {
            displayName = getActiveQueryText();
        }
        return displayName;
    }

    private void loadFiltersHistory(String query) {
        filtersHistory.clear();
        try {
            final Collection<String> history = viewer.getFilterManager().getQueryFilterHistory(query);
            filtersHistory.addAll(history);
        } catch (Throwable e) {
            log.debug("Error reading history", e);
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
        DBDDataFilter newFilter = new DBDDataFilter(viewer.getModel().getDataFilter());
        for (DBDAttributeConstraint ac : newFilter.getConstraints()) {
            ac.setCriteria(null);
        }
        newFilter.setWhere(condition);
        viewer.setDataFilter(newFilter, true);
        //viewer.getControl().setFocus();
    }

    void addFiltersHistory(String whereCondition)
    {
        final boolean oldFilter = filtersHistory.remove(whereCondition);
        filtersHistory.add(whereCondition);
        if (!oldFilter) {
            try {
                viewer.getFilterManager().saveQueryFilterValue(getActiveSourceQuery(), whereCondition);
            } catch (Throwable e) {
                log.debug("Error saving filter", e);
            }
        }

        setFilterValue(whereCondition);
    }

    Control getEditControl() {
        return filtersText;
    }

    void setFilterValue(String whereCondition) {
        if (whereCondition != null && !filtersText.getText().trim().equals(whereCondition.trim())) {
            filtersText.setText(whereCondition);
        }
    }

    @NotNull
    private Control createObjectPanel(Shell popup) throws PartInitException {
        Composite panel = new Composite(popup, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
//        gl.marginWidth = 0;
        gl.marginHeight = 0;
//        gl.horizontalSpacing = 0;
        panel.setLayout(gl);

        Label iconLabel = new Label(panel, SWT.NONE);
        DBPImage activeObjectImage = getActiveObjectImage();
        if (activeObjectImage != null) {
            iconLabel.setImage(DBeaverIcons.getImage(activeObjectImage));
        }
        iconLabel.setToolTipText(CoreMessages.sql_editor_resultset_filter_panel_label);
        iconLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        iconLabel.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        iconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                openEditorForActiveQuery();
            }
        });
        Composite editorPH = new Composite(panel, SWT.NONE);
        editorPH.setLayoutData(new GridData(GridData.FILL_BOTH));
        editorPH.setLayout(new FillLayout());

        try {
            UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
            if (serviceSQL != null) {
                Object sqlPanel = serviceSQL.createSQLPanel(viewer.getSite(), editorPH, viewer, DEFAULT_QUERY_TEXT, getActiveQueryText());
                if (sqlPanel instanceof TextViewer) {
                    StyledText textWidget = ((TextViewer) sqlPanel).getTextWidget();
                    //textWidget.setAlwaysShowScrollBars(false);

                    panel.setBackground(textWidget.getBackground());

                    return textWidget;
                }
            }

            return null;
        } catch (DBException e) {
            throw new PartInitException("Error creating SQL panel", e);
        }
    }

    private void openEditorForActiveQuery() {
        DBSDataContainer dataContainer = viewer.getDataContainer();
        String editorName;
        if (dataContainer instanceof DBSEntity) {
            editorName = dataContainer.getName();
        } else {
            editorName = "Query";
        }
        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        if (serviceSQL != null) {
            serviceSQL.openSQLConsole(
                dataContainer == null || dataContainer.getDataSource() == null ? null : dataContainer.getDataSource().getContainer(),
                editorName,
                getActiveQueryText()
            );
        }
    }

    @Override
    public IContentProposal[] getProposals(String contents, int position) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        if (viewer.getDataContainer() != null) {
            syntaxManager.init(viewer.getDataContainer().getDataSource());
        }
        SQLWordPartDetector wordDetector = new SQLWordPartDetector(new Document(contents), syntaxManager, position);
        final String word = wordDetector.getFullWord().toLowerCase(Locale.ENGLISH);
        final List<IContentProposal> proposals = new ArrayList<>();

        final DBRRunnableWithProgress reader = monitor -> {
            DBDAttributeBinding[] attributes = viewer.getModel().getAttributes();
            for (DBDAttributeBinding attribute : attributes) {
                final String name = DBUtils.getUnQuotedIdentifier(attribute.getDataSource(), attribute.getName());
                if (CommonUtils.isEmpty(word) || name.toLowerCase(Locale.ENGLISH).startsWith(word)) {
                    final String content = DBUtils.getQuotedIdentifier(attribute) + " ";
                    proposals.add(
                            new ContentProposal(
                                    content,
                                    attribute.getName(),
                                    DBInfoUtils.makeObjectDescription(monitor, attribute.getAttribute(), false),
                                    content.length()));
                }
            }
        };
        SystemJob searchJob = new SystemJob("Extract attribute proposals", reader);
        searchJob.schedule();
        UIUtils.waitJobCompletion(searchJob);

        return proposals.toArray(new IContentProposal[0]);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IUndoManager.class) {
            return adapter.cast(filtersTextViewer.getUndoManager());
        }
        return null;
    }

    private class FilterPanel extends Canvas {
        protected boolean hover = false;
        FilterPanel(Composite parent, int style) {
            super(parent, style);

            GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_VERTICAL);
            setLayoutData(gd);
            addPaintListener(this::paintPanel);
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
        static final int MIN_INFO_PANEL_WIDTH = 300;
        static final int MIN_INFO_PANEL_HEIGHT = 100;
        static final int MAX_INFO_PANEL_HEIGHT = 400;
        private Shell popup;

        ActiveObjectPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setToolTipText(CoreMessages.sql_editor_resultset_filter_panel_btn_open_console);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {
                    openEditorForActiveQuery();
                }

                @Override
                public void mouseDown(final MouseEvent e) {
                    UIUtils.asyncExec(() -> showObjectInfoPopup(e));
                }
            });
        }

        private void showObjectInfoPopup(MouseEvent e) {
            if (popup != null) {
                popup.dispose();
            }

            if ((e.stateMask & SWT.CTRL) != 0) {
                openEditorForActiveQuery();
                return;
            }

            popup = new Shell(getShell(), SWT.ON_TOP | SWT.RESIZE);
            popup.setLayout(new FillLayout());
            Control editControl;
            try {
                editControl = createObjectPanel(popup);
            } catch (PartInitException e1) {
                DBUserInterface.getInstance().showError("Object info", "Error opening object info", e1);
                popup.dispose();
                return;
            }

            if (editControl != null) {
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
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            int maxWidth = 0;
            for (Control control = viewer.getControl().getParent(); maxWidth == 0 && control != null; control = control.getParent()) {
                maxWidth = control.getSize().x / 4;
            }
            Point textSize = sizingGC.textExtent(activeDisplayName);
            DBPImage activeObjectImage = getActiveObjectImage();
            if (activeObjectImage != null) {
                Image image = DBeaverIcons.getImage(activeObjectImage);
                textSize.x += image.getBounds().width + 4;
            }
            return new Point(
                Math.max(MIN_FILTER_TEXT_WIDTH, Math.min(textSize.x + 10, maxWidth)),
                Math.min(textSize.y + 6, MIN_FILTER_TEXT_HEIGHT));
        }

        @Override
        protected void paintPanel(PaintEvent e) {
            Point hintSize = computeSize(SWT.DEFAULT, SWT.DEFAULT);
            int panelHeight = hintSize.y;
            e.gc.setForeground(shadowColor);
            if (hover) {
                e.gc.setBackground(hoverBgColor);
                e.gc.fillRectangle(e.x, e.y, e.width - 3, panelHeight);
                e.gc.drawLine(
                    e.x + e.width - 4, e.y,
                    e.x + e.width - 4, e.y + e.height);
            } else {
                e.gc.drawLine(
                    e.x + e.width - 4, e.y + 2,
                    e.x + e.width - 4, e.y + e.height - 4);
            }

            e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_DARK_GREEN));
            e.gc.setClipping(e.x, e.y, e.width - 8, e.height);

            int textOffset = 2;
            DBPImage activeObjectImage = getActiveObjectImage();
            if (activeObjectImage != null) {
                Image icon = DBeaverIcons.getImage(activeObjectImage);
                Rectangle iconBounds = icon.getBounds();
                e.gc.drawImage(icon, 2, (panelHeight - iconBounds.height) / 2);
                textOffset += iconBounds.width + 2;
            }
            int textHeight = e.gc.getFontMetrics().getHeight();
            e.gc.drawText(activeDisplayName, textOffset, (panelHeight - textHeight) / 2);
            e.gc.setClipping((Rectangle) null);
        }
    }

    private class HistoryPanel extends FilterPanel {

        private final Image dropImageE, dropImageD;
        private TableItem hoverItem;
        private Shell popup;

        HistoryPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setToolTipText("Filters history");
            dropImageE = DBeaverIcons.getImage(UIIcon.DROP_DOWN);
            dropImageD = new Image(dropImageE.getDevice(), dropImageE, SWT.IMAGE_GRAY);

            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_VERTICAL);
            gd.heightHint = MIN_FILTER_TEXT_HEIGHT;
            gd.widthHint = dropImageE.getBounds().width + 2;
            setLayoutData(gd);

            addDisposeListener(e -> UIUtils.dispose(dropImageD));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    showFilterHistoryPopup();
                }
            });
        }

        @Override
        protected void paintPanel(PaintEvent e) {
            e.gc.setForeground(shadowColor);
            e.gc.drawLine(
                    e.x + 2, e.y + 2,
                    e.x + 2, e.y + e.height - 4);
            if (hover) {
                e.gc.drawImage(dropImageE, e.x + 4, e.y + 2);
            } else {
                e.gc.drawImage(dropImageD, e.x + 4, e.y + 2);
            }
        }

        private void showFilterHistoryPopup() {
            if (popup != null) {
                popup.dispose();
            }
            popup = new Shell(getShell(), SWT.NO_TRIM | SWT.ON_TOP | SWT.RESIZE);
            popup.setLayout(new FillLayout());
            Table editControl = createFilterHistoryPanel(popup);

            Point parentRect = getDisplay().map(filtersText, null, new Point(0, 0));
            Rectangle displayRect = getMonitor().getClientArea();
            final Point filterTextSize = filtersText.getSize();
            int width = filterTextSize.x + historyPanel.getSize().x + editFilterPanel.getSize().x + refreshPanel.getSize().x;
            int height = Math.min(MAX_HISTORY_PANEL_HEIGHT, editControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
            int x = parentRect.x;
            int y = parentRect.y + getSize().y;
            if (y + height > displayRect.y + displayRect.height) {
                y = parentRect.y - height;
            }
            popup.setBounds(x, y, width, height);
            int tableWidth = editControl.getSize().x - editControl.getBorderWidth() * 2;
            final ScrollBar vsb = editControl.getVerticalBar();
            if (vsb != null) {
                tableWidth -= vsb.getSize().x;
            }
            editControl.getColumn(0).setWidth(tableWidth);
            popup.setVisible(true);
            editControl.setFocus();

            editControl.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    popup.dispose();
                }
            });
        }

        @NotNull
        private Table createFilterHistoryPanel(final Shell popup) {
            final Table historyTable = new Table(popup, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
            new TableColumn(historyTable, SWT.NONE);

            if (filtersHistory.isEmpty()) {
                // nothing
            } else {
                String curFilterValue = filtersText.getText();
                for (int i = filtersHistory.size(); i > 0; i--) {
                    String hi = filtersHistory.get(i - 1);
                    if (!CommonUtils.equalObjects(hi, curFilterValue)) {
                        new TableItem(historyTable, SWT.NONE).setText(hi);
                    }
                }
                //historyTable.deselectAll();
                if (historyTable.getItemCount() > 0) {
                    historyTable.setSelection(0);
                }
            }

            historyTable.addMouseTrackListener(new MouseTrackAdapter() {
                @Override
                public void mouseHover(MouseEvent e) {
                    //hoverItem = historyTable.getItem(new Point(e.x, e.y));
                }

                @Override
                public void mouseExit(MouseEvent e) {
                    hoverItem = null;
                }
            });
            historyTable.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    TableItem item = hoverItem;
                    if (item == null) {
                        final int selectionIndex = historyTable.getSelectionIndex();
                        if (selectionIndex != -1) {
                            item = historyTable.getItem(selectionIndex);
                        }
                    }
                    if (item != null && !item.isDisposed()) {
                        switch (e.keyCode) {
                            case SWT.DEL:
                                final String filterValue = item.getText();
                                try {
                                    viewer.getFilterManager().deleteQueryFilterValue(getActiveSourceQuery(), filterValue);
                                } catch (DBException e1) {
                                    log.warn("Error deleting filter value [" + filterValue + "]", e1);
                                }
                                filtersHistory.remove(filterValue);
                                item.dispose();
                                hoverItem = null;
                                break;
                            case SWT.CR:
                            case SWT.SPACE:
                                final String newFilter = item.getText();
                                popup.dispose();
                                setFilterValue(newFilter);
                                setCustomDataFilter();
                                break;
                            case SWT.ARROW_UP:
                                if (historyTable.getSelectionIndex() <= 0) {
                                    popup.dispose();
                                }
                                break;
                        }
                    }
                }
            });
            historyTable.addMouseMoveListener(e -> hoverItem = historyTable.getItem(new Point(e.x, e.y)));
            historyTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    if (hoverItem != null) {
                        final String newFilter = hoverItem.getText();
                        popup.dispose();
                        setFilterValue(newFilter);
                        setCustomDataFilter();
                    }
                }
            });

            return historyTable;
        }

    }

    private class EditFilterPanel extends FilterPanel {

        private final Image enabledImageExpand, disabledImageExpand;
        private final Image enabledImageCollapse, disabledImageCollapse;

        EditFilterPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setToolTipText("Expand filter panel");
            enabledImageExpand = DBeaverIcons.getImage(UIIcon.FIT_WINDOW);
            disabledImageExpand = new Image(enabledImageExpand.getDevice(), enabledImageExpand, SWT.IMAGE_GRAY);
            enabledImageCollapse = DBeaverIcons.getImage(UIIcon.ORIGINAL_SIZE);
            disabledImageCollapse = new Image(enabledImageCollapse.getDevice(), enabledImageCollapse, SWT.IMAGE_GRAY);
            addDisposeListener(e -> {
                UIUtils.dispose(disabledImageExpand);
                UIUtils.dispose(disabledImageCollapse);
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    toggleFilterPanel();
                }
            });

            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_VERTICAL);
            gd.heightHint = MIN_FILTER_TEXT_HEIGHT;
            gd.widthHint = enabledImageExpand.getBounds().width + 4;
            setLayoutData(gd);
        }

        @Override
        protected void paintPanel(PaintEvent e) {
            e.gc.setForeground(shadowColor);
            if (hover) {
                e.gc.drawImage(filterExpanded ? enabledImageCollapse : enabledImageExpand, e.x, e.y + 2);
            } else {
                e.gc.drawImage(filterExpanded ? disabledImageCollapse : disabledImageExpand, e.x, e.y + 2);
            }
        }
    }

    private void toggleFilterPanel() {
        filterExpanded = !filterExpanded;

        GridData gd = (GridData) filtersText.getLayoutData();
        gd.heightHint = filtersText.getLineHeight() * (filterExpanded ? 5 : 1);

        this.getParent().layout(true);
    }

    private class RefreshPanel extends FilterPanel {

        private final Image enabledImage, disabledImage;

        RefreshPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setToolTipText(CoreMessages.controls_resultset_viewer_action_refresh);
            enabledImage = DBeaverIcons.getImage(UIIcon.RS_REFRESH);
            disabledImage = new Image(enabledImage.getDevice(), enabledImage, SWT.IMAGE_GRAY);
            addDisposeListener(e -> UIUtils.dispose(disabledImage));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    if (!viewer.isRefreshInProgress() && e.x > 8) {
                        viewer.refreshData(null);
                        redraw();
                    }
                }
            });

            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_VERTICAL);
            gd.heightHint = MIN_FILTER_TEXT_HEIGHT;
            gd.widthHint = 10 + enabledImage.getBounds().width + 6;
            setLayoutData(gd);
        }

        @Override
        protected void paintPanel(PaintEvent e) {
            e.gc.setForeground(shadowColor);
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
        HistoryMenuListener(ToolItem item, boolean back) {
            this.dropdown = item;
            this.back = back;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            int historyPosition = viewer.getHistoryPosition();
            List<ResultSetViewer.HistoryStateItem> stateHistory = viewer.getStateHistory();
            if (e.detail == SWT.ARROW) {
                ToolItem item = (ToolItem) e.widget;
                Rectangle rect = item.getBounds();
                Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));

                if (historyMenu != null) {
                    historyMenu.dispose();
                }
                historyMenu = new Menu(dropdown.getParent().getShell());
                for (int i = historyPosition + (back ? -1 : 1); i >= 0 && i < stateHistory.size(); i += back ? -1 : 1) {
                    MenuItem mi = new MenuItem(historyMenu, SWT.NONE);
                    ResultSetViewer.HistoryStateItem state = stateHistory.get(i);
                    mi.setText(state.describeState());
                    final int statePosition = i;
                    mi.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            viewer.navigateHistory(statePosition);
                        }
                    });
                }
                historyMenu.setLocation(pt.x, pt.y + rect.height);
                historyMenu.setVisible(true);
            } else {
                int newPosition = back ? historyPosition - 1 : historyPosition + 1;
                viewer.navigateHistory(newPosition);
            }
        }
    }

}
