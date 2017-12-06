package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.debug.core.model.DebugElement;

public class DatabaseDebugElement extends DebugElement {
    
    public DatabaseDebugElement(IDatabaseDebugTarget target)
    {
        super(target);
    }

    public IDatabaseDebugTarget geDatabaseDebugTarget() {
        return (IDatabaseDebugTarget) getDebugTarget();
    }

    @Override
    public String getModelIdentifier()
    {
        return getDebugTarget().getModelIdentifier();
    }

}
