/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLCommentToken;
import org.jkiss.utils.CommonUtils;

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
        String[] singleLineComments = syntaxManager.getKeywordManager().getSingleLineComments();
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
                            comment = SQLConstants.ML_COMMENT_START + comment.substring(slc.length()) + SQLConstants.ML_COMMENT_END;
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
