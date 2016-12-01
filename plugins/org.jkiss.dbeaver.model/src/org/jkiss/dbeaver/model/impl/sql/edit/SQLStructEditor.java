/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.sql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * JDBC struct editor
 */
public abstract class SQLStructEditor<OBJECT_TYPE extends DBSObject, CONTAINER_TYPE extends DBSObject>
    extends SQLObjectEditor<OBJECT_TYPE, CONTAINER_TYPE>
    implements DBEStructEditor<OBJECT_TYPE>
{

    protected abstract void addStructObjectCreateActions(List<DBEPersistAction> actions, StructCreateCommand command);

    @Override
    public StructCreateCommand makeCreateCommand(OBJECT_TYPE object)
    {
        return new StructCreateCommand(object, ModelMessages.model_jdbc_create_new_object);
    }

    protected Collection<NestedObjectCommand> getNestedOrderedCommands(final StructCreateCommand structCommand)
    {
        List<NestedObjectCommand> nestedCommands = new ArrayList<>();
        nestedCommands.addAll(structCommand.getObjectCommands().values());
        Collections.sort(nestedCommands, new Comparator<NestedObjectCommand>() {
            @Override
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

    protected void createObjectReferences(DBRProgressMonitor monitor, DBECommandContext commandContext, ObjectCreateCommand createCommand) throws DBException {
        OBJECT_TYPE object = createCommand.getObject();
        final DBERegistry editorsRegistry = object.getDataSource().getContainer().getPlatform().getEditorsRegistry();
        for (Class childType : getChildTypes()) {
            Collection<? extends DBSObject> children = getChildObjects(monitor, object, childType);
            if (!CommonUtils.isEmpty(children)) {
                SQLObjectEditor<DBSObject, CONTAINER_TYPE> nestedEditor = getObjectEditor(editorsRegistry, childType);
                if (nestedEditor != null) {
                    for (DBSObject child : children) {
                        ObjectCreateCommand childCreateCommand = (ObjectCreateCommand) nestedEditor.makeCreateCommand(child);
                        //((StructCreateCommand)createCommand).aggregateCommand(childCreateCommand);
                        commandContext.addCommand(childCreateCommand, null, false);
                    }
                }
            }
        }
    }

    protected  <T extends DBSObject> SQLObjectEditor<T, OBJECT_TYPE> getObjectEditor(DBERegistry editorsRegistry, Class<T> type) {
        final Class<? extends T> childType = getChildType(type);
        return childType == null ? null : editorsRegistry.getObjectManager(childType, SQLObjectEditor.class);
    }

    protected <T> Class<? extends T> getChildType(Class<T> type) {
        for (Class<?> childType : getChildTypes()) {
            if (type.isAssignableFrom(childType)) {
                return (Class<? extends T>) childType;
            }
        }
        return null;
    }

    @Override
    public Collection<? extends DBSObject> getChildObjects(DBRProgressMonitor monitor, OBJECT_TYPE object, Class<? extends DBSObject> childType) throws DBException {
        return null;
    }

    protected class StructCreateCommand extends ObjectCreateCommand
        implements DBECommandAggregator<OBJECT_TYPE> {

        private final Map<DBPObject, NestedObjectCommand> objectCommands = new LinkedHashMap<>();

        public StructCreateCommand(OBJECT_TYPE object, String table)
        {
            super(object, table);
            objectCommands.put(getObject(), this);
        }

        public Map<DBPObject, NestedObjectCommand> getObjectCommands()
        {
            return objectCommands;
        }

        @Override
        public boolean aggregateCommand(DBECommand<?> command)
        {
            if (command instanceof NestedObjectCommand) {
                objectCommands.put(command.getObject(), (NestedObjectCommand) command);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void resetAggregatedCommands()
        {
            objectCommands.clear();
            objectCommands.put(getObject(), this);
        }

        @Override
        public DBEPersistAction[] getPersistActions()
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addStructObjectCreateActions(actions, this);
            addObjectExtraActions(actions, this);
            return actions.toArray(new DBEPersistAction[actions.size()]);
        }
    }



}

