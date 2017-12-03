package org.jkiss.dbeaver.postgresql.internal.debug.core.model;

import org.jkiss.dbeaver.debug.core.model.DatabaseDebugElement;
import org.jkiss.dbeaver.debug.core.model.IDatabaseDebugTarget;
import org.jkiss.dbeaver.postgresql.debug.core.PgSqlDebugCore;

public class PgSqlDebugElement extends DatabaseDebugElement {

    public PgSqlDebugElement(IDatabaseDebugTarget target)
    {
        super(PgSqlDebugCore.MODEL_IDENTIFIER, target);
    }

    @Override
    public String getModelIdentifier()
    {
        return PgSqlDebugCore.MODEL_IDENTIFIER;
    }
    
}
