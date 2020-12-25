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


import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;


/**
 * A position based completion proposal.
 *
 * @since 3.0
 */
final class SQLVariableCompletionProposal implements ICompletionProposal, ICompletionProposalExtension2 {

    private TemplateVariable variable;
	/** The string to be displayed in the completion proposal popup */
	private String displayString;
	/** The replacement string */
	private String replacementString;
	/** The replacement position. */
	private Position replacementPosition;
	/** The cursor position after this proposal has been applied */
	private int cursorPosition;
	/** The image to be displayed in the completion proposal popup */
	private Image image;
	/** The context information of this proposal */
	private IContextInformation contextInformation;
	/** The additional info of this proposal */
	private String additionalProposalInfo;

    public SQLVariableCompletionProposal(TemplateVariable variable, String replacementString, Position replacementPosition, int cursorPosition) {
		this(variable, replacementString, replacementPosition, cursorPosition, null, null, null, null);
	}

	public SQLVariableCompletionProposal(TemplateVariable variable, String replacementString, Position replacementPosition, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo) {
		Assert.isNotNull(replacementString);
		Assert.isTrue(replacementPosition != null);
        this.variable = variable;
		this.replacementString = replacementString;
		this.replacementPosition = replacementPosition;
		this.cursorPosition = cursorPosition;
		this.image = image;
		this.displayString = displayString;
		this.contextInformation = contextInformation;
		this.additionalProposalInfo = additionalProposalInfo;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		try {
			document.replace(replacementPosition.getOffset(), replacementPosition.getLength(), replacementString);
		} catch (BadLocationException x) {
			// ignore
		}
        // Reset variable settings
        if (variable instanceof SQLVariable) {
            SQLVariable sqlVariable = (SQLVariable) variable;
            if (!replacementString.equals(sqlVariable.getCurrentChoice())) {
                sqlVariable.setCurrentChoice(replacementString);
/*
                for (SQLVariable var : sqlVariable.getContext().getVariables()) {
                    if (var != sqlVariable) {
                        TemplateVariableResolver resolver = var.getResolver();
                        if (resolver != null) {
                            resolver.resolve(var, sqlVariable.getContext());
                        }
                    }
                }
*/
            }
        }
    }

	public Point getSelection(IDocument document) {
		return new Point(replacementPosition.getOffset() + cursorPosition, 0);
	}

	public IContextInformation getContextInformation() {
		return contextInformation;
	}

	public Image getImage() {
		return image;
	}

	public String getDisplayString() {
		if (displayString != null)
			return displayString;
		return replacementString;
	}

	public String getAdditionalProposalInfo() {
		return additionalProposalInfo;
	}

	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		apply(viewer.getDocument());
	}

	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	public void unselected(ITextViewer viewer) {
	}

	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		try {
			String content= document.get(replacementPosition.getOffset(), offset - replacementPosition.getOffset());
			if (replacementString.startsWith(content))
				return true;
		} catch (BadLocationException e) {
			// ignore concurrently modified document
		}
		return false;
	}

}
