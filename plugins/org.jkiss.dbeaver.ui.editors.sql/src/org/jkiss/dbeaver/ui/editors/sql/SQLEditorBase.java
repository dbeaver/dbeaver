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
package org.jkiss.dbeaver.ui.editors.sql;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.internal.dialogs.PropertyDialog;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.templates.ITemplatesPage;
import org.eclipse.ui.themes.IThemeManager;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.parser.*;
import org.jkiss.dbeaver.model.sql.semantics.SQLDocumentSyntaxContext;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.resultset.ThemeConstants;
import org.jkiss.dbeaver.ui.editors.AbstractStorageEditorInput;
import org.jkiss.dbeaver.ui.editors.BaseTextEditorCommands;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.preferences.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLBackgroundParsingJob;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLEditorOutlinePage;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLEditorSemanticMarkersManager;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLSemanticErrorAnnotation;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLCharacterPairMatcher;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLEditorCompletionContext;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLProblemAnnotation;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleScanner;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesPage;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLSymbolInserter;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SQL Executor
 */
public abstract class SQLEditorBase extends BaseTextEditor implements
    DBPContextProvider,
    IErrorVisualizer,
    DBPPreferenceListener {

    static protected final Log log = Log.getLog(SQLEditorBase.class);
    public static final long MAX_FILE_LENGTH_FOR_RULES = 1024 * 1000 * 2; // 2MB
    private static final int SCROLL_ON_RESIZE_THRESHOLD_PX = 10;

    static final String STATS_CATEGORY_SELECTION_STATE = "SelectionState";

    static {
        // SQL editor preferences. Do this here because it initializes display
        // (that's why we can't run it in prefs initializer classes which run before workbench creation)
        {
            IPreferenceStore editorStore = EditorsUI.getPreferenceStore();
            editorStore.setDefault(SQLPreferenceConstants.MATCHING_BRACKETS, true);
            editorStore.setDefault(SQLPreferenceConstants.MATCHING_BRACKETS_HIGHLIGHT, true);
            //editorStore.setDefault(SQLPreferenceConstants.MATCHING_BRACKETS_COLOR, "128,128,128"); //$NON-NLS-1$

            // Enable "delete spaces as tabs" option by default
            // We use hardcoded constants instead of AbstractDecoratedTextEditorPreferenceConstants.EDITOR_DELETE_SPACES_AS_TABS
            // to allow compile on older Eclipse versions
            editorStore.setDefault("removeSpacesAsTabs", true);
        }
    }

    @NotNull
    private final SQLSyntaxManager syntaxManager;
    @NotNull
    private final SQLRuleScanner ruleScanner;
    @Nullable
    private SQLParserContext parserContext;
    private ProjectionSupport projectionSupport;
    private SQLBackgroundParsingJob backgroundParsingJob;
    private SQLEditorOutlinePage outlinePage;
    private SQLEditorSemanticMarkersManager semanticMarkersManager;

    //private Map<Annotation, Position> curAnnotations;

    //private IAnnotationAccess annotationAccess;
    private boolean hasVerticalRuler = true;
    private SQLTemplatesPage templatesPage;
    private IPropertyChangeListener themeListener;
    private SQLEditorControl editorControl;

    private ICharacterPairMatcher characterPairMatcher;
    private SQLCompletionContext completionContext;
    private SQLOccurrencesHighlighter occurrencesHighlighter;
    private SQLSymbolInserter sqlSymbolInserter;

    private int lastQueryErrorPosition = -1;

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

        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(this);
    }

    @Override
    public void gotoMarker(IMarker marker) {
        try {
            if (marker.getAttribute(SQLSemanticErrorAnnotation.MARKER_ATTRIBUTE_NAME) instanceof SQLSemanticErrorAnnotation annotation) {
                final IAnnotationModel annotationModel = this.getAnnotationModel();
                final TextViewer textViewer = this.getTextViewer();
                if (annotationModel != null && textViewer != null) {
                    Position position = annotationModel.getPosition(annotation);
                    if (!position.isDeleted) {
                        textViewer.setSelectedRange(position.getOffset(), position.getLength());
                        textViewer.revealRange(position.getOffset(), position.getLength());
                    }
                    return;
                }
            }
        } catch (CoreException e) {
            log.error("Error retrieving marker attribute", e);
        }

        super.gotoMarker(marker);
    }

    public SQLDocumentSyntaxContext getSyntaxContext() {
        return backgroundParsingJob == null ? null : backgroundParsingJob.getCurrentContext();
    }

    @Nullable
    public SQLQueryCompletionContext obtainCompletionContext(DBRProgressMonitor monitor, @NotNull Position completionRequestPostion) {
        return backgroundParsingJob == null ? null : backgroundParsingJob.obtainCompletionContext(monitor, completionRequestPostion);
    }

    @Override
    protected void setDocumentProvider(IEditorInput input) {
        if (input instanceof StringEditorInput) {
            if (!(getDocumentProvider() instanceof NonFileDocumentProvider)) {
                IDocumentProvider prov = new NonFileDocumentProvider(this, (StringEditorInput) input);
                setDocumentProvider(prov);
            }
        }
        super.setDocumentProvider(input);
    }

    @Override
    protected boolean isReadOnly() {
        IDocumentProvider provider = getDocumentProvider();
        return provider instanceof IDocumentProviderExtension &&
            ((IDocumentProviderExtension) provider).isReadOnly(getEditorInput());
    }

    public static boolean isBigScript(@Nullable IEditorInput editorInput) {
        if (editorInput != null) {
            File file = EditorUtils.getLocalFileFromInput(editorInput);
            return file != null && file.length() > getBigScriptFileLengthBoundary();
        }
        return false;
    }
    
    static long getBigScriptFileLengthBoundary() {
        return DBWorkbench.getPlatform().getPreferenceStore().getLong(SQLPreferenceConstants.SCRIPT_BIG_FILE_LENGTH_BOUNDARY);
    }
    
    static boolean isReadEmbeddedBinding() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ);
    }

    static boolean isWriteEmbeddedBinding() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_WRITE);
    }

    public boolean isAdvancedHighlightingEnabled() {
        return this.getActivePreferenceStore().getBoolean(SQLModelPreferences.ADVANCED_HIGHLIGHTING_ENABLE);
    }

    public boolean isReadMetadataForQueryAnalysisEnabled() {
        DBPPreferenceStore prefStore = this.getActivePreferenceStore();
        return prefStore.getBoolean(SQLModelPreferences.READ_METADATA_FOR_SEMANTIC_ANALYSIS)
            && !prefStore.getBoolean(ModelPreferences.META_DISABLE_EXTRA_READ);
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
        if (this instanceof DBPDataSourceContainerProvider) {
            DBPDataSourceContainer container = ((DBPDataSourceContainerProvider) this).getDataSourceContainer();
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

    @Nullable
    public SQLRuleManager getRuleManager() {
        if (parserContext == null) {
            return null;
        }
        return parserContext.getRuleManager();
    }

    @NotNull
    public SQLRuleScanner getRuleScanner() {
        return ruleScanner;
    }

    @Nullable
    public IAnnotationModel getAnnotationModel() {
        final ISourceViewer viewer = getSourceViewer();
        return viewer != null ? viewer.getAnnotationModel() : null;
    }

    @Nullable
    public ProjectionAnnotationModel getProjectionAnnotationModel() {
        final ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
        return viewer != null ? viewer.getProjectionAnnotationModel() : null;
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

        ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
        projectionSupport = new ProjectionSupport(
            projectionViewer,
            getAnnotationAccess(),
            getSharedColors());
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error"); //$NON-NLS-1$
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning"); //$NON-NLS-1$
        projectionSupport.install();

        projectionViewer.doOperation(ProjectionViewer.TOGGLE);

        ISourceViewer sourceViewer = getSourceViewer();

        // Symbol inserter
        {
            sqlSymbolInserter = new SQLSymbolInserter(this);

            loadActivePreferenceSettings();

            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(sqlSymbolInserter);
            }
        }

        if (sourceViewer != null) {
            final StyledText widget = sourceViewer.getTextWidget();

            // Context listener
            EditorUtils.trackControlContext(getSite(), widget, SQLEditorContributions.SQL_EDITOR_CONTROL_CONTEXT);

            // Mouse listener that moves cursor upon clicking with the right mouse button
            widget.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    if (e.button != 3) {
                        return;
                    }

                    final StyledText widget = sourceViewer.getTextWidget();
                    final ISelectionProvider selectionProvider = sourceViewer.getSelectionProvider();
                    final ITextSelection selection = (ITextSelection) selectionProvider.getSelection();

                    int widgetOffset = widget.getOffsetAtPoint(new Point(e.x, e.y));
                    
                    if (widgetOffset < 0) {
                        int lineIndex = widget.getLineIndex(e.y);
                        if (lineIndex + 1 >= widget.getLineCount()) {
                            widgetOffset = widget.getCharCount();
                        } else {
                            widgetOffset = widget.getOffsetAtLine(lineIndex + 1) - widget.getLineDelimiter().length();
                        }
                    }
                    
                    if (widgetOffset < 0) {
                        return;
                    }
                    
                    int modelOffset = sourceViewer instanceof ITextViewerExtension5 vext ? vext.widgetOffset2ModelOffset(widgetOffset) : widgetOffset;

                    boolean withinExistingSelection = false;

                    if (selection instanceof IBlockTextSelection) {
                        for (IRegion region : ((IBlockTextSelection) selection).getRegions()) {
                            if (within(region, modelOffset)) {
                                withinExistingSelection = true;
                                break;
                            }
                        }
                    } else {
                        withinExistingSelection = within(new Region(selection.getOffset(), selection.getLength()), modelOffset);
                    }

                    if (!withinExistingSelection) {
                        selectionProvider.setSelection(new TextSelection(modelOffset, 0));
                    }
                }

                private boolean within(@NotNull IRegion region, int index) {
                    return region.getLength() > 0 && index >= region.getOffset() && index < region.getOffset() + region.getLength();
                }
            });

            // A listener that reveals obscured part of the document the cursor was located in before the control was resized
            widget.addControlListener(new ControlAdapter() {
                private int lastHeight;

                @Override
                public void controlResized(ControlEvent e) {
                    final int currentHeight = widget.getSize().y;
                    final int lastHeight = this.lastHeight;
                    this.lastHeight = currentHeight;

                    if (Math.abs(currentHeight - lastHeight) < SCROLL_ON_RESIZE_THRESHOLD_PX) {
                        return;
                    }

                    try {
                        final IDocument document = sourceViewer.getDocument();
                        final int visibleLine = sourceViewer.getBottomIndex();
                        final int currentLine = document.getLineOfOffset(sourceViewer.getSelectedRange().x);

                        if (currentLine > visibleLine) {
                            final int revealToLine = Math.min(document.getNumberOfLines() - 1, currentLine + 1);
                            final int revealToOffset = document.getLineOffset(revealToLine);
                            sourceViewer.revealRange(revealToOffset, 0);
                        }
                    } catch (BadLocationException ignored) {
                    }
                }
            });
        }
    }

    protected void loadActivePreferenceSettings() {
        DBPPreferenceStore preferenceStore = getActivePreferenceStore();
        boolean closeSingleQuotes = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES);
        boolean closeDoubleQuotes = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES);
        boolean closeBrackets = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS);

        sqlSymbolInserter.setCloseSingleQuotesEnabled(closeSingleQuotes);
        sqlSymbolInserter.setCloseDoubleQuotesEnabled(closeDoubleQuotes);
        sqlSymbolInserter.setCloseBracketsEnabled(closeBrackets);
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
        return true;
    }

    @Override
    protected IVerticalRuler createVerticalRuler() {
        return hasVerticalRuler ? super.createVerticalRuler() : new VerticalRuler(0);
    }

    protected void setHasVerticalRuler(boolean hasVerticalRuler) {
        this.hasVerticalRuler = hasVerticalRuler;
    }

    protected ISharedTextColors getSharedColors() {
        return UIUtils.getSharedTextColors();
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        if (getDocumentProvider() instanceof NonFileDocumentProvider) {
            setDocumentProvider((IDocumentProvider) null);
        }

        handleInputChange(input);

        final IFile file = GeneralUtils.adapt(input, IFile.class);
        if (file != null && SQLEditorUtils.isNewScriptFile(file)) {
            // Move cursor to the end of the file past script template
            UIUtils.asyncExec(() -> selectAndReveal(Integer.MAX_VALUE, 0));
        }

        super.doSetInput(input);
    }

    @Override
    public void doSave(IProgressMonitor progressMonitor) {
        try {
            super.doSave(progressMonitor);
        } catch (Exception e) {
            log.error("Error saving SQL editor");
        }

        handleInputChange(getEditorInput());
    }

    protected void doTextEditorSave(DBRProgressMonitor monitor) {
        super.doSave(monitor.getNestedMonitor());
    }

    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        fAnnotationAccess = getAnnotationAccess();
        fOverviewRuler = createOverviewRuler(getSharedColors());

        SQLEditorSourceViewer sourceViewer = createSourceViewer(parent, ruler, styles, fOverviewRuler);

        getSourceViewerDecorationSupport(sourceViewer);

        SQLMatchingCharacterPainter matchPainter = new SQLMatchingCharacterPainter(sourceViewer, characterPairMatcher);
        matchPainter.setColor(getSharedColors().getColor(
            PreferenceConverter.getColor(getPreferenceStore(), "writeOccurrenceIndicationColor")));
        matchPainter.setHighlightCharacterAtCaretLocation(true);
        sourceViewer.addPainter(matchPainter);

        return sourceViewer;
    }

    protected SourceViewerDecorationSupport getSourceViewerDecorationSupport(ISourceViewer viewer) {
        if (fSourceViewerDecorationSupport == null) {
            fSourceViewerDecorationSupport = new SQLSourceViewerDecorationSupport(viewer, getOverviewRuler(), getAnnotationAccess(), getSharedColors());
            configureSourceViewerDecorationSupport(fSourceViewerDecorationSupport);
        }
        return fSourceViewerDecorationSupport;
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

/*
        support.setCharacterPairMatcher(characterPairMatcher);
        support.setMatchingCharacterPainterPreferenceKeys(
            SQLPreferenceConstants.MATCHING_BRACKETS,
            SQLPreferenceConstants.MATCHING_BRACKETS_COLOR,
            SQLPreferenceConstants.MATCHING_BRACKETS_HIGHLIGHT,
            null);//SQLPreferenceConstants.MATCHING_BRACKETS_HIGHLIGHT);
*/

        super.configureSourceViewerDecorationSupport(support);

        if (UIStyles.isDarkHighContrastTheme()) {
            support.setCursorLinePainterPreferenceKeys(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE, ThemeConstants.COLOR_SQL_RESULT_LINES_SELECTED);
        }
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
            styles,
            this::getActivePreferenceStore
        );
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
        } else if (IContentOutlinePage.class.equals(required)) {
            return (T) getOverviewOutlinePage();
        }
    
        return super.getAdapter(required);
    }

    @NotNull
    public IContentOutlinePage getOverviewOutlinePage() {
        if (this.getSyntaxContext() == null) {
            this.reloadSyntaxRules();
        }
        if ((outlinePage == null || outlinePage.getControl() == null || outlinePage.getControl().isDisposed())) {
            outlinePage = new SQLEditorOutlinePage(this);
        }
        return outlinePage;
    }

    public SQLTemplatesPage getTemplatesPage() {
        if (templatesPage == null)
            templatesPage = new SQLTemplatesPage(this);
        return templatesPage;
    }

    @Override
    public void dispose() {
        DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(this);
        if (this.semanticMarkersManager != null) {
            this.semanticMarkersManager.dispose();
        }
        if (this.backgroundParsingJob != null) {
            this.backgroundParsingJob.dispose();
        }
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

        clearProblems(null);

        if (themeListener != null) {
            PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeListener);
            themeListener = null;
        }

        SQLEditorSourceViewerConfiguration viewerConfiguration = getViewerConfiguration();
        if (viewerConfiguration != null) {
            viewerConfiguration.saveFoldingState();
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

        SQLEditorCustomActions.registerCustomActions(this);
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

        addAction(menu, GROUP_SQL_EXTRAS, SQLEditorContributor.ACTION_CONTENT_ASSIST_PROPOSAL);
        addAction(menu, GROUP_SQL_EXTRAS, SQLEditorContributor.ACTION_CONTENT_ASSIST_TIP);
        addAction(menu, GROUP_SQL_EXTRAS, SQLEditorContributor.ACTION_CONTENT_ASSIST_INFORMATION);
        menu.insertBefore(ITextEditorActionConstants.GROUP_COPY, ActionUtils.makeCommandContribution(getSite(), SQLEditorCommands.CMD_NAVIGATE_OBJECT));

        TextViewer textViewer = getTextViewer();
        if (!isReadOnly() && textViewer != null && textViewer.isEditable()) {
            MenuManager formatMenu = new MenuManager(SQLEditorMessages.sql_editor_menu_format, "format");
            IAction formatAction = getAction(SQLEditorContributor.ACTION_CONTENT_FORMAT_PROPOSAL);
            if (formatAction != null) {
                formatMenu.add(formatAction);
            }
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.morph.delimited.list"));
            formatMenu.add(getAction(ITextEditorActionConstants.UPPER_CASE));
            formatMenu.add(getAction(ITextEditorActionConstants.LOWER_CASE));
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.trim.spaces"));
            formatMenu.add(new Separator());
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.word.wrap"));
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.comment.single"));
            formatMenu.add(ActionUtils.makeCommandContribution(getSite(), "org.jkiss.dbeaver.ui.editors.sql.comment.multi"));
            menu.insertAfter(GROUP_SQL_ADDITIONS, formatMenu);
        }

        //menu.remove(IWorkbenchActionConstants.MB_ADDITIONS);
    }

    public void reloadSyntaxRules() {
        if (SQLEditorUtils.isSQLSyntaxParserApplied(this.getEditorInput()) && this.isAdvancedHighlightingEnabled()) {
            if (this.backgroundParsingJob == null) {
                this.backgroundParsingJob = new SQLBackgroundParsingJob(this);
            }
            if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED) && this.semanticMarkersManager == null) {
                this.semanticMarkersManager = new SQLEditorSemanticMarkersManager(this);
            }
        } else {
            if (this.backgroundParsingJob != null) {
                this.backgroundParsingJob.dispose();
                this.backgroundParsingJob = null;
            }
        }
        if (this.semanticMarkersManager != null && (
            this.backgroundParsingJob == null ||
            !this.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED)
        )) {
            this.semanticMarkersManager.dispose();
            this.semanticMarkersManager = null;
        }

        // Refresh syntax
        SQLDialect dialect = getSQLDialect();
        IDocument document = getDocument();
        syntaxManager.init(dialect, getActivePreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(getDataSourceContainerForSyntaxRuleReloading(), !SQLEditorUtils.isSQLSyntaxParserApplied(getEditorInput()));
        ruleScanner.refreshRules(getDataSourceContainerForSyntaxRuleReloading(), ruleManager, this);
        if (getDataSource() != null) {
            parserContext = new SQLParserContext(getDataSource(), syntaxManager, ruleManager, document != null ? document : new Document());
        } else {
            parserContext = new SQLParserContext(getDataSourceContainerForSyntaxRuleReloading(), syntaxManager, ruleManager, document != null ? document : new Document());
        }

        if (document instanceof IDocumentExtension3) {
            IDocumentPartitioner partitioner = new FastPartitioner(
                new SQLPartitionScanner(getDataSource(), dialect, ruleManager),
                SQLParserPartitions.SQL_CONTENT_TYPES);
            partitioner.connect(document);
            try {
                ((IDocumentExtension3) document).setDocumentPartitioner(SQLParserPartitions.SQL_PARTITIONING, partitioner);
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

        if (this.backgroundParsingJob != null) {
            this.backgroundParsingJob.setup();
        }
        if (this.semanticMarkersManager != null) {
            this.semanticMarkersManager.refresh();
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
        if (selection instanceof IBlockTextSelection) {
            return SQLScriptParser.extractActiveQuery(parserContext, ((IBlockTextSelection) selection).getRegions());
        } else {
            return SQLScriptParser.extractActiveQuery(parserContext, selection.getOffset(), selection.getLength());
        }
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

    public void setCompletionContext(SQLCompletionContext completionContext) {
        this.completionContext = completionContext;
    }

    List<SQLQueryParameter> parseQueryParameters(SQLQuery query) {
        if (parserContext == null) {
            return null;
        }
        SQLParserContext context;
        if (getDataSource() != null) {
            context = new SQLParserContext(getDataSource(), parserContext.getSyntaxManager(), parserContext.getRuleManager(), new Document(query.getText()));
        } else {
            context = new SQLParserContext(getDataSourceContainerForSyntaxRuleReloading(), parserContext.getSyntaxManager(), parserContext.getRuleManager(), new Document(query.getText()));
        }
        return SQLScriptParser.parseParametersAndVariables(context, 0, query.getLength());
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
        return ArrayUtils.concatArrays(ids, new String[] {
            PrefPageSQLEditor.PAGE_ID,
            PrefPageSQLExecute.PAGE_ID,
            PrefPageSQLCodeEditing.PAGE_ID,
            PrefPageSQLCompletion.PAGE_ID,
            PrefPageSQLFormat.PAGE_ID,
            PrefPageSQLResources.PAGE_ID,
            PrefPageSQLTemplates.PAGE_ID
        });
    }

    @Override
    public boolean visualizeError(@NotNull DBRProgressMonitor monitor, @NotNull Throwable error) {
        IDocument document = getDocument();
        String text = document == null ? "" : document.get();
        SQLQuery query = new SQLQuery(getDataSource(), text, 0, text.length());
        return visualizeQueryErrors(monitor, query, error, null);
    }

    /**
     * Error handling
     */
    boolean visualizeQueryErrors(@NotNull DBRProgressMonitor monitor, @NotNull SQLQuery query, @NotNull Throwable error, @Nullable SQLQuery originalQuery) {
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

                    for (int index = 0; index < positions.length; index++) {
                        DBPErrorAssistant.ErrorPosition pos = positions[index];
                        int errorOffset = 0;
                        if (pos.line < 0) {
                            if (pos.position >= 0) {
                                // Only position
                                errorOffset = queryStartOffset + pos.position;
                                final SQLWordPartDetector detector = new SQLWordPartDetector(getDocument(), getSyntaxManager(), errorOffset);
                                final int length = detector.getLength() > 0
                                    ? detector.getLength()
                                    : queryLength - pos.position;
                                if (addProblem(GeneralUtils.getFirstMessage(error), new Position(errorOffset, length))) {
                                    scrolled = true;
                                } else if (index == 0) {
                                    getSelectionProvider().setSelection(new TextSelection(errorOffset, 0));
                                    scrolled = true;
                                }
                                if (originalQuery != null) {
                                    IDocument document = getDocument();
                                    if (document != null) {
                                        int errorLine = document.getLineOfOffset(errorOffset);
                                        if (errorLine >= 0) {
                                            // Start position of the getLineOfOffset method is 0 but SQL Editor lines start from the 1
                                            pos.line = errorLine + 1;
                                        }
                                    }
                                }
                            }
                        } else {
                            // Line + position
                            IDocument document = getDocument();
                            if (document != null) {
                                int startLine = document.getLineOfOffset(queryStartOffset);
                                errorOffset = document.getLineOffset(startLine + pos.line);
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
                                    errorOffset = queryStartOffset + queryLength - 1;
                                }
                                // Try to add a problem marker, otherwise select text containing error if it's the first error
                                if (addProblem(GeneralUtils.getFirstMessage(error), new Position(errorOffset, errorLength))) {
                                    scrolled = true;
                                } else if (index == 0) {
                                    getSelectionProvider().setSelection(new TextSelection(errorOffset, errorLength));
                                    scrolled = true;
                                }
                            }
                        }
                        if (originalQuery != null) {
                            originalQuery.addExtraErrorMessage("\n" + SQLEditorMessages.sql_editor_error_position + ":" + (pos.line > 0 ? " line: " + pos.line : "") +
                                (pos.position > 0 ? " pos: " + pos.position : ""));
                            if (index == 0) {
                                lastQueryErrorPosition = errorOffset;
                            }
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

    protected boolean addProblem(@Nullable String message, @NotNull Position position) {
        if (!getActivePreferenceStore().getBoolean(SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED)) {
            return false;
        }

        final IResource resource = GeneralUtils.adapt(getEditorInput(), IResource.class);
        final IAnnotationModel annotationModel = getAnnotationModel();

        if (resource == null || annotationModel == null) {
            return false;
        }

        try {
            final IMarker marker = resource.createMarker(SQLProblemAnnotation.MARKER_TYPE);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, message);
            marker.setAttribute(IMarker.TRANSIENT, true);
            // For some reason, these two cause the annotation to de-sync from this marker:
            // MarkerUtilities.setCharStart(marker, position.offset);
            // MarkerUtilities.setCharEnd(marker, position.offset + position.length);
            annotationModel.addAnnotation(new SQLProblemAnnotation(marker), position);
        } catch (CoreException e) {
            log.error("Error creating problem marker", e);
        }

        // We don't want to show this view every time because it makes everyone mad.
        // But there's a catch: the user can't remove the annotation outside of this
        // view, and also can't open this view without knowing about it in advance.
        // Should we display a confirmation dialog with "Remember my choice" option?

        /*
        try {
            UIUtils.getActiveWorkbenchWindow().getActivePage().showView(IPageLayout.ID_PROBLEM_VIEW, null, IWorkbenchPage.VIEW_VISIBLE);
        } catch (PartInitException e) {
            log.debug("Error opening problem view", e);
        }
        */

        return true;
    }

    protected void clearProblems(@Nullable SQLQuery query) {
        if (query == null) {
            final IResource resource = GeneralUtils.adapt(getEditorInput(), IResource.class);

            if (resource != null && resource.exists()) {
                try {
                    resource.deleteMarkers(SQLProblemAnnotation.MARKER_TYPE, false, IResource.DEPTH_ONE);
                } catch (CoreException e) {
                    log.error("Error deleting problem markers", e);
                }
            }
        } else {
            final IAnnotationModel annotationModel = getAnnotationModel();

            if (annotationModel != null) {
                for (Iterator<Annotation> it = annotationModel.getAnnotationIterator(); it.hasNext(); ) {
                    final Annotation annotation = it.next();

                    if (annotation instanceof SQLProblemAnnotation) {
                        final Position position = annotationModel.getPosition(annotation);

                        if (position.overlapsWith(query.getOffset(), query.getLength()) ||
                            (!position.isDeleted() && query.getOffset() + query.getLength() == position.getOffset() + position.getLength())
                        ) {
                            // We need to delete markers though. Maybe only when there is no line position?
                            try {
                                ((SQLProblemAnnotation) annotation).getMarker().delete();
                            } catch (CoreException e) {
                                log.error("Error deleting problem marker", e);
                            }
                            annotationModel.removeAnnotation(annotation);
                        }
                    }
                }
            }
        }
    }

    public boolean isFoldingEnabled() {
        return SQLEditorUtils.isSQLSyntaxParserApplied(getEditorInput())
            && DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.FOLDING_ENABLED);
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
                if (selection instanceof ITextSelection textSelection) {
                    txt.append(textSelection.getLength()).append(" | ");
                    if (textSelection.getLength() <= 0) {
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

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        switch (event.getProperty()) {
            case SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES:
                sqlSymbolInserter.setCloseSingleQuotesEnabled(CommonUtils.toBoolean(event.getNewValue()));
                return;
            case SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES:
                sqlSymbolInserter.setCloseDoubleQuotesEnabled(CommonUtils.toBoolean(event.getNewValue()));
                return;
            case SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS:
                sqlSymbolInserter.setCloseBracketsEnabled(CommonUtils.toBoolean(event.getNewValue()));
                return;
            case SQLPreferenceConstants.FOLDING_ENABLED: {
                final ProjectionAnnotationModel annotationModel = this.getProjectionAnnotationModel();
                if (annotationModel != null) {
                    annotationModel.removeAllAnnotations();
                    this.reloadSourceViewerConfiguration();
                }
                return;
            }
            case SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED:
                this.clearProblems(null);
                return;
            case SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR:
            case SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION:
                occurrencesHighlighter.updateInput(this.getEditorInput());
            case SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS:
            case SQLPreferenceConstants.SQL_FORMAT_ACTIVE_QUERY:
            case SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE:
            case ModelPreferences.META_DISABLE_EXTRA_READ:
            case SQLPreferenceConstants.READ_METADATA_FOR_SEMANTIC_ANALYSIS:
            case ModelPreferences.SQL_FORMAT_KEYWORD_CASE:
            case ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA:
            case ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET:
            case ModelPreferences.SQL_FORMAT_INSERT_DELIMITERS_IN_EMPTY_LINES:
            case AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH:
            case AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS:
                this.reloadSyntaxRules();
                return;
            case SQLPreferenceConstants.ADVANCED_HIGHLIGHTING_ENABLE:
                this.reloadSourceViewerConfiguration();
                this.reloadSyntaxRules();
                if (this.outlinePage != null) {
                    this.outlinePage.refresh();
                }
                return;
        }
    }

    public void refreshAdvancedServices() {
        if (this.outlinePage != null) {
            this.outlinePage.refresh();
        }
        if (this.semanticMarkersManager != null) {
            this.semanticMarkersManager.refresh();
        }
    }
    
    private void reloadSourceViewerConfiguration() {
        SourceViewerConfiguration configuration = this.getSourceViewerConfiguration();
        SQLEditorSourceViewer sourceViewer = (SQLEditorSourceViewer) this.getSourceViewer();
        sourceViewer.unconfigure();
        sourceViewer.configure(configuration);
    }
    
    void setLastQueryErrorPosition(int lastQueryErrorPosition) {
        this.lastQueryErrorPosition = lastQueryErrorPosition;
    }

    int getLastQueryErrorPosition() {
        return lastQueryErrorPosition;
    }

    protected boolean isNavigationTarget(Annotation annotation) {
        if (annotation instanceof SpellingAnnotation) {
            // Iterate over spelling problems only if we do not have problems
            for (Iterator<Annotation> i = getAnnotationModel().getAnnotationIterator(); i.hasNext(); ) {
                Annotation anno = i.next();
                if (anno instanceof SQLProblemAnnotation) {
                    return false;
                }
            }
            return true;
        }
        return super.isNavigationTarget(annotation);
    }

    @Nullable
    protected DBPDataSourceContainer getDataSourceContainerForSyntaxRuleReloading() {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? null : dataSource.getContainer();
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
            if (preferencePages.length > 0 && (shell == null || !shell.isDisposed())) {
                PropertyDialog.createDialogOn(shell, null, new StructuredSelection(getEditorInput())).open();
                //PreferencesUtil.createPreferenceDialogOn(shell, preferencePages[0], preferencePages, getEditorInput()).open();
            }
        }
    }

    private static class NonFileDocumentProvider extends SQLObjectDocumentProvider {

        private final StringEditorInput editorInput;

        public NonFileDocumentProvider(SQLEditorBase editor, StringEditorInput editorInput) {
            super(editor);
            this.editorInput = editorInput;
        }

        @Override
        protected String loadSourceText(DBRProgressMonitor monitor) throws DBException {
            return editorInput.getBuffer().toString();
        }

        @Override
        protected void saveSourceText(DBRProgressMonitor monitor, String text) throws DBException {
            editorInput.setText(text);
        }

        @Override
        public boolean isReadOnly(Object element) {
            if (element instanceof AbstractStorageEditorInput) {
                return ((AbstractStorageEditorInput) element).isReadOnly();
            }
            return editorInput.isReadOnly();
        }
    }

}
