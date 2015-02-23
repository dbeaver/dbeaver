/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.views.navigator.database;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectRename;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class DatabaseNavigatorTree extends Composite implements IDBNListener
{
    static final Log log = Log.getLog(DatabaseNavigatorTree.class);

    private TreeViewer viewer;
    private DBNModel model;
    private TreeEditor treeEditor;
    private boolean checkEnabled;

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style)
    {
        this(parent, rootNode, style, false);
    }

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style, boolean showRoot)
    {
        super(parent, SWT.NONE);
        this.setLayout(new FillLayout());
        this.model = DBNModel.getInstance();
        this.model.addListener(this);
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (model != null) {
                    model.removeListener(DatabaseNavigatorTree.this);
                    model = null;
                }
            }
        });

        checkEnabled = (style & SWT.CHECK) != 0;

        // Create tree
        final ISelection defaultSelection = new StructuredSelection(rootNode);
        // TODO: there are problems with this tree when we have a lot of items.
        // TODO: I may set SWT.SINGLE style and it'll solve the problem at least when traversing tree
        // TODO: But we need multiple selection (to copy, export, etc)
        // TODO: need to do something with it
        int treeStyle = SWT.H_SCROLL | SWT.V_SCROLL | style;
        if (checkEnabled) {
            this.viewer = new CheckboxTreeViewer(this, treeStyle);
//            ((CheckboxTreeViewer)this.viewer).setCheckStateProvider(new ICheckStateProvider() {
//                @Override
//                public boolean isChecked(Object element) {
//                    TreeItem widget = (TreeItem) viewer.testFindItem(element);
//                    return widget.getChecked();
//                }
//
//                @Override
//                public boolean isGrayed(Object element) {
//                    return element instanceof DBNContainer;
//                }
//            });
        } else {
            this.viewer = new TreeViewer(this, treeStyle) {
                @Override
                public ISelection getSelection()
                {
                    ISelection selection = super.getSelection();
                    return selection.isEmpty() ? defaultSelection : selection;
                }
            };
        }
        this.viewer.getTree().setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
        this.viewer.setUseHashlookup(true);
        if (rootNode.getParentNode() == null) {
            //this.viewer.setAutoExpandLevel(2);
        }
        this.viewer.setLabelProvider(new DatabaseNavigatorLabelProvider(this.viewer));
        this.viewer.setContentProvider(new DatabaseNavigatorContentProvider(this.viewer, showRoot));
        this.viewer.setInput(new DatabaseNavigatorContent(rootNode));

        initEditor();
    }

    public DBNNode getModel()
    {
        DatabaseNavigatorContent content = (DatabaseNavigatorContent) this.viewer.getInput();
        return content.getRootNode();
    }

    private void initEditor()
    {
        Tree treeControl = this.viewer.getTree();

        treeEditor = new TreeEditor(treeControl);
        treeEditor.horizontalAlignment = SWT.LEFT;
        treeEditor.verticalAlignment = SWT.TOP;
        treeEditor.grabHorizontal = false;
        treeEditor.minimumWidth = 50;

        //treeControl.addSelectionListener(new TreeSelectionAdapter());
        if (!checkEnabled) {
            // Add rename listener only for non CHECK trees
            treeControl.addMouseListener(new TreeSelectionAdapter());
        }
    }

    @NotNull
    public TreeViewer getViewer()
    {
        return viewer;
    }

    @Override
    public void nodeChanged(final DBNEvent event)
    {
        switch (event.getAction()) {
            case ADD:
            case REMOVE:
                final DBNNode parentNode = event.getNode().getParentNode();
                if (parentNode != null) {
                    UIUtils.runInUI(null, new Runnable() {
                        @Override
                        public void run()
                        {
                            if (!viewer.getControl().isDisposed()) {
                                if (!parentNode.isDisposed()) {
                                    viewer.refresh(getViewerObject(parentNode));
                                }
                            }
                        }
                    });
                }
                break;
            case UPDATE:
                UIUtils.runInUI(null, new Runnable() {
                    @Override
                    public void run()
                    {
                        if (!viewer.getControl().isDisposed() && !viewer.isBusy()) {
                            if (event.getNode() != null) {
                                switch (event.getNodeChange()) {
                                    case LOAD:
                                        viewer.refresh(getViewerObject(event.getNode()));
                                        expandNodeOnLoad(event.getNode());
                                        break;
                                    case UNLOAD:
                                        viewer.collapseToLevel(event.getNode(), -1);
                                        viewer.refresh(getViewerObject(event.getNode()));
                                        break;
                                    case REFRESH:
                                        viewer.update(getViewerObject(event.getNode()), null);
                                        break;
                                    case LOCK:
                                    case UNLOCK:
                                    case STRUCT_REFRESH:
                                        viewer.refresh(getViewerObject(event.getNode()));
                                        break;
                                }
                            } else {
                                log.warn("Null node object");
                            }
                        }
                    }
                });
                break;
            default:
                break;
        }
    }

    private void expandNodeOnLoad(final DBNNode node)
    {
        if (node instanceof DBNDataSource && DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT)) {
            try {
                DBRRunnableWithResult<DBNNode> runnable = new DBRRunnableWithResult<DBNNode>() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try {
                            result = finaActiveNode(monitor, node);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                };
                DBeaverUI.runInProgressService(runnable);
                if (runnable.getResult() != null) {
                    showNode(runnable.getResult());
                    viewer.expandToLevel(runnable.getResult(), 1);
                }
            } catch (InvocationTargetException e) {
                log.error("Can't expand node", e.getTargetException());
            } catch (InterruptedException e) {
                // skip it
            }
        }
    }

    private DBNNode finaActiveNode(DBRProgressMonitor monitor, DBNNode node) throws DBException
    {
        List<? extends DBNNode> children = node.getChildren(monitor);
        if (!CommonUtils.isEmpty(children)) {
            if (children.get(0) instanceof DBNContainer) {
                // Use only first folder to search
                return finaActiveNode(monitor, children.get(0));
            }
            for (DBNNode child : children) {
                if (NavigatorUtils.isDefaultElement(child)) {
                    return child;
                }
            }
        }

        return node;
    }

    Object getViewerObject(DBNNode node)
    {
        if (((DatabaseNavigatorContent) viewer.getInput()).getRootNode() == node) {
            return viewer.getInput();
        } else {
            return node;
        }
    }

    public void showNode(DBNNode node) {
        viewer.reveal(node);
        viewer.setSelection(new StructuredSelection(node));
    }

    public void reloadTree(final DBNNode rootNode)
    {
        DatabaseNavigatorTree.this.viewer.setInput(new DatabaseNavigatorContent(rootNode));
    }

    private class TreeSelectionAdapter implements MouseListener {

        private volatile TreeItem curSelection;
        private volatile RenameJob renameJob = new RenameJob();

        private volatile boolean doubleClick = false;

        @Override
        public synchronized void mouseDoubleClick(MouseEvent e)
        {
            curSelection = null;
            renameJob.canceled = true;
        }

        @Override
        public void mouseDown(MouseEvent e)
        {
        }

        @Override
        public void mouseUp(MouseEvent e)
        {
            if ((e.stateMask & SWT.BUTTON1) == 0) {
                curSelection = null;
                return;
            }
            changeSelection(e);
        }

        public void changeSelection(MouseEvent e) {
            disposeOldEditor();
            final TreeItem newSelection = viewer.getTree().getItem(new Point(e.x, e.y));
            if (newSelection == null) {
                //curSelection = null;
                return;
            }

            if (!(newSelection.getData() instanceof DBNNode) ||
                !(ActionUtils.isCommandEnabled(IWorkbenchCommandConstants.FILE_RENAME, DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart()))) {
                curSelection = null;
                return;
            }
            if (curSelection != null && curSelection == newSelection) {
                renameJob.schedule(1000);
            }
            curSelection = newSelection;
        }

        private class RenameJob extends AbstractUIJob {
            private volatile boolean canceled = false;
            public RenameJob()
            {
                super("Rename ");
            }

            @Override
            protected IStatus runInUIThread(DBRProgressMonitor monitor)
            {
                try {
                    if (!viewer.getTree().isDisposed() && viewer.getTree().isFocusControl() && curSelection != null && !canceled) {
                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run()
                            {
                                if (curSelection != null) {
                                    renameItem(curSelection);
                                }
                            }
                        });
                    }
                } finally {
                    canceled = false;
                }
                return Status.OK_STATUS;
            }

        }
    }

    private void renameItem(final TreeItem item)
    {
        // Clean up any previous editor control
        disposeOldEditor();
        if (item.isDisposed()) {
            return;
        }
        final DBNNode node = (DBNNode) item.getData();

        Text text = new Text(viewer.getTree(), SWT.BORDER);
        text.setText(node.getNodeName());
        text.selectAll();
        text.setFocus();
        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
            {
                disposeOldEditor();
            }
        });
        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR) {
                    Text text = (Text) treeEditor.getEditor();
                    final String newName = text.getText();
                    disposeOldEditor();
                    viewer.getTree().setFocus();
                    if (!CommonUtils.isEmpty(newName) && !newName.equals(node.getNodeName())) {
                        NavigatorHandlerObjectRename.renameNode(DBeaverUI.getActiveWorkbenchWindow(), node, newName);
                    }
                } else if (e.keyCode == SWT.ESC) {
                    disposeOldEditor();
                    viewer.getTree().setFocus();
                }
            }
        });
        final Rectangle itemBounds = item.getBounds(0);
        final Rectangle treeBounds = viewer.getTree().getBounds();
        treeEditor.minimumWidth = Math.max(itemBounds.width, 50);
        treeEditor.minimumWidth = Math.min(treeEditor.minimumWidth, treeBounds.width - (itemBounds.x - treeBounds.x) - item.getImageBounds(0).width - 4);

        treeEditor.setEditor(text, item, 0);
    }

    private void disposeOldEditor()
    {
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

}
