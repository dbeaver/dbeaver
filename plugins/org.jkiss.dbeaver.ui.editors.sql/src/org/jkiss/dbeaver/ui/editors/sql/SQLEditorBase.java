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
package org.jkiss.dbeaver.ui.editors.sql;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.texteditor.templates.ITemplatesPage;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.editors.BaseTextEditorCommands;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.preferences.*;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLCharacterPairMatcher;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLEditorCompletionContext;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleScanner;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesPage;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLSymbolInserter;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SQL Executor
 */
public abstract class SQLEditorBase extends BaseTextEditor implements DBPContextProvider, IErrorVisualizer {

    static protected final Log log = Log.getLog(SQLEditorBase.class);
    private static final long MAX_FILE_LENGTH_FOR_RULES = 2000000;

    static final String STATS_CATEGORY_SELECTION_STATE = "SelectionState";

    static {
        // SQL editor preferences. Do this here because it initializes display
        // (that's why we can't run it in prefs initializer classes which run before workbench creation)
        {
            IPreferenceStore editorStore = EditorsUI.getPreferenceStore();
            editorStore.setDefault(SQLPreferenceConstants.MATCHING_BRACKETS, true);
            //editorStore.setDefault(SQLPreferenceConstants.MATCHING_BRACKETS_COLOR, "128,128,128"); //$NON-NLS-1$
        }
    }

    @NotNull
    private final SQLSyntaxManager syntaxManager;
    @NotNull
    private final SQLRuleScanner ruleScanner;
    @Nullable
    private SQLParserContext parserContext;
    private ProjectionSupport projectionSupport;

    private ProjectionAnnotationModel annotationModel;
    //private Map<Annotation, Position> curAnnotations;

    //private IAnnotationAccess annotationAccess;
    private boolean hasVerticalRuler = true;
    private SQLTemplatesPage templatesPage;
    private IPropertyChangeListener themeListener;
    private SQLEditorControl editorControl;

    private ICharacterPairMatcher characterPairMatcher;
    private SQLEditorCompletionContext completionContext;
    private SQLOccurrencesHighlighter occurrencesHighlighter;

    public SQLEditorBase() {
        super();
        syntaxManager = new SQLSyntaxManager();
        ruleScanner = new SQLRuleScanner();
        themeListener = new IPropertyChangeListener() {
            long lastUpdateTime = 0;

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME) ||
                    event.getProperty().startsWith("org.jkiss.dbeaver.sql.editor")) {
                    if (lastUpdateTime > 0 && System.currentTimeMillis() - lastUpdateTime < 500) {
                        // Do not update too often (theme change may trigger this hundreds of times)
                        return;
                    }
                    lastUpdateTime = System.currentTimeMillis();
                    UIUtils.asyncExec(() -> {
                        ISourceViewer sourceViewer = getSourceViewer();
                        if (sourceViewer != null) {
                            reloadSyntaxRules();
                            // Reconfigure to let comments/strings colors to take effect
                            sourceViewer.configure(getSourceViewerConfiguration());
                        }
                    });
                }
            }
        };
        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeListener);

        //setDocumentProvider(new SQLDocumentProvider());
        setSourceViewerConfiguration(new SQLEditorSourceViewerConfiguration(this, getPreferenceStore()));
        setKeyBindingScopes(getKeyBindingContexts());  //$NON-NLS-1$

        completionContext = new SQLEditorCompletionContext(this);
    }

    @Override
    protected boolean isReadOnly() {
        IDocumentProvider provider = getDocumentProvider();
        return provider instanceof IDocumentProviderExtension &&
            ((IDocumentProviderExtension) provider).isReadOnly(getEditorInput());
    }

    static boolean isBigScript(@Nullable IEditorInput editorInput) {
        if (editorInput != null) {
            File file = EditorUtils.getLocalFileFromInput(editorInput);
            return file != null && file.length() > MAX_FILE_LENGTH_FOR_RULES;
        }
        return false;
    }

    static boolean isReadEmbeddedBinding() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ);
    }

    static boolean isWriteEmbeddedBinding() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_WRITE);
    }

    private void handleInputChange(IEditorInput input) {
        occurrencesHighlighter.updateInput(input);
    }

    @Override
    protected void updateSelectionDependentActions() {
        super.updateSelectionDependentActions();
        updateStatusField(STATS_CATEGORY_SELECTION_STATE);
    }

    protected String[] getKeyBindingContexts() {
        return new String[]{
            TEXT_EDITOR_CONTEXT,
            SQLEditorContributions.SQL_EDITOR_CONTEXT,
            SQLEditorContributions.SQL_EDITOR_SCRIPT_CONTEXT,
            SQLEditorContributions.SQL_EDITOR_CONTROL_CONTEXT};
    }

    @Override
    protected void initializeEditor() {
        super.initializeEditor();
        this.occurrencesHighlighter = new SQLOccurrencesHighlighter(this);
        setEditorContextMenuId(SQLEditorContributions.SQL_EDITOR_CONTEXT_MENU_ID);
        setRulerContextMenuId(SQLEditorContributions.SQL_RULER_CONTEXT_MENU_ID);
    }

    public DBPDataSource getDataSource() {
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
        return dataSource == null ? DBWorkbench.getPlatform().getPreferenceStore() : dataSource.getContainer().getPreferenceStore();
    }

    @Override
    protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
        if (!occurrencesHighlighter.handlePreferenceStoreChanged(event)) {
            super.handlePreferenceStoreChanged(event);
        }
    }

    @NotNull
    public SQLDialect getSQLDialect() {
        DBPDataSource dataSource = getDataSource();
        // Refresh syntax
        if (dataSource != null) {
            return dataSource.getSQLDialect();
        }
        return BasicSQLDialect.INSTANCE;
    }

    @NotNull
    public SQLSyntaxManager getSyntaxManager() {
        return syntaxManager;
    }

    @NotNull
    public SQLRuleScanner getRuleScanner() {
        return ruleScanner;
    }

    public ProjectionAnnotationModel getAnnotationModel() {
        return annotationModel;
    }

    public SQLEditorSourceViewerConfiguration getViewerConfiguration() {
        return (SQLEditorSourceViewerConfiguration) super.getSourceViewerConfiguration();
    }

    @Override
    public void createPartControl(Composite parent) {
        setRangeIndicator(new DefaultRangeIndicator());

        editorControl = new SQLEditorControl(parent, this);
        super.createPartControl(editorControl);

        if (occurrencesHighlighter.isEnabled()) {
            occurrencesHighlighter.installOccurrencesFinder();
        }

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

        {
            // Context listener
            EditorUtils.trackControlContext(getSite(), getViewer().getTextWidget(), SQLEditorContributions.SQL_EDITOR_CONTROL_CONTEXT);
        }
    }

    public SQLEditorControl getEditorControlWrapper() {
        return editorControl;
    }

    @Override
    public void updatePartControl(IEditorInput input) {
        super.updatePartControl(input);
    }

    protected IOverviewRuler createOverviewRuler(ISharedTextColors sharedColors) {
        if (isOverviewRulerVisible()) {
            return super.createOverviewRuler(sharedColors);
        } else {
            return new OverviewRuler(getAnnotationAccess(), 0, sharedColors);
        }
    }

    @Override
    protected boolean isOverviewRulerVisible() {
        return false;
    }

    // Most left ruler
    @Override
    protected IVerticalRulerColumn createAnnotationRulerColumn(CompositeRuler ruler) {
        if (isAnnotationRulerVisible()) {
            return super.createAnnotationRulerColumn(ruler);
        } else {
            return new AnnotationRulerColumn(0, getAnnotationAccess());
        }
    }

    protected boolean isAnnotationRulerVisible() {
        return false;
    }

    @Override
    protected IVerticalRuler createVerticalRuler() {
        return hasVerticalRuler ? super.createVerticalRuler() : new VerticalRuler(0);
    }

    void setHasVerticalRuler(boolean hasVerticalRuler) {
        this.hasVerticalRuler = hasVerticalRuler;
    }

    protected ISharedTextColors getSharedColors() {
        return UIUtils.getSharedTextColors();
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        handleInputChange(input);
        super.doSetInput(input);
    }

    @Override
    public void doSave(IProgressMonitor progressMonitor) {
        super.doSave(progressMonitor);

        handleInputChange(getEditorInput());
    }

    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        fAnnotationAccess = getAnnotationAccess();
        fOverviewRuler = createOverviewRuler(getSharedColors());

        SQLEditorSourceViewer sourceViewer = createSourceViewer(parent, ruler, styles, fOverviewRuler);

        getSourceViewerDecorationSupport(sourceViewer);

        return sourceViewer;
    }

    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
        char[] matchChars = SQLConstants.BRACKETS; //which brackets to match
        try {
            characterPairMatcher = new SQLCharacterPairMatcher(this, matchChars,
                SQLParserPartitions.SQL_PARTITIONING,
                true);
        } catch (Throwable e) {
            // If we below Eclipse 4.2.1
            characterPairMatcher = new SQLCharacterPairMatcher(this, matchChars, SQLParserPartitions.SQL_PARTITIONING);
        }
        support.setCharacterPairMatcher(characterPairMatcher);
        support.setMatchingCharacterPainterPreferenceKeys(SQLPreferenceConstants.MATCHING_BRACKETS, SQLPreferenceConstants.MATCHING_BRACKETS_COLOR);
        super.configureSourceViewerDecorationSupport(support);
    }

    public ICharacterPairMatcher getCharacterPairMatcher() {
        return characterPairMatcher;
    }

    @NotNull
    private SQLEditorSourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles, IOverviewRuler overviewRuler) {
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> required) {
        if (projectionSupport != null) {
            T adapter = projectionSupport.getAdapter(getSourceViewer(), required);
            if (adapter != null) {
                return adapter;
            }
        }
        if (ITemplatesPage.class.equals(required)) {
            return (T) getTemplatesPage();
        }

        return super.getAdapter(required);
    }

    public SQLTemplatesPage getTemplatesPage() {
        if (templatesPage == null)
            templatesPage = new SQLTemplatesPage(this);
        return templatesPage;
    }

    @Override
    public void dispose() {
        this.occurrencesHighlighter.dispose();
/*
        if (this.activationListener != null) {
            Shell shell = this.getEditorSite().getShell();
            if (shell != null && !shell.isDisposed()) {
                shell.removeShellListener(this.activationListener);
            }
            this.activationListener = null;
        }
*/

        if (themeListener != null) {
            PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeListener);
            themeListener = null;
        }

        super.dispose();
    }

    @Override
    protected void createActions() {
        super.createActions();

        ResourceBundle bundle = ResourceBundle.getBundle(SQLEditorMessages.BUNDLE_NAME);

        IAction action = new TextOperationAction(
            bundle,
            SQLEditorContributor.getActionResourcePrefix(SQLEditorContributor.ACTION_CONTENT_ASSIST_PROPOSAL),
            this,
            ISourceViewer.CONTENTASSIST_PROPOSALS);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        setAction(SQLEditorContributor.ACTION_CONTENT_ASSIST_PROPOSAL, action);

        action = new TextOperationAction(
            bundle,
            SQLEditorContributor.getActionResourcePrefix(SQLEditorContributor.ACTION_CONTENT_ASSIST_TIP),
            this,
            ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
        setAction(SQLEditorContributor.ACTION_CONTENT_ASSIST_TIP, action);

        action = new TextOperationAction(
            bundle,
            SQLEditorContributor.getActionResourcePrefix(SQLEditorContributor.ACTION_CONTENT_ASSIST_INFORMATION),
            this,
            ISourceViewer.INFORMATION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_INFORMATION);
        setAction(SQLEditorContributor.ACTION_CONTENT_ASSIST_INFORMATION, action);

        action = new TextOperationAction(
            bundle,
            SQLEditorContributor.getActionResourcePrefix(SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL),
            this,
            ISourceViewer.FORMAT);
        action.setActionDefinitionId(BaseTextEditorCommands.CMD_CONTENT_FORMAT);
        setAction(SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL, action);

        setAction(ITextEditorActionConstants.CONTEXT_PREFERENCES, new ShowPreferencesAction());
/*
        // Add the task action to the Edit pulldown menu (bookmark action is  'free')
        ResourceAction ra = new AddTaskAction(bundle, "AddTask.", this);
        ra.setHelpContextId(ITextEditorHelpContextIds.ADD_TASK_ACTION);
        ra.setActionDefinitionId(ITextEditorActionDefinitionIds.ADD_TASK);
        setAction(IDEActionFactory.ADD_TASK.getId(), ra);
*/
    }

    // Exclude input additions. Get rid of tons of crap from debug/team extensions
    @Override
    protected boolean isEditorInputIncludedInContextMenu() {
        return false;
    }

    @Override
    public void editorContextMenuAboutToShow(IMenuManager menu) {
        menu.add(new GroupMarker(GROUP_SQL_ADDITIONS));

        super.editorContextMenuAboutToShow(menu);

        //menu.add(new Separator("content"));//$NON-NLS-1$
        addAction(menu, GROUP_SQL_EXTRAS, SQLEditorContributor.ACTION_CONTENT_ASSIST_PROPOSAL);
        addAction(menu, GROUP_SQL_EXTRAS, SQLEditorContributor.ACTION_CONTENT_ASSIST_TIP);
        addAction(menu, GROUP_SQL_EXTRAS, SQLEditorContributor.ACTION_CONTENT_ASSIST_INFORMATION);
        menu.insertBefore(ITextEditorActionConstants.GROUP_COPY, ActionUtils.makeCommandContribution(getSite(), SQLEditorCommands.CMD_NAVIGATE_OBJECT));

        if (!isReadOnly() && getTextViewer().isEditable()) {
            MenuManager formatMenu = new MenuManager(SQLEditorMessages.sql_editor_menu_format, "format");
            IAction formatAction = getAction(SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL);
            if (formatAction != null) {
                formatMenu.add(formatAction);
            }
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.morph.delimited.list"));
            formatMenu.add(getAction(ITextEditorActionConstants.UPPER_CASE));
            formatMenu.add(getAction(ITextEditorActionConstants.LOWER_CASE));
            formatMenu.add(new Separator());
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.word.wrap"));
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.comment.single"));
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.comment.multi"));
            menu.insertAfter(GROUP_SQL_ADDITIONS, formatMenu);
        }

        //menu.remove(IWorkbenchActionConstants.MB_ADDITIONS);
    }

    public void reloadSyntaxRules() {
        // Refresh syntax
        SQLDialect dialect = getSQLDialect();
        IDocument document = getDocument();

        syntaxManager.init(dialect, getActivePreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(getDataSource(), SQLEditorBase.isBigScript(getEditorInput()));

        ruleScanner.refreshRules(getDataSource(), ruleManager);
        parserContext = new SQLParserContext(SQLEditorBase.this, syntaxManager, ruleManager, document != null ? document : new Document());

        if (document instanceof IDocumentExtension3) {
            IDocumentPartitioner partitioner = new FastPartitioner(
                new SQLPartitionScanner(getDataSource(), dialect),
                SQLParserPartitions.SQL_CONTENT_TYPES);
            partitioner.connect(document);
            try {
                ((IDocumentExtension3)document).setDocumentPartitioner(SQLParserPartitions.SQL_PARTITIONING, partitioner);
            } catch (Throwable e) {
                log.warn("Error setting SQL partitioner", e); //$NON-NLS-1$
            }

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

        final IVerticalRuler verticalRuler = getVerticalRuler();

        /*if (isReadOnly()) {
            //Color fgColor = ruleScanner.getColor(SQLConstants.CONFIG_COLOR_TEXT);
            Color bgColor = ruleScanner.getColor(SQLConstants.CONFIG_COLOR_DISABLED);
            TextViewer textViewer = getTextViewer();
            if (textViewer != null) {
                final StyledText textWidget = textViewer.getTextWidget();
                textWidget.setBackground(bgColor);
                if (verticalRuler != null && verticalRuler.getControl() != null) {
                    verticalRuler.getControl().setBackground(bgColor);
                }
            }
        }*/

        // Update configuration
        if (getSourceViewerConfiguration() instanceof SQLEditorSourceViewerConfiguration) {
            ((SQLEditorSourceViewerConfiguration) getSourceViewerConfiguration()).onDataSourceChange();
        }
        if (verticalRuler != null) {
            verticalRuler.update();
        }
    }

    boolean hasActiveQuery() {
        IDocument document = getDocument();
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
    public SQLScriptElement extractActiveQuery() {
        if (parserContext == null) {
            return null;
        }
        ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
        return SQLScriptParser.extractActiveQuery(parserContext, selection.getOffset(), selection.getLength());
    }

    public SQLScriptElement extractQueryAtPos(int currentPos) {
        return parserContext == null ? null : SQLScriptParser.extractQueryAtPos(parserContext, currentPos);
    }

    public SQLScriptElement extractNextQuery(boolean next) {
        if (parserContext == null) {
            return null;
        }
        ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
        int offset = selection.getOffset();
        return SQLScriptParser.extractNextQuery(parserContext, offset, next);
    }

    public List<SQLScriptElement> extractScriptQueries(int startOffset, int length, boolean scriptMode, boolean keepDelimiters, boolean parseParameters) {
        if (parserContext == null) {
            return null;
        }
        return SQLScriptParser.extractScriptQueries(parserContext, startOffset, length, scriptMode, keepDelimiters, parseParameters);
    }

    public SQLCompletionContext getCompletionContext() {
        return completionContext;
    }

    List<SQLQueryParameter> parseQueryParameters(SQLQuery query) {
        if (parserContext == null) {
            return null;
        }
        SQLParserContext context = new SQLParserContext(SQLEditorBase.this, parserContext.getSyntaxManager(), parserContext.getRuleManager(), new Document(query.getText()));
        return SQLScriptParser.parseParameters(context, 0, query.getLength());
    }

    public boolean isDisposed() {
        return
            getSourceViewer() == null ||
                getSourceViewer().getTextWidget() == null ||
                getSourceViewer().getTextWidget().isDisposed();
    }

    @Nullable
    @Override
    public ICommentsSupport getCommentsSupport() {
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
        String[] more = new String[ids.length + 6];
        more[ids.length] = PrefPageSQLEditor.PAGE_ID;
        more[ids.length + 1] = PrefPageSQLExecute.PAGE_ID;
        more[ids.length + 2] = PrefPageSQLCompletion.PAGE_ID;
        more[ids.length + 3] = PrefPageSQLFormat.PAGE_ID;
        more[ids.length + 4] = PrefPageSQLResources.PAGE_ID;
        more[ids.length + 5] = PrefPageSQLTemplates.PAGE_ID;
        System.arraycopy(ids, 0, more, 0, ids.length);
        return more;
    }

    @Override
    public boolean visualizeError(@NotNull DBRProgressMonitor monitor, @NotNull Throwable error) {
        IDocument document = getDocument();
        String text = document == null ? "" : document.get();
        SQLQuery query = new SQLQuery(getDataSource(), text, 0, text.length());
        return scrollCursorToError(monitor, query, error);
    }

    /**
     * Error handling
     */
    boolean scrollCursorToError(@NotNull DBRProgressMonitor monitor, @NotNull SQLQuery query, @NotNull Throwable error) {
        try {
            DBCExecutionContext context = getExecutionContext();
            if (context == null) {
                return false;
            }
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
                            getSelectionProvider().setSelection(new TextSelection(queryStartOffset + pos.position, 0));
                            scrolled = true;
                        }
                    } else {
                        // Line + position
                        IDocument document = getDocument();
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
                            if (errorOffset >= queryStartOffset + queryLength) {
                                // This may happen if error position was incorrectly detected.
                                // E.g. in SQL Server when actual error happened in some stored procedure.
                                errorOffset  = queryStartOffset + queryLength - 1;
                            }
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

    public boolean isFoldingEnabled() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.FOLDING_ENABLED);
    }

    /**
     * Updates the status fields for the given category.
     *
     * @param category the category
     * @since 2.0
     */
    protected void updateStatusField(String category) {
        if (STATS_CATEGORY_SELECTION_STATE.equals(category)) {
            IStatusField field = getStatusField(category);
            if (field != null) {
                StringBuilder txt = new StringBuilder("Sel: ");
                ISelection selection = getSelectionProvider().getSelection();
                if (selection instanceof ITextSelection) {
                    ITextSelection textSelection = (ITextSelection) selection;
                    txt.append(textSelection.getLength()).append(" | ");
                    if (((ITextSelection) selection).getLength() <= 0) {
                        txt.append(0);
                    } else {
                        txt.append(textSelection.getEndLine() - textSelection.getStartLine() + 1);
                    }
                }
                field.setText(txt.toString());
            }
        } else {
            super.updateStatusField(category);
        }
    }

    ////////////////////////////////////////////////////////
    // Brackets

    protected class ShowPreferencesAction extends Action {
        ShowPreferencesAction() {
            super(SQLEditorMessages.editor_sql_preference, DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
        }  //$NON-NLS-1$

        public void run() {
            Shell shell = getSourceViewer().getTextWidget().getShell();
            String[] preferencePages = collectContextMenuPreferencePages();
            if (preferencePages.length > 0 && (shell == null || !shell.isDisposed()))
                PreferencesUtil.createPreferenceDialogOn(shell, preferencePages[0], preferencePages, getEditorInput()).open();
        }
    }

}
