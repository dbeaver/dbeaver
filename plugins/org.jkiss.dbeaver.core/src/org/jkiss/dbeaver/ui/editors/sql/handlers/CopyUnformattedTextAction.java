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

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleManager;
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
        SQLRuleManager ruleManager = sqlEditor.getRuleManager();
        SQLDialect dialect = sqlEditor.getSyntaxManager().getDialect();
        ruleManager.setRange(document, startPos, endPos - startPos);
        String[] singleLineComments = dialect.getSingleLineComments();
        Pair<String, String> multiLineComments = dialect.getMultiLineComments();
        boolean lastWhitespace = false;
        try {
            for (;;) {
                IToken token = ruleManager.nextToken();
                if (token.isEOF()) {
                    break;
                }
                int tokenOffset = ruleManager.getTokenOffset();
                final int tokenLength = ruleManager.getTokenLength();
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
                    comment = TextUtils.compactWhiteSpaces(comment);
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
