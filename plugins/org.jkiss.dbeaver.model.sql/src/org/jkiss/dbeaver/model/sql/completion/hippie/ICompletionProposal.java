/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.model.sql.completion.hippie;

import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jface.text.IDocument;


public interface ICompletionProposal {

    /**
     * Inserts the proposed completion into the given document.
     *
     * @param document the document into which to insert the proposed completion
     */
    void apply(IDocument document);

    /**
     * Returns the new selection after the proposal has been applied to
     * the given document in absolute document coordinates. If it returns
     * <code>null</code>, no new selection is set.
     *
     * A document change can trigger other document changes, which have
     * to be taken into account when calculating the new selection. Typically,
     * this would be done by installing a document listener or by using a
     * document position during {@link #apply(IDocument)}.
     *
     * @param document the document into which the proposed completion has been inserted
     * @return the new selection in absolute document coordinates
     */
    Point getSelection(IDocument document);

    String getAdditionalProposalInfo();

    String getDisplayString();


    /**
     * Returns optional context information associated with this proposal.
     * The context information will automatically be shown if the proposal
     * has been applied.
     *
     * @return the context information for this proposal or <code>null</code>
     */
    IContextInformation getContextInformation();
}
