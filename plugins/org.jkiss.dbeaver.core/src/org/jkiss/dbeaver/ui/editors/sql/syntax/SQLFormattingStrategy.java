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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.source.ISourceViewer;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.registry.sql.SQLFormatterConfigurationRegistry;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorSourceViewerConfiguration;

/**
 * The formatting strategy that transforms SQL keywords to upper case
 */
public class SQLFormattingStrategy extends ContextBasedFormattingStrategy
{
    private ISourceViewer sourceViewer;
    private SQLEditorSourceViewerConfiguration svConfig;
    private SQLSyntaxManager sqlSyntax;

    /**
     * According to profileName to determine which the database syntax keywords highlighted.
     */
    public SQLFormattingStrategy(ISourceViewer sourceViewer, SQLEditorSourceViewerConfiguration svConfig, SQLSyntaxManager syntax)
    {
        this.sourceViewer = sourceViewer;
        this.svConfig = svConfig;
        this.sqlSyntax = syntax;
    }

    @Override
    public void formatterStarts(String initialIndentation)
    {
    }

    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions)
    {
        final String[] indentPrefixes = svConfig.getIndentPrefixes(sourceViewer, IDocument.DEFAULT_CONTENT_TYPE);
        SQLFormatterConfiguration configuration = new SQLFormatterConfiguration(sqlSyntax);
        configuration.setIndentString(indentPrefixes[0]);

        SQLFormatter formatter = SQLFormatterConfigurationRegistry.getInstance().createFormatter(configuration);
        if (formatter == null) {
            return content;
        }
        return formatter.format(content, configuration);
    }

    @Override
    public void formatterStops()
    {
    }

}