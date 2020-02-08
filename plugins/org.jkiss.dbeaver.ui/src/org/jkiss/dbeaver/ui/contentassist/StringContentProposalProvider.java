package org.jkiss.dbeaver.ui.contentassist;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import java.util.ArrayList;
import java.util.List;

public class StringContentProposalProvider implements IContentProposalProvider {

    private String[] proposals;
    private String possibleChars;

    public StringContentProposalProvider() {
    }

    public StringContentProposalProvider(String... proposals) {
        setProposals(proposals);
    }

    public IContentProposal[] getProposals(String contents, int position) {
        List<ContentProposal> list = new ArrayList<>();
        int startPos = 0;
        for (int i = position - 1; i >= 0; i--) {
            char ch = Character.toUpperCase(contents.charAt(i));
            if (!Character.isLetterOrDigit(ch) && possibleChars.indexOf(ch) == -1) {
                startPos = i + 1;
                break;
            }
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
