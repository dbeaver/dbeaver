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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemKind;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionProposal;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryWordEntry;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SuggestionInformationControlCreator;
import org.jkiss.utils.CommonUtils;

public class SQLEditorQueryCompletionProposal extends SQLQueryCompletionProposal
    implements ICompletionProposal, ICompletionProposalExtension2,
    ICompletionProposalExtension3, ICompletionProposalExtension4, ICompletionProposalExtension5, ICompletionProposalExtension6 {

    private static final Log log = Log.getLog(SQLEditorQueryCompletionProposal.class);

    private final SQLEditorQueryCompletionProposalContext proposalContext;

    private Image cachedSwtImage = null;

    public SQLEditorQueryCompletionProposal(
        @NotNull SQLEditorQueryCompletionProposalContext proposalContext,
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
        super(
            proposalContext,
            itemKind,
            object,
            image,
            displayString,
            decorationString,
            description,
            replacementString,
            replacementOffset,
            replacementLength,
            filterString,
            proposalScore
        );

        this.proposalContext = proposalContext;
    }

    @NotNull
    @Override
    public SQLEditorQueryCompletionProposalContext getProposalContext() {
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
    public StyledString getStyledDisplayString() {
        StyledString result = new StyledString(this.getDisplayString(), this.proposalContext.getStyler(this.itemKind));

        if (CommonUtils.isNotEmpty(this.decorationString)) {
            result.append(this.decorationString, StyledString.DECORATIONS_STYLER);
        }

        return result;
    }

    @Override
    public boolean isAutoInsertable() {
        return true;
    }

    @Override
    public IInformationControlCreator getInformationControlCreator() {
        return this.object == null || !this.getProposalContext().getActivityTracker().isAdditionalInfoExpected()
            ? null
            : SuggestionInformationControlCreator.INSTANCE;
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
}
