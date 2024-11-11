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
package org.jkiss.dbeaver.model.sql.completion;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * SQL Completion proposal
 */
public class SQLCompletionProposalBase {

    public static final String PARAM_EXEC = "exec";

    public static final String PARAM_NO_SPACE = "no-space";

    private static final Log log = Log.getLog(SQLCompletionProposalBase.class);

    private final SQLCompletionRequest request;

    /**
     * The string to be displayed in the completion proposal popup.
     */
    private String displayString;
    /**
     * The replacement string.
     */
    protected String replacementString;
    protected String replacementFull;
    // Tail
    protected String replacementAfter;

    /**
     * The replacement offset.
     */
    protected int replacementOffset;
    /**
     * The replacement length.
     */
    protected int replacementLength;
    /**
     * The cursor position after this proposal has been applied.
     */
    protected int cursorPosition;

    private DBPKeywordType proposalType;
    private Object additionalProposalInfo;

    private DBPImage image;
    private DBPNamedObject object;
    private Map<String, Object> params;
    private int proposalScore;

    public SQLCompletionProposalBase(
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
        this.request = request;
        DBPDataSource dataSource = request.getContext().getDataSource();

        this.displayString = displayString;
        this.replacementString = replacementString;
        this.replacementFull = dataSource == null ?
            replacementString :
            DBUtils.getUnQuotedIdentifier(dataSource, replacementString.toLowerCase(Locale.ENGLISH)); // Convert to lower to compare IN VALIDATE FUNCTION
        this.cursorPosition = cursorPosition;
        this.image = image;
        this.proposalType = proposalType;
        this.additionalProposalInfo = object != null ? object : description;

        setPosition(request.getWordDetector());

        this.object = object;
        this.params = params;
    }

    public SQLCompletionRequest getRequest() {
        return this.request;
    }

    public DBPDataSource getDataSource() {
        return request.getContext().getDataSource();
    }

    public DBPNamedObject getObject() {
        return object;
    }

    protected void setPosition(SQLWordPartDetector wordDetector) {
        final String fullWord = wordDetector.getFullWord();
        final int curOffset = wordDetector.getCursorOffset() - wordDetector.getStartOffset();
        final char structSeparator = request.getContext().getSyntaxManager().getStructSeparator();
        DBPDataSource dataSource = request.getContext().getDataSource();

        final boolean proposalContainsStructSeparator = replacementString.indexOf(structSeparator) >= 0;
        boolean useFQName = dataSource != null && request.getContext().isUseFQNames() && proposalContainsStructSeparator;
        if (useFQName) {
            replacementOffset = wordDetector.getStartOffset();
            replacementLength = wordDetector.getLength();
        } else if ((!proposalContainsStructSeparator && !fullWord.equals(replacementString))
            || dataSource != null && containsQuotedIdentifier(dataSource, replacementString)
        ) {
            // Replace only last part
            int startOffset = fullWord.lastIndexOf(structSeparator, curOffset - 1);
            if (startOffset == -1) {
                startOffset = 0;
            } else if (startOffset > curOffset) {
                startOffset = fullWord.lastIndexOf(structSeparator, curOffset);
                if (startOffset == -1) {
                    startOffset = curOffset;
                } else {
                    startOffset++;
                }
            } else {
                startOffset++;
            }
            // End offset - number of character which to the right from replacement which we don't touch (e.g. in complex identifiers like xxx.zzz.yyy)
            int endOffset = fullWord.indexOf(structSeparator, curOffset);
            if (endOffset != -1) {
                endOffset = fullWord.length() - endOffset;
            } else {
                endOffset = 0;
            }
            replacementOffset = wordDetector.getStartOffset() + startOffset;
            // If we are at the begin of word (curOffset == 0) then do not replace the word to the right.
            boolean replaceWord = dataSource != null && request.getContext().isReplaceWords();
            if (replaceWord) {
                replacementLength = wordDetector.getEndOffset() - replacementOffset - endOffset;
            } else {
                replacementLength = curOffset - startOffset;
            }
            if (dataSource != null && DBUtils.isQuotedIdentifier(dataSource, fullWord)) {
                // Replace closing quote (#6244)
                replacementLength++;
            }
        } else {
            int startOffset = fullWord.indexOf(structSeparator);
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
            /*if (curOffset < fullWord.length() && Character.isLetterOrDigit(fullWord.charAt(curOffset)) && false) {
                // Do not replace full word if we are in the middle of word
                replacementLength = curOffset;
            } else */
            {
                replacementLength = endOffset - startOffset;
            }
        }
    }

    public String getExtraString() {
        try {
            VoidProgressMonitor monitor = new VoidProgressMonitor();
            if (object instanceof DBSObjectReference) {
                if (DBSProcedure.class.isAssignableFrom(((DBSObjectReference) object).getObjectType().getTypeClass())) {
                    object = ((DBSObjectReference) object).resolveObject(monitor);
                }
            }
            if (object instanceof DBSProcedure) {
                // Ad parameter marks
                boolean isExec = CommonUtils.toBoolean(params.get(PARAM_EXEC));

                Collection<? extends DBSProcedureParameter> parameters = ((DBSProcedure) object).getParameters(monitor);
                if (!CommonUtils.isEmpty(parameters)) {
                    StringBuilder params = new StringBuilder();
                    for (DBSProcedureParameter param : parameters) {
                        if (param.getParameterKind().isInput()) {
                            if (params.length() > 0) params.append(", ");
                            if (isExec) {
                                params.append(":").append(param.getName());
                            } else {
                                params.append(param.getParameterType().getFullTypeName());
                            }
                        }
                    }
                    return "(" + params.toString() + ")";
                } else {
                    return "()";
                }
            }
            return null;
        } catch (DBException e) {
            log.error("Error resolving procedure parameters", e);
            return null;
        }
    }

    public Object getAdditionalInfo(DBRProgressMonitor monitor) {
        if (!this.request.getActivityTracker().isAdditionalInfoExpected()) {
            return this.additionalProposalInfo instanceof String s ? s : null;
        }
        if (additionalProposalInfo == null && object == null) {
                additionalProposalInfo = SQLCompletionHelper.readAdditionalProposalInfo(monitor, request.getContext(), object, new String[]{displayString}, proposalType);
        }
        return object != null ? object : additionalProposalInfo;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String getReplacementString() {
        return replacementString;
    }

    public int getReplacementOffset() {
        return replacementOffset;
    }

    public int getReplacementLength() {
        return replacementLength;
    }

    public DBPKeywordType getProposalType() {
        return proposalType;
    }

    public DBPImage getObjectImage() {
        return image;
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

    public void setReplacementAfter(String replacementAfter) {
        this.replacementAfter = replacementAfter;
    }

    public int getProposalScore() {
        return proposalScore;
    }

    public void setProposalScore(int proposalScore) {
        this.proposalScore = proposalScore;
    }

    @Override
    public String toString() {
        return displayString;
    }

    // The proposal may contain identifier containing alias. Let's handle it using this ugly hack for now
    private static boolean containsQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String string) {
        final String[][] quotes = dataSource.getSQLDialect().getIdentifierQuoteStrings();
        if (quotes == null) {
            return false;
        }
        if (DBUtils.isQuotedIdentifier(dataSource, string)) {
            return true;
        }
        for (String[] pair : quotes) {
            if (!string.startsWith(pair[0])) {
                continue;
            }
            final int last = string.lastIndexOf(pair[1]);
            if (last > 0 && DBUtils.isQuotedIdentifier(dataSource, string.substring(0, last + 1))) {
                return true;
            }
        }
        return false;
    }
}
