/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.sql;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.texteditor.templates.ITemplatesPage;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.ext.ICommentsSupport;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLDelimiterToken;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesPage;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLSymbolInserter;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * SQL Executor
 */
public abstract class SQLEditorBase extends BaseTextEditor {
    static protected final Log log = Log.getLog(SQLEditorBase.class);

    @NotNull
    private final SQLSyntaxManager syntaxManager;
    private ProjectionSupport projectionSupport;

    private ProjectionAnnotationModel annotationModel;
    private Map<Annotation, Position> curAnnotations;

    private IAnnotationAccess annotationAccess;
    private boolean hasVerticalRuler = true;
    private SQLTemplatesPage templatesPage;
    private IPropertyChangeListener themeListener;
    private SourceViewerDecorationSupport decorationSupport;

    public SQLEditorBase()
    {
        super();
        syntaxManager = new SQLSyntaxManager();
        themeListener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event)
            {
                if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME) ||
                    event.getProperty().startsWith("org.jkiss.dbeaver.sql.editor")) {
                    reloadSyntaxRules();
                }
            }
        };
        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeListener);

        setDocumentProvider(new SQLDocumentProvider());
        setSourceViewerConfiguration(new SQLEditorSourceViewerConfiguration(
            this,
            syntaxManager));
        setKeyBindingScopes(new String[]{"org.eclipse.ui.textEditorScope", "org.jkiss.dbeaver.ui.editors.sql"});  //$NON-NLS-1$
    }

    public abstract DBCExecutionContext getExecutionContext();

    public final DBPDataSource getDataSource()
    {
        DBCExecutionContext context = getExecutionContext();
        return context == null ? null : context.getDataSource();
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

            IPreferenceStore preferenceStore = DBeaverCore.getGlobalPreferenceStore();
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

        if (decorationSupport != null) {
            decorationSupport.install(getPreferenceStore());
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

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException
    {
        IEditorInput oldInput = getEditorInput();
        super.doSetInput(input);
        if (oldInput != null && input != null && !oldInput.equals(input)) {
            // Editor input changed - it may be a result of resource change (move/rename)
            reloadSyntaxRules();
        }
    }

    protected ISharedTextColors getSharedColors()
    {
        return DBeaverUI.getSharedTextColors();
    }

    @Override
    protected ISourceViewer createSourceViewer(Composite parent,
                                               IVerticalRuler ruler, int styles)
    {
        OverviewRuler overviewRuler = null;
        if (hasAnnotations()) {
            overviewRuler = new OverviewRuler(
                getAnnotationAccess(),
                VERTICAL_RULER_WIDTH,
                getSharedColors());
        }
        SQLEditorSourceViewer sourceViewer = new SQLEditorSourceViewer(
            parent,
            ruler,
            overviewRuler,
            true,
            styles);

        if (hasAnnotations()) {
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

            // Configure decorations
            decorationSupport = new SourceViewerDecorationSupport(sourceViewer, overviewRuler, getAnnotationAccess(), getSharedColors());
            decorationSupport.setCursorLinePainterPreferenceKeys(SQLPreferenceConstants.CURRENT_LINE, SQLPreferenceConstants.CURRENT_LINE_COLOR);
            decorationSupport.setMarginPainterPreferenceKeys(SQLPreferenceConstants.PRINT_MARGIN, SQLPreferenceConstants.PRINT_MARGIN_COLOR, SQLPreferenceConstants.PRINT_MARGIN_COLUMN);
            decorationSupport.setSymbolicFontName(getFontPropertyPreferenceKey());
            decorationSupport.setCharacterPairMatcher(matcher);
            decorationSupport.setMatchingCharacterPainterPreferenceKeys(SQLPreferenceConstants.MATCHING_BRACKETS, SQLPreferenceConstants.MATCHING_BRACKETS_COLOR);
        }

        return sourceViewer;
    }

    private IAnnotationAccess getAnnotationAccess()
    {
        if (annotationAccess == null) {
            annotationAccess = new SQLMarkerAnnotationAccess();
        }
        return annotationAccess;
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
        if (decorationSupport != null) {
            decorationSupport.dispose();
            decorationSupport = null;
        }
        syntaxManager.dispose();
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

        ResourceBundle bundle = DBeaverActivator.getResourceBundle();

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
            SQLEditorContributor.getActionResourcePrefix(SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL),
            this,
            ISourceViewer.FORMAT);
        a.setActionDefinitionId(ICommandIds.CMD_CONTENT_FORMAT);
        setAction(SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL, a);

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
        addAction(menu, SQLEditorContributor.ACTION_CONTENT_ASSIST_PROPOSAL);
        addAction(menu, SQLEditorContributor.ACTION_CONTENT_ASSIST_TIP);
        addAction(menu, SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL);
        //addAction(menu, ACTION_DEFINE_FOLDING_REGION);
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    public void reloadSyntaxRules()
    {
        DBCExecutionContext executionContext = getExecutionContext();
        DBPDataSource dataSource = executionContext == null ? null : executionContext.getDataSource();
        // Refresh syntax
        if (dataSource instanceof SQLDataSource) {
            syntaxManager.setDataSource((SQLDataSource) dataSource);
            syntaxManager.refreshRules();

            Document document = getDocument();
            if (document != null) {
                IDocumentPartitioner partitioner = new FastPartitioner(
                    new SQLPartitionScanner(syntaxManager),
                    SQLPartitionScanner.SQL_PARTITION_TYPES);
                partitioner.connect(document);
                document.setDocumentPartitioner(SQLPartitionScanner.SQL_PARTITIONING, partitioner);

                ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
                if (projectionViewer != null && document.getLength() > 0) {
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
        }

        Color fgColor = getSyntaxManager().getColor(SQLConstants.CONFIG_COLOR_TEXT);
        Color bgColor = getSyntaxManager().getColor(!syntaxManager.isUnassigned() && dataSource == null ?
            SQLConstants.CONFIG_COLOR_DISABLED :
            SQLConstants.CONFIG_COLOR_BACKGROUND);
        if (fgColor != null) {
            getTextViewer().getTextWidget().setForeground(fgColor);
        }
        getTextViewer().getTextWidget().setBackground(bgColor);

        // Update configuration
        if (getSourceViewerConfiguration() instanceof SQLEditorSourceViewerConfiguration) {
            ((SQLEditorSourceViewerConfiguration) getSourceViewerConfiguration()).onDataSourceChange();
        }
        getVerticalRuler().update();
    }

    public boolean hasActiveQuery()
    {
        ISelectionProvider selectionProvider = getSelectionProvider();
        if (selectionProvider == null) {
            return false;
        }
        ITextSelection selection = (ITextSelection) selectionProvider.getSelection();
        String selText = selection.getText();
        if (CommonUtils.isEmpty(selText) && selection.getOffset() >= 0) {
            Document document = getDocument();
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
    protected SQLQuery extractActiveQuery()
    {
        SQLQuery sqlQuery;
        ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
        String selText = selection.getText().trim();
        selText = SQLUtils.trimQueryStatement(getSyntaxManager(), selText);
        if (!CommonUtils.isEmpty(selText)) {
            sqlQuery = new SQLQuery(this, selText, selection.getOffset(), selection.getLength());
        } else if (selection.getOffset() >= 0) {
            sqlQuery = extractQueryAtPos(selection.getOffset());
        } else {
            sqlQuery = null;
        }
        // Check query do not ends with delimiter
        // (this may occur if user selected statement including delimiter)
        if (sqlQuery == null || CommonUtils.isEmpty(sqlQuery.getQuery())) {
            return null;
        }
        sqlQuery.parseParameters(getDocument(), getSyntaxManager());
        return sqlQuery;
    }

    public SQLQuery extractQueryAtPos(int currentPos)
    {
        Document document = getDocument();
        if (document.getLength() == 0) {
            return null;
        }
        IDocumentPartitioner partitioner = document.getDocumentPartitioner(SQLPartitionScanner.SQL_PARTITIONING);
        if (partitioner != null) {
            // Move to default partition. We don't want to be in the middle of multi-line comment or string
            while (currentPos > 0 && !IDocument.DEFAULT_CONTENT_TYPE.equals(partitioner.getContentType(currentPos))) {
                currentPos--;
            }
        }

        // Extract part of document between empty lines
        int startPos = 0;
        int endPos = document.getLength();
        try {
            int currentLine = document.getLineOfOffset(currentPos);
            int lineOffset = document.getLineOffset(currentLine);
            int linesCount = document.getNumberOfLines();
            int firstLine = currentLine, lastLine = currentLine;
            while (firstLine > 0) {
                if (TextUtils.isEmptyLine(document, firstLine)) {
                    break;
                }
                firstLine--;
            }
            while (lastLine < linesCount) {
                if (TextUtils.isEmptyLine(document, lastLine)) {
                    if (partitioner == null || IDocument.DEFAULT_CONTENT_TYPE.equals(partitioner.getContentType(document.getLineOffset(lastLine)))) {
                        break;
                    }
                }
                lastLine++;
            }
            if (lastLine >= linesCount) {
                lastLine = linesCount - 1;
            }
            startPos = document.getLineOffset(firstLine);
            endPos = document.getLineOffset(lastLine) + document.getLineLength(lastLine);
            //String lastDelimiter = document.getLineDelimiter(lastLine);
            //if (lastDelimiter != null) {
            //    endPos += lastDelimiter.length();
            //}

            // Move currentPos at line begin
            currentPos = lineOffset;
        } catch (BadLocationException e) {
            log.warn(e);
        }

        // Parse range
        syntaxManager.setRange(document, startPos, endPos - startPos);
        int statementStart = startPos;
        int bracketDepth = 0;
        for (; ; ) {
            IToken token = syntaxManager.nextToken();
            int tokenOffset = syntaxManager.getTokenOffset();
            final int tokenLength = syntaxManager.getTokenLength();
            boolean isDelimiter = token instanceof SQLDelimiterToken;
            if (tokenLength == 1) {
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
            if (isDelimiter && bracketDepth > 0) {
                // Delimiter in some brackets - ignore it
                continue;
            }
            if (token.isEOF() || (isDelimiter && tokenOffset >= currentPos) || tokenOffset > endPos) {
                // get position before last token start
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
                    }
                    String queryText = document.get(statementStart, tokenOffset - statementStart);
                    queryText = queryText.trim();
                    if (queryText.endsWith(syntaxManager.getStatementDelimiter())) {
                        queryText = queryText.substring(0, queryText.length() - syntaxManager.getStatementDelimiter().length());
                    }
                    // make script line
                    SQLQuery statementInfo = new SQLQuery(this, queryText.trim(), statementStart, tokenOffset - statementStart);
                    return statementInfo;
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
        }
    }

    public synchronized void updateFoldingStructure(int offset, int length, List<Position> positions)
    {
        if (curAnnotations == null) {
            curAnnotations = new HashMap<Annotation, Position>();
        }
        List<Annotation> deletedAnnotations = new ArrayList<Annotation>();
        Map<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();

        // Delete all annotations if specified range
        for (Map.Entry<Annotation, Position> entry : curAnnotations.entrySet()) {
            int entryOffset = entry.getValue().getOffset();
            if (entryOffset >= offset && entryOffset < offset + length) {
                deletedAnnotations.add(entry.getKey());
            }
        }
        for (Annotation annotation : deletedAnnotations) {
            curAnnotations.remove(annotation);
        }

        // Add new annotations
        for (Position position : positions) {
            ProjectionAnnotation annotation = new ProjectionAnnotation();
            newAnnotations.put(annotation, position);
        }

        // Modify annotation set
        annotationModel.modifyAnnotations(
            deletedAnnotations.toArray(new Annotation[deletedAnnotations.size()]),
            newAnnotations,
            null);

        // Update current annotations
        curAnnotations.putAll(newAnnotations);
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
        DBCExecutionContext context = getExecutionContext();
        DBPDataSource dataSource = context == null ? null : context.getDataSource();
        if (dataSource instanceof SQLDataSource) {
            return ((SQLDataSource) dataSource).getSQLDialect();
        } else {
            return null;
        }
    }


}
