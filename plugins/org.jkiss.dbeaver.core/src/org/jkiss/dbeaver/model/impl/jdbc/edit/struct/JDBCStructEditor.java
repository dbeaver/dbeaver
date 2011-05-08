/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * JDBC struct editor
 */
public abstract class JDBCStructEditor<OBJECT_TYPE extends DBSEntity & DBPSaveableObject>
    extends JDBCObjectEditor<OBJECT_TYPE>
    implements DBEStructEditor<OBJECT_TYPE>
{

    protected abstract IDatabasePersistAction[] makePersistActions(StructCreateCommand command);

    protected class StructCreateCommand
        extends ObjectSaveCommand<OBJECT_TYPE>
        implements DBECommandAggregator<OBJECT_TYPE> {

        private final Map<DBPObject, ObjectChangeCommand> objectCommands = new IdentityHashMap<DBPObject, ObjectChangeCommand>();

        public StructCreateCommand(OBJECT_TYPE object)
        {
            super(object, "Create struct");
        }

        public Map<DBPObject, ObjectChangeCommand> getObjectCommands()
        {
            return objectCommands;
        }

        public boolean aggregateCommand(DBECommand<?> command)
        {
            if (ObjectChangeCommand.class.isAssignableFrom(command.getClass())) {
                objectCommands.put(command.getObject(), (ObjectChangeCommand) command);
                return true;
            } else {
                return false;
            }
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return makePersistActions(this);
        }
    }



}

