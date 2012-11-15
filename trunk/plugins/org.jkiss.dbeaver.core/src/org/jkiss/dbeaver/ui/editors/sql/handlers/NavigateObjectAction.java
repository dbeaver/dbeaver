/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLHyperlinkDetector;
import org.jkiss.utils.CommonUtils;

public class NavigateObjectAction extends Action implements IDataSourceProvider {
    private SQLEditorBase editor;

    public NavigateObjectAction()
    {
        super("Open database object");
    }

    public SQLEditorBase getEditor()
    {
        return editor;
    }

    @Override
    public String getText()
    {
/*
        try {
            IHyperlink hyperlink = getCurrentHyperlink();
            if (hyperlink != null) {
                IRegion selRegion = hyperlink.getHyperlinkRegion();
                String objectName = editor.getDocument().get(selRegion.getOffset(), selRegion.getLength());
                return "Open editor of '" + objectName + "'";
            }
            return "Navigate to object";
        } catch (BadLocationException e) {
        }
*/
        return super.getText();
    }

    public void setEditor(SQLEditorBase editor)
    {
        this.editor = editor;
    }

    @Override
    public void run()
    {
        IHyperlink hyperlink = getCurrentHyperlink();
        if (hyperlink != null) {
            IRegion selRegion2 = hyperlink.getHyperlinkRegion();
            editor.getTextViewer().setSelectedRange(selRegion2.getOffset(), selRegion2.getLength());
            hyperlink.open();
        }
    }

    private IHyperlink getCurrentHyperlink()
    {
        SQLHyperlinkDetector hyperlinkDetector = new SQLHyperlinkDetector(this, editor.getSyntaxManager());
        ITextSelection selection = (ITextSelection) editor.getTextViewer().getSelection();

        IRegion curRegion = new Region(selection.getOffset(), 0);
        IHyperlink[] hyperLinks = hyperlinkDetector.detectHyperlinks(editor.getTextViewer(), curRegion, false);
        return CommonUtils.isEmpty(hyperLinks) ? null : hyperLinks[0];
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return editor.getDataSource();
    }
}
