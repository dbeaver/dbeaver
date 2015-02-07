/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.text.link.*;
import org.eclipse.jface.text.link.InclusivePositionUpdater;
import org.eclipse.jface.text.templates.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;


/**
 * A template completion proposal.
 * <p>
 * Clients may subclass.</p>
 *
 * @since 3.0
 */
public class SQLTemplateCompletionProposal implements ICompletionProposal, ICompletionProposalExtension, ICompletionProposalExtension2, ICompletionProposalExtension3 {

    static final Log log = Log.getLog(SQLTemplateCompletionProposal.class);

    private final Template fTemplate;
    private final SQLContext fContext;
    private final Image fImage;
    private final IRegion fRegion;
    private int fRelevance;

    private IRegion fSelectedRegion; // initialized by apply()
    private String fDisplayString;
    private InclusivePositionUpdater fUpdater;

    /**
     * Creates a template proposal with a template and its context.
     *
     * @param template the template
     * @param context  the context in which the template was requested.
     * @param region   the region this proposal is applied to
     * @param image    the icon of the proposal.
     */
    public SQLTemplateCompletionProposal(Template template, SQLContext context, IRegion region, Image image)
    {
        this(template, context, region, image, 0);
    }

    /**
     * Creates a template proposal with a template and its context.
     *
     * @param template  the template
     * @param context   the context in which the template was requested.
     * @param image     the icon of the proposal.
     * @param region    the region this proposal is applied to
     * @param relevance the relevance of the proposal
     */
    public SQLTemplateCompletionProposal(Template template, SQLContext context, IRegion region, Image image, int relevance)
    {
        Assert.isNotNull(template);
        Assert.isNotNull(context);
        Assert.isNotNull(region);

        fTemplate = template;
        fContext = context;
        fImage = image;
        fRegion = region;

        fDisplayString = null;

        fRelevance = relevance;
    }

    /**
     * Returns the context in which the template was requested.
     *
     * @return the context in which the template was requested
     * @since 3.1
     */
    protected final TemplateContext getContext()
    {
        return fContext;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This method is no longer called by the framework and clients should overwrite
     *             {@link #apply(org.eclipse.jface.text.ITextViewer, char, int, int)} instead
     */
    public final void apply(IDocument document)
    {
        // not called anymore
    }

    /**
     * Inserts the template offered by this proposal into the viewer's document
     * and sets up a <code>LinkedModeUI</code> on the viewer to edit any of
     * the template's unresolved variables.
     *
     * @param viewer    {@inheritDoc}
     * @param trigger   {@inheritDoc}
     * @param stateMask {@inheritDoc}
     * @param offset    {@inheritDoc}
     */
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset)
    {

        IDocument document = viewer.getDocument();
        try {
            fContext.setReadOnly(false);
            int start;
            TemplateBuffer templateBuffer;
            {
                int oldReplaceOffset = getReplaceOffset();
                try {
                    // this may already modify the document (e.g. add imports)
                    templateBuffer = fContext.evaluate(fTemplate);
                } catch (TemplateException e1) {
                    fSelectedRegion = fRegion;
                    return;
                }

                start = getReplaceOffset();
                int shift = start - oldReplaceOffset;
                int end = Math.max(getReplaceEndOffset(), offset + shift);

                // insert template string
                String templateString = templateBuffer.getString();
                document.replace(start, end - start, templateString);
            }

            // translate positions
            LinkedModeModel model = new LinkedModeModel();
            TemplateVariable[] variables = templateBuffer.getVariables();
            boolean hasPositions = false;
            for (int i = 0; i != variables.length; i++) {
                SQLVariable variable = (SQLVariable) variables[i];

                if (variable.isUnambiguous())
                    continue;

                LinkedPositionGroup group = new LinkedPositionGroup();

                int[] offsets = variable.getOffsets();
                int length = variable.getLength();

                LinkedPosition first;
                {
                    String[] values = variable.getValues();
                    for (int j = 0; j < values.length; j++) {
                        ensurePositionCategoryInstalled(document, model);
                        Position pos = new Position(offsets[0] + start, length);
                        document.addPosition(getCategory(), pos);
                    }

                    if (values.length > 1) {
                        first = new SQLProposalPosition(document, offsets[0] + start, length, variable);
                        //variable.setProposalPosition(first);
                    } else {
                        first = new LinkedPosition(document, offsets[0] + start, length);
                    }
                }

                for (int j = 0; j != offsets.length; j++) {
                    if (j == 0)
                        group.addPosition(first);
                    else
                        group.addPosition(new LinkedPosition(document, offsets[j] + start, length));
                }

                model.addGroup(group);
                hasPositions = true;
            }

            if (hasPositions) {
                model.forceInstall();
                LinkedModeUI ui = new LinkedModeUI(model, viewer);
                ui.setExitPosition(viewer, getCaretOffset(templateBuffer) + start, 0, Integer.MAX_VALUE);
                ui.enter();

                fSelectedRegion = ui.getSelectedRegion();
            } else {
                ensurePositionCategoryRemoved(document);
                fSelectedRegion = new Region(getCaretOffset(templateBuffer) + start, 0);
            }

        } catch (BadLocationException e) {
            log.error(e);
            ensurePositionCategoryRemoved(document);
            fSelectedRegion = fRegion;
        } catch (BadPositionCategoryException e) {
            log.error(e);
            fSelectedRegion = fRegion;
        }

    }

    private void ensurePositionCategoryInstalled(final IDocument document, LinkedModeModel model)
    {
        if (!document.containsPositionCategory(getCategory())) {
            document.addPositionCategory(getCategory());
            fUpdater = new InclusivePositionUpdater(getCategory());
            document.addPositionUpdater(fUpdater);

            model.addLinkingListener(new ILinkedModeListener() {

                /*
                     * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
                     */
                public void left(LinkedModeModel environment, int flags)
                {
                    ensurePositionCategoryRemoved(document);
                }

                public void suspend(LinkedModeModel environment)
                {
                }

                public void resume(LinkedModeModel environment, int flags)
                {
                }
            });
        }
    }

    private void ensurePositionCategoryRemoved(IDocument document)
    {
        if (document.containsPositionCategory(getCategory())) {
            try {
                document.removePositionCategory(getCategory());
            } catch (BadPositionCategoryException e) {
                // ignore
            }
            document.removePositionUpdater(fUpdater);
        }
    }

    private String getCategory()
    {
        return "TemplateProposalCategory_" + toString(); //$NON-NLS-1$
    }

    private int getCaretOffset(TemplateBuffer buffer)
    {

        TemplateVariable[] variables = buffer.getVariables();
        for (int i = 0; i != variables.length; i++) {
            TemplateVariable variable = variables[i];
            if (variable.getType().equals(GlobalTemplateVariables.Cursor.NAME))
                return variable.getOffsets()[0];
        }

        return buffer.getString().length();
    }

    /**
     * Returns the offset of the range in the document that will be replaced by
     * applying this template.
     *
     * @return the offset of the range in the document that will be replaced by
     *         applying this template
     * @since 3.1
     */
    protected final int getReplaceOffset()
    {
        return fContext.getStart();
    }

    /**
     * Returns the end offset of the range in the document that will be replaced
     * by applying this template.
     *
     * @return the end offset of the range in the document that will be replaced
     *         by applying this template
     * @since 3.1
     */
    protected final int getReplaceEndOffset()
    {
        return fContext.getEnd();
    }

    public Point getSelection(IDocument document)
    {
        return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
    }

    public String getAdditionalProposalInfo()
    {
        try {
            fContext.setReadOnly(true);
            TemplateBuffer templateBuffer;
            try {
                templateBuffer = fContext.evaluate(fTemplate);
            } catch (TemplateException e) {
                return null;
            }

            return templateBuffer.getString();

        } catch (BadLocationException e) {
            return null;
        }
    }

    public String getDisplayString()
    {
        if (fDisplayString == null) {
            fDisplayString = fTemplate.getName() + " - " + fTemplate.getDescription();
        }
        return fDisplayString;
    }

    public Image getImage()
    {
        return fImage;
    }

    public IContextInformation getContextInformation()
    {
        return null;
    }

    /**
     * Returns the relevance.
     *
     * @return the relevance
     */
    public int getRelevance()
    {
        return fRelevance;
    }

    public IInformationControlCreator getInformationControlCreator()
    {
        return null;
    }

    public void selected(ITextViewer viewer, boolean smartToggle)
    {
    }

    public void unselected(ITextViewer viewer)
    {
    }

    public boolean validate(IDocument document, int offset, DocumentEvent event)
    {
        try {
            int replaceOffset = getReplaceOffset();
            if (offset >= replaceOffset) {
                String content = document.get(replaceOffset, offset - replaceOffset);
                return fTemplate.getName().toLowerCase().startsWith(content.toLowerCase());
            }
        } catch (BadLocationException e) {
            // concurrent modification - ignore
        }
        return false;
    }

    public CharSequence getPrefixCompletionText(IDocument document, int completionOffset)
    {
        return fTemplate.getName();
    }

    public int getPrefixCompletionStart(IDocument document, int completionOffset)
    {
        return getReplaceOffset();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This method is no longer called by the framework and clients should overwrite
     *             {@link #apply(org.eclipse.jface.text.ITextViewer, char, int, int)} instead
     */
    public void apply(IDocument document, char trigger, int offset)
    {
        // not called any longer
    }

    public boolean isValidFor(IDocument document, int offset)
    {
        // not called any longer
        return false;
    }

    public char[] getTriggerCharacters()
    {
        // no triggers
        return new char[0];
    }

    public int getContextInformationPosition()
    {
        return fRegion.getOffset();
    }
}
