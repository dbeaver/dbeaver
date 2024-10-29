/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.ProxyPropertyDescriptor;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.cache.DBSCompositeCache;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Database object editor
 */
public abstract class SQLObjectEditor<OBJECT_TYPE extends DBSObject, CONTAINER_TYPE extends DBSObject>
        extends AbstractObjectManager<OBJECT_TYPE>
        implements
        DBEObjectEditor<OBJECT_TYPE>,
        DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE> {

    public static final String OPTION_SKIP_CONFIGURATION = "skip.object.configuration";
    // This option may be set by object configurer, e.g. to create other linked objects.
    // For example constraint for a columns.
    // Value of this property must be instance of DBRRunnableWithProgress
    public static final String OPTION_ADDITIONAL_ACTION = "additional.actions";

    public static final String PATTERN_ITEM_INDEX = "%INDEX%"; //$NON-NLS-1$
    public static final String PATTERN_ITEM_TABLE = "%TABLE%"; //$NON-NLS-1$
    public static final String PATTERN_ITEM_INDEX_SHORT = "%INDEX_SHORT%"; //$NON-NLS-1$
    public static final String PATTERN_ITEM_CONSTRAINT = "%CONSTRAINT%"; //$NON-NLS-1$
    private static final int MAX_NAME_GEN_ATTEMPTS = 100;

    @Override
    public boolean canEditObject(OBJECT_TYPE object) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public final DBEPropertyHandler<OBJECT_TYPE> makePropertyHandler(OBJECT_TYPE object, DBPPropertyDescriptor property) {
        return new PropertyHandler(property);
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canDeleteObject(@NotNull OBJECT_TYPE object) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    //////////////////////////////////////////////////
    // Commands

    @Override
    public final OBJECT_TYPE createNewObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext commandContext,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) throws DBException {
        OBJECT_TYPE newObject;
        try {
            newObject = createDatabaseObject(monitor, commandContext, container, copyFrom, options);
        } catch (ClassCastException e) {
            throw new DBException("Can't create object here.\nWrong container type: " + container.getClass().getSimpleName());
        }
        if (!CommonUtils.getOption(options, OPTION_SKIP_CONFIGURATION)) {
            newObject = configureObject(monitor, commandContext, container, newObject, options);
            if (newObject == null) {
                return null;
            }
        }

        final ObjectCreateCommand createCommand = makeCreateCommand(newObject, options);
        commandContext.getUserParams().put(newObject, createCommand);
        commandContext.addCommand(createCommand, new CreateObjectReflector<>(this), true);

        createObjectReferences(monitor, commandContext, createCommand);

        for (;;) {
            // Process additional actions
            // Any additional action may add another action in options
            Object additionalAction = options.remove(OPTION_ADDITIONAL_ACTION);
            if (additionalAction instanceof DBRRunnableWithProgress) {
                try {
                    ((DBRRunnableWithProgress) additionalAction).run(monitor);
                } catch (InvocationTargetException e) {
                    throw new DBException("Error processing additional create action", e.getTargetException());
                } catch (InterruptedException e) {
                    // ignore
                }
            } else {
                break;
            }
        }

        return newObject;
    }

    protected void createObjectReferences(DBRProgressMonitor monitor, DBECommandContext commandContext, ObjectCreateCommand createCommand) throws DBException {
        // Do nothing. Derived implementations may add extra handling
    }

    @Override
    public void deleteObject(@NotNull DBECommandContext commandContext, @NotNull OBJECT_TYPE object, @NotNull Map<String, Object> options) throws DBException {
        commandContext.addCommand(
                new ObjectDeleteCommand(object, ModelMessages.model_jdbc_delete_object),
                new DeleteObjectReflector<>(this),
                true);
    }

    public ObjectCreateCommand makeCreateCommand(OBJECT_TYPE object, Map<String, Object> options) {
        return new ObjectCreateCommand(object, ModelMessages.model_jdbc_create_new_object, options);
    }

    protected abstract OBJECT_TYPE createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        Object container,
        Object copyFrom,
        @NotNull Map<String, Object> options) throws DBException;

    //////////////////////////////////////////////////
    // Actions

    protected abstract void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options) throws DBException;

    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {

    }

    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull NestedObjectCommand<OBJECT_TYPE, PropertyHandler> command,
        @NotNull Map<String, Object> options
    ) throws DBException {

    }

    protected void addObjectRenameActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectRenameCommand command,
        @NotNull Map<String, Object> options
    ) {
        // Base SQL syntax do not support object properties change
        throw new IllegalStateException("Object rename is not supported in " + getClass().getSimpleName()); //$NON-NLS-1$
    }

    protected void addObjectReorderActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectReorderCommand command,
        @NotNull Map<String, Object> options
    ) {
        if (command.getObject().isPersisted()) {
            // Not supported by implementation
            throw new IllegalStateException("Object reorder is not supported in " + getClass().getSimpleName()); //$NON-NLS-1$
        }
    }

    protected abstract void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException;

    //////////////////////////////////////////////////
    // Name generator

    protected void setNewObjectName(DBRProgressMonitor monitor, CONTAINER_TYPE container, OBJECT_TYPE table) {
        if (table instanceof DBPNamedObject2) {
            try {
                ((DBPNamedObject2)table).setName(getNewChildName(monitor, container));
            } catch (DBException e) {
                log.error("Error settings object name", e);
            }
        }
    }

    protected String getNewChildName(DBRProgressMonitor monitor, CONTAINER_TYPE container) throws DBException {
        return getNewChildName(monitor, container, getBaseObjectName());
    }

    protected String getBaseObjectName() {
        return "NewObject";
    }

    protected String getNewChildName(DBRProgressMonitor monitor, CONTAINER_TYPE container, String baseName) {
        try {
            for (int i = 0; i < MAX_NAME_GEN_ATTEMPTS; i++) {
                String tableName = DBObjectNameCaseTransformer.transformName(container.getDataSource(), i == 0 ? baseName : (baseName + "_" + i));
                DBSObject child = container instanceof DBSObjectContainer ? ((DBSObjectContainer)container).getChild(monitor, tableName) : null;
                if (child == null) {
                    return tableName;
                }
            }
            log.error("Error generating child object name: max attempts reached");
            return baseName;
        } catch (DBException e) {
            log.error("Error generating child object name", e);
            return DBObjectNameCaseTransformer.transformName(container.getDataSource(), baseName);
        }
    }

    //////////////////////////////////////////////////
    // Properties

    protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, CONTAINER_TYPE owner, DBECommandAbstract<OBJECT_TYPE> command, Map<String, Object> options) {
        return null;
    }

    protected void validateObjectProperty(OBJECT_TYPE object, DBPPropertyDescriptor property, Object value) throws DBException {

    }

    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options) throws DBException {

    }

    protected void processObjectRename(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options, String newName) throws DBException {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, options, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }

    protected void processObjectReorder(DBECommandContext commandContext, OBJECT_TYPE object, List<? extends DBPOrderedObject> siblings, int newPosition) throws DBException {
        ObjectReorderCommand command = new ObjectReorderCommand(object, siblings, ModelMessages.model_jdbc_reorder_object, newPosition);
        commandContext.addCommand(command, new ReorderObjectReflector(), true);
    }

    protected OBJECT_TYPE configureObject(
        DBRProgressMonitor monitor,
        DBECommandContext commandContext,
        Object parent,
        OBJECT_TYPE object,
        Map<String, Object> options
    ) {
        DBEObjectConfigurator<OBJECT_TYPE> configurator = GeneralUtils.adapt(object, DBEObjectConfigurator.class);
        if (configurator != null) {
            return configurator.configureObject(monitor, commandContext, parent, object, options);
        }
        return object;
    }

    protected class PropertyHandler
            extends ProxyPropertyDescriptor
            implements DBEPropertyHandler<OBJECT_TYPE>, DBEPropertyReflector<OBJECT_TYPE>, DBEPropertyValidator<OBJECT_TYPE> {
        private PropertyHandler(DBPPropertyDescriptor property) {
            super(property);
        }

        @Override
        public DBECommandComposite<OBJECT_TYPE, ? extends DBEPropertyHandler<OBJECT_TYPE>> createCompositeCommand(OBJECT_TYPE object) {
            return new ObjectChangeCommand(object);
        }

        @Override
        public void reflectValueChange(OBJECT_TYPE object, Object oldValue, Object newValue) {
        }

        @Override
        public String toString() {
            return original.getDisplayName();
        }

        @Override
        public int hashCode() {
            return original.getId().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null &&
                    obj.getClass() == PropertyHandler.class &&
                    //editor == ((PropertyHandler)obj).editor &&
                    getId().equals(((PropertyHandler) obj).getId());
        }

        @Override
        public void validate(OBJECT_TYPE object, Object value) throws DBException {
            validateObjectProperty(object, original, value);
        }

    }

    //////////////////////////////////////////////////
    // Command objects

    protected static abstract class NestedObjectCommand<OBJECT_TYPE extends DBSObject, HANDLER_TYPE extends DBEPropertyHandler<OBJECT_TYPE>> extends DBECommandComposite<OBJECT_TYPE, HANDLER_TYPE> {

        protected NestedObjectCommand(OBJECT_TYPE object, String title) {
            super(object, title);
        }

        public abstract String getNestedDeclaration(DBRProgressMonitor monitor, DBSObject owner, Map<String, Object> options);

    }

    protected static class EmptyCommand extends DBECommandAbstract<DBPObject> {
        public EmptyCommand(DBPObject object) {
            super(object, "Empty"); //$NON-NLS-1$
        }
    }

    protected class ObjectChangeCommand extends NestedObjectCommand<OBJECT_TYPE, PropertyHandler> {
        public ObjectChangeCommand(OBJECT_TYPE object) {
            super(object, "JDBC Composite"); //$NON-NLS-1$
        }

        @NotNull
        @Override
        public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull Map<String, Object> options) throws DBException {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectModifyActions(monitor, executionContext, actions, this, options);
            addObjectExtraActions(monitor, executionContext, actions, this, options);
            return actions.toArray(new DBEPersistAction[0]);
        }

        @Override
        public void validateCommand(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
            validateObjectProperties(monitor, this, options);
        }

        @Override
        public String getNestedDeclaration(DBRProgressMonitor monitor, DBSObject owner, Map<String, Object> options) {
            // It is a trick
            // This method may be invoked from another Editor with different OBJECT_TYPE and CONTAINER_TYPE
            // TODO: May be we should make ObjectChangeCommand static
            final StringBuilder decl = SQLObjectEditor.this.getNestedDeclaration(monitor, (CONTAINER_TYPE) owner, this, options);
            return CommonUtils.isEmpty(decl) ? null : decl.toString();
        }

        @Override
        public String toString() {
            return "CMD:UpdateObject:" + getObject();
        }
    }

    public class ObjectCreateCommand extends NestedObjectCommand<OBJECT_TYPE, PropertyHandler> implements DBECommandWithOptions {

        private Map<String, Object> options;

        protected ObjectCreateCommand(OBJECT_TYPE object, String title, Map<String, Object> options) {
            super(object, title);
            this.options = options;
        }

        @Override
        public Map<String, Object> getOptions() {
            return options;
        }

        @Override
        public void validateCommand(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
            OBJECT_TYPE newObject = getObject();
            if (!newObject.isPersisted()) {
                String objectName = newObject.getName();
                if (CommonUtils.isEmpty(objectName)) {
                    throw new DBException("Empty " + DBUtils.getObjectTypeName(newObject).toLowerCase() + " name");
                }
                DBSObjectCache<? extends DBSObject, OBJECT_TYPE> objectsCache = getObjectsCache(newObject);
                if (objectsCache != null) {
                    List<OBJECT_TYPE> cachedObjects;
                    if (objectsCache instanceof DBSCompositeCache) {
                        cachedObjects = ((DBSCompositeCache) objectsCache).getCachedObjects(newObject.getParentObject());
                    } else {
                        cachedObjects = objectsCache.getCachedObjects();
                    }
                    OBJECT_TYPE cachedObject = DBUtils.findObject(cachedObjects, objectName);
                    if (cachedObject != null && cachedObject != newObject && cachedObject.isPersisted()) {
                        throw new DBException(DBUtils.getObjectTypeName(cachedObject) + " '" + objectName + "' already exists");
                    }
                }
            }
        }

        @NotNull
        @Override
        public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull Map<String, Object> options) throws DBException {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectCreateActions(monitor, executionContext, actions, this, options);
            addObjectExtraActions(monitor, executionContext, actions, this, options);
            return actions.toArray(new DBEPersistAction[0]);
        }

        @Override
        public void updateModel() {
            super.updateModel();
            OBJECT_TYPE object = getObject();
            if (!object.isPersisted()) {
                if (object instanceof DBPSaveableObject) {
                    ((DBPSaveableObject) object).setPersisted(true);
                }
                DBUtils.fireObjectUpdate(object);
            }
        }

        @Override
        public String getNestedDeclaration(DBRProgressMonitor monitor, DBSObject owner, Map<String, Object> options) {
            // It is a trick
            // This method may be invoked from another Editor with different OBJECT_TYPE and CONTAINER_TYPE
            // TODO: May be we should make ObjectChangeCommand static
            final StringBuilder decl = SQLObjectEditor.this.getNestedDeclaration(monitor, (CONTAINER_TYPE) owner, this, options);
            return CommonUtils.isEmpty(decl) ? null : decl.toString();
        }

        @Override
        public String toString() {
            return "CMD:CreateObject:" + getObject();
        }
    }

    protected class ObjectDeleteCommand extends DBECommandDeleteObject<OBJECT_TYPE> {
        public ObjectDeleteCommand(OBJECT_TYPE table, String title) {
            super(table, title);
        }

        @NotNull
        @Override
        public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull Map<String, Object> options) throws DBException {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectDeleteActions(monitor, executionContext, actions, this, options);
            return actions.toArray(new DBEPersistAction[0]);
        }

        @Override
        public void updateModel() {
            OBJECT_TYPE object = getObject();
            DBSObjectCache<? extends DBSObject, OBJECT_TYPE> cache = getObjectsCache(object);
            if (cache != null) {
                cache.removeObject(object, false);
            }
        }

        @Override
        public String toString() {
            return "CMD:DeleteObject:" + getObject();
        }
    }

    protected class ObjectRenameCommand extends DBECommandAbstract<OBJECT_TYPE> implements DBECommandRename {
        private Map<String, Object> options;
        private String oldName;
        private String newName;

        public ObjectRenameCommand(OBJECT_TYPE object, String title, Map<String, Object> options, String newName) {
            super(object, title);
            this.options = options;
            this.oldName = object.getName();
            this.newName = newName;
        }

        public Map<String, Object> getOptions() {
            return options;
        }

        public String getOldName() {
            return oldName;
        }

        public String getNewName() {
            return newName;
        }

        @NotNull
        @Override
        public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull Map<String, Object> options) {
            if (CommonUtils.equalObjects(oldName, newName)) {
                return new DBEPersistAction[0];
            }
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectRenameActions(monitor, executionContext, actions, this, options);
            return actions.toArray(new DBEPersistAction[0]);
        }

        @NotNull
        @Override
        public DBECommand<?> merge(@NotNull DBECommand<?> prevCommand, @NotNull Map<Object, Object> userParams) {
            // We need to dismiss all rename commands if there is a create command in the command queue.
            // Otherwise we issue redundant rename commands
            // See https://github.com/dbeaver/dbeaver/issues/11917
            int hashCode = getObject().hashCode();
            String createId = "create#" + hashCode;
            Object createCmd = userParams.get(createId);
            if (createCmd != null) {
                return (DBECommand<?>) createCmd;
            }
            if (prevCommand instanceof SQLObjectEditor.ObjectCreateCommand) {
                userParams.put(createId, prevCommand);
                return prevCommand;
            }

            // We need very first and very last rename commands. They produce final rename
            String mergeId = "rename#" + hashCode;
            ObjectRenameCommand renameCmd = (ObjectRenameCommand) userParams.get(mergeId);
            if (renameCmd == null) {
                renameCmd = new ObjectRenameCommand(getObject(), getTitle(), options, newName);
                userParams.put(mergeId, renameCmd);
            } else {
                renameCmd.newName = newName;
                return renameCmd;
            }
            return super.merge(prevCommand, userParams);
        }

        @Override
        public String toString() {
            return "CMD:RenameObject:" + getObject();
        }
    }

    public class RenameObjectReflector implements DBECommandReflector<OBJECT_TYPE, ObjectRenameCommand> {

        @Override
        public void redoCommand(ObjectRenameCommand command) {
            if (command.getObject() instanceof DBPNamedObject2) {
                ((DBPNamedObject2) command.getObject()).setName(command.newName);

                // Update cache
                DBSObjectCache<? extends DBSObject, OBJECT_TYPE> cache = getObjectsCache(command.getObject());
                if (cache != null) {
                    cache.renameObject(command.getObject(), command.getOldName(), command.getNewName());
                }

                Map<String, Object> options = new LinkedHashMap<>(command.getOptions());
                options.put(DBEObjectRenamer.PROP_OLD_NAME, command.getOldName());
                options.put(DBEObjectRenamer.PROP_NEW_NAME, command.getNewName());

                DBUtils.fireObjectUpdate(command.getObject(), options, DBPEvent.RENAME);
            }
        }

        @Override
        public void undoCommand(ObjectRenameCommand command) {
            if (command.getObject() instanceof DBPNamedObject2) {
                ((DBPNamedObject2) command.getObject()).setName(command.oldName);

                // Update cache
                DBSObjectCache<? extends DBSObject, OBJECT_TYPE> cache = getObjectsCache(command.getObject());
                if (cache != null) {
                    cache.renameObject(command.getObject(), command.getNewName(), command.getOldName());
                }

                Map<String, Object> options = new LinkedHashMap<>(command.getOptions());
                DBUtils.fireObjectUpdate(command.getObject(), options, DBPEvent.RENAME);
            }
        }

    }

    protected class ObjectReorderCommand extends DBECommandAbstract<OBJECT_TYPE> {
        private List<? extends DBPOrderedObject> siblings;
        private int oldPosition;
        private int newPosition;

        ObjectReorderCommand(OBJECT_TYPE object, List<? extends DBPOrderedObject> siblings, String title, int newPosition) {
            super(object, title);
            this.siblings = siblings;
            this.oldPosition = ((DBPOrderedObject) object).getOrdinalPosition();
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

        @NotNull
        @Override
        public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull Map<String, Object> options) {
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectReorderActions(monitor, executionContext, actions, this, options);
            return actions.toArray(new DBEPersistAction[actions.size()]);
        }

        @NotNull
        @Override
        public DBECommand<?> merge(@NotNull DBECommand<?> prevCommand, @NotNull Map<Object, Object> userParams) {
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

        @Override
        public String toString() {
            return "CMD:ReorderPosition:" + getObject() + ":" + getOldPosition() + ":" + getNewPosition();
        }
    }

    public class ReorderObjectReflector implements DBECommandReflector<OBJECT_TYPE, ObjectReorderCommand> {

        @Override
        public void redoCommand(ObjectReorderCommand command) {
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
        public void undoCommand(ObjectReorderCommand command) {
            ((DBPOrderedObject) command.getObject()).setOrdinalPosition(command.oldPosition);
            final DBSObject parentObject = command.getObject().getParentObject();
            if (parentObject != null) {
                // We need to update order in navigator model
                DBUtils.fireObjectUpdate(parentObject, DBPEvent.REORDER);
            }
        }

    }

    public static class RefreshObjectReflector<OBJECT_TYPE extends DBSObject> implements DBECommandReflector<OBJECT_TYPE, DBECommandAbstract<OBJECT_TYPE>> {

        @Override
        public void redoCommand(DBECommandAbstract<OBJECT_TYPE> command) {
            DBUtils.fireObjectUpdate(command.getObject(), true);
        }

        @Override
        public void undoCommand(DBECommandAbstract<OBJECT_TYPE> command) {
            DBUtils.fireObjectUpdate(command.getObject(), true);
        }

    }

}

