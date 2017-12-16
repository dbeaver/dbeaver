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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLHyperlinkDetector;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

public class NavigateObjectHandler extends AbstractHandler {

    public NavigateObjectHandler()
    {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor != null) {
            SQLEditorBase sqlEditor = DBUtils.getAdapter(SQLEditorBase.class, activeEditor);
            if (sqlEditor != null) {
                IHyperlink hyperlink = getCurrentHyperlink(sqlEditor);
                if (hyperlink != null) {
                    IRegion selRegion2 = hyperlink.getHyperlinkRegion();
                    TextViewer textViewer = sqlEditor.getTextViewer();
                    if (textViewer != null) {
                        textViewer.setSelectedRange(selRegion2.getOffset(), selRegion2.getLength());
                    }
                    hyperlink.open();
                }
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
        if (!ArrayUtils.isEmpty(hyperLinks)) {
            return hyperLinks[0];
        }
        String lastKeyword = hyperlinkDetector.getLastKeyword();
        if (!CommonUtils.isEmpty(lastKeyword)) {
            IEditorStatusLine statusLine = (IEditorStatusLine)editor.getAdapter(IEditorStatusLine.class);
            if (statusLine != null) {
                statusLine.setMessage(true, "Can't find metadata object for name '" + lastKeyword + "'", (Image)null);
            }
            editor.getEditorControl().getDisplay().beep();
        }
        return null;
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
                editor.getSyntaxManager().getQuoteStrings());
            SQLIdentifierDetector.WordRegion wordRegion = wordDetector.detectIdentifier(document, new Region(selection.getOffset(), selection.getLength()));
            if (!wordRegion.isEmpty()) {
                element.setText("Open '" + wordRegion.word + "'");
            }
        }
    }
*/
}
