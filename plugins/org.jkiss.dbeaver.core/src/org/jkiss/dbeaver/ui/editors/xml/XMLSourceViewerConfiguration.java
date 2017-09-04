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
package org.jkiss.dbeaver.ui.editors.xml;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.RGB;
import org.jkiss.dbeaver.core.DBeaverUI;

import static org.jkiss.dbeaver.registry.editor.EntityEditorDescriptor.Type.editor;

public class XMLSourceViewerConfiguration extends SourceViewerConfiguration {
    static final RGB COLOR_XML_COMMENT = new RGB(128, 0, 0);
    static final RGB COLOR_PROC_INSTR = new RGB(128, 128, 128);
    static final RGB COLOR_STRING = new RGB(0, 128, 0);
    static final RGB COLOR_DEFAULT = new RGB(0, 0, 0);
    static final RGB COLOR_TAG = new RGB(0, 0, 128);

    private final ISharedTextColors colorManager;
    private final XMLEditor editor;

    public XMLSourceViewerConfiguration(XMLEditor editor) {
        this.colorManager = DBeaverUI.getSharedTextColors();
        this.editor = editor;
    }

    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[]{
            IDocument.DEFAULT_CONTENT_TYPE,
            XMLPartitionScanner.XML_COMMENT,
            XMLPartitionScanner.XML_TAG};
    }

    @Override
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
        return XMLPartitionScanner.XML_PARTITIONING;
    }

    @Override
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
        return new XMLDoubleClickStrategy();
    }

    private XMLTagScanner getXMLTagScanner() {
        XMLTagScanner tagScanner = new XMLTagScanner(colorManager);
        tagScanner.setDefaultReturnToken(
            new Token(
                new TextAttribute(
                    colorManager.getColor(COLOR_TAG))));
        return tagScanner;
    }

    @Override
    public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
        ContentFormatter formatter = new ContentFormatter();
        formatter.setDocumentPartitioning(IDocument.DEFAULT_CONTENT_TYPE);

        IFormattingStrategy formattingStrategy = new XMLFormattingStrategy();
        formatter.setFormattingStrategy(formattingStrategy, IDocument.DEFAULT_CONTENT_TYPE);

        formatter.enablePartitionAwareFormatting(false);

        return formatter;
    }

    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer)
    {
        ContentAssistant assistant = new ContentAssistant();
        assistant.setContentAssistProcessor(new XMLContentAssistantProcessor(),IDocument.DEFAULT_CONTENT_TYPE);
        assistant.enableAutoActivation(true);

        return assistant;
    }

    private XMLScanner getXMLScanner() {
        XMLScanner scanner = new XMLScanner(colorManager);
        scanner.setDefaultReturnToken(
            new Token(
                new TextAttribute(
                    colorManager.getColor(COLOR_DEFAULT))));
        return scanner;
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        PresentationReconciler reconciler = new PresentationReconciler();
        reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        DefaultDamagerRepairer dr =
            new DefaultDamagerRepairer(getXMLTagScanner());
        reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
        reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

        dr = new DefaultDamagerRepairer(getXMLScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        NonRuleBasedDamagerRepairer ndr =
            new NonRuleBasedDamagerRepairer(
                new TextAttribute(
                    colorManager.getColor(COLOR_XML_COMMENT)));
        reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
        reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

        return reconciler;
    }

    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        XMLReconcilingStrategy strategy = new XMLReconcilingStrategy();
        strategy.setEditor(editor);

        MonoReconciler reconciler = new MonoReconciler(strategy, false);

        return reconciler;
    }

}