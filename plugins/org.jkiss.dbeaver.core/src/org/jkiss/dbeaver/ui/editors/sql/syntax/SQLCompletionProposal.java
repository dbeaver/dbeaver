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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * SQL Completion proposal
 */
public class SQLCompletionProposal implements ICompletionProposal, ICompletionProposalExtension2 {

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
    /** The additional info of this proposal. */
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
        String additionalProposalInfo,
        DBPNamedObject object)
    {
        this.dataSource = request.editor.getDataSource();
        this.syntaxManager = request.editor.getSyntaxManager();
        this.displayString = displayString;
        this.replacementString = replacementString;
        this.replacementFull = replacementString.toLowerCase(Locale.ENGLISH);
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

        setPosition(request.wordDetector);

        this.object = object;
        this.simpleMode = request.simpleMode;
    }

    public static String makeObjectDescription(DBRProgressMonitor monitor, DBPNamedObject object, boolean html) {
        StringBuilder info = new StringBuilder();
        PropertyCollector collector = new PropertyCollector(object, false);
        collector.collectProperties();
        for (DBPPropertyDescriptor descriptor : collector.getPropertyDescriptors2()) {
            if (descriptor.isRemote()) {
                // Skip lazy properties
                continue;
            }
            Object propValue = collector.getPropertyValue(monitor, descriptor.getId());
            if (propValue == null) {
                continue;
            }
            String propString;
            if (propValue instanceof DBPNamedObject) {
                propString = ((DBPNamedObject) propValue).getName();
            } else {
                propString = DBValueFormatting.getDefaultValueDisplayString(propValue, DBDDisplayFormat.UI);
            }
            if (html) {
                info.append("<b>").append(descriptor.getDisplayName()).append(":  </b>");
                info.append(propString);
                info.append("<br>");
            } else {
                info.append(descriptor.getDisplayName()).append(": ").append(propString).append("\n");
            }
        }
        return info.toString();
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
            endOffset = wordDetector.getCursorOffset();//wordDetector.getEndOffset();
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
    public String getAdditionalProposalInfo()
    {
        if (additionalProposalInfo == null && object != null) {
            additionalProposalInfo = makeObjectDescription(VoidProgressMonitor.INSTANCE, object, true);
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
