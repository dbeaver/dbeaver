package org.jkiss.dbeaver.debug.sourcelookup;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

public class ProcedureSourceLookupDirector extends AbstractSourceLookupDirector {

    @Override
    public void initializeParticipants() {
        addParticipants(new ISourceLookupParticipant[] {new ProcedureSourceLookupParticipant()});
    }

}
