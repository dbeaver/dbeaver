/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Script command
 */
public class DatabaseObjectScriptCommand<OBJECT_TYPE extends DBSObject> extends DBECommandAbstract<OBJECT_TYPE> {

    private String script;

    public DatabaseObjectScriptCommand(OBJECT_TYPE object, String title, String script)
    {
        super(object, title);
        this.script = script;
    }

    @Override
    public void updateModel()
    {
    }

    @Override
    public IDatabasePersistAction[] getPersistActions()
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                getTitle(),
                script)
        };
    }

}