/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.source.ISourceViewer;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
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

        SQLFormatter formatter = configuration.createFormatter();
        return formatter.format(content, configuration);
    }

    @Override
    public void formatterStops()
    {
    }

}