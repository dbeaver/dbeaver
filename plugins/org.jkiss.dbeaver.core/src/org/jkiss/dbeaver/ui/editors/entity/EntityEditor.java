/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityEditorDescriptor;
import org.jkiss.dbeaver.registry.EntityEditorsRegistry;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.ViewSQLDialog;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.views.properties.PropertyPageTabbed;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * EntityEditor
 */
public class EntityEditor extends MultiPageDatabaseEditor implements INavigatorModelView, ISaveablePart2
{
    static final Log log = LogFactory.getLog(EntityEditor.class);

    private static Map<Class<?>, String> defaultPageMap = new HashMap<Class<?>, String>();

    private Map<String, IEditorPart> editorMap = new HashMap<String, IEditorPart>();
    private DBECommandAdapter commandListener;

    public EntityEditor()
    {
    }

    public DBSObject getDatabaseObject()
    {
        return getEditorInput().getDatabaseObject();
    }

    public DBEObjectCommander getObjectCommander()
    {
        return getEditorInput().getObjectCommander();
    }

    @Override
    public void dispose()
    {
//        if (getObjectCommander() != null && getObjectCommander().isDirty()) {
//            getObjectCommander().resetChanges();
//        }
        if (commandListener != null && getObjectCommander() != null) {
            getObjectCommander().removeCommandListener(commandListener);
            commandListener = null;
        }
        if (getDatabaseObject() != null) {
            DBNDatabaseNode treeNode = getEditorInput().getTreeNode();
            if (getDatabaseObject().isPersisted()) {
                // Reset node name (if it was set by editor)
                treeNode.setNodeName(null);
            } else {
                // If edited object is still not persisted then remove object's node
                if (treeNode instanceof DBNDatabaseItem) {
                    DBNNode parentNode = treeNode.getParentNode();
                    if (parentNode instanceof DBNDatabaseFolder) {
                        try {
                            ((DBNDatabaseFolder)parentNode).removeChildItem(treeNode);
                        } catch (DBException e) {
                            log.error(e);
                        }
                    }
                }
            }
        }

        super.dispose();
    }

    @Override
    public boolean isDirty()
    {
        return getObjectCommander() != null && getObjectCommander().isDirty();
    }

    /**
     * Saves data in all nested editors
     * @param monitor progress monitor
     */
    public void doSave(IProgressMonitor monitor)
    {
        if (!isDirty()) {
            return;
        }

        monitor.beginTask("Preview changes", 1);
        int previewResult = showChanges(true);
        monitor.done();

        if (previewResult == IDialogConstants.PROCEED_ID) {
            Throwable error = null;
            try {
                getObjectCommander().saveChanges(new DefaultProgressMonitor(monitor));
            } catch (DBException e) {
                error = e;
            }
            final Throwable showError = error;
            Display.getDefault().asyncExec(new Runnable() {
                public void run()
                {
                    if (showError != null) {
                        UIUtils.showErrorDialog(getSite().getShell(), "Could not save '" + getDatabaseObject().getName() + "'", null, showError);
                    }
                    firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
        }
    }

    public void revertChanges()
    {
        if (isDirty()) {
            getObjectCommander().resetChanges();
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void undoChanges()
    {
        if (getObjectCommander() != null && getObjectCommander().canUndoCommand()) {
            getObjectCommander().undoCommand();
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void redoChanges()
    {
        if (getObjectCommander() != null && getObjectCommander().canRedoCommand()) {
            getObjectCommander().redoCommand();
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public int showChanges(boolean allowSave)
    {
        if (getObjectCommander() == null) {
            return IDialogConstants.CANCEL_ID;
        }
        Collection<? extends DBECommand> commands = getObjectCommander().getCommands();
        StringBuilder script = new StringBuilder();
        for (DBECommand command : commands) {
            try {
                command.validateCommand();
            } catch (final DBException e) {
                Display.getDefault().syncExec(new Runnable() {
                    public void run()
                    {
                        UIUtils.showErrorDialog(getSite().getShell(), "Validation", e.getMessage());
                    }
                });
                return IDialogConstants.CANCEL_ID;
            }
            IDatabasePersistAction[] persistActions = command.getPersistActions();
            if (!CommonUtils.isEmpty(persistActions)) {
                for (IDatabasePersistAction action : persistActions) {
                    if (script.length() > 0) {
                        script.append('\n');
                    }
                    script.append(action.getScript());
                    script.append(getObjectCommander().getDataSourceContainer().getDataSource().getInfo().getScriptDelimiter());
                }
            }
        }

        ChangesPreviewer changesPreviewer = new ChangesPreviewer(script, allowSave);
        getSite().getShell().getDisplay().syncExec(changesPreviewer);
        return changesPreviewer.getResult();
/*

        Shell shell = getSite().getShell();
        ViewTextDialog dialog = new ViewTextDialog(shell, "Script", script.toString());
        dialog.setTextWidth(0);
        dialog.setTextHeight(0);
        dialog.setImage(DBIcon.SQL_PREVIEW.getImage());
        dialog.open();
*/
    }

    protected void createPages()
    {
        // Command listener
        commandListener = new DBECommandAdapter() {
            @Override
            public void onCommandChange(DBECommand command)
            {
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        };
        getObjectCommander().addCommandListener(commandListener);

        // Property listener
        addPropertyListener(new IPropertyListener() {
            public void propertyChanged(Object source, int propId)
            {
                if (propId == IEditorPart.PROP_DIRTY) {
                    EntityEditorPropertyTester.firePropertyChange(EntityEditorPropertyTester.PROP_DIRTY);
                    EntityEditorPropertyTester.firePropertyChange(EntityEditorPropertyTester.PROP_CAN_UNDO);
                    EntityEditorPropertyTester.firePropertyChange(EntityEditorPropertyTester.PROP_CAN_REDO);
                }
            }
        });

        super.createPages();

        EntityEditorsRegistry editorsRegistry = DBeaverCore.getInstance().getEditorsRegistry();
        DBSObject databaseObject = getEditorInput().getDatabaseObject();

        // Add object editor page
        EntityEditorDescriptor defaultEditor = editorsRegistry.getMainEntityEditor(databaseObject.getClass());
        boolean mainAdded = false;
        if (defaultEditor != null) {
            mainAdded = addEditorTab(defaultEditor);
        }
        if (mainAdded) {
            DBNNode node = getEditorInput().getTreeNode();
            setPageText(0, "Properties");
            setPageToolTip(0, node.getNodeType() + " Properties");
            setPageImage(0, node.getNodeIconDefault());
        }
/*
        if (!mainAdded) {
            try {
                DBNNode node = getEditorInput().getTreeNode();
                int index = addPage(new DefaultObjectEditor(node), getEditorInput());
                setPageText(index, "Properties");
                if (node instanceof DBNDatabaseNode) {
                    setPageToolTip(index, ((DBNDatabaseNode)node).getMeta().getLabel() + " Properties");
                }
                setPageImage(index, node.getNodeIconDefault());
            } catch (PartInitException e) {
                log.error("Error creating object editor");
            }
        }
*/

        // Add contributed pages
        addContributions(EntityEditorDescriptor.POSITION_START);

        // Add navigator tabs
        //addNavigatorTabs();

        // Add contributed pages
        addContributions(EntityEditorDescriptor.POSITION_END);

        String defPageId = getEditorInput().getDefaultPageId();
        if (defPageId == null) {
            defPageId = defaultPageMap.get(getEditorInput().getDatabaseObject().getClass()); 
        }
        if (defPageId != null) {
            IEditorPart defEditorPage = editorMap.get(defPageId);
            if (defEditorPage != null) {
                setActiveEditor(defEditorPage);
            }
        }
    }

    private void addNavigatorTabs()
    {
        // Collect tabs from navigator tree model
        final List<TabInfo> tabs = new ArrayList<TabInfo>();
        DBRRunnableWithProgress tabsCollector = new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
            {
                tabs.addAll(collectTabs(monitor));
            }
        };
        DBNDatabaseNode node = getEditorInput().getTreeNode();
        try {
            if (node.isLazyNode()) {
                DBeaverCore.getInstance().runInProgressService(tabsCollector);
            } else {
                tabsCollector.run(VoidProgressMonitor.INSTANCE);
            }
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // just go further
        }

        for (TabInfo tab : tabs) {
            addNodeTab(tab);
        }
    }

    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);

        DBSObject object = getEditorInput().getDatabaseObject();
        IEditorPart editor = getEditor(newPageIndex);
        for (Map.Entry<String,IEditorPart> entry : editorMap.entrySet()) {
            if (entry.getValue() == editor) {
                defaultPageMap.put(object.getClass(), entry.getKey());
                break;
            }
        }
    }

    public int promptToSaveOnClose()
    {
        final int result = ConfirmationDialog.showConfirmDialog(
            getSite().getShell(),
            PrefConstants.CONFIRM_ENTITY_EDIT_CLOSE,
            ConfirmationDialog.QUESTION_WITH_CANCEL,
            getDatabaseObject().getName());
        if (result == IDialogConstants.YES_ID) {
//            getWorkbenchPart().getSite().getPage().saveEditor(this, false);
            return ISaveablePart2.YES;
        } else if (result == IDialogConstants.NO_ID) {
            return ISaveablePart2.NO;
        } else {
            return ISaveablePart2.CANCEL;
        }
    }

    private static class TabInfo {
        DBNDatabaseNode node;
        DBXTreeNode meta;
        private TabInfo(DBNDatabaseNode node)
        {
            this.node = node;
        }
        private TabInfo(DBNDatabaseNode node, DBXTreeNode meta)
        {
            this.node = node;
            this.meta = meta;
        }
        public String getName()
        {
            return meta == null ? node.getNodeName() : meta.getLabel();
        }
    }

    private List<TabInfo> collectTabs(DBRProgressMonitor monitor)
    {
        List<TabInfo> tabs = new ArrayList<TabInfo>();

        // Add all nested folders as tabs
        DBNNode node = getEditorInput().getTreeNode();
        if (node instanceof DBNDataSource && !((DBNDataSource)node).getDataSourceContainer().isConnected()) {
            // Do not add children tabs
        } else if (node != null) {
            try {
                List<? extends DBNNode> children = node.getChildren(monitor);
                if (children != null) {
                    for (DBNNode child : children) {
                        if (child instanceof DBNDatabaseFolder) {
                            monitor.subTask("Add folder '" + child.getNodeName() + "'");
                            tabs.add(new TabInfo((DBNDatabaseFolder)child));
                        }
                    }
                }
            } catch (DBException e) {
                log.error("Error initializing entity editor", e);
            }
            // Add itself as tab (if it has child items)
            if (node instanceof DBNDatabaseNode) {
                DBNDatabaseNode databaseNode = (DBNDatabaseNode)node;
                List<DBXTreeNode> subNodes = databaseNode.getMeta().getChildren(databaseNode);
                if (subNodes != null) {
                    for (DBXTreeNode child : subNodes) {
                        if (child instanceof DBXTreeItem) {
                            try {
                                if (!((DBXTreeItem)child).isOptional() || databaseNode.hasChildren(monitor, child)) {
                                    monitor.subTask("Add node '" + node.getNodeName() + "'");
                                    tabs.add(new TabInfo((DBNDatabaseNode)node, child));
                                }
                            } catch (DBException e) {
                                log.debug("Can't add child items tab", e);
                            }
                        }
                    }
                }
            }
        }
        return tabs;
    }

    private void addContributions(String position)
    {
        EntityEditorsRegistry editorsRegistry = DBeaverCore.getInstance().getEditorsRegistry();
        List<EntityEditorDescriptor> descriptors = editorsRegistry.getEntityEditors(getEditorInput().getDatabaseObject().getClass(), position);
        for (EntityEditorDescriptor descriptor : descriptors) {
            addEditorTab(descriptor);
        }
    }

    private boolean addEditorTab(EntityEditorDescriptor descriptor)
    {
        try {
            IEditorPart editor = descriptor.createEditor();
            if (editor == null) {
                return false;
            }
            int index = addPage(editor, getEditorInput());
            setPageText(index, descriptor.getName());
            if (descriptor.getIcon() != null) {
                setPageImage(index, descriptor.getIcon());
            }
            if (!CommonUtils.isEmpty(descriptor.getDescription())) {
                setPageToolTip(index, descriptor.getDescription());
            }
            editorMap.put(descriptor.getId(), editor);
            return true;
        } catch (Exception ex) {
            log.error("Error adding nested editor", ex);
            return false;
        }
    }

    private void addNodeTab(TabInfo tabInfo)
    {
        try {
            EntityNodeEditor nodeEditor = new EntityNodeEditor(tabInfo.node, tabInfo.meta);
            int index = addPage(nodeEditor, getEditorInput());
            if (tabInfo.meta == null) {
                setPageText(index, tabInfo.node.getNodeName());
                setPageImage(index, tabInfo.node.getNodeIconDefault());
                setPageToolTip(index, getEditorInput().getTreeNode().getNodeType() + " " + tabInfo.node.getNodeName());
            } else {
                setPageText(index, tabInfo.meta.getLabel());
                if (tabInfo.meta.getDefaultIcon() != null) {
                    setPageImage(index, tabInfo.meta.getDefaultIcon());
                } else {
                    setPageImage(index, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
                }
                setPageToolTip(index, tabInfo.meta.getLabel());
            }
            editorMap.put("node." + tabInfo.getName(), nodeEditor);
        } catch (PartInitException ex) {
            log.error("Error adding nested editor", ex);
        }
    }

    public void refreshPart(final Object source)
    {
        // Reinit object manager
        final boolean persisted = getEditorInput().getDatabaseObject().isPersisted();
        if (persisted) {
            // Refresh visual content in parts
            getSite().getShell().getDisplay().asyncExec(new Runnable() { public void run() {

                int pageCount = getPageCount();
                for (int i = 0; i < pageCount; i++) {
                    IWorkbenchPart part = getEditor(i);
                    if (part instanceof IRefreshablePart) {
                        ((IRefreshablePart)part).refreshPart(source);
                    }
                }
            }});
        }
        setPartName(getEditorInput().getName());
        setTitleImage(getEditorInput().getImageDescriptor());
    }

    public DBNNode getRootNode() {
        return getEditorInput().getTreeNode();
    }

    public Viewer getNavigatorViewer()
    {
        IWorkbenchPart activePart = getActiveEditor();
        if (activePart instanceof INavigatorModelView) {
            return ((INavigatorModelView)activePart).getNavigatorViewer();
        }
        return null;
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == IPropertySheetPage.class) {
            return new PropertyPageTabbed();
        }
        return super.getAdapter(adapter);
    }

    private class ChangesPreviewer implements Runnable {

        private final StringBuilder script;
        private final boolean allowSave;
        private int result;

        public ChangesPreviewer(StringBuilder script, boolean allowSave)
        {
            this.script = script;
            this.allowSave = allowSave;
        }

        public void run()
        {
            ViewSQLDialog dialog = new ViewSQLDialog(
                getEditorSite(),
                getDataSource().getContainer(),
                allowSave ? "Persist Changes" : "Preview Changes", 
                script.toString());
            dialog.setShowSaveButton(allowSave);
            dialog.setImage(DBIcon.SQL_PREVIEW.getImage());
            result = dialog.open();
        }

        public int getResult()
        {
            return result;
        }
    }

}
