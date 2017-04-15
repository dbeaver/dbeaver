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
package org.jkiss.dbeaver.ui.controls.itemlist;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.INavigatorListener;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNodeHandler;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * NodeListControl
 */
public abstract class NodeListControl extends ObjectListControl<DBNNode> implements IDataSourceContainerProvider, INavigatorModelView, INavigatorListener {
    static final Log log = Log.getLog(NodeListControl.class);

    private final IWorkbenchSite workbenchSite;
    private DBNNode rootNode;
    private DBXTreeNode nodeMeta;
    private final NodeSelectionProvider selectionProvider;

    protected NodeListControl(Composite parent, int style, final IWorkbenchSite workbenchSite, DBNNode rootNode, IContentProvider contentProvider)
    {
        super(parent, style, contentProvider);
        this.workbenchSite = workbenchSite;
        this.rootNode = rootNode;

        this.selectionProvider = new NodeSelectionProvider(super.getSelectionProvider());

        // Add context menu
        NavigatorUtils.addContextMenu(workbenchSite, getItemsViewer());

        setDoubleClickHandler(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event)
            {
                // Run default node action
                DBNNode node = NavigatorUtils.getSelectedNode(getItemsViewer());
                if (node == null || !node.allowsOpen()) {
                    return;
                }
                openNodeEditor(node);
            }
        });

        // Add drag and drop support
        NavigatorUtils.addDragAndDropSupport(getItemsViewer());

        DBeaverCore.getInstance().getNavigatorModel().addListener(this);
    }

    protected void openNodeEditor(DBNNode node) {
        IServiceLocator serviceLocator = workbenchSite != null ?
            workbenchSite :
            DBeaverUI.getActiveWorkbenchWindow();
        NavigatorUtils.executeNodeAction(DBXTreeNodeHandler.Action.open, node, serviceLocator);
    }

    public NodeListControl(
        Composite parent,
        int style,
        final IWorkbenchSite workbenchSite,
        DBNNode rootNode,
        DBXTreeNode nodeMeta)
    {
        this(parent, style, workbenchSite, rootNode, createContentProvider(rootNode, nodeMeta));
        this.nodeMeta = nodeMeta;
    }

    public IWorkbenchSite getWorkbenchSite() {
        return workbenchSite;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        if (rootNode instanceof DBNDatabaseNode) {
            return ((DBNDatabaseNode) rootNode).getDataSourceContainer();
        }
        return null;
    }

    @Override
    public void disposeControl()
    {
        selectionProvider.dispose();
        DBeaverCore.getInstance().getNavigatorModel().removeListener(this);
        super.disposeControl();
    }

    @Override
    public ISelectionProvider getSelectionProvider()
    {
        return selectionProvider;
    }

    private static IContentProvider createContentProvider(DBNNode node, DBXTreeNode metaNode)
    {
        if (node instanceof DBNDatabaseNode) {
            final DBNDatabaseNode dbNode = (DBNDatabaseNode) node;
            if (metaNode == null) {
                metaNode = dbNode.getMeta();
            }
            final List<DBXTreeNode> inlineMetas = collectInlineMetas(dbNode, metaNode);

            if (!inlineMetas.isEmpty()) {
                return new TreeContentProvider() {
                    @Override
                    public boolean hasChildren(Object parentElement)
                    {
                        return parentElement instanceof DBNDatabaseNode &&
                            ((DBNDatabaseNode) parentElement).hasChildren(false);
                    }

                    @Override
                    public Object[] getChildren(Object parentElement)
                    {
                        if (parentElement instanceof DBNDatabaseNode) {
                            try {
                                // Read children with void progress monitor because inline children SHOULD be already cached
                                DBNNode[] children = NavigatorUtils.getNodeChildrenFiltered(new VoidProgressMonitor(), (DBNDatabaseNode)parentElement, false);
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

    private static List<DBXTreeNode> collectInlineMetas(DBNDatabaseNode node, DBXTreeNode meta)
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
        if (getRootNode() instanceof DBNDatabaseNode) {
            DBNDatabaseNode dbNode = (DBNDatabaseNode) getRootNode();
            List<Class<?>> baseTypes = dbNode.getChildrenTypes(nodeMeta);
            // Collect base types for inline children
            return CommonUtils.isEmpty(baseTypes) ? null : baseTypes.toArray(new Class<?>[baseTypes.size()]);
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
        return item instanceof DBSWrapper ? ((DBSWrapper)item).getObject() : item;
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

    @NotNull
    @Override
    protected String getListConfigId(List<Class<?>> classList) {
        StringBuilder sb = new StringBuilder("NodeList");
        for (Class theClass : classList) {
            sb.append("/").append(theClass.getSimpleName());
        }
        return sb.toString();
    }

    @Override
    protected PropertySourceAbstract createListPropertySource()
    {
        if (workbenchSite instanceof IWorkbenchPartSite && ((IWorkbenchPartSite) workbenchSite).getPart() instanceof IDatabaseEditor) {
            return new NodeListPropertySource(((IDatabaseEditor) ((IWorkbenchPartSite) workbenchSite).getPart()).getEditorInput().getCommandContext());
        } else {
            return super.createListPropertySource();
        }
    }

    @Override
    public void nodeChanged(final DBNEvent event)
    {
        if (isDisposed()) {
            return;
        }
        if (event.getNode().isChildOf(getRootNode())) {
            if (event.getAction() != DBNEvent.Action.UPDATE) {
                // Add or remove - just reload list content
                loadData(false);
            } else {
                DBeaverUI.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        getItemsViewer().update(event.getNode(), null);
                    }
                });
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
        public boolean isHyperlink(Object cellValue)
        {
            Object ownerObject = null;
            if (rootNode instanceof DBNDatabaseNode) {
                ownerObject = ((DBNDatabaseNode) rootNode).getValueObject();
            }
            return cellValue instanceof DBSObject && cellValue != ownerObject;
        }

        @Override
        public void navigateHyperlink(Object cellValue)
        {
            if (cellValue instanceof DBSObject) {
                NavigatorHandlerObjectOpen.openEntityEditor((DBSObject) cellValue);
            }
        }

    }

    private class NodeListPropertySource extends PropertySourceEditable {

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
            if (!(rootNode instanceof DBNDatabaseNode)) {
                return false;
            }
            final Class<?> curClass = editableValue.getClass();
            final Object valueObject = ((DBNDatabaseNode) rootNode).getValueObject();
            if (valueObject == null) {
                return false;
            }
            DBEObjectEditor objectEditor = EntityEditorsRegistry.getInstance().getObjectManager(curClass, DBEObjectEditor.class);
            return objectEditor != null && editableValue instanceof DBPObject && objectEditor.canEditObject((DBPObject) editableValue);
        }

        @Override
        public DBPPropertyDescriptor[] getPropertyDescriptors2()
        {
            Set<DBPPropertyDescriptor> props = getAllProperties();
            return props.toArray(new DBPPropertyDescriptor[props.size()]);
        }

    }


    private class NodeSelectionProvider implements ISelectionProvider, ISelectionChangedListener {

        private final ISelectionProvider original;
        private final List<ISelectionChangedListener> listeners = new ArrayList<>();
        private final StructuredSelection defaultSelection;

        public NodeSelectionProvider(ISelectionProvider original)
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
}