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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.hyperlink.*;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.indent.SQLAutoIndentStrategy;
import org.jkiss.dbeaver.ui.editors.sql.indent.SQLCommentAutoIndentStrategy;
import org.jkiss.dbeaver.ui.editors.sql.indent.SQLStringAutoIndentStrategy;
import org.jkiss.dbeaver.ui.editors.sql.syntax.*;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLAnnotationHover;
import org.jkiss.utils.ArrayUtils;


/**
 * This class defines the editor add-ons; content assist, content formatter,
 * highlighting, auto-indent strategy, double click strategy.
 */
public class SQLEditorSourceViewerConfiguration extends TextSourceViewerConfiguration {
    private static final Log log = Log.getLog(SQLEditorSourceViewerConfiguration.class);

    @Nullable
    private final SQLReconcilingStrategy reconcilingStrategy;

    /**
     * The editor with which this configuration is associated.
     */
    private final SQLEditorBase editor;
    private final SQLRuleScanner ruleManager;
    private final SQLContextInformer contextInformer;
    private final IPreferenceStore preferenceStore;

    private IContentAssistProcessor completionProcessor;
    private SQLHyperlinkDetector hyperlinkDetector;

    /**
     * This class implements a single token scanner.
     */
    static class SingleTokenScanner extends BufferedRuleBasedScanner {
        public SingleTokenScanner(TextAttribute attribute) {
            setDefaultReturnToken(new Token(attribute));
        }
    }

    /**
     * Constructs an instance of this class with the given SQLEditor to
     * configure.
     *
     * @param editor the SQLEditor to configure
     */
    public SQLEditorSourceViewerConfiguration(SQLEditorBase editor, IPreferenceStore preferenceStore) {
        this(editor, preferenceStore, new SQLReconcilingStrategy(editor));
    }

    public SQLEditorSourceViewerConfiguration(
        SQLEditorBase editor,
        IPreferenceStore preferenceStore,
        @Nullable SQLReconcilingStrategy reconcilingStrategy
    ) {
        super(preferenceStore);
        this.editor = editor;
        this.preferenceStore = preferenceStore;
        this.ruleManager = editor.getRuleScanner();
        this.contextInformer = new SQLContextInformer(editor, editor.getSyntaxManager());
        this.hyperlinkDetector = new SQLHyperlinkDetector(this.contextInformer);
        this.reconcilingStrategy = reconcilingStrategy;
    }

    public IPreferenceStore getPreferenceStore() {
        return preferenceStore;
    }

    public SQLContextInformer getContextInformer() {
        return contextInformer;
    }

    public SQLHyperlinkDetector getHyperlinkDetector() {
        return hyperlinkDetector;
    }

    @Override
    public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
        return new TextViewerUndoManager(200);
    }

    /**
     * Returns the annotation hover which will provide the information to be
     * shown in a hover popup window when requested for the given
     * source viewer.
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAnnotationHover(org.eclipse.jface.text.source.ISourceViewer)
     */
    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new SQLAnnotationHover(getSQLEditor());
    }

    @Nullable
    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
            return new IAutoEditStrategy[]{
                new SQLAutoIndentStrategy(SQLParserPartitions.SQL_PARTITIONING, sourceViewer, editor.getSyntaxManager())};
        } else if (SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT.equals(contentType) || SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT.equals(contentType)) {
            return new IAutoEditStrategy[]{new SQLCommentAutoIndentStrategy(SQLParserPartitions.SQL_PARTITIONING)};
        } else if (SQLParserPartitions.CONTENT_TYPE_SQL_STRING.equals(contentType)) {
            return new IAutoEditStrategy[]{new SQLStringAutoIndentStrategy(SQLParserPartitions.CONTENT_TYPE_SQL_STRING)};
        }
        return new IAutoEditStrategy[0];
    }

    /**
     * Returns the configured partitioning for the given source viewer. The partitioning is
     * used when the querying content types from the source viewer's input document.
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
     */
    @Override
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
        return SQLParserPartitions.SQL_PARTITIONING;
    }

    /**
     * Creates, initializes, and returns the ContentAssistant to use with this editor.
     */
    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        try {
            return createContentAssistant(sourceViewer);
        } catch (Throwable e) {
            log.error("Error creating content assistant", e);
            return null;
        }
    }

    @NotNull
    private SQLContentAssistant createContentAssistant(ISourceViewer sourceViewer) {
        DBPPreferenceStore store = editor.getActivePreferenceStore();

        final DBPPreferenceStore configStore = store;

        final SQLContentAssistant assistant = new SQLContentAssistant(editor);

        assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        // Set content assist processors for various content types.
        if (completionProcessor == null) {
            this.completionProcessor = new SQLCompletionProcessor(editor);
        }
        try {
            assistant.addContentAssistProcessor(completionProcessor, IDocument.DEFAULT_CONTENT_TYPE);
            assistant.addContentAssistProcessor(completionProcessor, SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED);
            assistant.addContentAssistProcessor(completionProcessor, SQLParserPartitions.CONTENT_TYPE_SQL_STRING);
        } catch (Throwable e) {
            // addContentAssistProcessor API was added in 4.12
            // Let's support older Eclipse versions
            assistant.setContentAssistProcessor(completionProcessor, IDocument.DEFAULT_CONTENT_TYPE);
        }

        // Configure how content assist information will appear.
        configureContentAssistant(store, assistant);
        assistant.setSorter(new SQLCompletionSorter(editor));

        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

        //In the future, a preference page will be added to customize foreground and background.
        Color foreground = new Color(UIUtils.getDisplay(), 0, 0, 0);
        Color background = new Color(UIUtils.getDisplay(), 255, 255, 255);

        assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
        assistant.setContextInformationPopupForeground(foreground);
        assistant.setContextInformationPopupBackground(background);
        //Set auto insert mode.
        assistant.enableAutoInsert(store.getBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
        assistant.setShowEmptyList(true);

        final DBPPreferenceListener prefListener = event -> {
            switch (event.getProperty()) {
                case SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION:
                    assistant.enableAutoActivation(
                        SQLEditorUtils.isSQLSyntaxParserApplied(editor.getEditorInput())
                        && configStore.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION)
                    );
                    break;
                case SQLPreferenceConstants.AUTO_ACTIVATION_DELAY:
                    assistant.setAutoActivationDelay(configStore.getInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
                    break;
                case SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO:
                    assistant.enableAutoInsert(configStore.getBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
                    break;
            }
        };

        ((SQLCompletionProcessor) completionProcessor).initAssistant(assistant);

        configStore.addPropertyChangeListener(prefListener);
        editor.getTextViewer().getControl().addDisposeListener(
            e -> configStore.removePropertyChangeListener(prefListener));

        return assistant;
    }

    private void configureContentAssistant(DBPPreferenceStore store, SQLContentAssistant assistant) {
        assistant.enableAutoActivation(store.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION));
        assistant.setAutoActivationDelay(store.getInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
        assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
    }

    @Override
    public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
        return parent -> new DefaultInformationControl(parent, false);
    }

    /**
     * Creates, configures, and returns the ContentFormatter to use.
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentFormatter(ISourceViewer)
     */
    @Override
    public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
        SQLContentFormatter formatter = new SQLContentFormatter(editor);
        formatter.setDocumentPartitioning(SQLParserPartitions.SQL_PARTITIONING);

        IFormattingStrategy formattingStrategy = new SQLFormattingStrategy(sourceViewer, this, editor.getSyntaxManager());
        for (String ct : SQLParserPartitions.SQL_CONTENT_TYPES) {
            formatter.setFormattingStrategy(formattingStrategy, ct);
        }

        formatter.enablePartitionAwareFormatting(false);

        return formatter;
    }

    /**
     * Returns the double-click strategy ready to be used in this viewer when double clicking
     * onto text of the given content type.  (Note: the same double-click strategy
     * object is used for all content types.)
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getDoubleClickStrategy(ISourceViewer, String)
     */
    @Override
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
        return new SQLDoubleClickStrategy();
    }

    /**
     * Creates, configures, and returns a presentation reconciler to help with
     * document changes.
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
     */
    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        // Create a presentation reconciler to handle handle document changes.
        PresentationReconciler reconciler = new PresentationReconciler();
        String docPartitioning = getConfiguredDocumentPartitioning(sourceViewer);
        reconciler.setDocumentPartitioning(docPartitioning);

        // Add a "damager-repairer" for changes in default text (SQL code).
        addContentTypeDamageRepairer(reconciler, IDocument.DEFAULT_CONTENT_TYPE);
        
        // rule for multiline comments
        // We just need a scanner that does nothing but returns a token with
        // the corresponding text attributes
        addContentTypeDamageRepairer(reconciler, SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT, SQLConstants.CONFIG_COLOR_COMMENT);
        // Add a "damager-repairer" for changes within one-line SQL comments.
        addContentTypeDamageRepairer(reconciler, SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT, SQLConstants.CONFIG_COLOR_COMMENT);
        SQLEditorBase sqlEditor = this.getSQLEditor();
        if (SQLEditorUtils.isSQLSyntaxParserApplied(sqlEditor.getEditorInput())) {
            // Add a "damager-repairer" for changes within string literals.
            addContentTypeDamageRepairer(reconciler, SQLParserPartitions.CONTENT_TYPE_SQL_STRING);
            if (sqlEditor.isAdvancedHighlightingEnabled()) {
                // Add a "damager-repairer" for changes within quoted literals.
                addContentTypeDamageRepairer(reconciler, SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED);
            } else {
                // Add a "damager-repairer" for changes within quoted literals.
                addContentTypeDamageRepairer(reconciler, SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED, SQLConstants.CONFIG_COLOR_DATATYPE);
            }
        } else {
            // Add a "damager-repairer" for changes within string literals.
            addContentTypeDamageRepairer(reconciler, SQLParserPartitions.CONTENT_TYPE_SQL_STRING, SQLConstants.CONFIG_COLOR_STRING);
            // Add a "damager-repairer" for changes within quoted literals.
            addContentTypeDamageRepairer(reconciler, SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED, SQLConstants.CONFIG_COLOR_DATATYPE);
        }
        // Add a "damager-repairer" for changes within control commands.
        addContentTypeDamageRepairer(reconciler, SQLParserPartitions.CONTENT_TYPE_SQL_CONTROL);

        return reconciler;
    }

    private void addContentTypeDamageRepairer(@NotNull PresentationReconciler reconciler, @NotNull String contentType) {
        addContentTypeDamageRepairer(reconciler, contentType, ruleManager);
    }
    
    private void addContentTypeDamageRepairer(
        @NotNull PresentationReconciler reconciler,
        @NotNull String contentType,
        @NotNull String colorId
    ) {
        Color color = ruleManager.getColor(colorId);
        if (UIStyles.isDarkHighContrastTheme()) {
            color = UIUtils.getInvertedColor(color);
        }
        addContentTypeDamageRepairer(reconciler, contentType, new SingleTokenScanner(new TextAttribute(color)));
    }

    private void addContentTypeDamageRepairer(
        @NotNull PresentationReconciler reconciler,
        @NotNull String contentType,
        @NotNull ITokenScanner scanner
    ) {
        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
        reconciler.setDamager(dr, contentType);
        reconciler.setRepairer(dr, contentType);
    }

    /**
     * Returns the SQLEditor associated with this object.
     *
     * @return the SQLEditor that this object configures
     */
    public SQLEditorBase getSQLEditor() {
        return editor;
    }

    /**
     * Returns the text hover which will provide the information to be shown
     * in a text hover popup window when requested for the given source viewer and
     * the given content type.
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTextHover(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
     */
    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        //return new BestMatchHover(this.getSQLEditor());
        return new SQLAnnotationHover(this.getSQLEditor());
    }

    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return SQLParserPartitions.SQL_CONTENT_TYPES;
    }

    @Override
    public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
        SQLDialect dialect = editor.getSQLDialect();
        return ArrayUtils.add(String.class, dialect.getSingleLineComments(), "");
    }

    @Override
    public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
        InformationPresenter presenter = new InformationPresenter(getInformationControlCreator(sourceViewer));
        presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        // Register information provider
        IInformationProvider provider = new SQLInformationProvider(getSQLEditor(), contextInformer);
        String[] contentTypes = getConfiguredContentTypes(sourceViewer);
        for (String contentType : contentTypes) {
            presenter.setInformationProvider(provider, contentType);
        }

        presenter.setSizeConstraints(60, 10, true, true);
        return presenter;
    }

    @Override
    public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
        return new MultipleHyperlinkPresenter(editor.getViewerConfiguration().getPreferenceStore()) {

        };
    }

    @Nullable
    @Override
    public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
        if (sourceViewer == null) {
            return null;
        }

        return new IHyperlinkDetector[]{
            hyperlinkDetector,
            new URLHyperlinkDetector()};
    }

    void onDataSourceChange() {
        contextInformer.refresh(editor.getSyntaxManager());
        ((IHyperlinkDetectorExtension) hyperlinkDetector).dispose();
        if (reconcilingStrategy != null) {
            reconcilingStrategy.onDataSourceChange();
        }
    }

    void saveFoldingState() {
        if (reconcilingStrategy != null) {
            reconcilingStrategy.saveState();
        }
    }

    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        if (reconcilingStrategy == null) {
            return null;
        }
        return new MonoReconciler(reconcilingStrategy, true);
    }
}
