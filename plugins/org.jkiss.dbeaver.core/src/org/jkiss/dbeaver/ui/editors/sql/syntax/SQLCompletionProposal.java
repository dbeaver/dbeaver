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

import org.jkiss.dbeaver.Log;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.utils.CommonUtils;

/**
 * SQL Completion proposal
 */
public class SQLCompletionProposal implements ICompletionProposal, ICompletionProposalExtension2 {

    private static final Log log = Log.getLog(SQLCompletionProposal.class);

    private SQLSyntaxManager syntaxManager;

    /** The string to be displayed in the completion proposal popup. */
    private String displayString;
    /** The replacement string. */
    private String replacementString;
    private String replacementFull;
    private String replacementLast;
    /** The replacement offset. */
    private int replacementOffset;
    /** The replacement length. */
    private int replacementLength;
    /** The cursor position after this proposal has been applied. */
    private int cursorPosition;
    /** The image to be displayed in the completion proposal popup. */
    private Image image;
    /** The context information of this proposal. */
    private IContextInformation contextInformation;
    /** The additional info of this proposal. */
    private String additionalProposalInfo;

    private DBPNamedObject object;

    public SQLCompletionProposal(
        SQLSyntaxManager syntaxManager,
        String displayString,
        String replacementString,
        SQLWordPartDetector wordDetector,
        int cursorPosition,
        @Nullable Image image,
        IContextInformation contextInformation,
        String additionalProposalInfo,
        DBPNamedObject object)
    {
        this.syntaxManager = syntaxManager;
        this.displayString = displayString;
        this.replacementString = replacementString;
        this.replacementFull = replacementString.toLowerCase();
        int divPos = this.replacementFull.lastIndexOf(syntaxManager.getStructSeparator());
        if (divPos == -1) {
            this.replacementLast = null;
        } else {
            this.replacementLast = this.replacementFull.substring(divPos + 1);
        }
        this.cursorPosition = cursorPosition;
        this.image = image;
        this.contextInformation = contextInformation;
        this.additionalProposalInfo = additionalProposalInfo;

        setPosition(wordDetector);

        this.object = object;
    }

    public DBPNamedObject getObject() {
        return object;
    }

    private void setPosition(SQLWordPartDetector wordDetector)
    {
        String fullWord = wordDetector.getFullWord();
        int curOffset = wordDetector.getCursorOffset() - wordDetector.getStartOffset();
        char structSeparator = syntaxManager.getStructSeparator();
        int startOffset = fullWord.lastIndexOf(structSeparator, curOffset);
        int endOffset = fullWord.indexOf(structSeparator, curOffset);
        if (endOffset == startOffset) {
            startOffset = -1;
        }
        if (startOffset != -1) {
            startOffset += wordDetector.getStartOffset() + 1;
        } else {
            startOffset = wordDetector.getStartOffset();
        }
        if (endOffset != -1) {
            endOffset += wordDetector.getStartOffset();
        } else {
            endOffset = wordDetector.getEndOffset();
        }
        replacementOffset = startOffset;
        replacementLength = endOffset - startOffset;
    }

    @Override
    public void apply(IDocument document) {
        try {
            document.replace(replacementOffset, replacementLength, replacementString);
        } catch (BadLocationException e) {
            // ignore
            log.debug(e);
        }
    }

    /*
     * @see ICompletionProposal#getSelection(IDocument)
     */
    @Override
    public Point getSelection(IDocument document) {
        return new Point(replacementOffset + cursorPosition, 0);
    }

    @Override
    public String getAdditionalProposalInfo()
    {
        if (additionalProposalInfo == null && object != null) {
            additionalProposalInfo = SQLCompletionProcessor.makeObjectDescription(VoidProgressMonitor.INSTANCE, object, true);
        }
        return additionalProposalInfo;
    }

    @Override
    public String getDisplayString()
    {
        return displayString;
    }

    @Override
    public Image getImage()
    {
        return image;
    }

    @Override
    public IContextInformation getContextInformation()
    {
        return contextInformation;
    }

    //////////////////////////////////////////////////////////////////
    // ICompletionProposalExtension2

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset)
    {
        apply(viewer.getDocument());
    }

    @Override
    public void selected(ITextViewer viewer, boolean smartToggle)
    {

    }

    @Override
    public void unselected(ITextViewer viewer)
    {

    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event)
    {
        final SQLWordPartDetector wordDetector = new SQLWordPartDetector(document, syntaxManager, offset);
        String wordPart = wordDetector.getWordPart();
        int divPos = wordPart.lastIndexOf(syntaxManager.getStructSeparator());
        if (divPos != -1) {
            wordPart = wordPart.substring(divPos + 1);
        }
        String wordLower = wordPart.toLowerCase();
        if (!CommonUtils.isEmpty(wordPart)) {
            if (
                (TextUtils.fuzzyScore(replacementFull, wordLower) > 0 && (CommonUtils.isEmpty(event.getText()) || TextUtils.fuzzyScore(replacementFull, event.getText()) > 0)) ||
                (this.replacementLast != null && TextUtils.fuzzyScore(this.replacementLast, wordLower) > 0)
                )
            {
                setPosition(wordDetector);
                return true;
            }
        }
        return false;
    }

    public boolean hasStructObject() {
        return object instanceof DBSObject || object instanceof DBSObjectReference;
    }

    public DBSObject getObjectContainer() {
        if (object instanceof DBSObject) {
            return ((DBSObject) object).getParentObject();
        } else if (object instanceof DBSObjectReference) {
            return ((DBSObjectReference) object).getContainer();
        } else {
            return null;
        }
    }

}
