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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.AbstractUIJob;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DefaultViewerToolTipSupport;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectRename;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class DatabaseNavigatorTree extends Composite implements INavigatorListener
{
    private static final Log log = Log.getLog(DatabaseNavigatorTree.class);

    private TreeViewer treeViewer;
    private DBNModel model;
    private TreeEditor treeEditor;
    private boolean checkEnabled;
    private INavigatorFilter navigatorFilter;
    private Text filterControl;
    private boolean inlineRenameEnabled = false;
    private DatabaseNavigatorItemRenderer itemRenderer;

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style)
    {
        this(parent, rootNode, style, false);
    }

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style, boolean showRoot) {
        this(parent, rootNode, style, showRoot, null);
    }

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style, boolean showRoot, INavigatorFilter navigatorFilter)
    {
        super(parent, SWT.NONE);
        this.setLayout(new FillLayout());
        this.navigatorFilter = navigatorFilter;
        this.model = DBWorkbench.getPlatform().getNavigatorModel();
        this.model.addListener(this);
        addDisposeListener(e -> {
            if (model != null) {
                model.removeListener(DatabaseNavigatorTree.this);
                model = null;
            }
        });

        treeViewer = doCreateTreeViewer(this, style);
//        treeViewer.getTree().addFocusListener(new FocusAdapter() {
//            @Override
//            public void focusGained(FocusEvent e) {
//                super.focusGained(e);
//            }
//        });

        Tree tree = treeViewer.getTree();
        tree.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
        treeViewer.setUseHashlookup(true);

        DatabaseNavigatorLabelProvider labelProvider = new DatabaseNavigatorLabelProvider(treeViewer);
        treeViewer.setLabelProvider(labelProvider);
        treeViewer.setContentProvider(new DatabaseNavigatorContentProvider(this, showRoot));

        if (false) {
            // We don't need it
            tree.addListener(SWT.PaintItem, new TreeBackgroundColorPainter(labelProvider));
        }

        if (rootNode != null) {
            setInput(rootNode);
        }

        new DefaultViewerToolTipSupport(treeViewer);

        initEditor();

        {
            tree.addListener(SWT.PaintItem, event -> paintItem(tree, event));
        }
    }

    public void setItemRenderer(DatabaseNavigatorItemRenderer itemRenderer) {
        this.itemRenderer = itemRenderer;
    }

    private void paintItem(Tree tree, Event event) {
        if (itemRenderer != null) {
            Object element = event.item.getData();
            if (element instanceof DBNNode) {
                itemRenderer.paintNodeDetails((DBNNode) element, tree, event.gc, event);
            }
        }
    }

    public void setInput(DBNNode rootNode) {
        treeViewer.setInput(new DatabaseNavigatorContent(rootNode));
    }

    public INavigatorFilter getNavigatorFilter() {
        return navigatorFilter;
    }

    public void setNavigatorFilter(INavigatorFilter navigatorFilter) {
        this.navigatorFilter = navigatorFilter;
        if (treeViewer != null) {
            treeViewer.addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    return navigatorFilter.select(element);
                }
            });
        }
    }

    @Nullable
    public Text getFilterControl() {
        return filterControl;
    }

    private TreeViewer doCreateTreeViewer(Composite parent, int style) {
        checkEnabled = (style & SWT.CHECK) != 0;

        // Create tree
        int treeStyle = SWT.H_SCROLL | SWT.V_SCROLL | style;
        if (checkEnabled) {
            CheckboxTreeViewer checkboxTreeViewer = new CheckboxTreeViewer(parent, treeStyle);
            if (navigatorFilter != null) {
                checkboxTreeViewer.addFilter(new ViewerFilter() {
                    @Override
                    public boolean select(Viewer viewer, Object parentElement, Object element) {
                        return navigatorFilter.select(element);
                    }
                });
            }
            return checkboxTreeViewer;
        } else {
            if (navigatorFilter != null) {
                CustomFilteredTree filteredTree = new CustomFilteredTree(this, treeStyle);
                filterControl = filteredTree.getFilterControl();
                return filteredTree.getViewer();
            } else {
                return doCreateNavigatorTreeViewer(parent, style);
            }
        }
    }

    private TreeViewer doCreateNavigatorTreeViewer(Composite parent, int style) {
        return new TreeViewer(parent, style) {
            @Override
            public ISelection getSelection() {
                ISelection selection = super.getSelection();
                if (!selection.isEmpty()) {
                    return selection;
                }
                Object rootNode = getInput();
                if (rootNode instanceof DatabaseNavigatorContent) {
                    rootNode = ((DatabaseNavigatorContent) rootNode).getRootNode();
                }
                return rootNode == null ? new TreeSelection() : new TreeSelection(new TreePath(new Object[] { rootNode } ));
            }
            protected void handleTreeExpand(TreeEvent event) {
                // Disable redraw during expand (its blinking)
                getTree().setRedraw(false);
                try {
                    super.handleTreeExpand(event);
                } finally {
                    getTree().setRedraw(true);
                }
            }
            protected void handleTreeCollapse(TreeEvent event) {
                getTree().setRedraw(false);
                try {
                    super.handleTreeCollapse(event);
                } finally {
                    getTree().setRedraw(true);
                }
            }
        };
    }

    public DBNNode getModel()
    {
        DatabaseNavigatorContent content = (DatabaseNavigatorContent) this.treeViewer.getInput();
        return content.getRootNode();
    }

    private void initEditor()
    {
        if (inlineRenameEnabled) {
            Tree treeControl = this.treeViewer.getTree();

            treeEditor = new TreeEditor(treeControl);
            treeEditor.horizontalAlignment = SWT.LEFT;
            treeEditor.verticalAlignment = SWT.TOP;
            treeEditor.grabHorizontal = false;
            treeEditor.minimumWidth = 50;

            if (!checkEnabled) {
                // Add rename listener only for non CHECK trees
                treeControl.addMouseListener(new TreeSelectionAdapter());
            }
        }
    }

    @NotNull
    public TreeViewer getViewer()
    {
        return treeViewer;
    }

    @NotNull
    public CheckboxTreeViewer getCheckboxViewer() {
        return (CheckboxTreeViewer) treeViewer;
    }

    @Override
    public void nodeChanged(final DBNEvent event)
    {
        switch (event.getAction()) {
            case ADD:
            case REMOVE: {
                final DBNNode node = event.getNode();
                final DBNNode parentNode = node.getParentNode();
                if (parentNode != null) {
                    if (!treeViewer.getControl().isDisposed()) {
                        if (!parentNode.isDisposed()) {
                            treeViewer.refresh(getViewerObject(parentNode));
                            if (event.getNodeChange() == DBNEvent.NodeChange.SELECT) {
                                treeViewer.reveal(node);
                                treeViewer.setSelection(new StructuredSelection(node));
                            }
                        }
                    }
                }
                break;
            }
            case UPDATE:
                if (!treeViewer.getControl().isDisposed() && !treeViewer.isBusy()) {
                    if (event.getNode() != null) {
                        switch (event.getNodeChange()) {
                            case LOAD:
                                treeViewer.refresh(getViewerObject(event.getNode()));
                                expandNodeOnLoad(event.getNode());
                                break;
                            case UNLOAD:
                                treeViewer.collapseToLevel(event.getNode(), -1);
                                treeViewer.update(getViewerObject(event.getNode()), null);
                                treeViewer.collapseToLevel(event.getNode(), -1);
                                break;
                            case REFRESH:
                                treeViewer.refresh(getViewerObject(event.getNode()), true);
                                break;
                            case LOCK:
                            case UNLOCK:
                            case STRUCT_REFRESH:
                                treeViewer.refresh(getViewerObject(event.getNode()));
                                break;
                        }
                    } else {
                        log.warn("Null node object");
                    }
                }
                break;
            default:
                break;
        }
    }

    private void expandNodeOnLoad(final DBNNode node)
    {
        if (node instanceof DBNDataSource && DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_EXPAND_ON_CONNECT)) {
            try {
                DBRRunnableWithResult<DBNNode> runnable = new DBRRunnableWithResult<DBNNode>() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
                        try {
                            result = findActiveNode(monitor, node);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                };
                UIUtils.runInProgressService(runnable);
                if (runnable.getResult() != null && !treeViewer.getTree().isDisposed()) {
                    showNode(runnable.getResult());
                    treeViewer.expandToLevel(runnable.getResult(), 1);
/*
                    // TODO: it is a bug in Eclipse Photon.
                    try {
                        treeViewer.expandToLevel(runnable.getResult(), 1, true);
                    } catch (Throwable e) {
                        treeViewer.expandToLevel(runnable.getResult(), 1);
                    }
*/
                }
            } catch (InvocationTargetException e) {
                log.error("Can't expand node", e.getTargetException());
            } catch (InterruptedException e) {
                // skip it
            }
        }
    }

    private DBNNode findActiveNode(DBRProgressMonitor monitor, DBNNode node) throws DBException
    {
        DBNNode[] children = node.getChildren(monitor);
        if (!ArrayUtils.isEmpty(children)) {
            if (children[0] instanceof DBNContainer) {
                // Use only first folder to search
                return findActiveNode(monitor, children[0]);
            }
            for (DBNNode child : children) {
                if (DBNUtils.isDefaultElement(child)) {
                    return child;
                }
            }
        }

        return node;
    }

    private Object getViewerObject(DBNNode node)
    {
        Object input = treeViewer.getInput();
        if (input instanceof DatabaseNavigatorContent && ((DatabaseNavigatorContent) input).getRootNode() == node) {
            return input;
        } else {
            return node;
        }
    }

    void showNode(DBNNode node) {
        treeViewer.reveal(node);
        treeViewer.setSelection(new StructuredSelection(node));
    }

    public void reloadTree(final DBNNode rootNode)
    {
        setInput(rootNode);
    }

    private static class TreeBackgroundColorPainter implements Listener {
        private DatabaseNavigatorLabelProvider labelProvider;
        TreeBackgroundColorPainter(DatabaseNavigatorLabelProvider labelProvider) {
            this.labelProvider = labelProvider;
        }

        public void handleEvent(Event event) {
            if ((event.detail & SWT.SELECTED) == 0 && (event.detail & SWT.HOT) == 0) {
                return; /// item not selected
            }

            TreeItem item = (TreeItem)event.item;
            Color colorBackground = labelProvider.getBackground(item.getData());
            if (colorBackground != null) {
                GC gc = event.gc;
                Color oldBackground = gc.getForeground();

                gc.setForeground(colorBackground);
                gc.drawRoundRectangle(event.x, event.y, event.width, event.height - 1, 3, 3);

                gc.setForeground(oldBackground);
            }
        }
    }

    private class TreeSelectionAdapter implements MouseListener {

        private volatile TreeItem curSelection;
        private volatile RenameJob renameJob;

        @Override
        public synchronized void mouseDoubleClick(MouseEvent e)
        {
            curSelection = null;
            if (renameJob != null) {
                renameJob.canceled = true;
            }
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

        void changeSelection(MouseEvent e) {
            disposeOldEditor();
            final TreeItem newSelection = treeViewer.getTree().getItem(new Point(e.x, e.y));
            if (newSelection == null) {
                return;
            }

            IWorkbenchPart activePart = UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart();
            if (!(newSelection.getData() instanceof DBNNode) ||
                activePart == null || !(ActionUtils.isCommandEnabled(IWorkbenchCommandConstants.FILE_RENAME, activePart.getSite()))) {
                curSelection = null;
                return;
            }
            if (curSelection != null && curSelection == newSelection && (renameJob == null || renameJob.selection == null)) {
                if (renameJob == null) {
                    renameJob = new RenameJob();
                }
                renameJob.selection = curSelection;
                renameJob.schedule(1000);
            }
            curSelection = newSelection;
        }

        private class RenameJob extends AbstractUIJob {
            private volatile boolean canceled = false;
            public TreeItem selection;

            RenameJob()
            {
                super("Rename ");
            }

            @Override
            protected IStatus runInUIThread(DBRProgressMonitor monitor)
            {
                try {
                    if (!treeViewer.getTree().isDisposed() && treeViewer.getTree().isFocusControl() && curSelection == selection && !canceled) {
                        final TreeItem itemToRename = selection;
                        UIUtils.asyncExec(() -> renameItem(itemToRename));
                    }
                } finally {
                    canceled = false;
                    selection = null;
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

        Text text = new Text(treeViewer.getTree(), SWT.BORDER);
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
                    treeViewer.getTree().setFocus();
                    if (!CommonUtils.isEmpty(newName) && !newName.equals(node.getNodeName())) {
                        NavigatorHandlerObjectRename.renameNode(
                            UIUtils.getActiveWorkbenchWindow(),
                            treeViewer.getControl().getShell(),
                            node,
                            newName);
                    }
                } else if (e.keyCode == SWT.ESC) {
                    disposeOldEditor();
                    treeViewer.getTree().setFocus();
                }
            }
        });
        final Rectangle itemBounds = item.getBounds(0);
        final Rectangle treeBounds = treeViewer.getTree().getBounds();
        treeEditor.minimumWidth = Math.max(itemBounds.width, 50);
        treeEditor.minimumWidth = Math.min(treeEditor.minimumWidth, treeBounds.width - (itemBounds.x - treeBounds.x) - item.getImageBounds(0).width - 4);
        treeEditor.minimumHeight = text.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

        treeEditor.setEditor(text, item, 0);
    }

    private void disposeOldEditor()
    {
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Filtered tree

    private static class TreeFilter extends PatternFilter {
        private final INavigatorFilter filter;
        private boolean hasPattern = false;

        TreeFilter(INavigatorFilter filter) {
            setIncludeLeadingWildcard(true);
            this.filter = filter;
        }

        public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
            int size = elements.length;
            ArrayList<Object> out = new ArrayList<>(size);
            for (Object element : elements) {
                if (select(viewer, parent, element)) {
                    out.add(element);
                }
            }
            return out.toArray();
        }

        @Override
        public void setPattern(String patternString) {
            this.hasPattern = !CommonUtils.isEmpty(patternString);
            super.setPattern(patternString);
        }

        public boolean isElementVisible(Viewer viewer, Object element){
            if (hasPattern && filter.filterFolders() && element instanceof DBNLocalFolder) {
                return hasVisibleConnections(viewer, (DBNLocalFolder)element);
            }
            if (filter.select(element)) {
                return true;
            }
            return super.isLeafMatch(viewer, element);
        }

        private boolean hasVisibleConnections(Viewer viewer, DBNLocalFolder folder) {
            DBNNode[] children = folder.getChildren(new VoidProgressMonitor());
            if (children == null) {
                return false;
            }
            for (DBNNode child : children) {
                if (child instanceof DBNLocalFolder) {
                    if (hasVisibleConnections(viewer, (DBNLocalFolder) child)) {
                        return true;
                    }
                } else if (isLeafMatch(viewer, child)) {
                    return true;
                }
            }
            return false;
        }

    }

    private static class CustomFilteredTree extends FilteredTree {
        CustomFilteredTree(DatabaseNavigatorTree navigatorTree, int treeStyle) {
            super(navigatorTree, treeStyle, new TreeFilter(navigatorTree.navigatorFilter), true);
            setInitialText(UINavigatorMessages.actions_navigator_search_tip);
            ((GridLayout)getLayout()).verticalSpacing = 0;

            UIUtils.addDefaultEditActionsSupport(UIUtils.getActiveWorkbenchWindow(), getFilterControl());
        }

        @Override
        protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
            return ((DatabaseNavigatorTree)getParent()).doCreateNavigatorTreeViewer(parent, style);
        }

        protected WorkbenchJob doCreateRefreshJob() {
            return new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    if (treeViewer.getControl().isDisposed()) {
                        return Status.CANCEL_STATUS;
                    }

                    String text = getFilterString();
                    if (text == null) {
                        return Status.OK_STATUS;
                    }
                    boolean initial = initialText != null && initialText.equals(text);
                    if (initial) {
                        getPatternFilter().setPattern(null);
                    } else {
                        getPatternFilter().setPattern(text);
                    }

                    final Control redrawFalseControl = treeComposite != null ? treeComposite
                            : treeViewer.getControl();
                    try {
                        // don't want the user to see updates that will be made to
                        // the tree
                        // we are setting redraw(false) on the composite to avoid
                        // dancing scrollbar
                        redrawFalseControl.setRedraw(false);
                        treeViewer.refresh(true);

                        if (text.length() > 0 && !initial) {
                            // enabled toolbar - there is text to clear
                            // and the list is currently being filtered
                            updateToolbar(true);
                        } else {
                            // disabled toolbar - there is no text to clear
                            // and the list is currently not filtered
                            updateToolbar(false);
                        }
                        ((DatabaseNavigatorTree)getParent()).onTreeRefresh();
                    } finally {
                        // done updating the tree - set redraw back to true
                        redrawFalseControl.setRedraw(true);
                    }

                    return Status.OK_STATUS;
                }
            };
        }
    }

    // Called by filtering job
    protected void onTreeRefresh() {

    }

}
