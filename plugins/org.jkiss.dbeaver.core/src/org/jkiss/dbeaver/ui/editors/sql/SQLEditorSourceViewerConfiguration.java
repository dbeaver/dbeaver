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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.hyperlink.*;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPPreferenceListener;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.ui.editors.sql.indent.SQLAutoIndentStrategy;
import org.jkiss.dbeaver.ui.editors.sql.indent.SQLCommentAutoIndentStrategy;
import org.jkiss.dbeaver.ui.editors.sql.indent.SQLStringAutoIndentStrategy;
import org.jkiss.dbeaver.ui.editors.sql.syntax.*;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLAnnotationHover;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLInformationProvider;


/**
 * This class defines the editor add-ons; content assist, content formatter,
 * highlighting, auto-indent strategy, double click strategy.
 */
public class SQLEditorSourceViewerConfiguration extends SourceViewerConfiguration {
    /**
     * The editor with which this configuration is associated.
     */
    private SQLEditorBase editor;
    private SQLRuleManager ruleManager;

    private IContentAssistProcessor completionProcessor;
    private IHyperlinkDetector hyperlinkDetector;

    /**
     * This class implements a single token scanner.
     */
    static class SingleTokenScanner extends BufferedRuleBasedScanner {
        public SingleTokenScanner(TextAttribute attribute)
        {
            setDefaultReturnToken(new Token(attribute));
        }
    }

    /**
     * Constructs an instance of this class with the given SQLEditor to
     * configure.
     *
     * @param editor the SQLEditor to configure
     */
    public SQLEditorSourceViewerConfiguration(
        SQLEditorBase editor)
    {
        this.editor = editor;
        this.ruleManager = editor.getRuleManager();
        this.completionProcessor = new SQLCompletionProcessor(editor);
        this.hyperlinkDetector = new SQLHyperlinkDetector(editor, editor.getSyntaxManager());
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
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer)
    {
        return new SQLAnnotationHover(getSQLEditor());
    }

    /*
     * @see SourceViewerConfiguration#getAutoIndentStrategy(ISourceViewer, String)
     */

    @Nullable
    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType)
    {
        if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
            return new IAutoEditStrategy[] { new SQLAutoIndentStrategy(SQLPartitionScanner.SQL_PARTITIONING) } ;
        } else if (SQLPartitionScanner.SQL_COMMENT.equals(contentType) || SQLPartitionScanner.SQL_MULTILINE_COMMENT.equals(contentType)) {
            return new IAutoEditStrategy[] { new SQLCommentAutoIndentStrategy(SQLPartitionScanner.SQL_PARTITIONING) } ;
        } else if (SQLPartitionScanner.SQL_STRING.equals(contentType)) {
            return new IAutoEditStrategy[] { new SQLStringAutoIndentStrategy(SQLPartitionScanner.SQL_STRING) };
        }
        return null;
    }

    /**
     * Returns the configured partitioning for the given source viewer. The partitioning is
     * used when the querying content types from the source viewer's input document.
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
     */
    @Override
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer)
    {
        return SQLPartitionScanner.SQL_PARTITIONING;
    }

    /**
     * Creates, initializes, and returns the ContentAssistant to use with this editor.
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(ISourceViewer)
     */
    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer)
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        final DBPDataSource dataSource = editor.getDataSource();
        if (dataSource != null) {
            store = dataSource.getContainer().getPreferenceStore();
        }

        final DBPPreferenceStore configStore = store;

        final ContentAssistant assistant = new ContentAssistant();

        assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        // Set content assist processors for various content types.
        if (completionProcessor != null) {
            assistant.setContentAssistProcessor(completionProcessor, IDocument.DEFAULT_CONTENT_TYPE);
        }

        // Configure how content assist information will appear.
        assistant.enableAutoActivation(store.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION));
        assistant.setAutoActivationDelay(store.getInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
        assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);

        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

        //In the future, a preference page will be added to customize foreground and background.
        Color foreground = new Color(DBeaverUI.getDisplay(), 0, 0, 0);
        Color background = new Color(DBeaverUI.getDisplay(), 255, 255, 255);

        assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
        assistant.setContextInformationPopupForeground(foreground);
        assistant.setContextInformationPopupBackground(background);
        //Set auto insert mode.
        assistant.enableAutoInsert(store.getBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));

        final DBPPreferenceListener prefListener = new DBPPreferenceListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent event)
            {
                if (event.getProperty().equals(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION)) {
                    assistant.enableAutoActivation(configStore.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION));
                } else if (event.getProperty().equals(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY)) {
                    assistant.setAutoActivationDelay(configStore.getInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
                } else if (event.getProperty().equals(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO)) {
                    assistant.enableAutoInsert(configStore.getBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
                }
            }
        };
        configStore.addPropertyChangeListener(prefListener);
        editor.getTextViewer().getControl().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                configStore.removePropertyChangeListener(prefListener);
            }
        });
        return assistant;

    }

    /*
    * @see SourceViewerConfiguration#getInformationControlCreator(ISourceViewer)
    * @since 2.0
    *
    */

    @Override
    public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer)
    {
        return new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(Shell parent)
            {
                return new DefaultInformationControl(parent, true);
            }
        };
    }

    /**
     * Creates, configures, and returns the ContentFormatter to use.
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentFormatter(ISourceViewer)
     */
    @Override
    public IContentFormatter getContentFormatter(ISourceViewer sourceViewer)
    {
        ContentFormatter formatter = new ContentFormatter();
        formatter.setDocumentPartitioning(SQLPartitionScanner.SQL_PARTITIONING);

        IFormattingStrategy formattingStrategy = new SQLFormattingStrategy(editor.getSyntaxManager());
        for (String ct : SQLPartitionScanner.SQL_PARTITION_TYPES) {
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
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType)
    {
        return new SQLDoubleClickStrategy();
    }

    /**
     * Creates, configures, and returns a presentation reconciler to help with
     * document changes.
     *
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
     */
    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer)
    {

        // Create a presentation reconciler to handle handle document changes.
        PresentationReconciler reconciler = new PresentationReconciler();
        String docPartitioning = getConfiguredDocumentPartitioning(sourceViewer);
        reconciler.setDocumentPartitioning(docPartitioning);

        // Add a "damager-repairer" for changes in default text (SQL code).
        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(ruleManager);

        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        // rule for multiline comments
        // We just need a scanner that does nothing but returns a token with
        // the corresponding text attributes
        dr = new DefaultDamagerRepairer(new SingleTokenScanner(
            new TextAttribute(ruleManager.getColor(SQLConstants.CONFIG_COLOR_COMMENT))));
        reconciler.setDamager(dr, SQLPartitionScanner.SQL_MULTILINE_COMMENT);
        reconciler.setRepairer(dr, SQLPartitionScanner.SQL_MULTILINE_COMMENT);

        // Add a "damager-repairer" for changes within one-line SQL comments.
        dr = new DefaultDamagerRepairer(new SingleTokenScanner(
            new TextAttribute(ruleManager.getColor(SQLConstants.CONFIG_COLOR_COMMENT))));
        reconciler.setDamager(dr, SQLPartitionScanner.SQL_COMMENT);
        reconciler.setRepairer(dr, SQLPartitionScanner.SQL_COMMENT);

        // Add a "damager-repairer" for changes within quoted literals.
        dr = new DefaultDamagerRepairer(
            new SingleTokenScanner(
                new TextAttribute(ruleManager.getColor(SQLConstants.CONFIG_COLOR_STRING))));
        reconciler.setDamager(dr, SQLPartitionScanner.SQL_STRING);
        reconciler.setRepairer(dr, SQLPartitionScanner.SQL_STRING);

//        // Add a "damager-repairer" for changes within delimited identifiers.
//        dr = new DefaultDamagerRepairer(
//            new SingleTokenScanner(
//                new TextAttribute(ruleManager.getColor(SQLSyntaxManager.CONFIG_COLOR_DELIMITER))));
//        reconciler.setDamager(dr, SQLPartitionScanner.SQL_DOUBLE_QUOTES_IDENTIFIER);
//        reconciler.setRepairer(dr, SQLPartitionScanner.SQL_DOUBLE_QUOTES_IDENTIFIER);

        return reconciler;
    }

    /**
     * Returns the SQLEditor associated with this object.
     *
     * @return the SQLEditor that this object configures
     */
    public SQLEditorBase getSQLEditor()
    {
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
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType)
    {
        //return new BestMatchHover(this.getSQLEditor());
        return new SQLAnnotationHover(this.getSQLEditor());
    }

    /**
     * Sets the <code>ISQLDBProposalsService</code> object that provides content
     * assist services for this viewer to the given object.
     *
    public void setDBProposalsService(SQLAssistProposalsService dbProposalsService)
    {
        fDBProposalsService = dbProposalsService;
        if (completionProcessor != null) {
            completionProcessor.setProposalsService(dbProposalsService);
        }
    }*/

    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer)
    {
        return SQLPartitionScanner.SQL_PARTITION_TYPES;
    }

    /*
	 * @see SourceViewerConfiguration#getDefaultPrefixes(ISourceViewer, String)
	 *  @since 2.0
	 */

    @Override
    public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType)
    {
        return new String[]
            {
                "--", ""
            }
            ; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer)
    {
        InformationPresenter presenter = new InformationPresenter(getInformationControlCreator(sourceViewer));
        presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        // Register information provider
        IInformationProvider provider = new SQLInformationProvider(getSQLEditor());
        String[] contentTypes = getConfiguredContentTypes(sourceViewer);
        for (String contentType : contentTypes) {
            presenter.setInformationProvider(provider, contentType);
        }

        presenter.setSizeConstraints(60, 10, true, true);
        return presenter;
    }

    @Override
    public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer)
    {
        return new MultipleHyperlinkPresenter(new RGB(0, 0, 255)) {

        };
    }

    @Nullable
    @Override
    public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer)
    {
        if (sourceViewer == null) {
            return null;
        }

        return new IHyperlinkDetector[]{
            hyperlinkDetector,
            new URLHyperlinkDetector()};
    }

    void onDataSourceChange()
    {
        if (hyperlinkDetector instanceof IHyperlinkDetectorExtension) {
            ((IHyperlinkDetectorExtension)hyperlinkDetector).dispose();
        }
    }

}