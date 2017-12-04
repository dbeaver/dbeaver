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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * SQL Completion proposal
 */
public class SQLCompletionProposal implements ICompletionProposal, ICompletionProposalExtension2,ICompletionProposalExtension5 {

    private static final Log log = Log.getLog(SQLCompletionProposal.class);

    private final DBPDataSource dataSource;
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
    private DBPKeywordType proposalType;
    private String additionalProposalInfo;
    private boolean simpleMode;

    private DBPNamedObject object;

    public SQLCompletionProposal(
        SQLCompletionAnalyzer.CompletionRequest request,
        String displayString,
        String replacementString,
        int cursorPosition,
        @Nullable Image image,
        IContextInformation contextInformation,
        DBPKeywordType proposalType,
        String description,
        DBPNamedObject object)
    {
        this.dataSource = request.editor.getDataSource();
        this.syntaxManager = request.editor.getSyntaxManager();
        this.displayString = displayString;
        this.replacementString = replacementString;
        this.replacementFull = this.dataSource == null ?
                replacementString :
                DBUtils.getUnQuotedIdentifier(this.dataSource, replacementString.toLowerCase(Locale.ENGLISH)); // Convert to lower to compare IN VALIDATE FUNCTION
        int divPos = this.replacementFull.lastIndexOf(syntaxManager.getStructSeparator());
        if (divPos == -1) {
            this.replacementLast = null;
        } else {
            this.replacementLast = this.replacementFull.substring(divPos + 1);
        }
        this.cursorPosition = cursorPosition;
        this.image = image;
        this.contextInformation = contextInformation;
        this.proposalType = proposalType;
        this.additionalProposalInfo = description;

        setPosition(request.wordDetector);

        this.object = object;
        this.simpleMode = request.simpleMode;
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
            // Replace from identifier start till next struct separator
            endOffset += wordDetector.getStartOffset();
        } else {
            // Replace from identifier start to the end of current identifier
            if (wordDetector.getWordPart().isEmpty()) {
                endOffset = wordDetector.getCursorOffset();
            } else {
                // Replace from identifier start to the end of current identifier
                endOffset = wordDetector.getEndOffset();
            }
        }
        replacementOffset = startOffset;
        replacementLength = endOffset - startOffset;
    }

    @Override
    public void apply(IDocument document) {
        try {
            if (dataSource != null) {
                if (dataSource.getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS)) {
                    boolean insertTrailingSpace = true;
                    if (object instanceof DBSObjectContainer) {
                        // Do not append trailing space after schemas/catalogs/etc.
                    } else {
                        int docLen = document.getLength();
                        if (docLen <= replacementOffset + replacementLength + 2) {
                            insertTrailingSpace = false;
                        } else {
                            insertTrailingSpace = document.getChar(replacementOffset + replacementLength) != ' ';
                        }
                        if (insertTrailingSpace) {
                            replacementString += " ";
                        }
                        cursorPosition++;
                    }
                }
            }
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
    public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
        if (additionalProposalInfo == null) {
            additionalProposalInfo = SQLContextInformer.readAdditionalProposalInfo(new DefaultProgressMonitor(monitor), dataSource, object, new String[] {displayString}, proposalType);
        }
        return additionalProposalInfo;
    }

    @Override
    public String getAdditionalProposalInfo()
    {
        return CommonUtils.toString(getAdditionalProposalInfo(new NullProgressMonitor()));
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
        if (event == null) {
            return false;
        }
        final SQLWordPartDetector wordDetector = new SQLWordPartDetector(document, syntaxManager, offset);
        String wordPart = wordDetector.getWordPart();
        int divPos = wordPart.lastIndexOf(syntaxManager.getStructSeparator());
        if (divPos != -1) {
            wordPart = wordPart.substring(divPos + 1);
        }
        String wordLower = wordPart.toLowerCase(Locale.ENGLISH);
        if (!CommonUtils.isEmpty(wordPart)) {
            boolean matched;
            if (simpleMode) {
                matched = replacementFull.startsWith(wordLower) &&
                    (CommonUtils.isEmpty(event.getText()) || replacementFull.contains(event.getText().toLowerCase(Locale.ENGLISH))) ||
                    (this.replacementLast != null && this.replacementLast.startsWith(wordLower));
            } else {
                matched = (TextUtils.fuzzyScore(replacementFull, wordLower) > 0 &&
                    (CommonUtils.isEmpty(event.getText()) || TextUtils.fuzzyScore(replacementFull, event.getText()) > 0)) ||
                    (this.replacementLast != null && TextUtils.fuzzyScore(this.replacementLast, wordLower) > 0);
            }

            if (matched) {
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

    @Override
    public String toString() {
        return displayString;
    }


}
