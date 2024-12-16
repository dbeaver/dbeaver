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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionHelper;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemKind;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryWordEntry;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SuggestionInformationControlCreator;
import org.jkiss.utils.CommonUtils;

public class SQLQueryCompletionProposal implements ICompletionProposal, ICompletionProposalExtension2, ICompletionProposalExtension3,
        ICompletionProposalExtension4, ICompletionProposalExtension5, ICompletionProposalExtension6 {

    private static final Log log = Log.getLog(SQLQueryCompletionProposal.class);
    private static final boolean DEBUG = false;

    private final SQLQueryCompletionProposalContext proposalContext;

    private final SQLQueryCompletionItemKind itemKind;

    private final DBSObject object;
    private final DBPImage image;

    private final String displayString;
    private final String decorationString;
    private final String description;

    private final String replacementString;
    private final int replacementOffset;
    private final int replacementLength;

    private final SQLQueryWordEntry filterString;

    private boolean cachedProposalInfoComputed = false;
    private Object cachedProposalInfo = null;
    private Image cachedSwtImage = null;

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
        @Nullable SQLQueryWordEntry filterString
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
    }

    @NotNull
    public SQLQueryCompletionProposalContext getProposalContext() {
        return this.proposalContext;
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    @Override
    public Image getImage() {
        return this.image == null
            ? null
            : this.cachedSwtImage != null
                ? this.cachedSwtImage
                : (this.cachedSwtImage = DBeaverIcons.getImage(this.image));
    }

    @Override
    public String getDisplayString() {
        return CommonUtils.isNotEmpty(this.displayString) ? this.displayString : this.replacementString.replaceAll("[\r\n]", "");
    }

    @Override
    public String getAdditionalProposalInfo() {
        return this.description;
    }

    @Override
    public StyledString getStyledDisplayString() {
        StyledString result = new StyledString(this.getDisplayString(), this.proposalContext.getStyler(this.itemKind));

        if (CommonUtils.isNotEmpty(this.decorationString)) {
            result.append(this.decorationString, StyledString.DECORATIONS_STYLER);
        }

        return result;
    }

    @Override
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

    @Override
    public boolean isAutoInsertable() {
        return true;
    }

    @Override
    public IInformationControlCreator getInformationControlCreator() {
        return this.object == null || !this.getProposalContext().getActivityTracker().isAdditionalInfoExpected() ? null : SuggestionInformationControlCreator.INSTANCE;
    }

    @Override
    public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
        return this.replacementString;
    }

    @Override
    public int getPrefixCompletionStart(IDocument document, int completionOffset) {
        return this.replacementOffset;
    }

    @Override
    public void selected(ITextViewer viewer, boolean smartToggle) {
        // do nothing
    }

    @Override
    public void unselected(ITextViewer viewer) {
        // do nothing
    }

    @Override
    public Point getSelection(IDocument document) {
        return new Point(Math.min(this.replacementOffset + this.replacementString.length(), document.getLength()), 0);
    }

    @Override
    public void apply(IDocument document) {
        try {
            document.replace(this.replacementOffset, this.replacementLength, this.replacementString);
        } catch (BadLocationException ex) {
            log.error("Error applying completion proposal", ex);
        }
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        IDocument document = viewer.getDocument();
        if (this.validate(document, offset, null)) {
            try {
                document.replace(this.replacementOffset, offset - this.replacementOffset, this.replacementString);
            } catch (BadLocationException ex) {
                log.error("Error applying completion proposal", ex);
            }
        }
    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        if (DEBUG) {
            log.debug("validate @" + offset);
        }
        this.getProposalContext().getActivityTracker().implicitlyTriggered();
        if (this.filterString != null && CommonUtils.isNotEmpty(this.filterString.filterString)) {
            try {
                int filterKeyStart = this.filterString.offset >= 0 ? this.filterString.offset : this.proposalContext.getRequestOffset();
                if (offset > document.getLength()) {
                    return false;
                } else {
                    String filterKey = document.get(filterKeyStart, offset - filterKeyStart);
                    if (DEBUG) {
                        log.debug("validate: " + filterString.string + " vs " + filterKey);
                    }
                    return filterString.filterString.contains(filterKey.toLowerCase());
                }
            } catch (BadLocationException ex) {
                log.error("Error validating completion proposal", ex);
            }
        }
        return true;
    }
}
