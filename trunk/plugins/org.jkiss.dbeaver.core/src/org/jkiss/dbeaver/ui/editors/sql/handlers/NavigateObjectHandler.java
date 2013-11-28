/*
 * Copyright (C) 2010-2013 Serge Rieder
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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLHyperlinkDetector;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLIdentifierDetector;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class NavigateObjectHandler extends AbstractHandler {

    public NavigateObjectHandler()
    {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor instanceof SQLEditorBase) {
            SQLEditorBase editor = (SQLEditorBase)activeEditor;
            IHyperlink hyperlink = getCurrentHyperlink(editor);
            if (hyperlink != null) {
                IRegion selRegion2 = hyperlink.getHyperlinkRegion();
                editor.getTextViewer().setSelectedRange(selRegion2.getOffset(), selRegion2.getLength());
                hyperlink.open();
            }
        }
        return null;
    }

    private IHyperlink getCurrentHyperlink(SQLEditorBase editor)
    {
        SQLHyperlinkDetector hyperlinkDetector = new SQLHyperlinkDetector(editor, editor.getSyntaxManager());
        ITextSelection selection = (ITextSelection) editor.getTextViewer().getSelection();

        IRegion curRegion = new Region(selection.getOffset(), 0);
        IHyperlink[] hyperLinks = hyperlinkDetector.detectHyperlinks(editor.getTextViewer(), curRegion, false);
        return CommonUtils.isEmpty(hyperLinks) ? null : hyperLinks[0];
    }

/*
    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        IWorkbenchPartSite partSite = UIUtils.getWorkbenchPartSite(element.getServiceLocator());
        if (partSite != null && partSite.getPart() instanceof SQLEditorBase) {
            SQLEditorBase editor = (SQLEditorBase)partSite.getPart();
            ITextSelection selection = (ITextSelection)editor.getSelectionProvider().getSelection();
            IDocument document = editor.getDocument();

            SQLIdentifierDetector wordDetector = new SQLIdentifierDetector(
                editor.getSyntaxManager().getStructSeparator(),
                editor.getSyntaxManager().getQuoteSymbol());
            SQLIdentifierDetector.WordRegion wordRegion = wordDetector.detectIdentifier(document, new Region(selection.getOffset(), selection.getLength()));
            if (!wordRegion.isEmpty()) {
                element.setText("Open '" + wordRegion.word + "'");
            }
        }
    }
*/
}
