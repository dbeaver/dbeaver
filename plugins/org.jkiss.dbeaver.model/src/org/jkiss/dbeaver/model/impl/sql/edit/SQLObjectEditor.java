/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.sql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.*;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.ProxyPropertyDescriptor;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JDBC object editor
 */
public abstract class SQLObjectEditor<OBJECT_TYPE extends DBSObject, CONTAINER_TYPE extends DBSObject>
    extends AbstractObjectManager<OBJECT_TYPE>
    implements
        DBEObjectEditor<OBJECT_TYPE>,
        DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE>
{
    public static final String PATTERN_ITEM_INDEX = "%INDEX%"; //$NON-NLS-1$
    public static final String PATTERN_ITEM_TABLE = "%TABLE%"; //$NON-NLS-1$
    public static final String PATTERN_ITEM_INDEX_SHORT = "%INDEX_SHORT%"; //$NON-NLS-1$
    public static final String PATTERN_ITEM_CONSTRAINT = "%CONSTRAINT%"; //$NON-NLS-1$

    @Override
    public boolean canEditObject(OBJECT_TYPE object)
    {
        return true;
    }

    @Override
    public final DBEPropertyHandler<OBJECT_TYPE> makePropertyHandler(OBJECT_TYPE object, DBPPropertyDescriptor property)
    {
        return new PropertyHandler(property);
    }

    @Override
    public boolean canCreateObject(CONTAINER_TYPE parent)
    {
        return true;
    }

    @Override
    public boolean canDeleteObject(OBJECT_TYPE object)
    {
        return true;
    }

    //////////////////////////////////////////////////
    // Commands

    @Override
    public final OBJECT_TYPE createNewObject(DBRProgressMonitor monitor, DBECommandContext commandContext, CONTAINER_TYPE parent, Object copyFrom) throws DBException {
        OBJECT_TYPE newObject;
        try {
            newObject = createDatabaseObject(monitor, commandContext, parent, copyFrom);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Can't create object here.\nWrong container type: " + parent.getClass().getSimpleName());
        }
        if (newObject == null) {
            return null;
        }

        final ObjectCreateCommand createCommand = makeCreateCommand(newObject);
        commandContext.getUserParams().put(newObject, createCommand);
        commandContext.addCommand(createCommand, new CreateObjectReflector<>(this), true);

        createObjectReferences(monitor, commandContext, createCommand);

        return newObject;
    }

    protected void createObjectReferences(DBRProgressMonitor monitor, DBECommandContext commandContext, ObjectCreateCommand createCommand) throws DBException {
        // Do nothing. Derived implementations may add extra handling
    }

    @Override
    public final void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options)
    {
        commandContext.addCommand(
            new ObjectDeleteCommand(object, ModelMessages.model_jdbc_delete_object),
            new DeleteObjectReflector<>(this),
            true);
    }

    public ObjectCreateCommand makeCreateCommand(OBJECT_TYPE object)
    {
        return new ObjectCreateCommand(object, ModelMessages.model_jdbc_create_new_object);
    }

    protected abstract OBJECT_TYPE createDatabaseObject(
        DBRProgressMonitor monitor,
        DBECommandContext context,
        CONTAINER_TYPE parent,
        Object copyFrom) throws DBException;

    //////////////////////////////////////////////////
    // Actions

    protected abstract void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command);

    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {

    }

    protected void addObjectExtraActions(List<DBEPersistAction> actions, NestedObjectCommand<OBJECT_TYPE, PropertyHandler> command)
    {

    }

    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command)
    {
        // Base SQL syntax do not support object properties change
        throw new IllegalStateException("Object rename is not supported in " + getClass().getSimpleName()); //$NON-NLS-1$
    }

    protected abstract void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command);

    //////////////////////////////////////////////////
    // Properties

    protected StringBuilder getNestedDeclaration(CONTAINER_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command)
    {
        return null;
    }

    protected void validateObjectProperty(OBJECT_TYPE object, DBPPropertyDescriptor property, Object value) throws DBException
    {

    }

    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {

    }

    protected void processObjectRename(DBECommandContext commandContext, OBJECT_TYPE object, String newName) throws DBException
    {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }

    protected class PropertyHandler
        extends ProxyPropertyDescriptor
        implements DBEPropertyHandler<OBJECT_TYPE>, DBEPropertyReflector<OBJECT_TYPE>, DBEPropertyValidator<OBJECT_TYPE>
    {
        private PropertyHandler(DBPPropertyDescriptor property)
        {
            super(property);
        }

        @Override
        public DBECommandComposite<OBJECT_TYPE, ? extends DBEPropertyHandler<OBJECT_TYPE>> createCompositeCommand(OBJECT_TYPE object)
        {
            return new ObjectChangeCommand(object);
        }

        @Override
        public void reflectValueChange(OBJECT_TYPE object, Object oldValue, Object newValue)
        {
        }

        @Override
        public String toString()
        {
            return original.getDisplayName();
        }

        @Override
        public int hashCode()
        {
            return original.getId().hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj != null &&
                obj.getClass() == PropertyHandler.class &&
                //editor == ((PropertyHandler)obj).editor &&
                getId().equals(((PropertyHandler) obj).getId());
        }

        @Override
        public void validate(OBJECT_TYPE object, Object value) throws DBException
        {
            validateObjectProperty(object, original, value);
        }

    }

    //////////////////////////////////////////////////
    // Command objects

    protected static abstract class NestedObjectCommand<OBJECT_TYPE extends DBSObject, HANDLER_TYPE extends DBEPropertyHandler<OBJECT_TYPE>> extends DBECommandComposite<OBJECT_TYPE, HANDLER_TYPE> {

        protected NestedObjectCommand(OBJECT_TYPE object, String title)
        {
            super(object, title);
        }

        public abstract String getNestedDeclaration(DBSObject owner);

    }

    protected class ObjectChangeCommand extends NestedObjectCommand<OBJECT_TYPE, PropertyHandler>
    {
        public ObjectChangeCommand(OBJECT_TYPE object)
        {
            super(object, "JDBC Composite"); //$NON-NLS-1$
        }

        @Override
        public DBEPersistAction[] getPersistActions()
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectModifyActions(actions, this);
            addObjectExtraActions(actions, this);
            return actions.toArray(new DBEPersistAction[actions.size()]);
        }

        @Override
        public void validateCommand() throws DBException
        {
            validateObjectProperties(this);
        }

        @Override
        public String getNestedDeclaration(DBSObject owner)
        {
            // It is a trick
            // This method may be invoked from another Editor with different OBJECT_TYPE and CONTAINER_TYPE
            // TODO: May be we should make ObjectChangeCommand static
            final StringBuilder decl = SQLObjectEditor.this.getNestedDeclaration((CONTAINER_TYPE) owner, this);
            return CommonUtils.isEmpty(decl) ? null : decl.toString();
        }
    }

    protected class ObjectCreateCommand extends NestedObjectCommand<OBJECT_TYPE, PropertyHandler> {

        protected ObjectCreateCommand(OBJECT_TYPE object, String title)
        {
            super(object, title);
        }

        @Override
        public DBEPersistAction[] getPersistActions()
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectCreateActions(actions, this);
            addObjectExtraActions(actions, this);
            return actions.toArray(new DBEPersistAction[actions.size()]);
        }

        @Override
        public void updateModel()
        {
            super.updateModel();
            OBJECT_TYPE object = getObject();
            if (!object.isPersisted()) {
                if (object instanceof DBPSaveableObject) {
                    ((DBPSaveableObject)object).setPersisted(true);
                }
                DBUtils.fireObjectUpdate(object);
            }
        }

        @Override
        public String getNestedDeclaration(DBSObject owner)
        {
            // It is a trick
            // This method may be invoked from another Editor with different OBJECT_TYPE and CONTAINER_TYPE
            // TODO: May be we should make ObjectChangeCommand static
            final StringBuilder decl = SQLObjectEditor.this.getNestedDeclaration((CONTAINER_TYPE) owner, this);
            return CommonUtils.isEmpty(decl) ? null : decl.toString();
        }
    }

    protected class ObjectDeleteCommand extends DBECommandDeleteObject<OBJECT_TYPE> {
        protected ObjectDeleteCommand(OBJECT_TYPE table, String title)
        {
            super(table, title);
        }

        @Override
        public DBEPersistAction[] getPersistActions()
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectDeleteActions(actions, this);
            return actions.toArray(new DBEPersistAction[actions.size()]);
        }

        @Override
        public void updateModel()
        {
            OBJECT_TYPE object = getObject();
            DBSObjectCache<? extends DBSObject, OBJECT_TYPE> cache = getObjectsCache(object);
            if (cache != null) {
                cache.removeObject(object, false);
            }
        }
    }

    protected class ObjectRenameCommand extends DBECommandAbstract<OBJECT_TYPE> {
        private String oldName;
        private String newName;

        public ObjectRenameCommand(OBJECT_TYPE object, String title, String newName)
        {
            super(object, title);
            this.oldName = object.getName();
            this.newName = newName;
        }

        public String getOldName()
        {
            return oldName;
        }

        public String getNewName()
        {
            return newName;
        }

        @Override
        public DBEPersistAction[] getPersistActions()
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectRenameActions(actions, this);
            return actions.toArray(new DBEPersistAction[actions.size()]);
        }

        @Override
        public void updateModel()
        {
            if (getObject() instanceof DBPNamedObject2) {
                ((DBPNamedObject2)getObject()).setName(newName);
                DBUtils.fireObjectUpdate(getObject());
            }
        }
    }

    protected static class EmptyCommand extends DBECommandAbstract<DBPObject>
    {
        public EmptyCommand(DBPObject object)
        {
            super(object, "Empty"); //$NON-NLS-1$
        }
    }

    public class RenameObjectReflector implements DBECommandReflector<OBJECT_TYPE, ObjectRenameCommand> {

        @Override
        public void redoCommand(ObjectRenameCommand command)
        {
            if (command.getObject() instanceof DBPNamedObject2) {
                ((DBPNamedObject2)command.getObject()).setName(command.newName);
                DBUtils.fireObjectUpdate(command.getObject());
            }
        }

        @Override
        public void undoCommand(ObjectRenameCommand command)
        {
            if (command.getObject() instanceof DBPNamedObject2) {
                ((DBPNamedObject2)command.getObject()).setName(command.oldName);
                DBUtils.fireObjectUpdate(command.getObject());
            }
        }

    }

    public static class RefreshObjectReflector<OBJECT_TYPE extends DBSObject> implements DBECommandReflector<OBJECT_TYPE, DBECommandAbstract<OBJECT_TYPE>> {

        @Override
        public void redoCommand(DBECommandAbstract<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectSelect(command.getObject(), true);
        }

        @Override
        public void undoCommand(DBECommandAbstract<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectUpdate(command.getObject(), true);
        }

    }

}

