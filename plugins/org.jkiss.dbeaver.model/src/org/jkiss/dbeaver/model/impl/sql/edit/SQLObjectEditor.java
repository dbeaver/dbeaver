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
import org.jkiss.dbeaver.model.*;
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

    protected abstract void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options);

    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {

    }

    protected void addObjectExtraActions(List<DBEPersistAction> actions, NestedObjectCommand<OBJECT_TYPE, PropertyHandler> command, Map<String, Object> options)
    {

    }

    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        // Base SQL syntax do not support object properties change
        throw new IllegalStateException("Object rename is not supported in " + getClass().getSimpleName()); //$NON-NLS-1$
    }

    protected void addObjectReorderActions(List<DBEPersistAction> actions, ObjectReorderCommand command, Map<String, Object> options)
    {
        if (command.getObject().isPersisted()) {
            // Not supported by implementation
            throw new IllegalStateException("Object reorder is not supported in " + getClass().getSimpleName()); //$NON-NLS-1$
        }
    }

    protected abstract void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options);

    //////////////////////////////////////////////////
    // Properties

    protected StringBuilder getNestedDeclaration(CONTAINER_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command, Map<String, Object> options)
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

    protected void processObjectReorder(DBECommandContext commandContext, OBJECT_TYPE object, List<? extends DBPOrderedObject> siblings, int newPosition) throws DBException
    {
        ObjectReorderCommand command = new ObjectReorderCommand(object, siblings, ModelMessages.model_jdbc_reorder_object, newPosition);
        commandContext.addCommand(command, new ReorderObjectReflector(), true);
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

        public abstract String getNestedDeclaration(DBSObject owner, Map<String, Object> options);

    }

    protected static class EmptyCommand extends DBECommandAbstract<DBPObject>
    {
        public EmptyCommand(DBPObject object)
        {
            super(object, "Empty"); //$NON-NLS-1$
        }
    }

    protected class ObjectChangeCommand extends NestedObjectCommand<OBJECT_TYPE, PropertyHandler>
    {
        public ObjectChangeCommand(OBJECT_TYPE object)
        {
            super(object, "JDBC Composite"); //$NON-NLS-1$
        }

        @Override
        public DBEPersistAction[] getPersistActions(Map<String, Object> options)
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectModifyActions(actions, this, options);
            addObjectExtraActions(actions, this, options);
            return actions.toArray(new DBEPersistAction[actions.size()]);
        }

        @Override
        public void validateCommand() throws DBException
        {
            validateObjectProperties(this);
        }

        @Override
        public String getNestedDeclaration(DBSObject owner, Map<String, Object> options)
        {
            // It is a trick
            // This method may be invoked from another Editor with different OBJECT_TYPE and CONTAINER_TYPE
            // TODO: May be we should make ObjectChangeCommand static
            final StringBuilder decl = SQLObjectEditor.this.getNestedDeclaration((CONTAINER_TYPE) owner, this, options);
            return CommonUtils.isEmpty(decl) ? null : decl.toString();
        }
    }

    protected class ObjectCreateCommand extends NestedObjectCommand<OBJECT_TYPE, PropertyHandler> {

        protected ObjectCreateCommand(OBJECT_TYPE object, String title)
        {
            super(object, title);
        }

        @Override
        public DBEPersistAction[] getPersistActions(Map<String, Object> options)
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectCreateActions(actions, this, options);
            addObjectExtraActions(actions, this, options);
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
        public String getNestedDeclaration(DBSObject owner, Map<String, Object> options)
        {
            // It is a trick
            // This method may be invoked from another Editor with different OBJECT_TYPE and CONTAINER_TYPE
            // TODO: May be we should make ObjectChangeCommand static
            final StringBuilder decl = SQLObjectEditor.this.getNestedDeclaration((CONTAINER_TYPE) owner, this, options);
            return CommonUtils.isEmpty(decl) ? null : decl.toString();
        }
    }

    protected class ObjectDeleteCommand extends DBECommandDeleteObject<OBJECT_TYPE> {
        protected ObjectDeleteCommand(OBJECT_TYPE table, String title)
        {
            super(table, title);
        }

        @Override
        public DBEPersistAction[] getPersistActions(Map<String, Object> options)
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectDeleteActions(actions, this, options);
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
        public DBEPersistAction[] getPersistActions(Map<String, Object> options)
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectRenameActions(actions, this, options);
            return actions.toArray(new DBEPersistAction[actions.size()]);
        }

        @Override
        public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams) {
            // We need very first and very last rename commands. They produce final rename
            final String mergeId = "rename" + getObject().hashCode();
            ObjectRenameCommand renameCmd = (ObjectRenameCommand) userParams.get(mergeId);
            if (renameCmd == null) {
                renameCmd = new ObjectRenameCommand(getObject(), getTitle(), newName);
                userParams.put(mergeId, renameCmd);
            } else {
                renameCmd.newName = newName;
                return renameCmd;
            }
            return super.merge(prevCommand, userParams);
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

    protected class ObjectReorderCommand extends DBECommandAbstract<OBJECT_TYPE> {
        private List<? extends DBPOrderedObject> siblings;
        private int oldPosition;
        private int newPosition;

        public ObjectReorderCommand(OBJECT_TYPE object, List<? extends DBPOrderedObject> siblings, String title, int newPosition)
        {
            super(object, title);
            this.siblings = siblings;
            this.oldPosition = ((DBPOrderedObject)object).getOrdinalPosition();
            this.newPosition = newPosition;
        }

        public List<? extends DBPOrderedObject> getSiblings() {
            return siblings;
        }

        public int getOldPosition() {
            return oldPosition;
        }

        public int getNewPosition() {
            return newPosition;
        }

        @Override
        public DBEPersistAction[] getPersistActions(Map<String, Object> options)
        {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectReorderActions(actions, this, options);
            return actions.toArray(new DBEPersistAction[actions.size()]);
        }

        @Override
        public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams) {
            // We need very first and very last reorder commands. They produce final rename
            final String mergeId = "reorder" + getObject().hashCode();
            ObjectReorderCommand reorderCmd = (ObjectReorderCommand) userParams.get(mergeId);
            if (reorderCmd == null) {
                reorderCmd = new ObjectReorderCommand(getObject(), siblings, getTitle(), newPosition);
                userParams.put(mergeId, reorderCmd);
            } else {
                reorderCmd.newPosition = newPosition;
                return reorderCmd;
            }
            return super.merge(prevCommand, userParams);
        }
    }

    public class ReorderObjectReflector implements DBECommandReflector<OBJECT_TYPE, ObjectReorderCommand> {

        @Override
        public void redoCommand(ObjectReorderCommand command)
        {
            OBJECT_TYPE object = command.getObject();

            // Update positions in sibling objects
            for (DBPOrderedObject sibling : command.getSiblings()) {
                if (sibling != object) {
                    int siblingPosition = sibling.getOrdinalPosition();
                    if (command.newPosition < command.oldPosition) {
                        if (siblingPosition >= command.newPosition && siblingPosition < command.oldPosition) {
                            sibling.setOrdinalPosition(siblingPosition + 1);
                        }
                    } else {
                        if (siblingPosition <= command.newPosition && siblingPosition > command.oldPosition) {
                            sibling.setOrdinalPosition(siblingPosition - 1);
                        }
                    }
                }
            }

            // Update target object position
            ((DBPOrderedObject) object).setOrdinalPosition(command.newPosition);
            // Refresh object AND parent
            final DBSObject parentObject = object.getParentObject();
            if (parentObject != null) {
                // We need to update order in navigator model
                DBUtils.fireObjectUpdate(parentObject, DBPEvent.REORDER);
            }
        }

        @Override
        public void undoCommand(ObjectReorderCommand command)
        {
            ((DBPOrderedObject)command.getObject()).setOrdinalPosition(command.oldPosition);
            final DBSObject parentObject = command.getObject().getParentObject();
            if (parentObject != null) {
                // We need to update order in navigator model
                DBUtils.fireObjectUpdate(parentObject, DBPEvent.REORDER);
            }
        }

    }

    public static class RefreshObjectReflector<OBJECT_TYPE extends DBSObject> implements DBECommandReflector<OBJECT_TYPE, DBECommandAbstract<OBJECT_TYPE>> {

        @Override
        public void redoCommand(DBECommandAbstract<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectRefresh(command.getObject());
        }

        @Override
        public void undoCommand(DBECommandAbstract<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectUpdate(command.getObject(), true);
        }

    }

}

