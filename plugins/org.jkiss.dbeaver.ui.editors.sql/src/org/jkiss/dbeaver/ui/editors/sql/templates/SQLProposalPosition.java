/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.ProposalPosition;

public class SQLProposalPosition extends ProposalPosition {

	private SQLVariable variable;

	public SQLProposalPosition(IDocument document, int offset, int length, SQLVariable variable) {
		super(document, offset, length, new ICompletionProposal[0]);
        this.variable = variable;
	}

	public ICompletionProposal[] getChoices() {
		return variable.getProposals(this, getLength());
	}

}
