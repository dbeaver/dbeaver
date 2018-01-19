package org.jkiss.dbeaver.debug.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.jkiss.dbeaver.debug.core.model.DatabaseStackFrame;

public class ProcedureSourceLookupParticipant extends AbstractSourceLookupParticipant {

    @Override
    public String getSourceName(Object object) throws CoreException {
        if (object instanceof DatabaseStackFrame) {
            DatabaseStackFrame stackFrame = (DatabaseStackFrame) object;
            return stackFrame.getName();
        }
        return String.valueOf(object);
    }

}
