/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.action.MenuManager;
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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.SystemJob;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentProposalExt;
import org.jkiss.dbeaver.ui.controls.DoubleClickMouseAdapter;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.ui.controls.resultset.colors.ResetAllColorAction;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.SpreadsheetCommandHandler;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
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
class ResultSetFilterPanel extends Composite implements IContentProposalProvider, DBPAdaptable
{
    private static final Log log = Log.getLog(ResultSetFilterPanel.class);

    private static final int MIN_FILTER_TEXT_HEIGHT = 20;
    private static final int MAX_HISTORY_PANEL_HEIGHT = 200;

    private final ResultSetViewer viewer;
    private final boolean compactMode;

    private final ActiveObjectPanel activeObjectPanel;
    private final FilterExpandPanel filterExpandPanel;
    //private final RefreshPanel refreshPanel;
    private final HistoryPanel historyPanel;
    private final ExecutePanel executePanel;

    private final TextViewer filtersTextViewer;
    private final StyledText filtersText;
    private final ContentProposalAdapter filtersProposalAdapter;

    private ToolBar filterToolbar;
    private ToolItem filtersClearButton;
    private ToolItem historyBackButton;
    private ToolItem historyForwardButton;

    private final Composite filterComposite;

    private final Color shadowColor;

    private String activeDisplayName = ResultSetViewer.DEFAULT_QUERY_TEXT;

    private String prevQuery = null;
    private final List<String> filtersHistory = new ArrayList<>();
    private Menu historyMenu;
    private boolean filterExpanded = false;

    ResultSetFilterPanel(ResultSetViewer rsv, Composite parent, boolean compactMode) {
        super(parent, SWT.NONE);
        this.viewer = rsv;
        this.compactMode = compactMode;

        CSSUtils.setCSSClass(this, DBStyles.COLORED_BY_CONNECTION_TYPE);

        GridLayout gl = new GridLayout(compactMode ? 2 : 4, false);
        gl.marginHeight = 3;
        gl.marginWidth = 3;
        this.setLayout(gl);

        boolean isDark = UIStyles.isDarkTheme();
        this.shadowColor = getDisplay().getSystemColor(isDark ? SWT.COLOR_WIDGET_LIGHT_SHADOW : SWT.COLOR_WIDGET_NORMAL_SHADOW);

        {
            this.filterComposite = new Composite(this, SWT.BORDER);

            gl = new GridLayout(5, false);
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            gl.horizontalSpacing = 0;
            gl.verticalSpacing = 0;
            this.filterComposite.setLayout(gl);
            this.filterComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            CSSUtils.setCSSClass(this.filterComposite, DBStyles.COLORED_BY_CONNECTION_TYPE);

            if (!compactMode) {
                this.activeObjectPanel = new ActiveObjectPanel(filterComposite);
            } else {
                this.activeObjectPanel = null;
            }
            this.filterExpandPanel = new FilterExpandPanel(filterComposite);

            this.filtersTextViewer = new TextViewer(filterComposite, SWT.MULTI);
            this.filtersTextViewer.setDocument(new Document());
            this.filtersText = this.filtersTextViewer.getTextWidget();
            this.filtersText.setForeground(UIStyles.getDefaultTextForeground());
            this.filtersText.setFont(BaseThemeSettings.instance.baseFont);
            TextViewerUndoManager undoManager = new TextViewerUndoManager(200);
            undoManager.connect(filtersTextViewer);
            this.filtersTextViewer.setUndoManager(undoManager);

            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.verticalIndent = 1;
            this.filtersText.setLayoutData(gd);
            StyledTextUtils.fillDefaultStyledTextContextMenu(filtersText);
            StyledTextUtils.enableDND(this.filtersText);

            this.executePanel = new ExecutePanel(filterComposite);
            //this.refreshPanel = new RefreshPanel(filterComposite);
            this.historyPanel = new HistoryPanel(filterComposite);

            // Register filters text in focus service
            UIUtils.addDefaultEditActionsSupport(viewer.getSite(), this.filtersText);

            UIUtils.addEmptyTextHint(this.filtersText, styledText ->
                viewer.supportsDataFilter() ?
                    ResultSetMessages.sql_editor_resultset_filter_panel_text_enter_sql_to_filter:
                    ResultSetMessages.sql_editor_resultset_filter_panel_text_enter_filter_not_support
            );
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
                    executePanel.setEnabled(true);
                    executePanel.redraw();
//                    if (filtersClearButton != null) {
//                        filtersClearButton.setEnabled(!CommonUtils.isEmpty(filterText));
//                    }
                    filtersProposalAdapter.refresh();
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
                    } else if (e.keyCode == SWT.SPACE) {
                        // close the proposal window, if space is pressed.
                        filtersProposalAdapter.closeProposalPopup();
                    }
                }
            });

            ResultSetFilterContentAdapter contentAdapter = new ResultSetFilterContentAdapter(viewer);
            filtersProposalAdapter = ContentAssistUtils.installContentProposal(
                filtersText,
                contentAdapter,
                this);
        }

        // Handle all shortcuts by filters editor, not by host editor
        TextEditorUtils.enableHostEditorKeyBindingsSupport(viewer.getSite(), this.filtersText);

        if (!compactMode) {
            filterToolbar = new ToolBar(this, SWT.HORIZONTAL | SWT.RIGHT);
            filterToolbar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));

            filtersClearButton = new ToolItem(filterToolbar, SWT.NO_FOCUS | SWT.DROP_DOWN);
            filtersClearButton.setImage(DBeaverIcons.getImage(UIIcon.ERASE));
            filtersClearButton.setToolTipText(ActionUtils.findCommandDescription(ResultSetHandlerMain.CMD_FILTER_CLEAR_SETTING, viewer.getSite(), false));
            filtersClearButton.addSelectionListener(new EraseItemListener());
            filtersClearButton.setEnabled(false);

            ToolItem filtersCustomButton = new ToolItem(filterToolbar, SWT.NO_FOCUS | SWT.DROP_DOWN);
            filtersCustomButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER));
            filtersCustomButton.setToolTipText(ActionUtils.findCommandDescription(ResultSetHandlerMain.CMD_FILTER_EDIT_SETTINGS, viewer.getSite(), false));
            filtersCustomButton.setEnabled(true);
            filtersCustomButton.addSelectionListener(new CustomFilterListener());

            //UIUtils.createToolBarSeparator(filterToolbar, SWT.VERTICAL);

            historyBackButton = new ToolItem(filterToolbar, SWT.DROP_DOWN | SWT.NO_FOCUS);
            historyBackButton.setImage(DBeaverIcons.getImage(UIIcon.RS_BACK));
            historyBackButton.setEnabled(false);
            historyBackButton.addSelectionListener(new HistoryMenuListener(historyBackButton, true));

            historyForwardButton = new ToolItem(filterToolbar, SWT.DROP_DOWN | SWT.NO_FOCUS);
            historyForwardButton.setImage(DBeaverIcons.getImage(UIIcon.RS_FORWARD));
            historyForwardButton.setEnabled(false);
            historyForwardButton.addSelectionListener(new HistoryMenuListener(historyForwardButton, false));
        }

        CSSUtils.setMimicControl(this, filtersText);
        CSSUtils.setMimicControl(this.filterComposite, filtersText);
        if (filterExpandPanel != null) CSSUtils.setMimicControl(filterExpandPanel, filtersText);
        if (executePanel != null) CSSUtils.setMimicControl(executePanel, filtersText);
        if (historyPanel != null) CSSUtils.setMimicControl(historyPanel, filtersText);
        if (filterToolbar != null) CSSUtils.setMimicControl(filterToolbar, filtersText);

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
            executePanel.setEnabled(supportsDataFilter);
            if (filtersClearButton != null) {
                filtersClearButton.setEnabled(true);
//                    viewer.getModel().getDataFilter().hasFilters() ||
//                    viewer.getModel().getDataFilter().hasOrdering() ||
//                    !CommonUtils.isEmpty(filterText));
            }
            // Update history buttons
            if (historyBackButton != null) {
                if (historyPosition > 0) {
                    historyBackButton.setEnabled(true);
                    historyBackButton.setToolTipText(
                        stateHistory.get(historyPosition - 1).describeState() +
                            " (" + ActionUtils.findCommandDescription(IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY, viewer.getSite(), true) + ")");
                } else {
                    historyBackButton.setEnabled(false);
                }
            }
            if (historyForwardButton != null) {
                if (historyPosition < stateHistory.size() - 1) {
                    historyForwardButton.setEnabled(true);
                    historyForwardButton.setToolTipText(
                        stateHistory.get(historyPosition + 1).describeState() +
                            " (" + ActionUtils.findCommandDescription(IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY, viewer.getSite(), true) + ")");
                } else {
                    historyForwardButton.setEnabled(false);
                }
            }
        }
        filterComposite.setBackground(filtersText.getBackground());

        {
            String displayName = getActiveSourceQueryNormalized();
            if (prevQuery == null || !prevQuery.equals(displayName)) {
                prevQuery = displayName;
            }

            activeDisplayName = CommonUtils.notEmpty(CommonUtils.truncateString(displayName, 200));
            if (CommonUtils.isEmpty(activeDisplayName)) {
                activeDisplayName = ResultSetViewer.DEFAULT_QUERY_TEXT;
            }

            if (enableFilters && !CommonUtils.equalObjects(prevQuery, displayName)) {
                filtersHistory.clear();
            }
        }

        filterComposite.layout();
        redrawPanels();
    }

    private void enablePanelControls(boolean enable) {
        setRedraw(false);
        try {
            if (filterToolbar != null) {
                filterToolbar.setEnabled(enable);
            }
            this.filterExpandPanel.setEnabled(enable);
            historyPanel.setEnabled(enable);
            filtersText.setEditable(viewer.supportsDataFilter());
            executePanel.setEnabled(enable);
        } finally {
            setRedraw(true);
        }
    }

    private boolean isFiltersAvailable() {
        DBSDataContainer dataContainer = viewer.getDataContainer();
        return dataContainer != null && dataContainer.isFeatureSupported(DBSDataContainer.FEATURE_DATA_FILTER);
    }

    private void redrawPanels() {
        if (activeObjectPanel != null && !activeObjectPanel.isDisposed()) {
            activeObjectPanel.redraw();
        }
        if (historyPanel != null && !historyPanel.isDisposed()) {
            historyPanel.redraw();
        }
        if (filterExpandPanel != null && !filterExpandPanel.isDisposed() ){
            filterExpandPanel.redraw();
        }
        if (executePanel != null && !executePanel.isDisposed()) {
            executePanel.redraw();
        }
        if (filterToolbar != null && !filterToolbar.isDisposed()) {
            filterToolbar.redraw();
        }
    }

    public String getFilterText() {
        return filtersText.getText();
    }

    @Nullable
    private DBPImage getActiveObjectImage() {
        DBSDataContainer dataContainer = viewer.getDataContainer();
        if (dataContainer instanceof DBSEntity) {
            DBPDataSource dataSource = viewer.getDataContainer().getDataSource();
            if (dataSource != null) {
                DBNDatabaseNode dcNode = dataContainer.getDataSource().getContainer().getProject()
                    .getNavigatorModel().findNode(dataContainer);
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
            displayName = viewer.getActiveQueryText();
        }
        return displayName;
    }

    @NotNull
    private String getActiveSourceQueryNormalized() {
        String displayName = getActiveSourceQuery();
        Pattern mlCommentsPattern = Pattern.compile("/\\*.*\\*/", Pattern.DOTALL);
        Matcher m = mlCommentsPattern.matcher(displayName);
        if (m.find()) {
            displayName = m.replaceAll("");
        }

        displayName = displayName.replaceAll("--.+", "");
        displayName = CommonUtils.compactWhiteSpaces(displayName);

        return displayName;
    }

    private void loadFiltersHistory(String query) {
        filtersHistory.clear();
        try {
            if (ResultSetViewer.DEFAULT_QUERY_TEXT.equals(query)) {
                return;
            }
            DBCExecutionContext context = viewer.getExecutionContext();
            if (context == null) {
                return;
            }
            final Collection<String> history = viewer.getFilterManager().getQueryFilterHistory(context, query);
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
        try {
            SQLUtils.appendConditionString(
                viewer.getModel().getDataFilter(),
                context.getDataSource(),
                null,
                currentCondition,
                true
            );
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Can't set filter", "Cannot set custom filter", e);
            return;
        }
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
                DBCExecutionContext context = viewer.getExecutionContext();
                if (context != null) {
                    viewer.getFilterManager().saveQueryFilterValue(context, getActiveSourceQueryNormalized(), whereCondition);
                }
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
        iconLabel.setToolTipText(ResultSetMessages.sql_editor_resultset_filter_panel_label);
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
                Object sqlPanel = serviceSQL.createSQLPanel(viewer.getSite(), editorPH, viewer, ResultSetViewer.DEFAULT_QUERY_TEXT, false, viewer.getActiveQueryText());
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
                null, // This is workaround to open new SQL Editor with this dataContainer schema/catalog, not default
                dataContainer,
                editorName,
                viewer.getActiveQueryText());
        }
    }

    @Override
    public IContentProposal[] getProposals(String contents, int position) {
    	if(!viewer.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL)) {
    		return null;
    	}
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        DBPDataSource dataSource = viewer.getDataSource();
        if (dataSource != null) {
            syntaxManager.init(dataSource.getSQLDialect(), dataSource.getContainer().getPreferenceStore());
        }
        SQLWordPartDetector wordDetector = new SQLWordPartDetector(new Document(contents), syntaxManager, position);
        final List<IContentProposal> proposals = new ArrayList<>();

        String attrName = wordDetector.getFullWord().toLowerCase(Locale.ENGLISH);

        final DBRRunnableWithProgress reader = monitor -> {
            DBDAttributeBinding[] attributes = viewer.getModel().getAttributes();
            for (DBDAttributeBinding attribute : attributes) {
                if (attribute.isCustom()) {
                    continue;
                }
                final String name = DBUtils.getUnQuotedIdentifier(attribute.getDataSource(), attribute.getName());
                if (CommonUtils.isEmpty(attrName) || name.toLowerCase(Locale.ENGLISH).startsWith(attrName)) {
                    final String content = DBUtils.getQuotedIdentifier(attribute) + " ";
                    proposals.add(
                        new ContentProposalExt(
                            content,
                            attribute.getName(),
                            DBInfoUtils.makeObjectDescription(monitor, attribute.getAttribute(), false),
                            content.length(),
                            DBValueFormatting.getObjectImage(attribute)));
                }
            }
        };
        SystemJob searchJob = new SystemJob("Extract attribute proposals", reader);
        searchJob.schedule();
        UIUtils.waitJobCompletion(searchJob);

        String[] filterKeywords = {
            SQLConstants.KEYWORD_AND,
            SQLConstants.KEYWORD_OR,
            SQLConstants.KEYWORD_IS,
            SQLConstants.KEYWORD_NOT,
            SQLConstants.KEYWORD_NULL,
            SQLConstants.KEYWORD_BETWEEN,
            SQLConstants.KEYWORD_LIKE};

        for (String kw : filterKeywords) {
            if (attrName.isEmpty() || kw.startsWith(attrName.toUpperCase())) {
                if (dataSource != null) {
                    kw = dataSource.getSQLDialect().storesUnquotedCase().transform(kw);
                }
                proposals.add(new ContentProposal(kw + " ", kw + ": SQL expression keyword"));
            }
        }

        return proposals.toArray(new IContentProposal[0]);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IUndoManager.class) {
            return adapter.cast(filtersTextViewer.getUndoManager());
        }
        return null;
    }

    private static class FilterPanel extends Canvas {
        protected boolean hover = false;
        FilterPanel(Composite parent, int style) {
            super(parent, style);

            GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_VERTICAL);
            setLayoutData(gd);
            addPaintListener(this::paintPanel);
            addMouseMoveListener(e -> {
                if (!hover) {
                    hover = true;
                    redraw();
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
        static final int MIN_INFO_PANEL_WIDTH = 300;
        static final int MIN_INFO_PANEL_HEIGHT = 100;
        static final int MAX_INFO_PANEL_HEIGHT = 400;
        private Shell popup;

        ActiveObjectPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setToolTipText(ResultSetMessages.sql_editor_resultset_filter_panel_btn_open_console);
            //setLayoutData(new GridData(GridData.FILL_BOTH));

            this.addMouseListener(new DoubleClickMouseAdapter() {
                @Override
                public void onMouseDoubleClick(@NotNull MouseEvent e) {
                    openEditorForActiveQuery();
                }

                @Override
                public void onMouseSingleClick(@NotNull MouseEvent e) {
                    UIUtils.asyncExec(() -> showObjectInfoPopup(e));
                }
            });
        }

        @Override
        public void dispose() {
            super.dispose();
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
                DBWorkbench.getPlatformUI().showError("Object info", "Error opening object info", e1);
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
            int maxWidth = 0;
            for (Control control = viewer.getControl().getParent(); maxWidth == 0 && control != null; control = control.getParent()) {
                maxWidth = control.getSize().x / 4;
            }
            GC sizingGC = new GC(this);
            sizingGC.setFont(BaseThemeSettings.instance.baseFont);
            Point textSize = sizingGC.textExtent(activeDisplayName);
            int maxFilterTextWidth = sizingGC.textExtent("X".repeat(24)).x;
            sizingGC.dispose();
            DBPImage activeObjectImage = getActiveObjectImage();
            if (activeObjectImage != null) {
                Image image = DBeaverIcons.getImage(activeObjectImage);
                textSize.x += image.getBounds().width + 4;
            }
            return new Point(
                Math.max(maxFilterTextWidth, Math.min(textSize.x + 10, maxWidth)),
                filterExpanded ? filtersText.getSize().y : Math.min(textSize.y + 6, MIN_FILTER_TEXT_HEIGHT));
        }

        @Override
        protected void paintPanel(PaintEvent e) {
            Point hintSize = computeSize(SWT.DEFAULT, SWT.DEFAULT);
            int panelHeight = hintSize.y;
            e.gc.setFont(BaseThemeSettings.instance.baseFont);
            e.gc.setForeground(shadowColor);
            if (hover) {
                Color background = e.gc.getBackground();
                boolean isDarkBG = UIUtils.isDark(background.getRGB());
                RGB blendRGB = isDarkBG ? new RGB(255, 255, 255) : new RGB(0, 0, 0);
                RGB hoverRgb = UIUtils.blend(background.getRGB(), blendRGB, 90);
                Color hoverBgColor = UIUtils.getSharedTextColors().getColor(hoverRgb);

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
                e.gc.drawImage(icon, 2, 2);
                textOffset += iconBounds.width + 2;
            }
            int textHeight = e.gc.getFontMetrics().getHeight();
            e.gc.drawText(activeDisplayName, textOffset, (e.height - textHeight) / 2);
            e.gc.setClipping((Rectangle) null);
        }
    }

    private class HistoryPanel extends FilterPanel {

        private final Image dropImageE, dropImageD;
        private TableItem hoverItem;
        private Shell popup;

        HistoryPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setToolTipText(ResultSetMessages.filter_panel_filters_history_text);
            dropImageE = DBeaverIcons.getImage(UIIcon.DROP_DOWN);
            dropImageD = new Image(dropImageE.getDevice(), dropImageE, SWT.IMAGE_GRAY);

            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_VERTICAL);
            gd.heightHint = MIN_FILTER_TEXT_HEIGHT;
            gd.widthHint = dropImageE.getBounds().width + 6;
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
            int width = filterTextSize.x + historyPanel.getSize().x + filterExpandPanel.getSize().x + executePanel.getSize().x;// + refreshPanel.getSize().x;
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
                loadFiltersHistory(activeDisplayName);
            }

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
                                    DBCExecutionContext context = viewer.getExecutionContext();
                                    if (context != null) {
                                        viewer.getFilterManager().deleteQueryFilterValue(
                                            context,
                                            getActiveSourceQueryNormalized(),
                                            filterValue
                                        );
                                    }
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

    private class FilterExpandPanel extends FilterPanel {

        private final Image enabledImageExpand, disabledImageExpand;
        private final Image enabledImageCollapse, disabledImageCollapse;

        FilterExpandPanel(Composite addressBar) {
            super(addressBar, SWT.NONE);
            setToolTipText(ResultSetMessages.filter_panel_expand_panel_text);
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
                    togglePanelExpand();
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

    private void togglePanelExpand() {
        filterExpanded = !filterExpanded;

        GridData gd = (GridData) filtersText.getLayoutData();
        gd.heightHint = filtersText.getLineHeight() * (filterExpanded ? 5 : 1);

        this.getParent().layout(true);
    }

    private abstract class ToolItemPanel extends FilterPanel {

        private final Image enabledImage, disabledImage;
        private final int style;

        protected ToolItemPanel(Composite addressBar, DBPImage image, String toolTip, int style) {
            super(addressBar, SWT.NONE);
            this.style = style;
            setToolTipText(toolTip);
            enabledImage = DBeaverIcons.getImage(image);
            disabledImage = new Image(enabledImage.getDevice(), enabledImage, SWT.IMAGE_GRAY);
            addDisposeListener(e -> UIUtils.dispose(disabledImage));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    if (executeAction(e)) {
                        redraw();
                    }
                }
            });

            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_VERTICAL);
            gd.heightHint = MIN_FILTER_TEXT_HEIGHT;
            gd.widthHint = 4 + enabledImage.getBounds().width;
            if ((this.style & SWT.LEFT) == SWT.LEFT) gd.widthHint += 4;
            if ((this.style & SWT.RIGHT) == SWT.RIGHT) gd.widthHint += 4;
            setLayoutData(gd);
        }

        @Override
        protected void paintPanel(PaintEvent e) {
            e.gc.setForeground(shadowColor);
            int x = e.x;
            if ((this.style & SWT.LEFT) == SWT.LEFT) {
                x += 4;
                e.gc.drawLine(x, e.y + 2, x, e.y + e.height - 4);
                x += 6;
            }
            if (viewer.isRefreshInProgress()) {
                e.gc.drawImage(DBeaverIcons.getImage(UIIcon.CLOSE), x, e.y + 2);
            } else if (isItemEnabled()) {
                e.gc.drawImage(enabledImage, x, e.y + 2);
            } else {
                e.gc.drawImage(disabledImage, x, e.y + 2);
            }
            if ((this.style & SWT.RIGHT) == SWT.RIGHT) {
                x += enabledImage.getBounds().width + 4;
                e.gc.drawLine(x, e.y + 2, x, e.y + e.height - 4);
            }
        }

        protected abstract boolean isItemEnabled();

        protected abstract boolean executeAction(MouseEvent e);

    }

    private class RefreshPanel extends ToolItemPanel {

        RefreshPanel(Composite addressBar) {
            super(addressBar, UIIcon.REFRESH, ResultSetMessages.controls_resultset_viewer_action_refresh, SWT.RIGHT);
        }

        @Override
        protected boolean isItemEnabled() {
            return !viewer.isRefreshInProgress();
        }

        @Override
        protected boolean executeAction(MouseEvent e) {
            if (isItemEnabled()) {
                viewer.refreshData(null);
                return true;
            }
            return false;
        }
    }

    private class ExecutePanel extends ToolItemPanel {

        ExecutePanel(Composite addressBar) {
            super(addressBar, UIIcon.SQL_EXECUTE, ResultSetMessages.sql_editor_resultset_filter_panel_btn_apply, SWT.NONE);
        }

        @Override
        protected boolean isItemEnabled() {
            if (viewer.isRefreshInProgress() && isEnabled()) {
                return false;
            }
            DBCExecutionContext context = viewer.getExecutionContext();
            if (context == null) {
                return false;
            }
            StringBuilder currentCondition = new StringBuilder();
            try {
                SQLUtils.appendConditionString(
                    viewer.getModel().getDataFilter(),
                    context.getDataSource(),
                    null,
                    currentCondition,
                    true
                );
            } catch (DBException e) {
                log.error(e);
                return false;
            }
            return !currentCondition.toString().trim().equals(filtersText.getText().trim());
        }

        @Override
        protected boolean executeAction(MouseEvent e) {
            if (isItemEnabled()) {
                setCustomDataFilter();
                return true;
            }
            return false;
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

    private abstract static class AbstractDropDownListener extends SelectionAdapter {
        private Menu ddMenu;
        private MenuManager menuManager;

        @Override
        public void widgetSelected(SelectionEvent e) {
            if (e.detail == SWT.ARROW) {
                ToolItem item = (ToolItem) e.widget;
                {
                    if (menuManager == null) {
                        menuManager = new MenuManager();
                        item.addDisposeListener(e1 -> menuManager.dispose());
                    } else {
                        menuManager.removeAll();
                    }

                    fillDropDownMenu(menuManager);

                    if (ddMenu == null) {
                        ddMenu = menuManager.createContextMenu(item.getParent().getShell());
                    } else {
                        menuManager.update();
                    }
                }
                Rectangle rect = item.getBounds();
                Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
                ddMenu.setLocation(pt.x, pt.y + rect.height);
                ddMenu.setVisible(true);
            } else {
                executeDefaultAction();
            }
        }

        protected abstract void fillDropDownMenu(MenuManager menuManager);

        protected abstract void executeDefaultAction();
    }

    private class EraseItemListener extends AbstractDropDownListener {
        @Override
        protected void fillDropDownMenu(MenuManager menuManager) {
            menuManager.add(ActionUtils.makeCommandContribution(viewer.getSite(), ResultSetHandlerMain.CMD_FILTER_CLEAR_SETTING));
            if (viewer.getDataFilter().hasHiddenAttributes()) {
                menuManager.add(ActionUtils.makeCommandContribution(
                    viewer.getSite(),
                    SpreadsheetCommandHandler.CMD_SHOW_COLUMNS,
                    ResultSetMessages.controls_resultset_viewer_show_hidden_columns,
                    null));
            }
            if (viewer.hasColorOverrides()) {
                menuManager.add(new ResetAllColorAction(viewer));
            }
            if (viewer.hasColumnTransformers()) {
                menuManager.add(new FilterResetAllTransformersAction(viewer));
            }
            if (viewer.getDataFilter().hasPinnedAttributes()) {
                menuManager.add(new FilterResetAllPinsAction(viewer));
            }
            if (menuManager.getSize() > 1) {
                menuManager.add(new FilterResetAllSettingsAction(viewer));
            }
        }

        @Override
        protected void executeDefaultAction() {
            viewer.clearDataFilter(true);
        }
    }

    private class CustomFilterListener extends AbstractDropDownListener {
        @Override
        protected void fillDropDownMenu(MenuManager menuManager) {
            menuManager.add(ActionUtils.makeCommandContribution(
                viewer.getSite(), ResultSetHandlerMain.CMD_FILTER_EDIT_SETTINGS));
            if (viewer.getDataContainer() instanceof DBSEntity) {
                menuManager.add(ActionUtils.makeCommandContribution(
                    viewer.getSite(), ResultSetHandlerMain.CMD_FILTER_SAVE_SETTING));
            }
        }

        @Override
        protected void executeDefaultAction() {
            viewer.showFilterSettingsDialog();
        }
    }

}
