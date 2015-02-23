/*
 * Copyright (C) 2010-2015 Serge Rieder
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLCommentToken;
import org.jkiss.utils.Pair;

/**
 * CopyUnformattedTextAction
 */
public class CopyUnformattedTextAction extends Action {

    private SQLEditorBase sqlEditor;

    public CopyUnformattedTextAction()
    {
        super("Copy Unformatted Text");
        setToolTipText("Copies unformatted text to clipboard");
    }

    @Override
    public void run()
    {
        if (sqlEditor == null) {
            return;
        }
        ITextSelection selection = (ITextSelection) sqlEditor.getSelectionProvider().getSelection();
        IDocument document = sqlEditor.getDocumentProvider().getDocument(sqlEditor.getEditorInput());
        if (document == null) {
            return;
        }
        int startPos, endPos;
        if (selection.getLength() > 1) {
            startPos = selection.getOffset();
            endPos = startPos + selection.getLength();
        } else {
            startPos = 0;
            endPos = document.getLength();
        }

        StringBuilder result = new StringBuilder();
        SQLSyntaxManager syntaxManager = sqlEditor.getSyntaxManager();
        syntaxManager.setRange(document, startPos, endPos - startPos);
        String[] singleLineComments = syntaxManager.getDialect().getSingleLineComments();
        Pair<String, String> multiLineComments = syntaxManager.getDialect().getMultiLineComments();
        boolean lastWhitespace = false;
        try {
            for (;;) {
                IToken token = syntaxManager.nextToken();
                if (token.isEOF()) {
                    break;
                }
                int tokenOffset = syntaxManager.getTokenOffset();
                final int tokenLength = syntaxManager.getTokenLength();
                if (token.isWhitespace()) {
                    if (!lastWhitespace) {
                        result.append(' ');
                    }
                    lastWhitespace = true;
                } else if (token instanceof SQLCommentToken) {
                    String comment = document.get(tokenOffset, tokenLength);
                    for (String slc : singleLineComments) {
                        if (comment.startsWith(slc)) {
                            if (multiLineComments != null) {
                                comment = multiLineComments.getFirst() + comment.substring(slc.length()) + multiLineComments.getSecond();
                            }
                            break;
                        }
                    }
                    comment = comment.replaceAll("\\s+", " ");
                    result.append(comment);
                } else {
                    lastWhitespace = false;
                    result.append(document.get(tokenOffset, tokenLength));
                }
            }
        } catch (BadLocationException e) {
            // dump
            e.printStackTrace();
        }

        UIUtils.setClipboardContents(
            Display.getCurrent(),
            TextTransfer.getInstance(),
            result.toString().trim());
    }

    public void setEditor(SQLEditorBase textEditor)
    {
        setEnabled(textEditor != null);
        this.sqlEditor = textEditor;
    }
}
