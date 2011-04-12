/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * SQL Completion proposal
 */
public class SQLCompletionProposal implements ICompletionProposal, ICompletionProposalExtension2 {

    private SQLSyntaxManager syntaxManager;

    /** The string to be displayed in the completion proposal popup. */
    private String displayString;
    /** The replacement string. */
    private String replacementString;
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

    public SQLCompletionProposal(SQLSyntaxManager syntaxManager, String displayString, String replacementString, SQLWordPartDetector wordDetector, int cursorPosition, Image image, IContextInformation contextInformation, String additionalProposalInfo)
    {
        this.syntaxManager = syntaxManager;
        this.displayString = displayString;
        this.replacementString = replacementString;
        this.cursorPosition = cursorPosition;
        this.image = image;
        this.contextInformation = contextInformation;
        this.additionalProposalInfo = additionalProposalInfo;

        setPosition(wordDetector);
    }

    private void setPosition(SQLWordPartDetector wordDetector)
    {
        String wordPart = wordDetector.getWordPart();
        int divPos = wordPart.lastIndexOf(syntaxManager.getStructSeparator());
        if (divPos == -1) {
            replacementOffset = wordDetector.getOffset();
            replacementLength = wordPart.length();
        } else {
            replacementOffset = wordDetector.getOffset() + divPos + 1;
            replacementLength = wordPart.length() - divPos - 1;
        }
    }

    public void apply(IDocument document) {
        try {
            document.replace(replacementOffset, replacementLength, replacementString);
        } catch (BadLocationException x) {
            // ignore
        }
    }

    /*
     * @see ICompletionProposal#getSelection(IDocument)
     */
    public Point getSelection(IDocument document) {
        return new Point(replacementOffset + cursorPosition, 0);
    }

    public String getAdditionalProposalInfo()
    {
        return additionalProposalInfo;
    }

    public String getDisplayString()
    {
        return displayString;
    }

    public Image getImage()
    {
        return image;
    }

    public IContextInformation getContextInformation()
    {
        return contextInformation;
    }

    //////////////////////////////////////////////////////////////////
    // ICompletionProposalExtension2

    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset)
    {
        apply(viewer.getDocument());
    }

    public void selected(ITextViewer viewer, boolean smartToggle)
    {

    }

    public void unselected(ITextViewer viewer)
    {

    }

    public boolean validate(IDocument document, int offset, DocumentEvent event)
    {
        final SQLWordPartDetector wordDetector = new SQLWordPartDetector(document, syntaxManager, offset);
        String wordPart = wordDetector.getWordPart();
        int divPos = wordPart.lastIndexOf(syntaxManager.getStructSeparator());
        if (divPos != -1) {
            wordPart = wordPart.substring(divPos + 1);
        }
        if (!CommonUtils.isEmpty(wordPart) && replacementString.startsWith(wordPart)) {
            setPosition(wordDetector);
            return true;
        } else {
            return false;
        }
    }
}
