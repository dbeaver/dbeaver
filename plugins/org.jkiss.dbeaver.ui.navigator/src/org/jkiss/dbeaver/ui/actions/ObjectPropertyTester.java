/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPOrderedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.registry.ObjectManagerRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.actions.exec.SQLNativeExecutorDescriptor;
import org.jkiss.dbeaver.ui.actions.exec.SQLNativeExecutorRegistry;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectCreateNew;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * ObjectPropertyTester
 */
public class ObjectPropertyTester extends PropertyTester {
    //static final Log log = Log.getLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.object";
    public static final String PROP_CAN_OPEN = "canOpen";
    public static final String PROP_CAN_CREATE_SINGLE = "canCreateSingle";
    public static final String PROP_CAN_CREATE_MULTI = "canCreateMulti";
    public static final String PROP_CAN_PASTE = "canPaste";
    public static final String PROP_CAN_DELETE = "canDelete";
    public static final String PROP_CAN_RENAME = "canRename";
    public static final String PROP_CAN_MOVE_UP = "canMoveUp";
    public static final String PROP_CAN_MOVE_DOWN = "canMoveDown";
    public static final String PROP_CAN_FILTER = "canFilter";
    public static final String PROP_CAN_FILTER_OBJECT = "canFilterObject";
    public static final String PROP_HAS_FILTER = "hasFilter";
    public static final String PROP_HAS_TOOLS = "hasTools";
    public static final String PROP_SUPPORTS_CREATING_INDEX = "supportsIndexCreate";
    public static final String PROP_SUPPORTS_CREATING_CONSTRAINT = "supportsConstraintCreate";
    public static final String PROP_PROJECT_RESOURCE_EDITABLE = "projectResourceEditable";
    public static final String PROP_PROJECT_RESOURCE_VIEWABLE = "projectResourceViewable";
    public static final String PROP_SUPPORTS_NATIVE_EXECUTION = "supportsNativeExecution";

    public ObjectPropertyTester() {
        super();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        DBNNode node = RuntimeUtils.getObjectAdapter(receiver, DBNNode.class);
        if (node == null) {
            return false;
        }
        Display display = Display.getCurrent();
        if (display == null) {
            return false;
        }
//System.out.println("TEST " + property + " ON " + node.getName());

        switch (property) {
            case PROP_CAN_OPEN:
                return node.isPersisted();
            case PROP_CAN_CREATE_SINGLE: {
                return canCreateObject(node, true);
            }
            case PROP_CAN_CREATE_MULTI: {
                return canCreateObject(node, false);
            }
            case PROP_CAN_PASTE: {
                // We cannot interact with clipboard in property testers (#6489).
                // It breaks context menu (and maybe something else) omn some OSes.
/*
                Clipboard clipboard = new Clipboard(display);
                try {
                    if (clipboard.getContents(TreeNodeTransfer.getInstance()) == null) {
                        return false;
                    }
                } finally {
                    clipboard.dispose();
                }
*/
                if (node instanceof DBNResource) {
                    return property.equals(PROP_CAN_PASTE);
                }
                return canCreateObject(node, null);
                // Do not check PASTE command state. It requires clipboard contents check
                // which means UI interaction which can break menu popup [RedHat]
                // and also is a slow operation. So let paste be always enabled.
/*
            // Check objects in clipboard
            Collection<DBNNode> cbNodes = TreeNodeTransfer.getFromClipboard();
            if (cbNodes == null) {
                return false;
            }
            for (DBNNode nodeObject : cbNodes) {
                if (nodeObject.isManagable() && nodeObject instanceof DBSWrapper) {
                    DBSObject pasteObject = ((DBSWrapper)nodeObject).getObject();
                    if (pasteObject == null || objectType != pasteObject.getClass()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
*/
            }
            case PROP_SUPPORTS_NATIVE_EXECUTION:
                if (receiver instanceof DBNResource dbnResource) {
                    List<DBPDataSourceContainer> associatedDataSources
                        = (List<DBPDataSourceContainer>) dbnResource.getAssociatedDataSources();
                    if (CommonUtils.isEmpty(associatedDataSources)) {
                        return false;
                    }
                    DBPDataSourceContainer dbpDataSourceContainer = associatedDataSources.get(0);
                    SQLNativeExecutorDescriptor executorDescriptor = SQLNativeExecutorRegistry.getInstance()
                        .getExecutorDescriptor(dbpDataSourceContainer);
                    try {
                        return executorDescriptor != null && executorDescriptor.getNativeExecutor() != null;
                    } catch (DBException e) {
                        return false;
                    }
                }
                return false;
            case PROP_CAN_DELETE: {
                if (node instanceof DBNDataSource || node instanceof DBNLocalFolder) {
                    return nodeProjectHasPermission(node, RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);
                }

                if (DBNUtils.isReadOnly(node)) {
                    return false;
                }
                if (isResourceNode(node) && !nodeProjectHasPermission(node, RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT)) {
                    return false;
                }

                if (node instanceof DBSWrapper wrapper) {
                    DBSObject object = wrapper.getObject();
                    if (object == null || DBUtils.isReadOnly(object) || !(node.getParentNode() instanceof DBNContainer) ||
                        !DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR)
                    ) {
                        return false;
                    }
                    DBEObjectMaker objectMaker = getObjectManager(object.getClass(), DBEObjectMaker.class);
                    return objectMaker != null && objectMaker.canDeleteObject(object);
                } else if (node instanceof DBNResource dbnResource) {
                    if ((dbnResource.getFeatures() & DBPResourceHandler.FEATURE_DELETE) != 0) {
                        return true;
                    }
                } else if (node instanceof DBNProject projectNode) {
                    DBPProject project = projectNode.getProject();
                    return project != project.getWorkspace().getActiveProject();
                } else if (isResourceNode(node)) {
                    return true;
                }
                break;
            }
            case PROP_CAN_RENAME: {
                if (node instanceof DBNDataSource || node instanceof DBNLocalFolder) {
                    return nodeProjectHasPermission(node, RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);
                }
                if (isResourceNode(node) && !nodeProjectHasPermission(node, RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT)) {
                    return false;
                }
                if (node.supportsRename()) {
                    return true;
                }
                if (node instanceof DBNDatabaseNode) {
                    if (DBNUtils.isReadOnly(node) || !DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR)) {
                        return false;
                    }
                    DBSObject object = ((DBNDatabaseNode) node).getObject();
                    if (object != null) {
                        DBEObjectRenamer objectRenamer = getObjectManager(object.getClass(), DBEObjectRenamer.class);
                        return !DBUtils.isReadOnly(object) &&
                                object.isPersisted() &&
                                node.getParentNode() instanceof DBNContainer &&
                                objectRenamer != null && objectRenamer.canRenameObject(object);
                    }
                }
                break;
            }
            case PROP_CAN_MOVE_UP:
            case PROP_CAN_MOVE_DOWN: {
                if (node instanceof DBNDatabaseNode dbNode) {
                    if (DBNUtils.isReadOnly(node)) {
                        return false;
                    }
                    DBSObject object = dbNode.getObject();
                    if (object instanceof DBPOrderedObject) {
                        DBEObjectReorderer objectReorderer = getObjectManager(object.getClass(), DBEObjectReorderer.class);
                        if (objectReorderer != null) {
                            final int position = ((DBPOrderedObject) object).getOrdinalPosition();
                            if (property.equals(PROP_CAN_MOVE_UP)) {
                                return position > objectReorderer.getMinimumOrdinalPosition(object);
                            }
                            return position < objectReorderer.getMaximumOrdinalPosition(object);
                        }
                    }
                }
                break;
            }
            case PROP_CAN_FILTER: {
                if (node instanceof DBNDataSource ds && ds.getDataSource() == null) {
                    return false;
                }
                if (node instanceof DBNDatabaseItem) {
                    node = node.getParentNode();
                }
                if (node instanceof DBNDatabaseNode dbNode && dbNode.getItemsMeta() != null) {
                    return true;
                }
                break;
            }
            case PROP_CAN_FILTER_OBJECT: {
                if (node.getParentNode() instanceof DBNDatabaseNode dbNode && dbNode.getItemsMeta() != null) {
                    return true;
                }
                break;
            }
            case PROP_HAS_FILTER: {
                if (node instanceof DBNDatabaseItem) {
                    node = node.getParentNode();
                }
                if (node instanceof DBNDatabaseNode dbNode && dbNode.getItemsMeta() != null) {
                    DBSObjectFilter filter = dbNode.getNodeFilter(dbNode.getItemsMeta(), true);
                    if ("defined".equals(expectedValue)) {
                        return filter != null && !filter.isEmpty();
                    } else {
                        return filter != null && !filter.isNotApplicable();
                    }
                }
                break;
            }
            case PROP_SUPPORTS_CREATING_INDEX:
                return supportsCreatingColumnObject(node, DBSTableIndex.class);
            case PROP_SUPPORTS_CREATING_CONSTRAINT:
                return supportsCreatingColumnObject(node, DBSEntityConstraint.class);
            case PROP_PROJECT_RESOURCE_EDITABLE:
                return nodeProjectHasPermission(node, RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT);
            case PROP_PROJECT_RESOURCE_VIEWABLE:
                return nodeProjectHasPermission(node, RMConstants.PERMISSION_PROJECT_RESOURCE_VIEW);
        }
        return false;
    }

    private static boolean isResourceNode(DBNNode node) {
        return node.getAdapter(IResource.class) != null;
    }

    /**
     * Check whether the owner project of the specified node has required permissions
     */
    public static boolean nodeProjectHasPermission(@NotNull DBNNode node, @NotNull String permissionName) {
        DBPProject ownerProject = node.getOwnerProjectOrNull();
        return ownerProject != null && ownerProject.hasRealmPermission(permissionName);
    }

    public static boolean canCreateObject(DBNNode node, Boolean onlySingle) {
        if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR)) {
            return false;
        }
        if (node instanceof DBNProject && DBWorkbench.isDistributed()) {
            return false;
        }
        if (node instanceof DBNDatabaseNode dbNode) {
            if (dbNode.isVirtual()) {
                // Can't create virtual objects
                return false;
            }
            DBPDataSource dataSource = dbNode.getDataSource();
            if (dataSource != null && dataSource.getInfo().isReadOnlyMetaData()) {
                return false;
            }
            if (!(node instanceof DBNDataSource) && isMetadataChangeDisabled(((DBNDatabaseNode)node))) {
                return false;
            }
        }
        if (onlySingle == null) {
            // Just try to find first create handler
            if (node instanceof DBNDataSource || node instanceof DBNLocalFolder) {
                // We always can create datasource
                return node.getOwnerProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);
            }

            Class<?> objectType;
            if (!(node instanceof DBNContainer)) {
                if (node.getParentNode() instanceof DBNContainer) {
                    node = node.getParentNode();
                }
            }
            DBNContainer container;
            if (node instanceof DBNContainer) {
                // Try to detect child type
                objectType = ((DBNContainer) node).getChildrenClass();
                container = (DBNContainer) node;
            } else {
                return false;
            }
            if (DBNUtils.isReadOnly(node)) {
                return false;
            }

            if (node instanceof DBSWrapper && DBUtils.isReadOnly(((DBSWrapper) node).getObject())) {
                return false;
            }
            if (objectType == null) {
                return false;
            }
            DBEObjectMaker<?,?> objectMaker = getObjectManager(objectType, DBEObjectMaker.class);
            if (objectMaker == null) {
                return false;
            }
            return objectMaker.canCreateObject(container.getValueObject());
        }
        if (DBNUtils.isReadOnly(node)) {
            return false;
        }

        // Check whether only single object type can be created or multiple ones
        List<IContributionItem> createItems = NavigatorHandlerObjectCreateNew.fillCreateMenuItems(null, node);

        if (onlySingle) {
            return createItems.size() == 1;
        } else {
            return createItems.size() > 1;
        }
    }

    public static boolean isMetadataChangeDisabled(DBNDatabaseNode node) {
        DBNBrowseSettings navSettings = node.getDataSourceContainer().getNavigatorSettings();
        return navSettings.isHideFolders() || navSettings.isShowOnlyEntities();
    }

    private static <T extends DBEObjectManager> T getObjectManager(Class<?> objectType, Class<T> managerType)
    {
        return ObjectManagerRegistry.getInstance().getObjectManager(objectType, managerType);
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

    private static boolean supportsCreatingColumnObject(@Nullable DBNNode node, @NotNull Class<?> supertype) {
        if (!(node instanceof DBNDatabaseItem databaseItem)) {
            return false;
        }
        DBSObject attributeObject = databaseItem.getObject();
        if (!(attributeObject instanceof DBSEntityAttribute entityAttribute)) {
            return false;
        }
        DBPDataSource dataSource = entityAttribute.getDataSource();
        if (dataSource == null || dataSource.getInfo().isReadOnlyMetaData()) {
            return false;
        }
        DBSEntity entityObject = entityAttribute.getParentObject();
        DBEStructEditor<?> structEditor = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(entityObject.getClass(), DBEStructEditor.class);
        if (structEditor == null) {
            return false;
        }
        for (Class<?> childType: structEditor.getChildTypes()) {
            DBEObjectMaker<?, ?> maker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(childType, DBEObjectMaker.class);
            if (maker != null && maker.canCreateObject(entityObject) && supertype.isAssignableFrom(childType)) {
                return true;
            }
        }
        return false;
    }
}
