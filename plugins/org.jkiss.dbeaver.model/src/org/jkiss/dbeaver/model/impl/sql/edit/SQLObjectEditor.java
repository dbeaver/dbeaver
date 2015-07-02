/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.*;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.impl.ProxyPropertyDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * JDBC object editor
 */
public abstract class SQLObjectEditor<OBJECT_TYPE extends DBSObject & DBPSaveableObject, CONTAINER_TYPE extends DBSObject>
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

    @Override
    public final OBJECT_TYPE createNewObject(DBECommandContext commandContext, CONTAINER_TYPE parent, Object copyFrom)
    {
        OBJECT_TYPE newObject = createDatabaseObject(commandContext, parent, copyFrom);
        if (newObject == null) {
            return null;
        }

        final ObjectCreateCommand createCommand = makeCreateCommand(newObject);
        commandContext.getUserParams().put(newObject, createCommand);
        commandContext.addCommand(createCommand, new CreateObjectReflector<OBJECT_TYPE>(this), true);

        return newObject;
    }

    @Override
    public final void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options)
    {
        commandContext.addCommand(
            new ObjectDeleteCommand(object, ModelMessages.model_jdbc_delete_object),
            new DeleteObjectReflector<OBJECT_TYPE>(this),
            true);
    }

    public ObjectCreateCommand makeCreateCommand(OBJECT_TYPE object)
    {
        return new ObjectCreateCommand(object, ModelMessages.model_jdbc_create_new_object);
    }

    protected abstract OBJECT_TYPE createDatabaseObject(
        DBECommandContext context,
        CONTAINER_TYPE parent,
        Object copyFrom);

    protected abstract DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command);

    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        // Base SQL syntax do not support object properties change
        throw new IllegalStateException("Object modification is not supported in " + getClass().getSimpleName()); //$NON-NLS-1$
    }

    protected DBEPersistAction[] makeObjectRenameActions(ObjectRenameCommand command)
    {
        // Base SQL syntax do not support object properties change
        throw new IllegalStateException("Object rename is not supported in " + getClass().getSimpleName()); //$NON-NLS-1$
    }

    protected abstract DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command);

    protected StringBuilder getNestedDeclaration(CONTAINER_TYPE owner, DBECommandComposite<OBJECT_TYPE, PropertyHandler> command)
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
        commandContext.addCommand(command, new RenameObjectReflector());
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

    protected static abstract class NestedObjectCommand<OBJECT_TYPE extends DBSObject & DBPSaveableObject, HANDLER_TYPE extends DBEPropertyHandler<OBJECT_TYPE>> extends DBECommandComposite<OBJECT_TYPE, HANDLER_TYPE> {

        protected NestedObjectCommand(OBJECT_TYPE object, String title)
        {
            super(object, title);
        }

        public abstract String getNestedDeclaration(DBSObject owner);

    }

    protected class ObjectChangeCommand extends NestedObjectCommand<OBJECT_TYPE, PropertyHandler>
    {
        private ObjectChangeCommand(OBJECT_TYPE object)
        {
            super(object, "JDBC Composite"); //$NON-NLS-1$
        }

        @Override
        public DBEPersistAction[] getPersistActions()
        {
            return makeObjectModifyActions(this);
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
            return makeObjectCreateActions(this);
        }

        @Override
        public void updateModel()
        {
            super.updateModel();
            OBJECT_TYPE object = getObject();
            if (!object.isPersisted()) {
                object.setPersisted(true);
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
            return makeObjectDeleteActions(this);
        }

        @Override
        public void updateModel()
        {
            OBJECT_TYPE object = getObject();
            DBSObjectCache<? extends DBSObject, OBJECT_TYPE> cache = getObjectsCache(object);
            if (cache != null) {
                cache.removeObject(object);
            }
        }
    }

    protected class ObjectRenameCommand extends DBECommandAbstract<OBJECT_TYPE> {
        private String oldName;
        private String newName;

        protected ObjectRenameCommand(OBJECT_TYPE object, String title, String newName)
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
            return makeObjectRenameActions(this);
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

}

