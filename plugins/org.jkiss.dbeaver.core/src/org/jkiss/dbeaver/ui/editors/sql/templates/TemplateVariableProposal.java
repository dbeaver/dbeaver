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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * A proposal for insertion of template variables.
 */
public class TemplateVariableProposal implements ICompletionProposal {

	private TemplateVariableResolver resolver;
	private int offset;
	private int length;
	private ITextViewer viewer;

	private Point fSelection;
	private final boolean fIncludeBrace;

	/**
	 * Creates a template variable proposal.
	 *
	 * @param variable the template variable
	 * @param offset the offset to replace
	 * @param length the length to replace
	 * @param viewer the viewer
	 * @param includeBrace whether to also replace the ${
	 */
	public TemplateVariableProposal(TemplateVariableResolver variable, int offset, int length, ITextViewer viewer, boolean includeBrace) {
		resolver = variable;
		this.offset = offset;
		this.length = length;
		this.viewer = viewer;
		fIncludeBrace= includeBrace;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {

		try {
			String variable;
			String type= resolver.getType();
			if (type.equals("dollar")) //$NON-NLS-1$
				variable= "$$"; //$NON-NLS-1$
			else if (fIncludeBrace)
				variable= "${" + type + '}'; //$NON-NLS-1$
			else
				variable= type;
			document.replace(offset, length, variable);
			fSelection= new Point(offset + variable.length(), 0);

		} catch (BadLocationException e) {
            UIUtils.showErrorDialog(
                viewer.getTextWidget().getShell(), "Variable proposal", "Variable proposal failed", e);
		}
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return fSelection;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return resolver.getDescription();
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return resolver.getType();
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return null;
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}
}
