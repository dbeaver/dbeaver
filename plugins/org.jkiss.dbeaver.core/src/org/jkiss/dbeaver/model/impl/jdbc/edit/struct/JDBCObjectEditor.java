/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.*;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.properties.ProxyPropertyDescriptor;

import java.util.Map;

/**
 * JDBC object editor
 */
public abstract class JDBCObjectEditor<OBJECT_TYPE extends DBSObject & DBPSaveableObject, CONTAINER_TYPE extends DBSObject>
    extends JDBCObjectManager<OBJECT_TYPE>
    implements
        DBEObjectEditor<OBJECT_TYPE>,
        DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE>
{
    public static final String PATTERN_ITEM_INDEX = "%INDEX%";
    public static final String PATTERN_ITEM_TABLE = "%TABLE%";
    public static final String PATTERN_ITEM_INDEX_SHORT = "%INDEX_SHORT%";
    public static final String PATTERN_ITEM_CONSTRAINT = "%CONSTRAINT%";

    public final DBEPropertyHandler<OBJECT_TYPE> makePropertyHandler(OBJECT_TYPE object, IPropertyDescriptor property)
    {
        return new PropertyHandler(property);
    }

    public final OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext commandContext, CONTAINER_TYPE parent, Object copyFrom)
    {
        OBJECT_TYPE newObject = createDatabaseObject(workbenchWindow, activeEditor, commandContext, parent, copyFrom);
        if (newObject == null) {
            return null;
        }

        //context.addCommandBatch(commands, );
        final ObjectCreateCommand createCommand = makeCreateCommand(newObject);
        commandContext.getUserParams().put(newObject, createCommand);
        commandContext.addCommand(createCommand, new CreateObjectReflector(), true);

        return newObject;
    }

    public final void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options)
    {
        commandContext.addCommand(
            new ObjectDeleteCommand(object, "Delete object"),
            new DeleteObjectReflector<OBJECT_TYPE>(), true);
    }

    ObjectCreateCommand makeCreateCommand(OBJECT_TYPE object)
    {
        return new ObjectCreateCommand(object, "Create new object");
    }

/*
    protected void makeInitialCommands(
        OBJECT_TYPE object,
        DBECommandContext context,
        DBECommand<OBJECT_TYPE> createCommand)
    {
        List<DBECommand> commands = new ArrayList<DBECommand>();
        commands.add(createCommand);

        PropertyCollector propertyCollector = new PropertyCollector(object, false);
        propertyCollector.collectProperties();
        for (IPropertyDescriptor prop : propertyCollector.getPropertyDescriptors()) {
            if (prop instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) prop).isEditPossible()) {
                final Object propertyValue = propertyCollector.getPropertyValue(prop.getId());
                if (propertyValue != null) {
                    commands.add(new DBECommandProperty<OBJECT_TYPE>(object, new PropertyHandler(prop), propertyValue, propertyValue));
                }
            }
        }

        context.addCommandBatch(commands, new CreateObjectReflector(), true);
    }
*/

    protected abstract OBJECT_TYPE createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor,
        DBECommandContext context,
        CONTAINER_TYPE parent,
        Object copyFrom);

    protected abstract IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command);

    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        // Base SQL syntax do not support object properties change
        throw new IllegalStateException("Object modification is not supported in " + getClass().getSimpleName());
    }

    protected IDatabasePersistAction[] makeObjectRenameActions(ObjectRenameCommand command)
    {
        // Base SQL syntax do not support object properties change
        throw new IllegalStateException("Object rename is not supported in " + getClass().getSimpleName());
    }

    protected abstract IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command);

    protected StringBuilder getNestedDeclaration(CONTAINER_TYPE owner, DBECommandComposite<OBJECT_TYPE, PropertyHandler> command)
    {
        return null;
    }

    protected void validateObjectProperty(OBJECT_TYPE object, IPropertyDescriptor property, Object value) throws DBException
    {

    }

    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {

    }

    protected void processObjectRename(DBECommandContext commandContext, OBJECT_TYPE object, String newName) throws DBException
    {
        ObjectRenameCommand command = new ObjectRenameCommand(object, "Rename object", newName);
        commandContext.addCommand(command, new RenameObjectReflector());
    }

    protected class PropertyHandler
        extends ProxyPropertyDescriptor
        implements DBEPropertyHandler<OBJECT_TYPE>, DBEPropertyReflector<OBJECT_TYPE>, DBEPropertyValidator<OBJECT_TYPE>
    {
        private PropertyHandler(IPropertyDescriptor property)
        {
            super(property);
        }

        public DBECommandComposite<OBJECT_TYPE, ? extends DBEPropertyHandler<OBJECT_TYPE>> createCompositeCommand(OBJECT_TYPE object)
        {
            return new ObjectChangeCommand(object);
        }

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
            super(object, "JDBC Composite");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return makeObjectModifyActions(this);
        }

        @Override
        public void validateCommand() throws DBException
        {
            validateObjectProperties(this);
        }

        public String getNestedDeclaration(DBSObject owner)
        {
            // It is a trick
            // This method may be invoked from another Editor with different OBJECT_TYPE and CONTAINER_TYPE
            // TODO: May be we should make ObjectChangeCommand static
            final StringBuilder decl = JDBCObjectEditor.this.getNestedDeclaration((CONTAINER_TYPE) owner, this);
            return CommonUtils.isEmpty(decl) ? null : decl.toString();
        }
    }

    protected class ObjectCreateCommand extends NestedObjectCommand<OBJECT_TYPE, PropertyHandler> {

        protected ObjectCreateCommand(OBJECT_TYPE object, String title)
        {
            super(object, title);
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return makeObjectCreateActions(this);
        }

        @Override
        public void updateModel()
        {
            super.updateModel();
            if (!getObject().isPersisted()) {
                getObject().setPersisted(true);
                DBUtils.fireObjectUpdate(getObject());
            }
        }

        public String getNestedDeclaration(DBSObject owner)
        {
            // It is a trick
            // This method may be invoked from another Editor with different OBJECT_TYPE and CONTAINER_TYPE
            // TODO: May be we should make ObjectChangeCommand static
            final StringBuilder decl = JDBCObjectEditor.this.getNestedDeclaration((CONTAINER_TYPE) owner, this);
            return CommonUtils.isEmpty(decl) ? null : decl.toString();
        }
    }

    protected class ObjectDeleteCommand extends DBECommandDeleteObject<OBJECT_TYPE> {
        protected ObjectDeleteCommand(OBJECT_TYPE table, String title)
        {
            super(table, title);
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return makeObjectDeleteActions(this);
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

        public IDatabasePersistAction[] getPersistActions()
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

        public void redoCommand(ObjectRenameCommand command)
        {
            if (command.getObject() instanceof DBPNamedObject2) {
                ((DBPNamedObject2)command.getObject()).setName(command.newName);
                DBUtils.fireObjectUpdate(command.getObject());
            }
        }

        public void undoCommand(ObjectRenameCommand command)
        {
            if (command.getObject() instanceof DBPNamedObject2) {
                ((DBPNamedObject2)command.getObject()).setName(command.oldName);
                DBUtils.fireObjectUpdate(command.getObject());
            }
        }

    }

}

