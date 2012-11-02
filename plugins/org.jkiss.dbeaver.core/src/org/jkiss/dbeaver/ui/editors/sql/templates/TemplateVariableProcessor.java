/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import java.util.*;


public class TemplateVariableProcessor implements IContentAssistProcessor {

	private static Comparator<TemplateVariableProposal> fgTemplateVariableProposalComparator= new Comparator<TemplateVariableProposal>() {
		public int compare(TemplateVariableProposal proposal0, TemplateVariableProposal proposal1) {
			return proposal0.getDisplayString().compareTo(proposal1.getDisplayString());
		}
	};


	/** the context type */
	private TemplateContextType fContextType;

	/**
	 * Sets the context type.
	 *
	 * @param contextType the context type
	 */
	public void setContextType(TemplateContextType contextType) {
		fContextType= contextType;
	}

	/**
	 * Gets the context type.
	 *
	 * @return the context type
	 */
	public TemplateContextType getContextType() {
		return fContextType;
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,	int documentOffset) {

		if (fContextType == null)
			return null;

		List<TemplateVariableProposal> proposals= new ArrayList<TemplateVariableProposal>();

		String text= viewer.getDocument().get();
		int start= getStart(text, documentOffset);
		int end= documentOffset;

		String string= text.substring(start, end);
		int colon= string.indexOf(':');
		boolean includeBrace= true;
		int offset= start;
		String prefix= string;
		if (colon != -1) {
			includeBrace= false;
			offset= start + colon + 1;
			prefix= string.substring(colon + 1);
		} else {
			int escape= string.indexOf("${"); //$NON-NLS-1$
			if (escape != -1) {
				offset= start + escape + 2;
				includeBrace= false;
				prefix= string.substring(escape + 2);
			}
		}
		if (prefix.equals("$")) //$NON-NLS-1$
			prefix= ""; //$NON-NLS-1$

		int length= end - offset;

		for (Iterator<TemplateVariableResolver> iterator= fContextType.resolvers(); iterator.hasNext(); ) {
			TemplateVariableResolver variable= iterator.next();

			if (variable.getType().startsWith(prefix))
				proposals.add(new TemplateVariableProposal(variable, offset, length, viewer, includeBrace));
		}

		Collections.sort(proposals, fgTemplateVariableProposalComparator);
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	/* Guesses the start position of the completion */
	private int getStart(String string, int end) {
		int start= end;

		if (start >= 1 && string.charAt(start - 1) == '$')
			return start - 1;

		while ((start != 0) && Character.isUnicodeIdentifierPart(string.charAt(start - 1)))
			start--;

		if (start >= 1 && string.charAt(start - 1) == ':') {
			start--;
			while ((start != 0) && Character.isUnicodeIdentifierPart(string.charAt(start - 1)))
				start--;
		}

		if (start >= 2 && string.charAt(start - 1) == '{' && string.charAt(start - 2) == '$')
			return start - 2;

		return end;
	}

	/*
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] {'$'};
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}

