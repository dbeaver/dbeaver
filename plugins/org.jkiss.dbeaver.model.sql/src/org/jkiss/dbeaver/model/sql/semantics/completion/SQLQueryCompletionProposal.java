/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics.completion;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.sql.completion.CompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionHelper;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class SQLQueryCompletionProposal extends CompletionProposalBase {

    private static final Log log = Log.getLog(SQLQueryCompletionProposal.class);
    protected static final boolean DEBUG = false;

    private final SQLQueryCompletionProposalContext proposalContext;

    protected final SQLQueryCompletionItemKind itemKind;

    protected final DBSObject object;
    protected final DBPImage image;

    protected final String displayString;
    protected final String decorationString;
    protected final String description;

    protected final String replacementString;
    protected final int replacementOffset;
    protected final int replacementLength;

    protected final SQLQueryWordEntry filterString;

    protected int proposalScore;

    protected boolean cachedProposalInfoComputed = false;
    protected Object cachedProposalInfo = null;

    public SQLQueryCompletionProposal(
        @NotNull SQLQueryCompletionProposalContext proposalContext,
        @NotNull SQLQueryCompletionItemKind itemKind,
        @Nullable DBSObject object,
        @Nullable DBPImage image,
        @Nullable String displayString,
        @Nullable String decorationString,
        @NotNull String description,
        @NotNull String replacementString,
        int replacementOffset,
        int replacementLength,
        @Nullable SQLQueryWordEntry filterString,
        int proposalScore
    ) {
        this.proposalContext = proposalContext;
        this.itemKind = itemKind;
        this.object = object;
        this.image = image;
        this.displayString = displayString;
        this.decorationString = decorationString;
        this.description = description;

        this.replacementString = replacementString;
        this.replacementOffset = replacementOffset;
        this.replacementLength = replacementLength;

        this.filterString = filterString;
        this.proposalScore = proposalScore;
    }

    public SQLQueryCompletionItemKind getItemKind() {
        return this.itemKind;
    }

    @Override
    public int getReplacementOffset() {
        return this.replacementOffset;
    }

    public int getReplacementLength() {
        return this.replacementLength;
    }

    @Override
    public String getReplacementString() {
        return this.displayString; // because actual replacement string includes extra whitespaces
    }

    public int getProposalScore() {
        return proposalScore;
    }

    @NotNull
    public SQLQueryCompletionProposalContext getProposalContext() {
        return this.proposalContext;
    }

    public String getDisplayString() {
        return CommonUtils.isNotEmpty(this.displayString) ? this.displayString : this.replacementString.replaceAll("[\r\n]", "");
    }

    public String getAdditionalProposalInfo() {
        return this.description;
    }

    public Object getAdditionalProposalInfo(IProgressMonitor progressMonitor) {
        if (!this.getProposalContext().getActivityTracker().isAdditionalInfoExpected()) {
            return this.description;
        }
        if (!this.cachedProposalInfoComputed) {
            DBRProgressMonitor monitor = new DefaultProgressMonitor(progressMonitor);
            if (this.object != null) {
                // preload object info, like at SQLCompletionAnalyzer.makeProposalsFromObject(..)
                // but maybe instead put it to SuggestionInformationControl.createTreeControl(..),
                //                where the DBNDatabaseNode is required but missing if not cached
                DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(monitor, this.object, true);
                this.cachedProposalInfo = this.object;
            } else if (this.itemKind == SQLQueryCompletionItemKind.RESERVED) {
                Object info = SQLCompletionHelper.readAdditionalProposalInfo(
                    monitor,
                    this.proposalContext.getCompletionContext(),
                    this.object,
                    new String[]{ this.getDisplayString() },
                    DBPKeywordType.KEYWORD
                );
                this.cachedProposalInfo = info == null || info.equals(this.getDisplayString()) ? this.description : info;
            } else {
                this.cachedProposalInfo = this.description;
            }
            this.cachedProposalInfoComputed = true;
        }
        return this.cachedProposalInfo;
    }

    public boolean isAutoInsertable() {
        return true;
    }

    public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
        return this.replacementString;
    }

    public int getPrefixCompletionStart(IDocument document, int completionOffset) {
        return this.replacementOffset;
    }

    public void apply(IDocument document) {
        try {
            document.replace(this.replacementOffset, this.replacementLength, this.replacementString);
        } catch (BadLocationException ex) {
            log.error("Error applying completion proposal", ex);
        }
    }

    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        if (DEBUG) {
            log.debug("validate @" + offset);
        }
        this.getProposalContext().getActivityTracker().implicitlyTriggered();
        if (this.filterString != null && CommonUtils.isNotEmpty(this.filterString.filterString)) {
            int filterKeyStart = this.filterString.offset >= 0 ? this.filterString.offset : this.proposalContext.getRequestOffset();
            try {
                if (offset > document.getLength()) {
                    return false;
                } else {
                    int filterKeyLength = offset - filterKeyStart;
                    if (filterKeyLength > 0) {
                        String filterKey = document.get(filterKeyStart, filterKeyLength);
                        if (DEBUG) {
                            log.debug("validate: " + filterString.string + " vs " + filterKey);
                        }
                        this.proposalScore = this.filterString.matches(filterKey, this.proposalContext.getCompletionContext().isSearchInsideNames());
                        return this.proposalScore > 0;
                    } else {
                        this.proposalScore = Integer.MAX_VALUE;
                        return true;
                    }
                }
            } catch (BadLocationException ex) {
                log.error("Error validating completion proposal", ex);
            }
        }
        return true;
    }
}
