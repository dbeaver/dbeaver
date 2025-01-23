/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.text.TextUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SuggestionInformationControlCreator;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;
import java.util.Map;

/**
 * SQL Completion proposal
 */
public class SQLCompletionProposal extends SQLCompletionProposalBase implements ICompletionProposal,
    ICompletionProposalExtension2, ICompletionProposalExtension3, ICompletionProposalExtension4, ICompletionProposalExtension5,
    ICompletionProposalExtension6 {

    private static final Log log = Log.getLog(SQLCompletionProposal.class);

    private final String replacementLast;
    
    private boolean isNeverAddSpaceAfter = false;

    public SQLCompletionProposal(
        SQLCompletionRequest request,
        String displayString,
        String replacementString,
        int cursorPosition,
        @Nullable DBPImage image,
        DBPKeywordType proposalType,
        String description,
        DBPNamedObject object,
        Map<String, Object> params)
    {
        super(request, displayString, replacementString, cursorPosition, image, proposalType, description, object, params);
        int divPos = this.replacementFull.lastIndexOf(request.getContext().getSyntaxManager().getStructSeparator());
        if (divPos == -1) {
            this.replacementLast = null;
        } else {
            this.replacementLast = this.replacementFull.substring(divPos + 1);
        }
        Object paramAlias = params.get(SQLCompletionProposalBase.PARAM_NO_SPACE);
        if (paramAlias != null && ((boolean) paramAlias)) {
            isNeverAddSpaceAfter = true;
        }
    }

    @Override
    public void apply(IDocument document) {
        try {
            String replaceOn = replacementString;
            String extraString = getExtraString();
            if (extraString != null) {
                replaceOn += extraString;
            }
            if (replacementAfter != null) {
                replaceOn += replacementAfter;
            }
            if (!isNeverAddSpaceAfter && getDataSource() != null) {
                if (getDataSource().getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS)) {
                    boolean insertTrailingSpace;
                    boolean hasClosingParenthesis = false;
                    int docLen = document.getLength();
                    int replacementSum = replacementOffset + replacementLength;
                    if (replacementSum < docLen) {
                        char nextChar = document.getChar(replacementSum);
                        hasClosingParenthesis = nextChar == ')' || nextChar == '}' || nextChar == ']'
                            || nextChar == '\'' || nextChar == '"';
                    }
                    if (getObject() instanceof DBSObjectContainer) {
                        // Do not append trailing space after schemas/catalogs/etc.
                    } else if (hasClosingParenthesis) {
                        // Do not append trailing space before closing parenthesis.
                    } else {
                        if (docLen <= replacementSum + 2) {
                            insertTrailingSpace = true;
                        } else {
                            final char ch = document.getChar(replacementSum);
                            insertTrailingSpace = !Character.isWhitespace(ch) || ch == '\r' || ch == '\n';
                        }
                        if (insertTrailingSpace) {
                            replaceOn += ' ';
                            cursorPosition++;
                        }
                    }
                }
            }
            document.replace(replacementOffset, replacementLength, replaceOn);
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
        int newOffset = replacementOffset + cursorPosition + (replacementAfter == null ? 0 : replacementAfter.length());
        if (newOffset > document.getLength()) {
            newOffset = document.getLength();
        }
        return new Point(newOffset, 0);
    }

    @Override
    public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
        return getAdditionalInfo(new DefaultProgressMonitor(monitor));
    }

    @Override
    public String getAdditionalProposalInfo() {
        return CommonUtils.toString(getAdditionalProposalInfo(new NullProgressMonitor()));
    }

    @Override
    public Image getImage() {
        DBPImage objectImage = getObjectImage();
        return objectImage == null ? null : DBeaverIcons.getImage(objectImage);
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    //////////////////////////////////////////////////////////////////
    // ICompletionProposalExtension2

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        apply(viewer.getDocument());
    }

    @Override
    public void selected(ITextViewer viewer, boolean smartToggle) {

    }

    @Override
    public void unselected(ITextViewer viewer) {

    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        if (event == null) {
            return false;
        }
        this.getRequest().getActivityTracker().implicitlyTriggered();
        SQLSyntaxManager syntaxManager = this.getRequest().getContext().getSyntaxManager();
        DBPDataSource dataSource = this.getRequest().getContext().getDataSource();
        final SQLWordPartDetector wordDetector = new SQLWordPartDetector(document, syntaxManager, offset);
        String wordPart = wordDetector.getWordPart();
        int divPos = wordPart.lastIndexOf(syntaxManager.getStructSeparator());
        if (divPos != -1) {
            if (divPos == wordPart.length() - 1) {
                // It is valid only if full word matches (it should be the only proposal)
                if (replacementString.equals(wordPart.substring(0, divPos))) {
                    {
                        // Call completion popup again
                        UIUtils.asyncExec(() -> {
                            IEditorPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
                            if (activeEditor != null) {
                                ITextViewer textViewer = activeEditor.getAdapter(ITextViewer.class);
                                if (textViewer != null) {
                                    textViewer.getTextOperationTarget().doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
                                }
                            }
                        });
                    }
                }
                return false;
            }
            wordPart = wordPart.substring(divPos + 1);
        }
        String wordLower = wordPart.toLowerCase(Locale.ENGLISH);
        if (!CommonUtils.isEmpty(wordPart)) {
            boolean matchContains = dataSource != null && dataSource.getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS);
            boolean matched;
            if (getObject() == null || !matchContains) {
                // For keywords use strict matching
                matched = (matchContains ? replacementFull.contains(wordLower) : replacementFull.startsWith(wordLower)) &&
                    (CommonUtils.isEmpty(event.getText()) || replacementFull.contains(event.getText().toLowerCase(Locale.ENGLISH))) ||
                    (this.replacementLast != null && this.replacementLast.startsWith(wordLower));
            } else {
                // For objects use fuzzy matching
                int score = TextUtils.fuzzyScore(replacementFull, wordLower);
                matched = (score > 0 &&
                    (CommonUtils.isEmpty(event.getText()) || TextUtils.fuzzyScore(replacementFull, event.getText()) > 0)) ||
                    (this.replacementLast != null && TextUtils.fuzzyScore(this.replacementLast, wordLower) > 0);
                if (matched) {
                    setProposalScore(score);
                }
            }

            if (matched) {
                setPosition(wordDetector);
                return true;
            }
        } else if (divPos != -1) {
            // Beginning of the last part of composite id.
            // Most likely it is a column name after an alias - all columns are valid
            if (getObject() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAutoInsertable() {
        return true;
    }

    @Override
    public StyledString getStyledDisplayString() {
        if (getProposalType() == DBPKeywordType.LITERAL) {
            StyledString styledString = new StyledString();
            styledString.append(getDisplayString(),
                StyledString.createColorRegistryStyler(SQLConstants.CONFIG_COLOR_STRING, null));
            return styledString;
        } else if (getProposalType() == DBPKeywordType.KEYWORD) {
            return new StyledString(getDisplayString(),
                StyledString.createColorRegistryStyler(SQLConstants.CONFIG_COLOR_KEYWORD, null));
        } else if (getProposalType() == DBPKeywordType.FUNCTION) {
            return new StyledString(getDisplayString(),
                StyledString.createColorRegistryStyler(SQLConstants.CONFIG_COLOR_FUNCTION, null));
        } else {
            return new StyledString(getDisplayString());
        }
    }

    @Override
    public IInformationControlCreator getInformationControlCreator() {
        if (hasStructObject() && this.getRequest().getActivityTracker().isAdditionalInfoExpected()) {
            return SuggestionInformationControlCreator.INSTANCE;
        } else {
            return null;
        }
    }

    @Override
    public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
        return getReplacementString();
    }

    @Override
    public int getPrefixCompletionStart(IDocument document, int completionOffset) {
        return getReplacementOffset();
    }
}
