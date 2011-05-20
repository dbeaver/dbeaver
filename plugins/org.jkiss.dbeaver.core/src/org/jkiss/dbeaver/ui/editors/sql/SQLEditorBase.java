/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLHyperlinkDetector;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLSymbolInserter;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL Executor
 */
public abstract class SQLEditorBase extends BaseTextEditor implements IDataSourceProvider
{
    static final Log log = LogFactory.getLog(SQLEditorBase.class);

    private SQLSyntaxManager syntaxManager;

    private ProjectionSupport projectionSupport;

    private ProjectionAnnotationModel annotationModel;
    private Map<Annotation, Position> curAnnotations;

    private IAnnotationAccess annotationAccess;
    private int rulerWidth = VERTICAL_RULER_WIDTH;

    public SQLEditorBase()
    {
        super();
        syntaxManager = new SQLSyntaxManager();

        setDocumentProvider(new SQLDocumentProvider());
        setSourceViewerConfiguration(new SQLEditorSourceViewerConfiguration(
            this,
            syntaxManager,
            getCompletionProcessor(),
            new SQLHyperlinkDetector(this, syntaxManager)));
    }

    protected IContentAssistProcessor getCompletionProcessor()
    {
        return null;
    }

    public SQLSyntaxManager getSyntaxManager()
    {
        return syntaxManager;
    }

    public IDocument getDocument()
    {
        IDocumentProvider provider = getDocumentProvider();
        return provider == null ? null : provider.getDocument(getEditorInput());
    }

    public ProjectionAnnotationModel getAnnotationModel()
    {
        return annotationModel;
    }

    public void createPartControl(Composite parent)
    {
        setRangeIndicator(new DefaultRangeIndicator());

        super.createPartControl(parent);

        ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
        projectionSupport = new ProjectionSupport(
            viewer,
            getAnnotationAccess(),
            getSharedColors());
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error");
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning");
        projectionSupport.install();

        viewer.doOperation(ProjectionViewer.TOGGLE);

        annotationModel = viewer.getProjectionAnnotationModel();

        // Symbol inserter
        {
            SQLSymbolInserter symbolInserter = new SQLSymbolInserter(this, getSourceViewer());

            IPreferenceStore preferenceStore = DBeaverCore.getInstance().getGlobalPreferenceStore();
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

    public void updatePartControl(IEditorInput input) {
        super.updatePartControl(input);
    }

    @Override
    protected IVerticalRuler createVerticalRuler()
    {
        return new VerticalRuler(rulerWidth);
    }

    public void setRulerWidth(int rulerWidth)
    {
        this.rulerWidth = rulerWidth;
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
        return DBeaverCore.getInstance().getSharedTextColors();
    }

    protected ISourceViewer createSourceViewer(Composite parent,
        IVerticalRuler ruler, int styles)
    {
        OverviewRuler overviewRuler = new OverviewRuler(
            getAnnotationAccess(),
            VERTICAL_RULER_WIDTH,
            getSharedColors());

        return new SQLEditorSourceViewer(
            parent,
            ruler,
            overviewRuler,
            true,
            styles);
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

    public Object getAdapter(Class required)
    {
        if (projectionSupport != null) {
            Object adapter = projectionSupport.getAdapter(
                getSourceViewer(), required);
            if (adapter != null)
                return adapter;
        }

        return super.getAdapter(required);
    }

    public void dispose()
    {
        if (syntaxManager != null) {
            syntaxManager.dispose();
            syntaxManager = null;
        }

        super.dispose();
    }

    public void reloadSyntaxRules()
    {
        // Refresh syntax
        final SQLSyntaxManager syntaxManager = getSyntaxManager();
        if (syntaxManager != null) {
            syntaxManager.changeDataSource(getDataSource());
        }

        Document document = (Document) getDocument();
        if (document != null) {
            IDocumentPartitioner partitioner = new FastPartitioner(
                new SQLPartitionScanner(syntaxManager),
                SQLPartitionScanner.SQL_PARTITION_TYPES );
            partitioner.connect( document );
            document.setDocumentPartitioner( SQLPartitionScanner.SQL_PARTITIONING, partitioner );

            ProjectionViewer projectionViewer = (ProjectionViewer)getSourceViewer();
            if (projectionViewer != null && document.getLength() > 0) {
                // Refresh viewer
                //projectionViewer.getTextWidget().redraw();
                try {
                    projectionViewer.reinitializeProjection();
                } catch (Throwable ex) {
                    // We can catch OutOfMemory here for too big/complex documents
                    log.warn("Can't initialize SQL syntax projection", ex);
                }
            }
        }

        // Update configuration
        if (getSourceViewerConfiguration() instanceof SQLEditorSourceViewerConfiguration) {
            ((SQLEditorSourceViewerConfiguration) getSourceViewerConfiguration()).onDataSourceChange();
        }
    }

    public void doOperation(int operation)
    {
        ((SQLEditorSourceViewer) getSourceViewer()).doOperation(operation);
    }

    public synchronized void updateFoldingStructure(int offset, int length, List<Position> positions)
    {
        if (curAnnotations == null) {
            curAnnotations = new HashMap<Annotation, Position>();
        }
        List<Annotation> deletedAnnotations = new ArrayList<Annotation>();
        Map<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();

        // Delete all annotations if specified range
        for (Map.Entry<Annotation,Position> entry : curAnnotations.entrySet()) {
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

    protected void syncExec(Runnable runnable)
    {
        Display.getDefault().syncExec(runnable);
    }

    protected void asyncExec(Runnable runnable)
    {
        Display.getDefault().asyncExec(runnable);
    }

    public boolean isDisposed()
    {
        return
            getSourceViewer() == null ||
            getSourceViewer().getTextWidget() == null ||
            getSourceViewer().getTextWidget().isDisposed();
    }

}