/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.jkiss.dbeaver.core.CoreMessages;
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
        return new StructCreateCommand(object, CoreMessages.model_jdbc_create_new_object);
    }

    protected Collection<NestedObjectCommand> getNestedOrderedCommands(final StructCreateCommand structCommand)
    {
        List<NestedObjectCommand> nestedCommands = new ArrayList<NestedObjectCommand>();
        nestedCommands.addAll(structCommand.getObjectCommands().values());
        Collections.sort(nestedCommands, new Comparator<NestedObjectCommand>() {
            public int compare(NestedObjectCommand o1, NestedObjectCommand o2)
            {
                final DBPObject object1 = o1.getObject();
                final DBPObject object2 = o2.getObject();
                if (object1 == structCommand.getObject()) {
                    return 1;
                } else if (object2 == structCommand.getObject()) {
                    return -1;
                }
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

        private final Map<DBPObject, NestedObjectCommand> objectCommands = new LinkedHashMap<DBPObject, NestedObjectCommand>();

        public StructCreateCommand(OBJECT_TYPE object, String table)
        {
            super(object, table);
        }

        public Map<DBPObject, NestedObjectCommand> getObjectCommands()
        {
            return objectCommands;
        }

        public boolean aggregateCommand(DBECommand<?> command)
        {
            if (command instanceof NestedObjectCommand) {
                objectCommands.put(command.getObject(), (NestedObjectCommand) command);
                return true;
            } else {
                return false;
            }
        }

        public void resetAggregatedCommands()
        {
            objectCommands.clear();
            objectCommands.put(getObject(), this);
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return makeStructObjectCreateActions(this);
        }
    }



}

