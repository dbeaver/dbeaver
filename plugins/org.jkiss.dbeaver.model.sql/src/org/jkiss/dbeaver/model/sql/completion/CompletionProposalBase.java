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
package org.jkiss.dbeaver.model.sql.completion;

import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPKeywordType;

public abstract class CompletionProposalBase {

    /**
     * The offset at which completion proposal will be applied
     */
    public abstract int getReplacementOffset();

    /**
     * The string that will be inserted to the replacement offset when completion proposal is applied
     */
    public abstract String getReplacementString();

    /**
     * The display string for the completion proposal.
     * This is typically used in the UI to show the proposal to the user.
     */
    public abstract String getDisplayString();

    /**
     * The type of the completion proposal.
     * This is used to categorize the proposal, such as keyword, object, etc.
     */
    public abstract DBPKeywordType getProposalType();

    /**
     * The length of the replacement string.
     * This is used to determine how many characters will be replaced in the document.
     */
    public abstract int getReplacementLength();

    /**
     * The score of the completion proposal.
     * This is used to rank the proposal among other proposals.
     * Higher scores indicate more relevant proposals.
     */
    public abstract int getProposalScore();

    /**
     * The sorter score of the completion proposal for specific type.
     * This is used to sort the proposals in the UI.
     * Higher scores indicate higher priority in sorting.
     */
    public abstract int getProposalTypeSorterScore();

    /**
     * Returns the image associated with the object of this proposal.
     * This is typically used to display an icon in the UI alongside the proposal.
     *
     * @return the image associated with the object of this proposal
     */
    public abstract DBPImage getObjectImage();

    @Override
    public boolean equals(Object o) {
        if (o instanceof CompletionProposalBase other) {
            return this.getReplacementOffset() == other.getReplacementOffset()
                && this.getReplacementString().equals(other.getReplacementString());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 7;
        hashCode = hashCode * 31 + this.getReplacementString().hashCode();
        hashCode = hashCode * 31 + Integer.hashCode(this.getReplacementOffset());
        return hashCode;
    }
}
