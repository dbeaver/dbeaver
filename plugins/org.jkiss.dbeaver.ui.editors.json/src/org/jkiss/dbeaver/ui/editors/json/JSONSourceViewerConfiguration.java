/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.json;


import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.text.NonRuleBasedDamagerRepairer;

/**
 * JSONSourceViewerConfiguration
 */
public class JSONSourceViewerConfiguration extends SourceViewerConfiguration {

    private JSONTextEditor textEditor;
    private JSONScanner jsonScanner;

    JSONSourceViewerConfiguration(JSONTextEditor textEditor) {
        super();
        this.textEditor = textEditor;
        this.jsonScanner = new JSONScanner();
    }

    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[]{
            IDocument.DEFAULT_CONTENT_TYPE,
            JSONPartitionScanner.JSON_STRING};
    }

    @Override
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
        return JSONPartitionScanner.JSON_PARTITIONING;
    }


    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        ColorRegistry colorRegistry = UIUtils.getColorRegistry();

        PresentationReconciler reconciler = new PresentationReconciler();
        reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(jsonScanner);
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        NonRuleBasedDamagerRepairer ndr =
            new NonRuleBasedDamagerRepairer(
                new TextAttribute(
                    colorRegistry.get(SQLConstants.CONFIG_COLOR_STRING)));
        reconciler.setDamager(ndr, JSONPartitionScanner.JSON_STRING);
        reconciler.setRepairer(ndr, JSONPartitionScanner.JSON_STRING);
        return reconciler;
    }

    @Override
    public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
        ContentFormatter formatter = new ContentFormatter();
        formatter.setDocumentPartitioning(IDocument.DEFAULT_CONTENT_TYPE);

        IFormattingStrategy formattingStrategy = new JSONFormattingStrategy(sourceViewer, this);
        formatter.setFormattingStrategy(formattingStrategy, IDocument.DEFAULT_CONTENT_TYPE);

        formatter.enablePartitionAwareFormatting(false);

        return formatter;
    }
}
