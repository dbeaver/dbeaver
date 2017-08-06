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
package org.jkiss.dbeaver.ui.editors.sql;


import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.texteditor.templates.ITemplatesPage;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.dbeaver.ui.IErrorVisualizer;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLControlToken;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLToken;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesPage;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLSymbolInserter;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.ui.preferences.*;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SQL Executor
 */
public abstract class SQLEditorBase extends BaseTextEditor implements IErrorVisualizer {
    static protected final Log log = Log.getLog(SQLEditorBase.class);
    public static final String SQL_EDITOR_CONTEXT = "org.jkiss.dbeaver.ui.editors.sql";

    static {
        // SQL editor preferences. Do this here because it initializes display
        // (that's why we can't run it in prefs initializer classes which run before workbench creation)
        {
            IPreferenceStore editorStore = EditorsPlugin.getDefault().getPreferenceStore();
            editorStore.setDefault(SQLPreferenceConstants.MATCHING_BRACKETS, true);
            editorStore.setDefault(SQLPreferenceConstants.MATCHING_BRACKETS_COLOR, "128,128,128");
        }
    }

    @NotNull
    private final SQLSyntaxManager syntaxManager;
    @NotNull
    private final SQLRuleManager ruleManager;
    private ProjectionSupport projectionSupport;

    private ProjectionAnnotationModel annotationModel;
    //private Map<Annotation, Position> curAnnotations;

    private IAnnotationAccess annotationAccess;
    private boolean hasVerticalRuler = true;
    private SQLTemplatesPage templatesPage;
    private IPropertyChangeListener themeListener;

    public SQLEditorBase()
    {
        super();
        syntaxManager = new SQLSyntaxManager();
        ruleManager = new SQLRuleManager(syntaxManager);
        themeListener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event)
            {
                if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME) ||
                    event.getProperty().startsWith("org.jkiss.dbeaver.sql.editor")) {
                    reloadSyntaxRules();
                    // Reconfigure to let comments/strings colors to take effect
                    getSourceViewer().configure(getSourceViewerConfiguration());
                }
            }
        };
        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeListener);

        //setDocumentProvider(new SQLDocumentProvider());
        setSourceViewerConfiguration(new SQLEditorSourceViewerConfiguration(this, getPreferenceStore()));
        setKeyBindingScopes(new String[]{TEXT_EDITOR_CONTEXT, SQL_EDITOR_CONTEXT});  //$NON-NLS-1$
    }

    @Nullable
    public abstract DBCExecutionContext getExecutionContext();

    public final DBPDataSource getDataSource() {
        DBCExecutionContext context = getExecutionContext();
        return context == null ? null : context.getDataSource();
    }

    public DBPPreferenceStore getActivePreferenceStore() {
        if (this instanceof IDataSourceContainerProvider) {
            DBPDataSourceContainer container = ((IDataSourceContainerProvider) this).getDataSourceContainer();
            if (container != null) {
                return container.getPreferenceStore();
            }
        }
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? DBeaverCore.getGlobalPreferenceStore() : dataSource.getContainer().getPreferenceStore();
    }

    @NotNull
    public SQLDialect getSQLDialect() {
        DBPDataSource dataSource = getDataSource();
        // Refresh syntax
        if (dataSource instanceof SQLDataSource) {
            return ((SQLDataSource) dataSource).getSQLDialect();
        }
        return BasicSQLDialect.INSTANCE;
    }

    public boolean hasAnnotations()
    {
        return false;
    }

    @NotNull
    public SQLSyntaxManager getSyntaxManager()
    {
        return syntaxManager;
    }

    @NotNull
    public SQLRuleManager getRuleManager() {
        return ruleManager;
    }

    public ProjectionAnnotationModel getAnnotationModel()
    {
        return annotationModel;
    }

    public SQLEditorSourceViewerConfiguration getViewerConfiguration()
    {
        return (SQLEditorSourceViewerConfiguration) super.getSourceViewerConfiguration();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        setRangeIndicator(new DefaultRangeIndicator());

        super.createPartControl(new SQLEditorControl(parent, this));

        ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
        projectionSupport = new ProjectionSupport(
            viewer,
            getAnnotationAccess(),
            getSharedColors());
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error"); //$NON-NLS-1$
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning"); //$NON-NLS-1$
        projectionSupport.install();

        viewer.doOperation(ProjectionViewer.TOGGLE);

        annotationModel = viewer.getProjectionAnnotationModel();

        // Symbol inserter
        {
            SQLSymbolInserter symbolInserter = new SQLSymbolInserter(this);

            DBPPreferenceStore preferenceStore = getActivePreferenceStore();
            boolean closeSingleQuotes = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES);
            boolean closeDoubleQuotes = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES);
            boolean closeBrackets = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS);

            symbolInserter.setCloseSingleQuotesEnabled(closeSingleQuotes);
            symbolInserter.setCloseDoubleQuotesEnabled(closeDoubleQuotes);
            symbolInserter.setCloseBracketsEnabled(closeBrackets);

            ISourceViewer sourceViewer = getSourceViewer();
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(symbolInserter);
            }
        }
    }

    @Override
    public void updatePartControl(IEditorInput input)
    {
        super.updatePartControl(input);
    }

    @Override
    protected IVerticalRuler createVerticalRuler()
    {
        return hasVerticalRuler ? super.createVerticalRuler() : new VerticalRuler(0);
    }

    public void setHasVerticalRuler(boolean hasVerticalRuler)
    {
        this.hasVerticalRuler = hasVerticalRuler;
    }

    protected ISharedTextColors getSharedColors()
    {
        return DBeaverUI.getSharedTextColors();
    }

    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles)
    {
        fAnnotationAccess= getAnnotationAccess();
        fOverviewRuler= createOverviewRuler(getSharedColors());

        SQLEditorSourceViewer sourceViewer = createSourceViewer(parent, ruler, styles, fOverviewRuler);

        getSourceViewerDecorationSupport(sourceViewer);

        return sourceViewer;
    }

    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
        char[] matchChars = {'(', ')', '[', ']', '{', '}'}; //which brackets to match
        ICharacterPairMatcher matcher;
        try {
            matcher = new DefaultCharacterPairMatcher(matchChars,
                SQLPartitionScanner.SQL_PARTITIONING,
                true);
        } catch (Throwable e) {
            // If we below Eclipse 4.2.1
            matcher = new DefaultCharacterPairMatcher(matchChars, SQLPartitionScanner.SQL_PARTITIONING);
        }
        support.setCharacterPairMatcher(matcher);
        support.setMatchingCharacterPainterPreferenceKeys(SQLPreferenceConstants.MATCHING_BRACKETS, SQLPreferenceConstants.MATCHING_BRACKETS_COLOR);
        super.configureSourceViewerDecorationSupport(support);
    }

    @NotNull
    protected SQLEditorSourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles, IOverviewRuler overviewRuler) {
        return new SQLEditorSourceViewer(
                parent,
                ruler,
                overviewRuler,
                true,
                styles);
    }

    @Override
    protected IAnnotationAccess createAnnotationAccess() {
        return new SQLMarkerAnnotationAccess();
    }
/*
    protected void adjustHighlightRange(int offset, int length)
    {
        ISourceViewer viewer = getSourceViewer();
        if (viewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
            extension.exposeModelRange(new Region(offset, length));
        }
    }
*/

    @Override
    public Object getAdapter(Class required)
    {
        if (projectionSupport != null) {
            Object adapter = projectionSupport.getAdapter(
                getSourceViewer(), required);
            if (adapter != null)
                return adapter;
        }
        if (ITemplatesPage.class.equals(required)) {
            return getTemplatesPage();
        }

        return super.getAdapter(required);
    }

    public SQLTemplatesPage getTemplatesPage()
    {
        if (templatesPage == null)
            templatesPage = new SQLTemplatesPage(this);
        return templatesPage;
    }

    @Override
    public void dispose()
    {
        if (themeListener != null) {
            PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeListener);
            themeListener = null;
        }

        super.dispose();
    }

    @Override
    protected void createActions()
    {
        super.createActions();

        ResourceBundle bundle = DBeaverActivator.getCoreResourceBundle();

        IAction a = new TextOperationAction(
            bundle,
            SQLEditorContributor.getActionResourcePrefix(SQLEditorContributor.ACTION_CONTENT_ASSIST_PROPOSAL),
            this,
            ISourceViewer.CONTENTASSIST_PROPOSALS);
        a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        setAction(SQLEditorContributor.ACTION_CONTENT_ASSIST_PROPOSAL, a);

        a = new TextOperationAction(
            bundle,
            SQLEditorContributor.getActionResourcePrefix(SQLEditorContributor.ACTION_CONTENT_ASSIST_TIP),
            this,
            ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
        a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
        setAction(SQLEditorContributor.ACTION_CONTENT_ASSIST_TIP, a);

        a = new TextOperationAction(
            bundle,
            SQLEditorContributor.getActionResourcePrefix(SQLEditorContributor.ACTION_CONTENT_ASSIST_INFORMATION),
            this,
            ISourceViewer.INFORMATION);
        a.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_INFORMATION);
        setAction(SQLEditorContributor.ACTION_CONTENT_ASSIST_INFORMATION, a);

        a = new TextOperationAction(
            bundle,
            SQLEditorContributor.getActionResourcePrefix(SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL),
            this,
            ISourceViewer.FORMAT);
        a.setActionDefinitionId(CoreCommands.CMD_CONTENT_FORMAT);
        setAction(SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL, a);

        setAction(ITextEditorActionConstants.CONTEXT_PREFERENCES, new Action("Preferences...") { //$NON-NLS-1$
            public void run() {
                Shell shell = getSourceViewer().getTextWidget().getShell();
                String[] preferencePages = collectContextMenuPreferencePages();
                if (preferencePages.length > 0 && (shell == null || !shell.isDisposed()))
                    PreferencesUtil.createPreferenceDialogOn(shell, preferencePages[0], preferencePages, getEditorInput()).open();
            }
        });
/*
        // Add the task action to the Edit pulldown menu (bookmark action is  'free')
        ResourceAction ra = new AddTaskAction(bundle, "AddTask.", this);
        ra.setHelpContextId(ITextEditorHelpContextIds.ADD_TASK_ACTION);
        ra.setActionDefinitionId(ITextEditorActionDefinitionIds.ADD_TASK);
        setAction(IDEActionFactory.ADD_TASK.getId(), ra);
*/
    }

    @Override
    public void editorContextMenuAboutToShow(IMenuManager menu)
    {
        super.editorContextMenuAboutToShow(menu);

        menu.add(new Separator("content"));//$NON-NLS-1$
        addAction(menu, GROUP_SQL_EXTRAS, SQLEditorContributor.ACTION_CONTENT_ASSIST_PROPOSAL);
        addAction(menu, GROUP_SQL_EXTRAS, SQLEditorContributor.ACTION_CONTENT_ASSIST_TIP);
        addAction(menu, GROUP_SQL_EXTRAS, SQLEditorContributor.ACTION_CONTENT_ASSIST_INFORMATION);
        {
            MenuManager formatMenu = new MenuManager("Format", "format");
            IAction formatAction = getAction(SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL);
            if (formatAction != null) {
                formatMenu.add(formatAction);
            }
            formatMenu.add(getAction(ITextEditorActionConstants.UPPER_CASE));
            formatMenu.add(getAction(ITextEditorActionConstants.LOWER_CASE));
            formatMenu.add(new Separator());
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.word.wrap"));
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.comment.single"));
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.comment.multi"));
            menu.insertAfter(GROUP_SQL_ADDITIONS, formatMenu);
        }
    }

    public void reloadSyntaxRules()
    {
        // Refresh syntax
        SQLDialect dialect = getSQLDialect();
        syntaxManager.init(dialect, getActivePreferenceStore());
        ruleManager.refreshRules(getDataSource(), getEditorInput());

        Document document = getDocument();
        if (document != null) {
            IDocumentPartitioner partitioner = new FastPartitioner(
                new SQLPartitionScanner(dialect),
                SQLPartitionScanner.SQL_CONTENT_TYPES);
            partitioner.connect(document);
            document.setDocumentPartitioner(SQLPartitionScanner.SQL_PARTITIONING, partitioner);

            ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
            if (projectionViewer != null && projectionViewer.getAnnotationModel() != null && document.getLength() > 0) {
                // Refresh viewer
                //projectionViewer.getTextWidget().redraw();
                try {
                    projectionViewer.reinitializeProjection();
                } catch (Throwable ex) {
                    // We can catch OutOfMemory here for too big/complex documents
                    log.warn("Can't initialize SQL syntax projection", ex); //$NON-NLS-1$
                }
            }
        }

/*
        Color fgColor = ruleManager.getColor(SQLConstants.CONFIG_COLOR_TEXT);
        Color bgColor = ruleManager.getColor(getDataSource() == null ?
            SQLConstants.CONFIG_COLOR_DISABLED :
            SQLConstants.CONFIG_COLOR_BACKGROUND);
        final StyledText textWidget = getTextViewer().getTextWidget();
        if (fgColor != null) {
            textWidget.setForeground(fgColor);
        }
        textWidget.setBackground(bgColor);
*/

        // Update configuration
        if (getSourceViewerConfiguration() instanceof SQLEditorSourceViewerConfiguration) {
            ((SQLEditorSourceViewerConfiguration) getSourceViewerConfiguration()).onDataSourceChange();
        }
        final IVerticalRuler verticalRuler = getVerticalRuler();
        if (verticalRuler != null) {
            verticalRuler.update();
        }
    }

    public boolean hasActiveQuery()
    {
        Document document = getDocument();
        if (document == null) {
            return false;
        }
        ISelectionProvider selectionProvider = getSelectionProvider();
        if (selectionProvider == null) {
            return false;
        }
        ITextSelection selection = (ITextSelection) selectionProvider.getSelection();
        String selText = selection.getText();
        if (CommonUtils.isEmpty(selText) && selection.getOffset() >= 0 && selection.getOffset() < document.getLength()) {
            try {
                IRegion lineRegion = document.getLineInformationOfOffset(selection.getOffset());
                selText = document.get(lineRegion.getOffset(), lineRegion.getLength());
            } catch (BadLocationException e) {
                log.warn(e);
                return false;
            }
        }

        return !CommonUtils.isEmptyTrimmed(selText);
    }

    @Nullable
    protected SQLScriptElement extractActiveQuery()
    {
        SQLScriptElement element;
        ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
        String selText = selection.getText().trim();
        selText = SQLUtils.trimQueryStatement(getSyntaxManager(), selText);
        if (!CommonUtils.isEmpty(selText)) {
            SQLScriptElement parsedElement = parseQuery(getDocument(), selection.getOffset(), selection.getOffset() + selection.getLength(), selection.getOffset(), false);
            if (parsedElement instanceof SQLControlCommand) {
                // This is a command
                element = parsedElement;
            } else {
                // Use selected query as is
                element = new SQLQuery(getDataSource(), selText, selection.getOffset(), selection.getLength());
            }
        } else if (selection.getOffset() >= 0) {
            element = extractQueryAtPos(selection.getOffset());
        } else {
            element = null;
        }
        // Check query do not ends with delimiter
        // (this may occur if user selected statement including delimiter)
        if (element == null || CommonUtils.isEmpty(element.getText())) {
            return null;
        }
        if (element instanceof SQLQuery && getActivePreferenceStore().getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED)) {
            ((SQLQuery)element).setParameters(parseParameters(getDocument(), (SQLQuery)element));
        }
        return element;
    }

    public SQLScriptElement extractQueryAtPos(int currentPos)
    {
        Document document = getDocument();
        if (document == null || document.getLength() == 0) {
            return null;
        }
        final int docLength = document.getLength();
        IDocumentPartitioner partitioner = document.getDocumentPartitioner(SQLPartitionScanner.SQL_PARTITIONING);
        if (partitioner != null) {
            // Move to default partition. We don't want to be in the middle of multi-line comment or string
            while (currentPos < docLength && isMultiCommentPartition(partitioner, currentPos)) {
                currentPos++;
            }
        }
        // Extract part of document between empty lines
        int startPos = 0;
        boolean useBlankLines = syntaxManager.isBlankLineDelimiter();
        final String[] statementDelimiters = syntaxManager.getStatementDelimiters();

        try {
            int currentLine = document.getLineOfOffset(currentPos);
            int lineOffset = document.getLineOffset(currentLine);
            if (TextUtils.isEmptyLine(document, currentLine)) {
                return null;
            }

            int firstLine = currentLine;
            while (firstLine > 0) {
                if (useBlankLines) {
                    if (TextUtils.isEmptyLine(document, firstLine) &&
                        isDefaultPartition(partitioner, document.getLineOffset(firstLine))) {
                        break;
                    }
                } else {
                    for (String delim : statementDelimiters) {
                        final int offset = TextUtils.getOffsetOf(document, firstLine, delim);
                        if (offset >= 0 && isDefaultPartition(partitioner, offset)) {
                            break;
                        }
                    }
                }
                firstLine--;
            }
            startPos = document.getLineOffset(firstLine);

            // Move currentPos at line begin
            currentPos = lineOffset;
        } catch (BadLocationException e) {
            log.warn(e);
        }
        return parseQuery(document, startPos, document.getLength(), currentPos, false);
    }

    public SQLScriptElement extractNextQuery(boolean next) {
        ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
        int offset = selection.getOffset();
        SQLScriptElement curElement = extractQueryAtPos(offset);
        if (curElement == null) {
            return null;
        }

        Document document = getDocument();
        if (document == null) {
            return null;
        }
        try {
            int docLength = document.getLength();
            int curPos;
            if (next) {
                final String[] statementDelimiters = syntaxManager.getStatementDelimiters();
                curPos = curElement.getOffset() + curElement.getLength();
                while (curPos < docLength) {
                    char c = document.getChar(curPos);
                    if (!Character.isWhitespace(c)) {
                        boolean isDelimiter = false;
                        for (String delim : statementDelimiters) {
                            if (delim.indexOf(c) != -1) {
                                isDelimiter = true;
                            }
                        }
                        if (!isDelimiter) {
                            break;
                        }
                    }
                    curPos++;
                }
            } else {
                curPos = curElement.getOffset() - 1;
                while (curPos >= 0) {
                    char c = document.getChar(curPos);
                    if (!Character.isWhitespace(c)) {
                        break;
                    }
                    curPos--;
                }
            }
            if (curPos <= 0 || curPos >= docLength) {
                return null;
            }
            return extractQueryAtPos(curPos);
        } catch (BadLocationException e) {
            log.warn(e);
            return null;
        }
    }

    private static boolean isDefaultPartition(IDocumentPartitioner partitioner, int currentPos) {
        return partitioner == null || IDocument.DEFAULT_CONTENT_TYPE.equals(partitioner.getContentType(currentPos));
    }

    private static boolean isMultiCommentPartition(IDocumentPartitioner partitioner, int currentPos) {
        return partitioner != null && SQLPartitionScanner.CONTENT_TYPE_SQL_MULTILINE_COMMENT.equals(partitioner.getContentType(currentPos));
    }

    protected void startScriptEvaluation() {
        ruleManager.startEval();
    }

    protected void endScriptEvaluation() {
        ruleManager.endEval();
    }

    protected SQLScriptElement parseQuery(final IDocument document, final int startPos, final int endPos, final int currentPos, final boolean scriptMode) {
        if (endPos - startPos <= 0) {
            return null;
        }
        SQLDialect dialect = getSQLDialect();

        // Parse range
        boolean useBlankLines = !scriptMode && syntaxManager.isBlankLineDelimiter();
        ruleManager.setRange(document, startPos, endPos - startPos);
        int statementStart = startPos;
        int bracketDepth = 0;
        boolean hasBlocks = false;
        boolean hasValuableTokens = false;
        boolean hasBlockHeader = false;
        String blockTogglePattern = null;
        int lastTokenLineFeeds = 0;
        for (; ; ) {
            IToken token = ruleManager.nextToken();
            int tokenOffset = ruleManager.getTokenOffset();
            int tokenLength = ruleManager.getTokenLength();
            int tokenType = token instanceof SQLToken ? ((SQLToken)token).getType() : SQLToken.T_UNKNOWN;

            boolean isDelimiter = tokenType == SQLToken.T_DELIMITER;
            boolean isControl = false;
            String delimiterText = null;
            if (isDelimiter) {
                // Save delimiter text
                try {
                    delimiterText = document.get(tokenOffset, tokenLength);
                } catch (BadLocationException e) {
                    log.debug(e);
                }
            } else if (useBlankLines && token.isWhitespace() && tokenLength >= 2) {
                // Check for blank line delimiter
                if (lastTokenLineFeeds + countLineFeeds(document, tokenOffset, tokenLength) >= 2) {
                    isDelimiter = true;
                }
            }
            lastTokenLineFeeds = 0;
            if (tokenLength == 1) {
                // Check for bracket block begin/end
                try {
                    char aChar = document.getChar(tokenOffset);
                    if (aChar == '(' || aChar == '{' || aChar == '[') {
                        bracketDepth++;
                    } else if (aChar == ')' || aChar == '}' || aChar == ']') {
                        bracketDepth--;
                    }
                } catch (BadLocationException e) {
                    log.warn(e);
                }
            }
            if (tokenType == SQLToken.T_BLOCK_HEADER) {
                bracketDepth++;
                hasBlocks = true;
                hasBlockHeader = true;
            } else if (tokenType == SQLToken.T_BLOCK_TOGGLE) {
                String togglePattern;
                try {
                    togglePattern = document.get(tokenOffset, tokenLength);
                } catch (BadLocationException e) {
                    log.warn(e);
                    togglePattern = "";
                }
                // Second toggle pattern must be the same as first one.
                // Toggles can be nested (PostgreSQL) and we need to count only outer
                if (bracketDepth == 1 && togglePattern.equals(blockTogglePattern)) {
                    bracketDepth--;
                    blockTogglePattern = null;
                } else if (bracketDepth == 0 && blockTogglePattern == null) {
                    bracketDepth++;
                    blockTogglePattern = togglePattern;
                } else {
                    log.debug("Block toggle token inside another block. Can't process it");
                }
                hasBlocks = true;
            } else if (tokenType == SQLToken.T_BLOCK_BEGIN) {
                if (!hasBlockHeader) {
                    bracketDepth++;
                }
                hasBlocks = true;
            } else if (bracketDepth > 0 && tokenType == SQLToken.T_BLOCK_END) {
                // Sometimes query contains END clause without BEGIN. E.g. CASE, IF, etc.
                // This END doesn't mean block
                if (hasBlocks) {
                    bracketDepth--;
                }
                hasBlockHeader = false;
            } else if (isDelimiter && bracketDepth > 0) {
                // Delimiter in some brackets - ignore it
                continue;
            } else if (tokenType == SQLToken.T_SET_DELIMITER || tokenType == SQLToken.T_CONTROL) {
                isDelimiter = true;
                isControl = true;
            } else if (tokenType == SQLToken.T_COMMENT) {
                lastTokenLineFeeds = tokenLength < 2 ? 0 : countLineFeeds(document, tokenOffset + tokenLength - 2, 2);
            }

            boolean cursorInsideToken = currentPos >= tokenOffset && currentPos < tokenOffset + tokenLength;
            if (isControl && (scriptMode || cursorInsideToken)) {
                // Control query
                try {
                    String controlText = document.get(tokenOffset, tokenLength);
                    String commandId = null;
                    if (token instanceof SQLControlToken) {
                        commandId = ((SQLControlToken) token).getCommandId();
                    }
                    return new SQLControlCommand(
                            getDataSource(),
                            syntaxManager,
                            controlText.trim(),
                            commandId,
                            tokenOffset,
                            tokenLength,
                            tokenType == SQLToken.T_SET_DELIMITER);
                } catch (BadLocationException e) {
                    log.warn("Can't extract control statement", e); //$NON-NLS-1$
                    return null;
                }
            }
            if (hasValuableTokens && (token.isEOF() || (isDelimiter && tokenOffset >= currentPos) || tokenOffset > endPos)) {
                if (tokenOffset > endPos) {
                    tokenOffset = endPos;
                }
                if (tokenOffset >= document.getLength()) {
                    // Sometimes (e.g. when comment finishing script text)
                    // last token offset is beyond document range
                    tokenOffset = document.getLength();
                }
                assert (tokenOffset >= currentPos);
                try {

                    // remove leading spaces
                    while (statementStart < tokenOffset && Character.isWhitespace(document.getChar(statementStart))) {
                        statementStart++;
                    }
                    // remove trailing spaces
                    while (statementStart < tokenOffset && Character.isWhitespace(document.getChar(tokenOffset - 1))) {
                        tokenOffset--;
                        tokenLength++;
                    }
                    if (tokenOffset == statementStart) {
                        // Empty statement
                        if (token.isEOF()) {
                            return null;
                        }
                        statementStart = tokenOffset + tokenLength;
                        continue;
                    }
                    String queryText = document.get(statementStart, tokenOffset - statementStart);
                    queryText = SQLUtils.fixLineFeeds(queryText);

                    if (isDelimiter && (hasBlocks ? dialect.isDelimiterAfterBlock() : dialect.isDelimiterAfterQuery())) {
                        if (delimiterText != null) {
                            queryText += delimiterText;
                        }
                    }
                    int queryEndPos = tokenOffset;
                    if (tokenType == SQLToken.T_DELIMITER) {
                        queryEndPos += tokenLength;
                    }
                    // make script line
                    return new SQLQuery(
                        getDataSource(),
                        queryText.trim(),
                        statementStart,
                            queryEndPos - statementStart);
                } catch (BadLocationException ex) {
                    log.warn("Can't extract query", ex); //$NON-NLS-1$
                    return null;
                }
            }
            if (isDelimiter) {
                statementStart = tokenOffset + tokenLength;
            }
            if (token.isEOF()) {
                return null;
            }
            if (!hasValuableTokens && !token.isWhitespace() && !isControl) {
                if (tokenType == SQLToken.T_COMMENT) {
                    hasValuableTokens = dialect.supportsCommentQuery();
                } else {
                    hasValuableTokens = true;
                }
            }
        }
    }

    private static int countLineFeeds(final IDocument document, final int offset, final int length) {
        int lfCount = 0;
        try {
            for (int i = offset; i < offset + length; i++) {
                if (document.getChar(i) == '\n') {
                    lfCount++;
                }
            }
        } catch (BadLocationException e) {
            log.error(e);
        }
        return lfCount;
    }

    protected List<SQLQueryParameter> parseParameters(IDocument document, SQLQuery query) {
        final SQLDialect sqlDialect = getSQLDialect();
        boolean supportParamsInDDL = getActivePreferenceStore().getBoolean(ModelPreferences.SQL_PARAMETERS_IN_DDL_ENABLED);
        boolean execQuery = false;
        List<SQLQueryParameter> parameters = null;
        ruleManager.setRange(document, query.getOffset(), query.getLength());

        boolean firstKeyword = true;
        for (;;) {
            IToken token = ruleManager.nextToken();
            int tokenOffset = ruleManager.getTokenOffset();
            final int tokenLength = ruleManager.getTokenLength();
            if (token.isEOF() || tokenOffset > query.getOffset() + query.getLength()) {
                break;
            }
            // Handle only parameters which are not in SQL blocks
            int tokenType = SQLToken.T_UNKNOWN;
            if (token instanceof SQLToken) {
                tokenType = ((SQLToken) token).getType();
            }
            if (token.isWhitespace() || tokenType == SQLToken.T_COMMENT) {
                continue;
            }
            if (!supportParamsInDDL) {
                if (firstKeyword) {
                    // Detect query type
                    try {
                        String tokenText = document.get(tokenOffset, tokenLength);
                        if (ArrayUtils.containsIgnoreCase(sqlDialect.getDDLKeywords(), tokenText)) {
                            // DDL doesn't support parameters
                            return null;
                        }
                        execQuery = ArrayUtils.containsIgnoreCase(sqlDialect.getExecuteKeywords(), tokenText);
                    } catch (BadLocationException e) {
                        log.warn(e);
                    }
                    firstKeyword = false;
                }
            }
            if (tokenType == SQLToken.T_PARAMETER && tokenLength > 0) {
                try {
                    String paramName = document.get(tokenOffset, tokenLength);
                    if (execQuery && paramName.equals("?")) {
                        // Skip ? parameters for stored procedures (they have special meaning? [DB2])
                        continue;
                    }

                    if (parameters == null) {
                        parameters = new ArrayList<>();
                    }

                    SQLQueryParameter parameter = new SQLQueryParameter(
                        parameters.size(),
                        paramName,
                        tokenOffset - query.getOffset(),
                        tokenLength);

                    SQLQueryParameter previous = null;
                    if (parameter.isNamed()) {
                        for (int i = parameters.size(); i > 0; i--) {
                            if (parameters.get(i - 1).getName().equals(paramName)) {
                                previous = parameters.get(i - 1);
                                break;
                            }
                        }
                    }
                    parameter.setPrevious(previous);
                    parameters.add(parameter);
                } catch (BadLocationException e) {
                    log.warn("Can't extract query parameter", e);
                }
            }
        }
        return parameters;
    }

    public boolean isDisposed()
    {
        return
            getSourceViewer() == null ||
                getSourceViewer().getTextWidget() == null ||
                getSourceViewer().getTextWidget().isDisposed();
    }

    @Nullable
    @Override
    public ICommentsSupport getCommentsSupport()
    {
        final SQLDialect dialect = getSQLDialect();
        return new ICommentsSupport() {
            @Nullable
            @Override
            public Pair<String, String> getMultiLineComments() {
                return dialect.getMultiLineComments();
            }

            @Override
            public String[] getSingleLineComments() {
                return dialect.getSingleLineComments();
            }
        };
    }

    protected String[] collectContextMenuPreferencePages() {
        String[] ids = super.collectContextMenuPreferencePages();
        String[] more = new String[ids.length + 5];
        more[ids.length] = PrefPageSQLEditor.PAGE_ID;
        more[ids.length + 1] = PrefPageSQLExecute.PAGE_ID;
        more[ids.length + 2] = PrefPageSQLCompletion.PAGE_ID;
        more[ids.length + 3] = PrefPageSQLFormat.PAGE_ID;
        more[ids.length + 4] = PrefPageSQLTemplates.PAGE_ID;
        System.arraycopy(ids, 0, more, 0, ids.length);
        return more;
    }

    @Override
    public boolean visualizeError(@NotNull DBRProgressMonitor monitor, @NotNull Throwable error) {
        Document document = getDocument();
        SQLQuery query = new SQLQuery(getDataSource(), document.get(), 0, document.getLength());
        return scrollCursorToError(monitor, query, error);
    }

    /**
     * Error handling
     */
    protected boolean scrollCursorToError(@NotNull DBRProgressMonitor monitor, @NotNull SQLQuery query, @NotNull Throwable error) {
        try {
            DBCExecutionContext context = getExecutionContext();
            boolean scrolled = false;
            DBPErrorAssistant errorAssistant = DBUtils.getAdapter(DBPErrorAssistant.class, context.getDataSource());
            if (errorAssistant != null) {
                DBPErrorAssistant.ErrorPosition[] positions = errorAssistant.getErrorPosition(
                    monitor, context, query.getText(), error);
                if (positions != null && positions.length > 0) {
                    int queryStartOffset = query.getOffset();
                    int queryLength = query.getLength();

                    DBPErrorAssistant.ErrorPosition pos = positions[0];
                    if (pos.line < 0) {
                        if (pos.position >= 0) {
                            // Only position
                            getSelectionProvider().setSelection(new TextSelection(queryStartOffset + pos.position, 1));
                            scrolled = true;
                        }
                    } else {
                        // Line + position
                        Document document = getDocument();
                        if (document != null) {
                            int startLine = document.getLineOfOffset(queryStartOffset);
                            int errorOffset = document.getLineOffset(startLine + pos.line);
                            int errorLength;
                            if (pos.position >= 0) {
                                errorOffset += pos.position;
                                errorLength = 1;
                            } else {
                                errorLength = document.getLineLength(startLine + pos.line);
                            }
                            if (errorOffset < queryStartOffset) errorOffset = queryStartOffset;
                            if (errorLength > queryLength) errorLength = queryLength;
                            getSelectionProvider().setSelection(new TextSelection(errorOffset, errorLength));
                            scrolled = true;
                        }
                    }
                }
            }
            return scrolled;
//            if (!scrolled) {
//                // Can't position on error - let's just select entire problem query
//                showStatementInEditor(result.getStatement(), true);
//            }
        } catch (Exception e) {
            log.warn("Error positioning on query error", e);
            return false;
        }
    }

}
