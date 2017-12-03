package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.debug.core.model.DebugElement;

public class DatabaseDebugElement extends DebugElement {
    
    private final String modelIdentifier;

    public DatabaseDebugElement(String modelIdentifier, IDatabaseDebugTarget target)
    {
        super(target);
        this.modelIdentifier = modelIdentifier;
    }

    public IDatabaseDebugTarget geDatabaseDebugTarget() {
        return (IDatabaseDebugTarget) getDebugTarget();
    }

    @Override
    public String getModelIdentifier()
    {
        return modelIdentifier;
    }

}
