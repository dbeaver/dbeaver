/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.*;
import org.eclipse.jface.text.templates.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.util.ExclusivePositionUpdater;


/**
 * A template completion proposal.
 * <p>
 * Clients may subclass.</p>
 *
 * @since 3.0
 */
public class SQLTemplateCompletionProposal extends TemplateProposal {

    private static final Log log = Log.getLog(SQLTemplateCompletionProposal.class);

    private IRegion selectedRegion;
    private IPositionUpdater positionUpdater;

    public SQLTemplateCompletionProposal(Template template, TemplateContext context, IRegion region, Image image) {
        this(template, context, region, image, 0);
    }

    public SQLTemplateCompletionProposal(Template template, TemplateContext context, IRegion region, Image image, int relevance) {
        super(template, context, region, image, relevance);
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        IDocument document = viewer.getDocument();
        try {
            getContext().setReadOnly(false);
            int oldReplaceOffset = getReplaceOffset();
            TemplateBuffer templateBuffer;
            try {
                templateBuffer = getContext().evaluate(getTemplate());
            } catch (TemplateException e) {
                selectedRegion = new Region(getReplaceOffset(), 0);
                return;
            }

            int start = getReplaceOffset();
            int shift = start - oldReplaceOffset;
            int end = Math.max(getReplaceEndOffset(), offset + shift);

            document.replace(start, end - start, templateBuffer.getString());

            LinkedModeModel model = new LinkedModeModel();
            boolean hasPositions = false;
            for (TemplateVariable variable : templateBuffer.getVariables()) {
                if (variable.isUnambiguous() && !(variable instanceof SQLVariable sqlVariable && sqlVariable.getResolver() != null)) {
                    continue;
                }

                LinkedPositionGroup group = new LinkedPositionGroup();
                int[] offsets = variable.getOffsets();
                int length = variable.getLength();
                LinkedPosition first = createLinkedPosition(document, model, variable, start, offsets[0], length);

                for (int i = 0; i < offsets.length; i++) {
                    group.addPosition(i == 0 ? first : new LinkedPosition(document, offsets[i] + start, length));
                }

                model.addGroup(group);
                hasPositions = true;
            }

            if (hasPositions) {
                model.forceInstall();
                LinkedModeUI ui = new LinkedModeUI(model, viewer);
                ui.setExitPosition(viewer, getCaretOffset(templateBuffer) + start, 0, Integer.MAX_VALUE);
                ui.enter();

                selectedRegion = ui.getSelectedRegion();
            } else {
                ensurePositionCategoryRemoved(document);
                selectedRegion = new Region(getCaretOffset(templateBuffer) + start, 0);
            }
        } catch (BadLocationException e) {
            MessageDialog.openError(viewer.getTextWidget().getShell(), "Template Evaluation Error", e.getMessage());
            ensurePositionCategoryRemoved(document);
            selectedRegion = new Region(getReplaceOffset(), 0);
        } catch (BadPositionCategoryException e) {
            MessageDialog.openError(viewer.getTextWidget().getShell(), "Template Evaluation Error", e.getMessage());
            selectedRegion = new Region(getReplaceOffset(), 0);
        }
    }

    @Override
    public Point getSelection(IDocument document) {
        return selectedRegion == null ? super.getSelection(document) : new Point(selectedRegion.getOffset(), selectedRegion.getLength());
    }

    /**
     * Creates a linked position with dynamic proposals for SQL template variables.
     */
    @NotNull
    private LinkedPosition createLinkedPosition(
        @NotNull IDocument document,
        @NotNull LinkedModeModel model,
        @NotNull TemplateVariable variable,
        int templateStartOffset,
        int variableOffset,
        int variableLength
    ) throws BadLocationException, BadPositionCategoryException {
        int positionOffset = variableOffset + templateStartOffset;
        if (variable instanceof SQLVariable sqlVariable) {
            return new SQLProposalPosition(document, positionOffset, variableLength, sqlVariable);
        }

        String[] values = variable.getValues();
        ICompletionProposal[] proposals = new ICompletionProposal[values.length];
        for (int i = 0; i < values.length; i++) {
            ensurePositionCategoryInstalled(document, model);
            Position position = new Position(positionOffset, variableLength);
            document.addPosition(getPositionCategory(), position);
            proposals[i] = new SQLVariableCompletionProposal(variable, values[i], position, variableLength);
        }

        return proposals.length > 1
            ? new ProposalPosition(document, positionOffset, variableLength, proposals)
            : new LinkedPosition(document, positionOffset, variableLength);
    }

    private void ensurePositionCategoryInstalled(@NotNull IDocument document, @NotNull LinkedModeModel model) {
        if (!document.containsPositionCategory(getPositionCategory())) {
            document.addPositionCategory(getPositionCategory());
            positionUpdater = new ExclusivePositionUpdater(getPositionCategory());
            document.addPositionUpdater(positionUpdater);

            model.addLinkingListener(new ILinkedModeListener() {
                @Override
                public void left(LinkedModeModel environment, int flags) {
                    ensurePositionCategoryRemoved(document);
                }

                @Override
                public void suspend(LinkedModeModel environment) {
                }

                @Override
                public void resume(LinkedModeModel environment, int flags) {
                }
            });
        }
    }

    private void ensurePositionCategoryRemoved(@NotNull IDocument document) {
        if (document.containsPositionCategory(getPositionCategory())) {
            try {
                document.removePositionCategory(getPositionCategory());
            } catch (BadPositionCategoryException e) {
                log.debug(e);
            }
            if (positionUpdater != null) {
                document.removePositionUpdater(positionUpdater);
                positionUpdater = null;
            }
        }
    }

    @NotNull
    private String getPositionCategory() {
        return "SQLTemplateProposalCategory_" + System.identityHashCode(this);
    }

    private int getCaretOffset(@NotNull TemplateBuffer buffer) {
        for (TemplateVariable variable : buffer.getVariables()) {
            if (variable.getType().equals(GlobalTemplateVariables.Cursor.NAME)) {
                return variable.getOffsets()[0];
            }
        }
        return buffer.getString().length();
    }

}
