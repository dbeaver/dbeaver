/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.contentassist;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import java.util.ArrayList;
import java.util.List;

public class StringContentProposalProvider implements IContentProposalProvider {

    private String[] proposals;
    private String possibleChars;
    private boolean constrainProposalList;
    private boolean stopPropose;

    public StringContentProposalProvider() {
    }

    public StringContentProposalProvider(String... proposals) {
        setProposals(proposals);
    }

    public StringContentProposalProvider(boolean constrainProposalList, String... proposals) {
        setProposals(proposals);
        this.constrainProposalList = constrainProposalList;
    }

    public IContentProposal[] getProposals(String contents, int position) {
        if (stopPropose && position < 2) {
            stopPropose = false;
        }
        List<ContentProposal> list = new ArrayList<>();
        int startPos = 0;
        for (int i = position - 1; i >= 0; i--) {
            char ch = Character.toUpperCase(contents.charAt(i));
            if (!Character.isLetterOrDigit(ch) && possibleChars.indexOf(ch) == -1) {
                startPos = i + 1;
                break;
            }
        }
        Character lastChar = null;
        if (contents.length() > 0) {
            lastChar = contents.charAt(contents.length() - 1);
        }
        if (lastChar != null && !Character.isLetterOrDigit(lastChar) && lastChar != '_' && lastChar != ' ' && constrainProposalList) {
            stopPropose = true; //stop proposing after parentheses or other characters
            return new IContentProposal[0];
        }
        if (stopPropose && position > 1) {
            return new IContentProposal[0];
        }
        String word = contents.substring(startPos, position);
        for (String proposal : proposals) {
            if (proposal.length() >= word.length() && proposal.substring(0, word.length()).equalsIgnoreCase(word)) {
                list.add(new ContentProposal(proposal));
            }
        }
        return list.toArray(new IContentProposal[0]);
    }

    public void setProposals(String[] items) {
        this.proposals = items;
        StringBuilder allChars = new StringBuilder();
        for (String prop : proposals) {
            for (char c : prop.toCharArray()) {
                c = Character.toUpperCase(c);
                if (allChars.indexOf(String.valueOf(c)) == -1) {
                    allChars.append(c);
                }
            }
        }
        this.possibleChars = allChars.toString();
    }
}
