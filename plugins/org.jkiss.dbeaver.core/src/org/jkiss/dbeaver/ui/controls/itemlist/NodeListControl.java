/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEStructEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.IDBNListener;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.ui.properties.PropertySourceEditable;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * NodeListControl
 */
public abstract class NodeListControl extends ObjectListControl<DBNNode> implements IDataSourceProvider, INavigatorModelView, IDBNListener, IMenuListener {
    //static final Log log = LogFactory.getLog(NodeListControl.class);

    private IWorkbenchPart workbenchPart;
    private DBNNode rootNode;
    private DBXTreeNode nodeMeta;
    private NodeSelectionProvider selectionProvider;

    public NodeListControl(
        Composite parent,
        int style,
        final IWorkbenchPart workbenchPart,
        DBNNode rootNode,
        DBXTreeNode nodeMeta)
    {
        super(parent, style, createContentProvider(rootNode, nodeMeta));
        this.workbenchPart = workbenchPart;
        this.rootNode = rootNode;
        this.nodeMeta = nodeMeta;
        this.selectionProvider = new NodeSelectionProvider(super.getSelectionProvider());

        if (workbenchPart != null) {
            // Add context menu
            ViewUtils.addContextMenu(workbenchPart, getSelectionProvider(), getItemsViewer().getControl(), this);
            // Add drag and drop support
            ViewUtils.addDragAndDropSupport(getItemsViewer());

            setDoubleClickHandler(new IDoubleClickListener() {
                public void doubleClick(DoubleClickEvent event)
                {
                    // Run default node action
                    DBNNode dbmNode = ViewUtils.getSelectedNode(getItemsViewer());
                    if (dbmNode == null) {
                        return;
                    }
                    ViewUtils.runCommand(dbmNode.getDefaultCommandId(), workbenchPart);
                }
            });
        }

        DBeaverCore.getInstance().getNavigatorModel().addListener(this);

        //getSelectionProvider().setSelection(new StructuredSelection(rootNode));
    }

    public DBPDataSource getDataSource()
    {
        if (rootNode instanceof DBNDatabaseNode) {
            return ((DBNDatabaseNode) rootNode).getObject().getDataSource();
        }
        return null;
    }

    @Override
    public void dispose()
    {
        selectionProvider.dispose();
        DBeaverCore.getInstance().getNavigatorModel().removeListener(this);
        super.dispose();
    }

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
                            ((DBNDatabaseNode) parentElement).allowsChildren();
                    }

                    @Override
                    public Object[] getChildren(Object parentElement)
                    {
                        if (parentElement instanceof DBNDatabaseNode) {
                            try {
                                // Read children with void progress monitor because inline children SHOULD be already cached
                                List<DBNDatabaseNode> children = ((DBNDatabaseNode) parentElement).getChildren(VoidProgressMonitor.INSTANCE);
                                if (CommonUtils.isEmpty(children)) {
                                    return null;
                                } else {
                                    return children.toArray();
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
        final List<DBXTreeNode> inlineMetas = new ArrayList<DBXTreeNode>();

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

    protected Class<?>[] getListBaseTypes()
    {
        List<Class<?>> baseTypes;
        // Collect base types for root node
        if (getRootNode() instanceof DBNDatabaseNode) {
            DBNDatabaseNode dbNode = (DBNDatabaseNode) getRootNode();
            baseTypes = dbNode.getChildrenTypes();
        } else {
            baseTypes = null;
        }

        // Collect base types for inline children
        return baseTypes == null || baseTypes.isEmpty() ? null : baseTypes.toArray(new Class<?>[baseTypes.size()]);
    }

    public Viewer getNavigatorViewer()
    {
        return getItemsViewer();
    }

    public DBNNode getRootNode() {
        return rootNode;
    }

    public DBXTreeNode getNodeMeta()
    {
        return nodeMeta;
    }

    @Override
    protected Object getObjectValue(DBNNode item)
    {
        return item instanceof DBSWrapper ? ((DBSWrapper)item).getObject() : item;
    }

    @Override
    protected Image getObjectImage(DBNNode item)
    {
        return item.getNodeIconDefault();
    }

    @Override
    protected boolean isNewObject(DBNNode objectValue)
    {
        return !objectValue.isPersisted();
    }

    @Override
    protected boolean isHyperlink(Object cellValue)
    {
        Object ownerObject = null;
        if (rootNode instanceof DBNDatabaseNode) {
            ownerObject = ((DBNDatabaseNode) rootNode).getValueObject();
        }
        return cellValue instanceof DBSObject && cellValue != ownerObject;
    }

    protected void navigateHyperlink(Object cellValue)
    {
        if (cellValue instanceof DBSObject) {
            DBNDatabaseNode node = NavigatorHandlerObjectOpen.getNodeByObject((DBSObject) cellValue);
            if (node != null) {
                NavigatorHandlerObjectOpen.openEntityEditor(node, null, DBeaverCore.getActiveWorkbenchWindow());
            }
        }
    }

    @Override
    protected PropertySourceAbstract createListPropertySource()
    {
        if (workbenchPart instanceof IDatabaseNodeEditor) {
            return new NodeListPropertySource(((IDatabaseNodeEditor) workbenchPart).getEditorInput().getCommandContext());
        } else {
            return super.createListPropertySource();
        }
    }

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
                Display.getDefault().asyncExec(new Runnable() {
                    public void run()
                    {
                        getItemsViewer().update(event.getNode(), null);
                    }
                });
            }
        }
    }

    public void menuAboutToShow(IMenuManager manager)
    {
        // Hook context menu
    }

    private class NodeListPropertySource extends PropertySourceEditable {

        private NodeListPropertySource(DBECommandContext commandContext)
        {
            super(commandContext, NodeListControl.this, NodeListControl.this);
        }

        public DBNNode getSourceObject()
        {
            return getCurrentListObject();
        }

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
            DBEStructEditor structEditor = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(valueObject.getClass(), DBEStructEditor.class);
            return structEditor != null && RuntimeUtils.isTypeSupported(curClass, structEditor.getChildTypes());
        }

        public IPropertyDescriptor[] getPropertyDescriptors()
        {
            Set<IPropertyDescriptor> props = getAllProperties();
            return props.toArray(new IPropertyDescriptor[props.size()]);
        }

    }


    private class NodeSelectionProvider implements ISelectionProvider, ISelectionChangedListener {

        private final ISelectionProvider original;
        private final List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();
        private final StructuredSelection defaultSelection;

        public NodeSelectionProvider(ISelectionProvider original)
        {
            this.original = original;
            this.defaultSelection = new StructuredSelection(rootNode);
            this.original.addSelectionChangedListener(this);
        }

        public void addSelectionChangedListener(ISelectionChangedListener listener)
        {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }

        public ISelection getSelection()
        {
            final ISelection selection = original.getSelection();
            if (selection == null || selection.isEmpty()) {
                return defaultSelection;
            } else {
                return selection;
            }
        }

        public void removeSelectionChangedListener(ISelectionChangedListener listener)
        {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }

        public void setSelection(ISelection selection)
        {
            if (selection == defaultSelection) {
                original.setSelection(new StructuredSelection());
            } else {
                original.setSelection(selection);
            }
        }

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