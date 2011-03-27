/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Script command
 */
public class DatabaseObjectScriptCommand<OBJECT_TYPE extends DBSObject> extends DBECommandImpl<OBJECT_TYPE> {

    private String script;

    public DatabaseObjectScriptCommand(String title, String script)
    {
        super(title);
        this.script = script;
    }

    public void updateModel(OBJECT_TYPE object)
    {
    }

    public IDatabasePersistAction[] getPersistActions(OBJECT_TYPE object)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                getTitle(),
                script)
        };
    }

}