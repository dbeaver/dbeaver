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
package org.jkiss.dbeaver.ui.navigator.itemlist;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNodeHandler;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.INavigatorNodeContainer;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * NodeListControl
 */
public abstract class NodeListControl extends ObjectListControl<DBNNode> implements DBPDataSourceContainerProvider, INavigatorModelView, INavigatorListener {
    private static final Log log = Log.getLog(NodeListControl.class);

    private final IWorkbenchSite workbenchSite;
    private DBNNode rootNode;
    private DBXTreeNode nodeMeta;
    private final NodeSelectionProvider selectionProvider;

    protected NodeListControl(Composite parent, int style, final IWorkbenchSite workbenchSite, DBNNode rootNode, IContentProvider contentProvider)
    {
        super(parent, style, contentProvider);
        this.workbenchSite = workbenchSite;
        this.rootNode = rootNode;

        this.selectionProvider = createSelectionProvider(super.getSelectionProvider());

        // Add context menu
        NavigatorUtils.addContextMenu(workbenchSite, getItemsViewer(), this.selectionProvider);

        setDoubleClickHandler(event -> {
            // Run default node action
            ISelection selection = getItemsViewer().getSelection();
            if (selection instanceof IStructuredSelection ss) {
                for (Object obj : ss.toList()) {
                    if (obj instanceof DBNNode node && node.allowsOpen()) {
                        openNodeEditor(node);
                    }
                }
            }
        });

        // Add drag and drop support
        NavigatorUtils.addDragAndDropSupport(getItemsViewer());
        if (workbenchSite != null) {
            EditorUtils.trackControlContext(workbenchSite, this.getItemsViewer().getControl(), INavigatorModelView.NAVIGATOR_CONTEXT_ID);
        }

        DBWorkbench.getPlatform().getNavigatorModel().addListener(this);

//        if (workbenchSite != null) {
//            UIUtils.addFocusTracker(workbenchSite, INavigatorModelView.NAVIGATOR_CONTROL_ID, getItemsViewer().getControl());
//        }
    }

    protected NodeSelectionProvider createSelectionProvider(ISelectionProvider selectionProvider) {
        return new NodeSelectionProvider(selectionProvider);
    }

    protected void openNodeEditor(DBNNode node) {
        IServiceLocator serviceLocator = workbenchSite != null ?
            workbenchSite :
            UIUtils.getActiveWorkbenchWindow();
        NavigatorUtils.executeNodeAction(DBXTreeNodeHandler.Action.open, node, serviceLocator);
    }

    NodeListControl(
            Composite parent,
            int style,
            final IWorkbenchSite workbenchSite,
            DBNNode rootNode,
            DBXTreeNode nodeMeta)
    {
        this(parent, style, workbenchSite, rootNode, createContentProvider(rootNode, nodeMeta));
        this.nodeMeta = nodeMeta;
    }

    IWorkbenchSite getWorkbenchSite() {
        return workbenchSite;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        if (rootNode instanceof DBNDatabaseNode node) {
            return node.getDataSourceContainer();
        }
        return null;
    }

    @Override
    public void disposeControl()
    {
        if (selectionProvider != null) {
            selectionProvider.dispose();
        }
        DBWorkbench.getPlatform().getNavigatorModel().removeListener(this);
        super.disposeControl();
    }

    @Override
    protected List<DBNNode> createViewerInput(Collection<DBNNode> objectList) {
        return new NodeListInput(objectList);
    }

    @Override
    public ISelectionProvider getSelectionProvider() {
        return selectionProvider;
    }

    private static IContentProvider createContentProvider(DBNNode node, DBXTreeNode metaNode)
    {
        if (node instanceof DBNDatabaseNode dbNode) {
            if (metaNode == null) {
                metaNode = dbNode.getMeta();
            }
            final List<DBXTreeNode> inlineMetas = collectInlineMetas(dbNode, metaNode);

            if (!inlineMetas.isEmpty() || !(node instanceof DBNDataSource) && dbNode.isDynamicStructObject()) {
                return new TreeContentProvider() {
                    @Override
                    public boolean hasChildren(Object parentElement) {
                        return parentElement instanceof DBNDatabaseNode node && node.hasChildren(false);
                    }

                    @Override
                    public Object[] getChildren(Object parentElement) {
                        if (parentElement instanceof DBNDatabaseNode node) {
                            try {
                                // Read children with void progress monitor because inline children SHOULD be already cached
                                DBNNode[] children = DBNUtils.getNodeChildrenFiltered(new VoidProgressMonitor(), node, false);
                                if (ArrayUtils.isEmpty(children)) {
                                    return null;
                                } else {
                                    return children;
                                }
                            } catch (DBException e) {
                                log.error(e);
                            }
                        }
                        return null;
                    }
                };
            }
        }
        return new ListContentProvider();
    }

    @Override
    protected boolean isDynamicObject(DBNNode object) {
        return object instanceof DBNDatabaseDynamicItem;
    }

    protected static List<DBXTreeNode> collectInlineMetas(DBNDatabaseNode node, DBXTreeNode meta)
    {
        final List<DBXTreeNode> inlineMetas = new ArrayList<>();

        if (meta instanceof DBXTreeFolder) {
            // If this is a folder - iterate through all its children
            for (DBXTreeNode metaChild : meta.getChildren(node)) {
                collectInlineChildren(metaChild, inlineMetas);
            }

        } else {
            // Just check child metas
            collectInlineChildren(meta, inlineMetas);
        }
        return inlineMetas;
    }

    private static void collectInlineChildren(DBXTreeNode meta, List<DBXTreeNode> inlineMetas)
    {
        final List<DBXTreeNode> metaChildren = meta.getChildren(null);
        if (!CommonUtils.isEmpty(metaChildren)) {
            for (DBXTreeNode child : metaChildren) {
                if (child.isInline()) {
                    inlineMetas.add(child);
                }
            }
        }
    }

    @Nullable
    @Override
    protected Class<?>[] getListBaseTypes(Collection<DBNNode> items)
    {
        // Collect base types for root node
        if (getRootNode() instanceof DBNDatabaseNode dbNode) {
            List<Class<?>> baseTypes = dbNode.getChildrenTypes(nodeMeta);
            if (CommonUtils.isEmpty(baseTypes) && dbNode instanceof DBNDatabaseFolder folder) {
                Class<? extends DBSObject> childrenClass = folder.getChildrenClass();
                if (childrenClass != null) {
                    return new Class[] { childrenClass };
                }
            }
            // Collect base types for inline children
            return CommonUtils.isEmpty(baseTypes) ? null : baseTypes.toArray(new Class<?>[0]);
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer()
    {
        return getItemsViewer();
    }

    @Override
    public DBNNode getRootNode() {
        return rootNode;
    }

    public void setRootNode(DBNNode rootNode) {
        this.rootNode = rootNode;
    }

    protected DBXTreeNode getNodeMeta()
    {
        return nodeMeta;
    }

    @Override
    protected Object getObjectValue(DBNNode item)
    {
        if (item instanceof DBSWrapper wrapper) {
            return wrapper.getObject();
        } else if (item instanceof DBNObjectNode node) {
            return node.getNodeObject();
        }
        return item;
    }

    @Override
    protected DBPImage getObjectImage(DBNNode item)
    {
        return item.getNodeIconDefault();
    }

    @Override
    protected boolean isNewObject(DBNNode objectValue)
    {
        return !objectValue.isPersisted();
    }

    @Override
    protected boolean isReadOnlyList() {
        DBPDataSourceContainer container = getDataSourceContainer();
        return container != null && container.isConnectionReadOnly();
    }

    @NotNull
    @Override
    protected String getListConfigId(List<Class<?>> classList) {
        StringBuilder sb = new StringBuilder("NodeList");
        for (Class<?> theClass : classList) {
            sb.append("/").append(theClass.getSimpleName());
        }
        return sb.toString();
    }

    @Override
    protected PropertySourceAbstract createListPropertySource()
    {
        if (workbenchSite instanceof IWorkbenchPartSite partSite && partSite.getPart() instanceof IDatabaseEditor de) {
            IEditorInput editorInput = de.getEditorInput();
            if (editorInput instanceof IDatabaseEditorInput dei) {
                return new NodeListPropertySource(dei.getCommandContext());
            }
        }
        return super.createListPropertySource();
    }

    @Override
    public void nodeChanged(final DBNEvent event)
    {
        if (isDisposed()) {
            return;
        }
        if (event.getNode().isChildOf(getRootNode())) {
            switch (event.getAction()) {
                case ADD:
                case REMOVE:
                    loadData(false, true);
                    break;
                case UPDATE:
                    getItemsViewer().update(event.getNode(), null);
                    break;
            }
        }
    }

    @Override
    protected ObjectViewerRenderer createRenderer()
    {
        return new NodeRenderer();
    }

    private class NodeRenderer extends ViewerRenderer {
        @Override
        public boolean isHyperlink(Object element, Object cellValue)
        {
            Object ownerObject = null;
            if (rootNode instanceof DBNDatabaseNode node) {
                ownerObject = node.getValueObject();
            }
            return cellValue instanceof DBSObject && cellValue != ownerObject;
        }

        @Override
        public void navigateHyperlink(Object cellValue)
        {
            if (cellValue instanceof DBSObject object) {
                NavigatorHandlerObjectOpen.openEntityEditor(object);
            }
        }

    }

    private class NodeListPropertySource extends PropertySourceEditable implements DBNNodeReference {

        private NodeListPropertySource(DBECommandContext commandContext)
        {
            super(commandContext, NodeListControl.this, NodeListControl.this);
        }

        @Override
        public DBNNode getSourceObject()
        {
            return getCurrentListObject();
        }

        @Override
        public Object getEditableValue()
        {
            return getObjectValue(getCurrentListObject());
        }

        @Override
        public boolean isEditable(Object editableValue)
        {
            if (editableValue == null) {
                return false;
            }
            final DBNNode rootNode = getRootNode();
            if (!(rootNode instanceof DBNDatabaseNode databaseNode)) {
                return false;
            }
            final Class<?> curClass = editableValue.getClass();
            final Object valueObject = databaseNode.getValueObject();
            if (valueObject == null) {
                return false;
            }
            DBEObjectEditor objectEditor = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(curClass, DBEObjectEditor.class);
            return objectEditor != null && editableValue instanceof DBPObject object && objectEditor.canEditObject(object)
                && DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
        }

        @Override
        public DBPPropertyDescriptor[] getProperties() {
            return getAllProperties().toArray(new DBPPropertyDescriptor[0]);
        }

        @Override
        public void setPropertyValue(@Nullable DBRProgressMonitor monitor, Object editableValue, ObjectPropertyDescriptor prop, Object newValue) throws IllegalArgumentException {
            super.setPropertyValue(monitor, editableValue, prop, newValue);
            resetLazyPropertyCache(getCurrentListObject(), prop.getId());
        }

        @Override
        public DBNNode getReferencedNode() {
            return NodeListControl.this.getRootNode();
        }
    }


    protected class NodeSelectionProvider implements ISelectionProvider, ISelectionChangedListener {

        private final ISelectionProvider original;
        private final List<ISelectionChangedListener> listeners = new ArrayList<>();
        private final StructuredSelection defaultSelection;

        NodeSelectionProvider(ISelectionProvider original)
        {
            this.original = original;
            this.defaultSelection = new StructuredSelection(rootNode);
            this.original.addSelectionChangedListener(this);
        }

        @Override
        public void addSelectionChangedListener(ISelectionChangedListener listener)
        {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }

        @Override
        public ISelection getSelection()
        {
            final ISelection selection = original.getSelection();
            if (selection == null || selection.isEmpty()) {
                return defaultSelection;
            } else {
                return selection;
            }
        }

        @Override
        public void removeSelectionChangedListener(ISelectionChangedListener listener)
        {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }

        @Override
        public void setSelection(ISelection selection)
        {
            if (selection == defaultSelection) {
                original.setSelection(new StructuredSelection());
            } else {
                original.setSelection(selection);
            }
            selectionChanged(new SelectionChangedEvent(this, selection));
        }

        @Override
        public void selectionChanged(SelectionChangedEvent event)
        {
            synchronized (listeners) {
                event = new SelectionChangedEvent(this, getSelection());
                for (ISelectionChangedListener listener : listeners) {
                    listener.selectionChanged(event);
                }
            }

        }

        void dispose()
        {
            this.original.removeSelectionChangedListener(this);
        }
    }

    private class NodeListInput extends ArrayList<DBNNode> implements INavigatorNodeContainer {
        public NodeListInput(Collection<DBNNode> objectList) {
            super(objectList);
        }

        @Override
        public DBNNode getRootNode() {
            return NodeListControl.this.getRootNode();
        }

    }
}