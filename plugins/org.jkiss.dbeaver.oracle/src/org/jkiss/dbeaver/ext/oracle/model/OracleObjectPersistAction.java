package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;

/**
 * Oracle persist action
 */
public class OracleObjectPersistAction extends AbstractDatabasePersistAction {

    private final OracleObjectType objectType;

    public OracleObjectPersistAction(OracleObjectType objectType, String title, String script)
    {
        super(title, script);
        this.objectType = objectType;
    }

    public OracleObjectPersistAction(OracleObjectType objectType, String script)
    {
        super(script);
        this.objectType = objectType;
    }

    public OracleObjectType getObjectType()
    {
        return objectType;
    }
}
