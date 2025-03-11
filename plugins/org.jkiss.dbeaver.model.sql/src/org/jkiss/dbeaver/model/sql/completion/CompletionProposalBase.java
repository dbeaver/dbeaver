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

public abstract class CompletionProposalBase {

    /**
     * The offset at which completion proposal will be applied
     */
    public abstract int getReplacementOffset();

    /**
     * The string that will be inserted to the replacement offset when completion proposal is applied
     */
    public abstract String getReplacementString();

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
