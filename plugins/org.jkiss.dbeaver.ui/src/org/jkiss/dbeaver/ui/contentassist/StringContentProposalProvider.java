package org.jkiss.dbeaver.ui.contentassist;

import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;

public class StringContentProposalProvider extends SimpleContentProposalProvider {

    public StringContentProposalProvider() {
    }

    public StringContentProposalProvider(String... proposals) {
        setProposals(proposals);
    }

}
