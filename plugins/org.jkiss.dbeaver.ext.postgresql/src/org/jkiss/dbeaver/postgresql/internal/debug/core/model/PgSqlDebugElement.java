package org.jkiss.dbeaver.postgresql.internal.debug.core.model;

import org.eclipse.debug.core.model.DebugElement;
import org.jkiss.dbeaver.postgresql.debug.core.PgSqlDebugCore;

public class PgSqlDebugElement extends DebugElement {

    public PgSqlDebugElement(PgSqlDebugTarget target)
    {
        super(target);
    }

    @Override
    public String getModelIdentifier()
    {
        return PgSqlDebugCore.MODEL_IDENTIFIER;
    }
    
    public PgSqlDebugTarget gePgSqlDebugTarget() {
        return (PgSqlDebugTarget) getDebugTarget();
    }

}
