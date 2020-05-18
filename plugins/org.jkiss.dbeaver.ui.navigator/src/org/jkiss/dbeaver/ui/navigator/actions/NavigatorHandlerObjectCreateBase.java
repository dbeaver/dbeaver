/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSInstanceLazy;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.ObjectPropertyTester;
import org.jkiss.dbeaver.ui.editors.DatabaseNodeEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public abstract class NavigatorHandlerObjectCreateBase extends NavigatorHandlerObjectBase {

    protected boolean createNewObject(final IWorkbenchWindow workbenchWindow, DBNNode element, DBNDatabaseNode copyFrom) {
        return createNewObject(workbenchWindow, element, null, copyFrom, false);
    }

    protected boolean createNewObject(final IWorkbenchWindow workbenchWindow, DBNNode element, @Nullable Class<?> newObjectType, DBNDatabaseNode copyFrom, boolean isFolder)
    {
        try {
            DBNNode container = null;
            if (isFolder || (element instanceof DBNContainer && !(element instanceof DBNDataSource))) {
                container = element;
            } else {
                DBNNode parentNode = element.getParentNode();
                if (parentNode instanceof DBNContainer) {
                    container = parentNode;
                }
            }
            if (container == null) {
                throw new DBException("Can't detect container for '" + element.getNodeName() + "'");
            }
            if (container instanceof DBNDatabaseNode && ObjectPropertyTester.isMetadataChangeDisabled((DBNDatabaseNode) container)) {
                throw new DBException("Object create not available in simple view mode");
            }
            if (newObjectType == null) {
                Class<?> childType = container instanceof DBNContainer ? ((DBNContainer) container).getChildrenClass() : null;
                if (childType == null) {
                    throw new DBException("Can't determine child element type for container '" + container + "'");
                }
                newObjectType = childType;
            }
            if (newObjectType == IProject.class) {
                return NavigatorHandlerProjectCreate.createNewProject(workbenchWindow);
            }

            DBSObject sourceObject = copyFrom == null ? null : copyFrom.getObject();
            final Object parentObject;
            if (container instanceof DBNDatabaseNode) {
                parentObject = ((DBNDatabaseNode) container).getValueObject();
            } else if (container instanceof DBNProject) {
                parentObject = ((DBNProject) container).getProject();
            } else if (container instanceof DBNProjectDatabases) {
                parentObject = container.getOwnerProject();
            } else if (container instanceof DBNLocalFolder) {
                parentObject = ((DBNLocalFolder) container).getFolder();
            } else {
                parentObject = null;
            }

            // Do not check for type - manager must do it. Potentially we can copy anything into anything.
//            if (sourceObject != null && !childType.isAssignableFrom(sourceObject.getClass())) {
//                throw new DBException("Can't create '" + childType.getName() + "' from '" + sourceObject.getClass().getName() + "'");
//            }

            // Check that child nodes are read an cached
            if (container.hasChildren(false) || parentObject instanceof DBSInstanceLazy) {
                try {
                    DBNNode finalContainer = container;
                    UIUtils.runInProgressService(monitor -> {
                        try {
                            if (finalContainer.hasChildren(false)) {
                                finalContainer.getChildren(monitor);
                            }
                            if (parentObject instanceof DBSInstanceLazy) {
                                ((DBSInstanceLazy) parentObject).checkInstanceConnection(monitor);
                            }
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    });
                } catch (InvocationTargetException e) {
                    DBWorkbench.getPlatformUI().showError("New object", "Error creating new object", e);
                } catch (InterruptedException e) {
                    // ignore
                }
            }


            DBEObjectManager<?> objectManager = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(newObjectType);
            if (objectManager == null) {
                throw new DBException("Object manager not found for type '" + newObjectType.getName() + "'");
            }
            DBEObjectMaker objectMaker = (DBEObjectMaker) objectManager;

            DBPDataSource dataSource = container instanceof DBNDatabaseNode ? ((DBNDatabaseNode) container).getDataSource() : null;

            final boolean openEditor = dataSource != null &&
                (objectMaker.getMakerOptions(dataSource) & DBEObjectMaker.FEATURE_EDITOR_ON_CREATE) != 0;
            CommandTarget commandTarget = getCommandTarget(
                workbenchWindow,
                container,
                newObjectType,
                openEditor);

            // Parent is model object - not node
            Map<String, Object> options = new HashMap<>();
            options.put(DBEObjectMaker.OPTION_CONTAINER, container);
            options.put(DBEObjectMaker.OPTION_OBJECT_TYPE, newObjectType);
            createDatabaseObject(commandTarget, objectMaker, parentObject instanceof DBPObject ? (DBPObject) parentObject : null, sourceObject, options);
        }
        catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError("Create object", null, e);
            return false;
        }

        return true;
    }

    private <OBJECT_TYPE extends DBSObject, CONTAINER_TYPE extends DBPObject> void createDatabaseObject(
        CommandTarget commandTarget,
        DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE> objectMaker,
        CONTAINER_TYPE parentObject,
        DBSObject sourceObject,
        Map<String, Object> options) throws DBException
    {
        ObjectCreator<OBJECT_TYPE, CONTAINER_TYPE> objectCreator = new ObjectCreator<>(objectMaker, commandTarget, parentObject, sourceObject, options);
        try {
            UIUtils.runInProgressService(objectCreator);
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("New object", "Error creating new object", e);
            return;
        } catch (InterruptedException e) {
            return;
        }
        if (objectCreator.newObject == null) {
            //DBWorkbench.getPlatformUI().showError("Null new object", "Internal error during object creation: NULL object returned (see logs).");
            return;
        }
        final CreateJob<OBJECT_TYPE, CONTAINER_TYPE> job = new CreateJob<>(commandTarget, objectMaker, parentObject, sourceObject, objectCreator.newObject);
        job.schedule();
    }

    static class CreateJob<OBJECT_TYPE extends DBSObject, CONTAINER_TYPE extends DBPObject> extends AbstractJob {
        private final CommandTarget commandTarget;
        private final DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE> objectMaker;
        private final CONTAINER_TYPE parentObject;
        private final DBSObject sourceObject;
        private OBJECT_TYPE newObject;

        public CreateJob(CommandTarget commandTarget, DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE> objectMaker, CONTAINER_TYPE parentObject, DBSObject sourceObject, OBJECT_TYPE newObject) {
            super("Create new database object with " + objectMaker.getClass().getSimpleName());
            setUser(true);
            setSystem(false);
            this.commandTarget = commandTarget;
            this.objectMaker = objectMaker;
            this.parentObject = parentObject;
            this.sourceObject = sourceObject;
            this.newObject = newObject;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            if (newObject == null) {
                return Status.CANCEL_STATUS;//GeneralUtils.makeErrorStatus("Null object returned");
            }
            monitor.beginTask("Save " + newObject.getClass().getSimpleName(), 3);
            try {
                if (parentObject instanceof DBSObject) {
                    if ((objectMaker.getMakerOptions(((DBSObject) parentObject).getDataSource()) & DBEObjectMaker.FEATURE_SAVE_IMMEDIATELY) != 0) {
                        // Save object manager's content
                        monitor.subTask("Save object");
                        commandTarget.getContext().saveChanges(monitor, DBPScriptObject.EMPTY_OPTIONS);
                        monitor.worked(2);
                        // Refresh new object (so it can load some props from database)
                        if (newObject instanceof DBPRefreshableObject) {
                            monitor.subTask("Load object from server");
                            final DBNDatabaseNode newChild = DBWorkbench.getPlatform().getNavigatorModel().findNode(newObject);
                            if (newChild != null) {
                                newChild.refreshNode(monitor, this);
                                newObject = (OBJECT_TYPE) newChild.getObject();
                            }
                            monitor.worked(1);
                        }
                    }
                }

                {
                    monitor.subTask("Obtain new object node");
                    // Wait for a few seconds to let listeners to add new object's node in navigator node
                    for (int i = 0; i < 50; i++) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        if (DBWorkbench.getPlatform().getNavigatorModel().findNode(newObject) != null) {
                            break;
                        }
                        RuntimeUtils.pause(100);
                    }
                    monitor.worked(1);
                }

                // Open object in UI thread
                addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(IJobChangeEvent event) {
                        UIUtils.syncExec(() -> {
                            openNewObject();
                        });
                    }
                });

                return Status.OK_STATUS;
            } catch (Exception e) {
                return GeneralUtils.makeExceptionStatus(e);
            } finally {
                monitor.done();
            }
        }

        private void openNewObject() {
            IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
            try {
                final DBNDatabaseNode newChild = DBWorkbench.getPlatform().getNavigatorModel().findNode(newObject);
                if (newChild != null) {
                    DatabaseNavigatorView view = UIUtils.findView(workbenchWindow, DatabaseNavigatorView.class);
                    if (view != null) {
                        view.showNode(newChild);
                    }
                    final boolean openEditor =
                        parentObject instanceof DBSObject &&
                        (objectMaker.getMakerOptions(((DBSObject) parentObject).getDataSource()) & DBEObjectMaker.FEATURE_EDITOR_ON_CREATE) != 0;
                    IDatabaseEditor editor = commandTarget.getEditor();
                    if (editor != null) {
                        // Just activate existing editor
                        workbenchWindow.getActivePage().activate(editor);
                    } else if (openEditor) {
                        // Open new one with existing context
                        DatabaseNodeEditorInput editorInput = new DatabaseNodeEditorInput(
                            newChild,
                            commandTarget.getContext());
                        workbenchWindow.getActivePage().openEditor(
                            editorInput,
                            EntityEditor.class.getName());
                    }
                } else {
                    throw new DBException("Can't find node corresponding to new object");
                }
            } catch (Throwable e) {
                DBWorkbench.getPlatformUI().showError("Create object", null, e);
            }
        }

    }

    private static class ObjectCreator<OBJECT_TYPE extends DBSObject, CONTAINER_TYPE extends DBPObject> implements DBRRunnableWithProgress {
        private final DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE> objectMaker;
        private final CommandTarget commandTarget;
        private final CONTAINER_TYPE parentObject;
        private final DBSObject sourceObject;
        private OBJECT_TYPE newObject;
        private Map<String, Object> options;

        public ObjectCreator(DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE> objectMaker, CommandTarget commandTarget, CONTAINER_TYPE parentObject, DBSObject sourceObject, Map<String, Object> options) {
            this.objectMaker = objectMaker;
            this.commandTarget = commandTarget;
            this.parentObject = parentObject;
            this.sourceObject = sourceObject;
            this.options = options;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
                newObject = objectMaker.createNewObject(monitor, commandTarget.getContext(), parentObject, sourceObject, options);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}