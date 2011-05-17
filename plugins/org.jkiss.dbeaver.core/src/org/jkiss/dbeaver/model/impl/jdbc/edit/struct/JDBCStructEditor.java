/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandAggregator;
import org.jkiss.dbeaver.model.edit.DBEStructEditor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.*;

/**
 * JDBC struct editor
 */
public abstract class JDBCStructEditor<OBJECT_TYPE extends DBSEntity & DBPSaveableObject, CONTAINER_TYPE extends DBSObject>
    extends JDBCObjectEditor<OBJECT_TYPE, CONTAINER_TYPE>
    implements DBEStructEditor<OBJECT_TYPE>
{

    protected abstract IDatabasePersistAction[] makeStructObjectCreateActions(StructCreateCommand command);

    ObjectCreateCommand makeCreateCommand(OBJECT_TYPE object)
    {
        return new StructCreateCommand(object, "Create new object");
    }

    protected Collection<ObjectChangeCommand> getNestedOrderedCommands(StructCreateCommand structCommand)
    {
        List<ObjectChangeCommand> nestedCommands = new ArrayList<ObjectChangeCommand>();
        nestedCommands.addAll(structCommand.getObjectCommands().values());
        Collections.sort(nestedCommands, new Comparator<ObjectChangeCommand>() {
            public int compare(ObjectChangeCommand o1, ObjectChangeCommand o2)
            {
                final DBPObject object1 = o1.getObject();
                final DBPObject object2 = o2.getObject();
                int order1 = -1, order2 = 1;
                Class<?>[] childTypes = getChildTypes();
                for (int i = 0, childTypesLength = childTypes.length; i < childTypesLength; i++) {
                    Class<?> childType = childTypes[i];
                    if (childType.isAssignableFrom(object1.getClass())) {
                        order1 = i;
                    }
                    if (childType.isAssignableFrom(object2.getClass())) {
                        order2 = i;
                    }
                }
                return order1 - order2;
            }
        });

        return nestedCommands;
    }

    protected class StructCreateCommand extends ObjectCreateCommand
        implements DBECommandAggregator<OBJECT_TYPE> {

        private final Map<DBPObject, ObjectChangeCommand> objectCommands = new IdentityHashMap<DBPObject, ObjectChangeCommand>();

        public StructCreateCommand(OBJECT_TYPE object, String table)
        {
            super(object, table);
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

        public void resetAggregatedCommands()
        {
            objectCommands.clear();
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return makeStructObjectCreateActions(this);
        }
    }



}

