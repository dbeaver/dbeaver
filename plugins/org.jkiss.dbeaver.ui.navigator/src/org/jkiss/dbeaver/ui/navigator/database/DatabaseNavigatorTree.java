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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.progress.WorkbenchJob;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.registry.RuntimeProjectPropertiesConstant;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressPainter;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.INavigatorItemRenderer;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectRename;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DatabaseNavigatorTree extends Composite implements INavigatorListener {

    private static final Log log = Log.getLog(DatabaseNavigatorTree.class);

    static final String TREE_DATA_STAT_MAX_SIZE = "nav.stat.maxSize";
    private static final String FILTER_TOOLBAR_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.navigator.filter.toolbar"; //$NON-NLS-1$
    private static final String DATA_TREE_CONTROL = DatabaseNavigatorTree.class.getSimpleName();
    private static final boolean INLINE_RENAME_ENABLED = false;

    private final TreeViewer treeViewer;
    private DBNModel model;
    private TreeEditor treeEditor;
    private boolean checkEnabled;
    private INavigatorFilter navigatorFilter;
    private Text filterControl;
    private INavigatorItemRenderer itemRenderer;

    private boolean filterShowConnected = false;
    private String filterPlaceholderText = UINavigatorMessages.actions_navigator_search_tip;
    private DatabaseNavigatorTreeFilterObjectType filterObjectType = DatabaseNavigatorTreeFilterObjectType.connection;
    private volatile ProgressPainter treeLoadingListener;

    // It is static to share loading nodes between all tree controls
    private static final Set<DBNNode> nodeInLoadingProcess = new HashSet<>();

    public static DatabaseNavigatorTree getFromShell(Display display) {
        if (display == null) {
            return null;
        }
        Control focusControl = display.getFocusControl();
        if (focusControl == null) {
            return null;
        }
        return getFromShell(focusControl.getShell());
    }

    public static DatabaseNavigatorTree getFromShell(Shell shell) {
        return (DatabaseNavigatorTree) shell.getData(DATA_TREE_CONTROL);
    }

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style) {
        this(parent, rootNode, style, false);
    }

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style, boolean showRoot) {
        this(parent, rootNode, style, showRoot, null, null);
    }

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style, boolean showRoot, INavigatorFilter navigatorFilter) {
        this(parent, rootNode, style, showRoot, navigatorFilter, null);
    }

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style, boolean showRoot, INavigatorFilter navigatorFilter, String filterPlaceholderText) {
        super(parent, SWT.NONE);

        if (UIUtils.isInDialog(parent)) {
            parent.getShell().setData(DATA_TREE_CONTROL, DatabaseNavigatorTree.this);
        }

        this.setLayout(new FillLayout());
        this.navigatorFilter = navigatorFilter;
        if (filterPlaceholderText != null) {
            this.filterPlaceholderText = filterPlaceholderText;
        }
        this.model = DBWorkbench.getPlatform().getNavigatorModel();
        assert this.model != null;
        this.model.addListener(this);
        addDisposeListener(e -> {
            if (model != null) {
                model.removeListener(DatabaseNavigatorTree.this);
                model = null;
            }
        });

        treeViewer = doCreateTreeViewer(this, style);

        Tree tree = treeViewer.getTree();
        tree.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
        treeViewer.setUseHashlookup(true);

        if (rootNode == null) {
            treeLoadingListener = new ProgressPainter(tree);
        }

        DatabaseNavigatorLabelProvider labelProvider = createLabelProvider(this);
        treeViewer.setLabelProvider(labelProvider);
        treeViewer.setContentProvider(createContentProvider(showRoot));

        if (rootNode != null) {
            setInput(rootNode);
        }

        new DatabaseNavigatorToolTipSupport(this);

        initEditor();

        this.setItemRenderer(new DefaultNavigatorNodeRenderer());

        {
            tree.addListener(SWT.PaintItem, event -> onPaintItem(tree, event));
            tree.getHorizontalBar().addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> tree.redraw()));
            if (false) {
                // See comments for StatisticsNavigatorNodeRenderer.PAINT_ACTION_HOVER
                Listener mouseListener = e -> {
                    TreeItem item = tree.getItem(new Point(e.x, e.y));
                    if (item != null) {
                        Rectangle itemBounds = item.getBounds();
                        Point treeSize = tree.getSize();
                        tree.redraw(itemBounds.x, itemBounds.y, treeSize.x, treeSize.y, false);
                    }
                };

                tree.addListener(SWT.MouseMove, mouseListener);
                //tree.addListener(SWT.MouseHover, mouseListener);
                tree.addListener(SWT.MouseEnter, mouseListener);
                tree.addListener(SWT.MouseExit, mouseListener);
            }
            {
                Listener mouseListener = e -> {
                    TreeItem item = tree.getItem(new Point(e.x, e.y));
                    if (item != null) {
                        Object element = item.getData();
                        if (element instanceof DBNNode node) {
                            Cursor cursor = itemRenderer.getCursor(node, tree, e);
                            if (tree.getCursor() != cursor) {
                                tree.setCursor(cursor);
                            }
                        }
                    }
                };
                tree.addListener(SWT.MouseHover, mouseListener);
                tree.addListener(SWT.MouseMove, mouseListener);
                tree.addListener(SWT.MouseEnter, mouseListener);
                tree.addListener(SWT.MouseExit, mouseListener);
            }
            tree.addListener(SWT.MouseDown, event -> onItemMouseDown(tree, event, false));
            tree.addListener(SWT.MouseDoubleClick, event -> onItemMouseDown(tree, event, true));
            LinuxKeyboardArrowsListener.installOn(tree);
        }

        new NodeLoadersPainter().schedule();
    }

    @NotNull
    protected DatabaseNavigatorContentProvider createContentProvider(boolean showRoot) {
        return new DatabaseNavigatorContentProvider(this, showRoot);
    }

    @NotNull
    protected DatabaseNavigatorLabelProvider createLabelProvider(DatabaseNavigatorTree tree) {
        return new DatabaseNavigatorLabelProvider(tree);
    }

    public boolean isFilterShowConnected() {
        return filterShowConnected;
    }

    public void setFilterShowConnected(boolean filterShowConnected) {
        this.filterShowConnected = filterShowConnected;
    }

    public DatabaseNavigatorTreeFilterObjectType getFilterObjectType() {
        return filterObjectType;
    }

    public void setFilterObjectType(DatabaseNavigatorTreeFilterObjectType filterObjectType) {
        this.filterObjectType = filterObjectType;
    }

    public ILabelDecorator getLabelDecorator() {
        return ((DatabaseNavigatorLabelProvider) treeViewer.getLabelProvider()).getLabelDecorator();
    }

    public void setLabelDecorator(ILabelDecorator labelDecorator) {
        ((DatabaseNavigatorLabelProvider) treeViewer.getLabelProvider()).setLabelDecorator(labelDecorator);
    }

    INavigatorItemRenderer getItemRenderer() {
        return itemRenderer;
    }

    void setItemRenderer(INavigatorItemRenderer itemRenderer) {
        this.itemRenderer = itemRenderer;
    }

    private void onPaintItem(Tree tree, Event event) {
        if (itemRenderer != null) {
            Object element = event.item.getData();
            if (element instanceof DBNNode node) {
                itemRenderer.paintNodeDetails(node, tree, event.gc, event);
            }
        }
    }

    private void onItemMouseDown(Tree tree, Event event, boolean defaultAction) {
        if (itemRenderer != null && event.button == 1) {
            TreeItem item = tree.getItem(new Point(event.x, event.y));
            if (item != null) {
                Object element = item.getData();
                if (element instanceof DBNNode node) {
                    itemRenderer.performAction(node, tree, event, defaultAction);
                }
            }
        }
    }

    public void setInput(DBNNode rootNode) {
        if (treeLoadingListener != null) {
            treeLoadingListener.close();
            treeLoadingListener = null;
        }
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
            if (navigatorFilter != null) {
                CustomFilteredTree filteredTree = new CustomFilteredTree(treeStyle) {
                    @Override
                    protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
                        return new CheckboxTreeViewer(parent, treeStyle);
                    }
                };
                filterControl = filteredTree.getFilterControl();
                return filteredTree.getViewer();

/*
                checkboxTreeViewer.addFilter(new ViewerFilter() {
                    @Override
                    public boolean select(Viewer viewer, Object parentElement, Object element) {
                        return navigatorFilter.select(element);
                    }
                });
*/
            } else {
                return new CheckboxTreeViewer(parent, treeStyle);
            }
        } else {
            if (navigatorFilter != null) {
                CustomFilteredTree filteredTree = new CustomFilteredTree(treeStyle);
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
                if (rootNode instanceof DatabaseNavigatorContent dnc) {
                    rootNode = dnc.getRootNode();
                }
                return rootNode == null ? new TreeSelection() : new TreeSelection(new TreePath(new Object[]{rootNode}));
            }

            protected void handleTreeExpand(TreeEvent event) {
                // Disable redraw during expand (its blinking)
                getTree().setRedraw(false);
                try {
                    if (event.item != null && event.item.getData() instanceof DBNProject dbnProject) {
                        //manual opening
                        dbnProject.getProject().setRuntimeProperty(RuntimeProjectPropertiesConstant.IS_USER_DECLINE_PROJECT_DECRYPTION,
                            Boolean.FALSE.toString());
                    }
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

    public DBNNode getModel() {
        DatabaseNavigatorContent content = (DatabaseNavigatorContent) this.treeViewer.getInput();
        return content.getRootNode();
    }

    private void initEditor() {
        if (INLINE_RENAME_ENABLED) {
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
    public TreeViewer getViewer() {
        return treeViewer;
    }

    @NotNull
    public CheckboxTreeViewer getCheckboxViewer() {
        return (CheckboxTreeViewer) treeViewer;
    }

    @Override
    public void nodeChanged(final DBNEvent event) {
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
                    switch (event.getNodeChange()) {
                        case BEFORE_LOAD:
                            startNodeLoadingVisualization(event.getNode());
                            break;
                        case AFTER_LOAD:
                            stopNodeLoadingVisualization(event.getNode());
                            break;
                        case LOAD:
                            treeViewer.refresh(getViewerObject(event.getNode()));
                            expandNodeOnLoad(event.getNode());
                            break;
                        case UNLOAD:
                            stopNodeLoadingVisualization(event.getNode());
                            treeViewer.collapseToLevel(event.getNode(), -1);
                            treeViewer.update(getViewerObject(event.getNode()), null);
                            treeViewer.collapseToLevel(event.getNode(), -1);
                            break;
                        case REFRESH:
//                                Widget item = treeViewer.testFindItem(event.getNode());
//                                if (item != null) {
//                                    item.setData(TREE_DATA_STAT_MAX_SIZE, null);
//                                }
                            treeViewer.refresh(getViewerObject(event.getNode()), true);
                            break;
                        case LOCK:
                        case UNLOCK:
                        case STRUCT_REFRESH:
                            treeViewer.refresh(getViewerObject(event.getNode()));
                            break;
                    }
                }
                break;
            default:
                break;
        }
    }

    private void startNodeLoadingVisualization(DBNNode node) {
        synchronized (nodeInLoadingProcess) {
            nodeInLoadingProcess.add(node);
        }
    }

    private void stopNodeLoadingVisualization(DBNNode node) {
        synchronized (nodeInLoadingProcess) {
            nodeInLoadingProcess.remove(node);
        }
    }

    private void expandNodeOnLoad(final DBNNode node) {
        if (node instanceof DBNDataSource && DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_EXPAND_ON_CONNECT)) {
            DBRRunnableWithResult<DBNNode> runnable = new DBRRunnableWithResult<>() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
                    try {
                        result = findActiveNode(monitor, node);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };
            // Run task with timeout. Don't use UI service to avoid UI interactions (see #10479)
            RuntimeUtils.runTask(runnable, "Find active node", 2000);
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
        }
    }

    @NotNull
    private DBNNode findActiveNode(@NotNull DBRProgressMonitor monitor, @NotNull DBNNode node) throws DBException {
        return findActiveNode(monitor, node, node);
    }

    @NotNull
    private DBNNode findActiveNode(@NotNull DBRProgressMonitor monitor, @NotNull DBNNode parent, @NotNull DBNNode node) throws DBException {
        DBNNode[] children = node.getChildren(monitor);
        if (!ArrayUtils.isEmpty(children)) {
            if (children[0] instanceof DBNContainer) {
                // Use only first folder to search
                return findActiveNode(monitor, node, children[0]);
            }
            for (DBNNode child : children) {
                if (DBNUtils.isDefaultElement(child)) {
                    // Find the deepest default element (either catalog or schema)
                    return findActiveNode(monitor, node, child);
                }
            }
        }

        return parent;
    }

    private Object getViewerObject(DBNNode node) {
        Object input = treeViewer.getInput();
        if (input instanceof DatabaseNavigatorContent dnc && dnc.getRootNode() == node) {
            return input;
        } else {
            return node;
        }
    }

    void showNode(DBNNode node) {
        treeViewer.reveal(node);
        treeViewer.setSelection(new StructuredSelection(node));
    }

    public void reloadTree(final DBNNode rootNode) {
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

            TreeItem item = (TreeItem) event.item;
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

    public static final Image[] IMG_LOADING = new Image[]{
        DBeaverIcons.getImage(UIIcon.LOADING0),
        DBeaverIcons.getImage(UIIcon.LOADING1),
        DBeaverIcons.getImage(UIIcon.LOADING2),
        DBeaverIcons.getImage(UIIcon.LOADING3),
        DBeaverIcons.getImage(UIIcon.LOADING4),
        DBeaverIcons.getImage(UIIcon.LOADING5),
        DBeaverIcons.getImage(UIIcon.LOADING6),
        DBeaverIcons.getImage(UIIcon.LOADING7)
    };

    private class NodeLoadersPainter extends UIJob {
        private static final long REPAINT_DELAY = 100;
        private static final long WAIT_DELAY = 500;
        private static final Image[] LOADING_ICONS = IMG_LOADING;

        private int ticksCount = 0;

        public NodeLoadersPainter() {
            super("NavigatorTreeLoadersPainterJob");
            setSystem(true);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            synchronized (nodeInLoadingProcess) {
                long nextDelay = WAIT_DELAY;
                if (!nodeInLoadingProcess.isEmpty()) {
                    ticksCount++;
                    for (DBNNode node : nodeInLoadingProcess) {
                        Widget widget = treeViewer.testFindItem(node);
                        if (widget instanceof TreeItem treeItem) {
                            treeItem.setImage(getCurrentImage());
                            nextDelay = REPAINT_DELAY;
                        }
                    }
                } else {
                    ticksCount = 0;
                }
                if (!treeViewer.getTree().isDisposed()) {
                    schedule(nextDelay);
                }
            }
            return Status.OK_STATUS;
        }

        private Image getCurrentImage() {
            int imgIndex = (ticksCount % LOADING_ICONS.length);
            return LOADING_ICONS[imgIndex];
        }

    }

    private class TreeSelectionAdapter implements MouseListener {

        private volatile TreeItem curSelection;
        private volatile RenameJob renameJob;

        @Override
        public synchronized void mouseDoubleClick(MouseEvent e) {
            curSelection = null;
            if (renameJob != null) {
                renameJob.canceled = true;
            }
        }

        @Override
        public void mouseDown(MouseEvent e) {
        }

        @Override
        public void mouseUp(MouseEvent e) {
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
                } else {
                    renameJob.cancel();
                }
                renameJob.selection = curSelection;
                renameJob.schedule(1000);
            }
            curSelection = newSelection;
        }

        private class RenameJob extends AbstractUIJob {
            private volatile boolean canceled = false;
            public TreeItem selection;

            RenameJob() {
                super("Rename ");
            }

            @Override
            protected IStatus runInUIThread(DBRProgressMonitor monitor) {
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

    private void renameItem(final TreeItem item) {
        // Clean up any previous editor control
        disposeOldEditor();
        if (item.isDisposed()) {
            return;
        }
        final DBNNode node = (DBNNode) item.getData();

        Text text = new Text(treeViewer.getTree(), SWT.BORDER);
        text.setText(node.getNodeDisplayName());
        text.selectAll();
        text.setFocus();
        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                disposeOldEditor();
            }
        });
        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR) {
                    Text text = (Text) treeEditor.getEditor();
                    final String newName = text.getText();
                    disposeOldEditor();
                    treeViewer.getTree().setFocus();
                    if (!CommonUtils.isEmpty(newName) && !newName.equals(node.getNodeDisplayName())) {
                        NavigatorHandlerObjectRename.renameNode(
                            UIUtils.getActiveWorkbenchWindow(),
                            treeViewer.getControl().getShell(),
                            node,
                            newName,
                            this);
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

    private void disposeOldEditor() {
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Filtered tree

    private class TreeFilter extends PatternFilter {
        private final INavigatorFilter filter;
        private boolean hasPattern = false;
        private TextMatcherExt matcher;
        private TextMatcherExt matcherShort;
        private String[] dotPattern;

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
            this.dotPattern = null;
            if (patternString != null) {
                String pattern = patternString;
                if (!patternString.endsWith(" ")) {
                    pattern = patternString + "*";
                }
                pattern = "*" + pattern;
                this.matcher = new TextMatcherExt(pattern, true, false);
                this.dotPattern = patternString.split("\\.");
                if (dotPattern.length == 2) {
                    String patternShort = dotPattern[1];
                    if (!patternShort.endsWith(" ")) {
                        patternShort = patternShort + "*";
                    }
                    patternShort = "*" + patternShort;
                    this.matcherShort = new TextMatcherExt(patternShort, true, false);
                } else {
                    this.dotPattern = null;
                }
            } else {
                super.setPattern(null);
            }
        }

        @Override
        protected boolean wordMatches(String text) {
            if (text == null) {
                return false;
            }
            if (matcher != null) {
                return matcher.match(text);
            }
            return super.wordMatches(text);
        }

        public boolean isElementVisible(Viewer viewer, Object element) {
            if (filterShowConnected && element instanceof DBNDataSource dataSource && !dataSource.getDataSourceContainer().isConnected()) {
                return false;
            }
            if ((filterShowConnected ||
                (hasPattern && getFilterObjectType() == DatabaseNavigatorTreeFilterObjectType.connection) ||
                (hasPattern && filter.filterFolders())) && element instanceof DBNLocalFolder
            ) {
                return hasVisibleConnections(viewer, (DBNLocalFolder) element);
            }
            if (!filter.select(element)) {
                return false;
            }

            boolean needToMatch = filter.filterObjectByPattern(element);
            if (!needToMatch && element instanceof DBNDatabaseNode node) {
                DBSObject object = node.getObject();
                switch (filterObjectType) {
                    case connection -> needToMatch = (object instanceof DBPDataSourceContainer);
                    case container -> {
                        needToMatch = object instanceof DBSSchema || object instanceof DBSCatalog;
                        if (needToMatch) {
                            try {
                                Class<? extends DBSObject> primaryChildType = ((DBSStructContainer) object).getPrimaryChildType(null);
                                needToMatch = !DBSStructContainer.class.isAssignableFrom(primaryChildType);
                            } catch (Exception e) {
                                log.debug(e);
                            }
                        }
                    }
                    default -> needToMatch = !(object instanceof DBPDataSourceContainer) &&
                                             !(object instanceof DBSSchema) &&
                                             !(object instanceof DBSCatalog) &&
                                             !(object instanceof DBNDatabaseFolder) &&
                                             !(object instanceof DBSTableColumn);
                }
            }
            if (!needToMatch) {
                return true;
            }
            String labelText = ((ILabelProvider) ((ContentViewer) viewer).getLabelProvider()).getText(element);
            if (labelText == null) {
                return false;
            }
            return isPatternMatched(labelText, element);
        }

        private boolean isPatternMatched(String labelText, Object element) {
            boolean patternMatched = wordMatches(labelText);
            if (!patternMatched) { // pattern is not matched - so we'll check, maybe format is schema.object
                if (dotPattern != null) {
                    Object item = null;
                    if (element instanceof DBNDatabaseItem di) {
                        item = di.getParentNode();
                    }
                    boolean schemaMatched = false;
                    while (item != null) {
                        if (item instanceof DBNDatabaseFolder df) {
                            item = df.getParentNode();
                        } else if (item instanceof DBNDatabaseItem di) {
                            DBSObject obj = di.getObject();
                            if (obj instanceof DBSStructContainer) {
                                String name = obj.getName();
                                if (name != null) {
                                    schemaMatched = name.equalsIgnoreCase(dotPattern[0]);
                                }
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (schemaMatched) {
                        return matcherShort.match(labelText);
                    }
                    return false;
                }
            }
            if (!patternMatched) { // Analyze description too
                if (element instanceof DBNDatabaseItem item) {
                    DBSObject obj = item.getObject();
                    labelText = obj == null ? null : obj.getDescription();
                    patternMatched = wordMatches(labelText);
                }
            }
            return patternMatched;
        }

        private boolean hasVisibleConnections(Viewer viewer, DBNLocalFolder folder) {
            DBNNode[] children = folder.getChildren(new VoidProgressMonitor());
            if (children == null) {
                return false;
            }
            for (DBNNode child : children) {
                if (child instanceof DBNLocalFolder lf) {
                    if (hasVisibleConnections(viewer, lf)) {
                        return true;
                    }
                } else if (isLeafMatch(viewer, child)) {
                    if (filterShowConnected && child instanceof DBNDataSource && !((DBNDataSource) child).getDataSourceContainer().isConnected()) {
                        continue;
                    }

                    return true;
                }
            }
            return false;
        }

    }

    private class CustomFilteredTree extends FilteredTree {

        CustomFilteredTree(int treeStyle) {
            super(DatabaseNavigatorTree.this, treeStyle, new TreeFilter(DatabaseNavigatorTree.this.navigatorFilter), true);
            try {
                if (treeViewer != null) {
                    treeViewer.setUseHashlookup(true);
                }
            } catch (Throwable e) {
                // May happen in old Eclipse versions
            }

            setInitialText(getFilterPlaceholderText());
            ((GridLayout) getLayout()).verticalSpacing = 0;

            UIUtils.addDefaultEditActionsSupport(UIUtils.getActiveWorkbenchWindow(), getFilterControl());
        }

        @Override
        protected Composite createFilterControls(Composite parent) {
            super.createFilterControls(parent);

            if (navigatorFilter instanceof DatabaseNavigatorTreeFilter) {
                ((GridLayout) parent.getLayout()).numColumns++;

                IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();

                ToolBarManager filterManager = new ToolBarManager();
                filterManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
                final IMenuService menuService = workbenchWindow.getService(IMenuService.class);
                if (menuService != null) {
                    menuService.populateContributionManager(filterManager, FILTER_TOOLBAR_CONTRIBUTION_ID);
                }

                filterManager.createControl(parent);

                parent.addDisposeListener(e -> filterManager.dispose());
            }

            return parent;
        }

        protected Text doCreateFilterText(Composite parent) {
            return new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
        }

        @Override
        protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
            return ((DatabaseNavigatorTree) getParent()).doCreateNavigatorTreeViewer(parent, style);
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

                        if (!text.isEmpty() && !initial) {
                            // enabled toolbar - there is text to clear
                            // and the list is currently being filtered
                            updateToolbar(true);
                        } else {
                            // disabled toolbar - there is no text to clear
                            // and the list is currently not filtered
                            updateToolbar(false);
                        }
                        ((DatabaseNavigatorTree) getParent()).onTreeRefresh();
                    } finally {
                        // done updating the tree - set redraw back to true
                        redrawFalseControl.setRedraw(true);
                    }

                    return Status.OK_STATUS;
                }
            };
        }
    }

    protected String getFilterPlaceholderText() {
        return filterPlaceholderText;
    }

    // Called by filtering job
    protected void onTreeRefresh() {

    }

}
