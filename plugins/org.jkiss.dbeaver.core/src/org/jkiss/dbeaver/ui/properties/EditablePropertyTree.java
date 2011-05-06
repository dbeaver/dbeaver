/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ImageUtils;

import java.text.Collator;
import java.util.*;
import java.util.List;

/**
 * Driver properties control
 */
public class EditablePropertyTree extends Composite {

    private boolean expandSingleRoot = true;
    private TreeViewer propsTree;
    private TreeEditor treeEditor;

    private Font boldFont;
    //private Color colorBlue;
    private Clipboard clipboard;
    private int selectedColumn = -1;
    private CellEditor curCellEditor;

    private String[] customCategories;

    public EditablePropertyTree(Composite parent, int style)
    {
        super(parent, style);

        //colorBlue = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
        clipboard = new Clipboard(getDisplay());

        this.setLayout(new GridLayout(1, false));
        //GridData gd = new GridData(GridData.FILL_BOTH);
        //this.setLayoutData(gd);

        setMarginVisible(true);
        initPropTree();
    }

    public void loadProperties(IPropertySource propertySource)
    {
        loadProperties(null, propertySource);
    }

    protected void loadProperties(TreeNode parent, IPropertySource propertySource)
    {
        // Make tree model
        customCategories = getCustomCategories();

        Map<String, TreeNode> categories = new LinkedHashMap<String, TreeNode>();
        final IPropertyDescriptor[] props = propertySource.getPropertyDescriptors();
        for (IPropertyDescriptor prop : props) {
            String categoryName = prop.getCategory();
            if (CommonUtils.isEmpty(categoryName)) {
                categoryName = "";
            }
            TreeNode category = categories.get(categoryName);
            if (category == null) {
                category = new TreeNode(parent, propertySource, categoryName);
                categories.put(categoryName, category);
            }
            new TreeNode(category, propertySource, prop);
        }
        if (customCategories != null) {
            for (String customCategory : customCategories) {
                TreeNode node = categories.get(customCategory);
                if (node == null) {
                    node = new TreeNode(parent, propertySource, customCategory);
                    categories.put(customCategory, node);
                }
            }
        }
        Object root;
        if (categories.size() == 1 && expandSingleRoot) {
            final Collection<TreeNode> values = categories.values();
            root = values.iterator().next();
        } else {
            root = categories.values();
        }

        if (propsTree != null) {
            propsTree.setInput(root);
            propsTree.expandAll();
            UIUtils.packColumns(propsTree.getTree(), true);
        }
        disposeOldEditor();
    }

    protected void addProperty(Object node, IPropertyDescriptor property)
    {
        if (node instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)node;
            while (treeNode.property != null) {
                treeNode = treeNode.parent;
            }
            final TreeNode newNode = new TreeNode(treeNode, treeNode.propertySource, property);
            handlePropertyCreate(newNode);
        }
    }

    protected void removeProperty(Object node)
    {
        if (node instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)node;
            if (treeNode.propertySource instanceof IPropertySourceEx) {
                ((IPropertySourceEx) treeNode.propertySource).resetPropertyValueToDefault(treeNode.property.getId());
            } else {
                treeNode.propertySource.resetPropertyValue(treeNode.property.getId());
            }
            treeNode.parent.children.remove(treeNode);
            handlePropertyRemove(treeNode);
        }
    }

    public void refresh()
    {
        disposeOldEditor();
        propsTree.refresh();
    }

    private void initPropTree()
    {
        PropsLabelProvider labelProvider = new PropsLabelProvider();

        propsTree = new TreeViewer(this, SWT.BORDER | SWT.FULL_SELECTION);
        propsTree.setContentProvider(new PropsContentProvider());
        propsTree.setLabelProvider(labelProvider);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.minimumHeight = 120;
        gd.heightHint = 120;
        gd.widthHint = 300;

        final Tree treeControl = propsTree.getTree();
        treeControl.setLayoutData(gd);
        treeControl.setHeaderVisible(true);
        treeControl.setLinesVisible(true);

        treeControl.addControlListener(new ControlAdapter() {
            private boolean packing = false;
            @Override
            public void controlResized(ControlEvent e) {
                if (!packing) {
                    try {
                        packing = true;
                        UIUtils.packColumns(treeControl, true);
                    }
                    finally {
                        packing = false;
                    }
                }
            }
        });
        treeControl.addListener(SWT.PaintItem, new PaintListener());
        this.boldFont = UIUtils.makeBoldFont(treeControl.getFont());

        ColumnViewerToolTipSupport.enableFor(propsTree, ToolTip.NO_RECREATE);

        TreeViewerColumn column = new TreeViewerColumn(propsTree, SWT.NONE);
        column.getColumn().setWidth(200);
        column.getColumn().setMoveable(true);
        column.getColumn().setText("Name");
        column.setLabelProvider(labelProvider);
        column.getColumn().addListener(SWT.Selection, new SortListener());


        column = new TreeViewerColumn(propsTree, SWT.NONE);
        column.getColumn().setWidth(120);
        column.getColumn().setMoveable(true);
        column.getColumn().setText("Value");
        column.setLabelProvider(labelProvider);

        /*
                List<? extends DBPProperty> props = ((DBPPropertyGroup) parent).getProperties();
                Collections.sort(props, new Comparator<DBPProperty>() {
                    public int compare(DBPProperty o1, DBPProperty o2)
                    {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                return props.toArray();

        */
        registerEditor();
        registerContextMenu();
    }

    private void disposeOldEditor()
    {
        if (curCellEditor != null) {
            curCellEditor.deactivate();
            curCellEditor.dispose();
            curCellEditor = null;
        }
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    private void registerEditor() {
        // Make an editor
        final Tree treeControl = propsTree.getTree();
        treeEditor = new TreeEditor(treeControl);
        treeEditor.horizontalAlignment = SWT.RIGHT;
        treeEditor.verticalAlignment = SWT.CENTER;
        treeEditor.grabHorizontal = true;
        treeEditor.minimumWidth = 50;

        treeControl.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                showEditor((TreeItem) e.item, true);
            }

            public void widgetSelected(SelectionEvent e) {
                showEditor((TreeItem) e.item, selectedColumn == 1 && (e.stateMask & SWT.BUTTON_MASK) != 0);
            }
        });
        treeControl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e)
            {
                TreeItem item = treeControl.getItem(new Point(e.x, e.y));
                if (item != null) {
                    selectedColumn = UIUtils.getColumnAtPos(item, e.x, e.y);
                } else {
                    selectedColumn = -1;
                }
            }
        });
        treeControl.addTraverseListener(new TraverseListener() {
            public void keyTraversed(TraverseEvent e)
            {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    // Set focus on editor
                    if (curCellEditor != null) {
                        curCellEditor.setFocus();
                    } else {
                        final TreeItem[] selection = treeControl.getSelection();
                        if (selection.length == 0) {
                            return;
                        }
                        showEditor(selection[0], true);
                    }
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            }
        });
    }

    private void showEditor(final TreeItem item, boolean isDef) {
        // Clean up any previous editor control
        disposeOldEditor();
        if (item == null) {
            return;
        }

        // Identify the selected row
        if (item.getData() instanceof TreeNode) {
            final Tree treeControl = propsTree.getTree();
            final TreeNode prop = (TreeNode)item.getData();
            if (prop.property == null) {
                return;
            }
            final CellEditor cellEditor = prop.property.createPropertyEditor(treeControl);
            if (cellEditor == null) {
                return;
            }
            final Object propertyValue = prop.propertySource.getPropertyValue(prop.property.getId());
            final ICellEditorListener cellEditorListener = new ICellEditorListener() {
                public void applyEditorValue()
                {
                    editorValueChanged(true, true);
                }

                public void cancelEditor()
                {
                    disposeOldEditor();
                }

                public void editorValueChanged(boolean oldValidState, boolean newValidState)
                {
                    if (newValidState) {
                        final Object value = cellEditor.getValue();
                        final Object oldValue = prop.propertySource.getPropertyValue(prop.property.getId());
                        if (!CommonUtils.equalObjects(oldValue, value)) {
                            prop.propertySource.setPropertyValue(
                                prop.property.getId(),
                                value);
                            handlePropertyChange(prop);
                        }
                    }
                }
            };
            cellEditor.addListener(cellEditorListener);
            if (propertyValue != null) {
                cellEditor.setValue(propertyValue);
            }
            curCellEditor = cellEditor;

            cellEditor.activate();
            final Control editorControl = cellEditor.getControl();
            if (editorControl != null) {
                editorControl.addTraverseListener(new TraverseListener() {
                    public void keyTraversed(TraverseEvent e)
                    {
                        if (e.detail == SWT.TRAVERSE_RETURN) {
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                            cellEditorListener.applyEditorValue();
                            disposeOldEditor();
                        } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                            new ActionResetProperty(prop, false).run();
                        }
                    }
                });
                treeEditor.setEditor(editorControl, item, 1);
            }
            if (isDef) {
                // Selected by mouse
                cellEditor.setFocus();
            }
        }
    }

    private void registerContextMenu() {
        // Register context menu
        {
            MenuManager menuMgr = new MenuManager();
            menuMgr.addMenuListener(new IMenuListener()
            {
                public void menuAboutToShow(final IMenuManager manager)
                {
                    final IStructuredSelection selection = (IStructuredSelection)propsTree.getSelection();

                    if (selection.isEmpty()) {
                        return;
                    }
                    final Object object = selection.getFirstElement();
                    if (object instanceof TreeNode) {
                        final TreeNode prop = (TreeNode)object;
                        if (prop.property != null) {
                            manager.add(new Action("Copy value") {
                                @Override
                                public void run() {
                                    TextTransfer textTransfer = TextTransfer.getInstance();
                                    clipboard.setContents(
                                        new Object[]{getPropertyValue(prop)},
                                        new Transfer[]{textTransfer});
                                }
                            });
                            if (isPropertyChanged(prop)) {
                                manager.add(new ActionResetProperty(prop, false));
                                if (!isCustomProperty(prop.property) &&
                                    prop.propertySource instanceof IPropertySourceEx)
                                {
                                    manager.add(new ActionResetProperty(prop, true));
                                }
                            }
                            manager.add(new Separator());
                        }
                        contributeContextMenu(manager, object, prop.category != null ? prop.category : prop.property.getCategory(), prop.property);
                    }
                }
            });

            menuMgr.setRemoveAllWhenShown(true);
            Menu menu = menuMgr.createContextMenu(propsTree.getControl());

            propsTree.getControl().setMenu(menu);
        }
    }

    private boolean isCustomProperty(IPropertyDescriptor property)
    {
        if (customCategories != null) {
            for (String category : customCategories) {
                if (category.equals(property.getCategory())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected String[] getCustomCategories()
    {
        return null;
    }

    protected void contributeContextMenu(IMenuManager manager, Object node, String category, IPropertyDescriptor property)
    {

    }

    private String getPropertyValue(TreeNode prop)
    {
        if (prop.category != null) {
            return prop.category;
        } else {
            return CommonUtils.toString(prop.propertySource.getPropertyValue(prop.property.getId()));
        }
    }

    private boolean isPropertyChanged(TreeNode prop)
    {
        return prop.propertySource.isPropertySet(prop.property.getId());
    }

    private void handlePropertyChange(TreeNode prop)
    {
        propsTree.update(prop, null);

        // Send modify event
        Event event = new Event();
        event.data = prop.property;
        this.notifyListeners(SWT.Modify, event);
    }

    protected void handlePropertyCreate(TreeNode prop) {
        handlePropertyChange(prop);
        propsTree.refresh(prop.parent);
        propsTree.expandToLevel(prop.parent, 1);
        propsTree.reveal(prop);
        propsTree.setSelection(new StructuredSelection(prop));
    }

    protected void handlePropertyRemove(TreeNode prop) {
        handlePropertyChange(prop);
        propsTree.refresh(prop.parent);
    }

    public void setMarginVisible(boolean visible)
    {
        GridLayout layout = (GridLayout) getLayout();
        if (visible) {
            layout.marginHeight = 5;
            layout.marginWidth = 5;
        } else {
            layout.marginHeight = 0;
            layout.marginWidth = 0;
        }
    }

    public void setExpandSingleRoot(boolean expandSingleRoot)
    {
        this.expandSingleRoot = expandSingleRoot;
    }

    private static class TreeNode {
        final TreeNode parent;
        final IPropertySource propertySource;
        final IPropertyDescriptor property;
        final String category;
        final List<TreeNode> children = new ArrayList<TreeNode>();

        private TreeNode(TreeNode parent, IPropertySource propertySource, IPropertyDescriptor property, String category)
        {
            this.parent = parent;
            this.propertySource = propertySource;
            this.property = property;
            this.category = category;
            if (parent != null) {
                parent.children.add(this);
            }
        }

        private TreeNode(TreeNode parent, IPropertySource propertySource, IPropertyDescriptor property)
        {
            this(parent, propertySource, property, null);
        }

        private TreeNode(TreeNode parent, IPropertySource propertySource, String category)
        {
            this(parent, propertySource, null, category);
        }
    }

    class PropsContentProvider implements IStructuredContentProvider, ITreeContentProvider
    {
        public void inputChanged(Viewer v, Object oldInput, Object newInput)
        {
        }

        public void dispose()
        {
        }

        public Object[] getElements(Object parent)
        {
            return getChildren(parent);
        }

        public Object getParent(Object child)
        {
            if (child instanceof TreeNode) {
                return ((TreeNode) child).parent;
            } else {
                return null;
            }
        }

        public Object[] getChildren(Object parent)
        {
            if (parent instanceof Collection) {
                return ((Collection) parent).toArray();
            } else if (parent instanceof TreeNode) {
                // Add all available property groups
                return ((TreeNode) parent).children.toArray();
            } else {
                return new Object[0];
            }
        }

        public boolean hasChildren(Object parent)
        {
            return getChildren(parent).length > 0;
        }
    }

    private class PropsLabelProvider extends CellLabelProvider
    {

        public String getText(Object obj, int columnIndex)
        {
            if (!(obj instanceof TreeNode)) {
                return "";
            }
            TreeNode node = (TreeNode)obj;
            if (columnIndex == 0) {
                if (node.category != null) {
                    return node.category;
                } else {
                    return node.property.getDisplayName();
                }
            } else {
                if (node.property != null) {
                    final Object propertyValue = node.propertySource.getPropertyValue(node.property.getId());
                    if (propertyValue instanceof Boolean) {
                        return "";
                    }
                    return CommonUtils.toString(propertyValue);
                } else {
                    return "";
                }
            }
        }

        public String getToolTipText(Object obj)
        {
            if (!(obj instanceof TreeNode)) {
                return "";
            }
            TreeNode node = (TreeNode)obj;
            if (node.category != null) {
                return node.category;
            } else {
                return node.property.getDescription();
            }
        }

        public Point getToolTipShift(Object object)
        {
            return new Point(5, 5);
        }

        public void update(ViewerCell cell)
        {
            Object element = cell.getElement();
            cell.setText(getText(element, cell.getColumnIndex()));
            boolean changed = false;
            if (element instanceof TreeNode && ((TreeNode) element).property != null) {
                changed = isPropertyChanged((TreeNode)element);
/*
                if (((DBPProperty)element).isRequired() && cell.getColumnIndex() == 0) {
                    cell.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
                }
*/
            }
            if (changed) {
                cell.setFont(boldFont);
            } else {
                cell.setFont(null);
            }
        }

    }

    private class SortListener implements Listener
    {
        int sortDirection = SWT.DOWN;
        TreeColumn prevColumn = null;

        public void handleEvent(Event e) {
            disposeOldEditor();

            Collator collator = Collator.getInstance(Locale.getDefault());
            TreeColumn column = (TreeColumn)e.widget;
            Tree tree = propsTree.getTree();
            if (prevColumn == column) {
                // Set reverse order
                sortDirection = (sortDirection == SWT.UP ? SWT.DOWN : SWT.UP);
            }
            prevColumn = column;
            tree.setSortColumn(column);
            tree.setSortDirection(sortDirection);

            propsTree.setSorter(new ViewerSorter(collator) {
                public int compare(Viewer viewer, Object e1, Object e2)
                {
                    int mul = (sortDirection == SWT.UP ? 1 : -1);
                    int result;
                    TreeNode n1 = (TreeNode) e1, n2 = (TreeNode) e2;
                    if (n1.property != null && n2.property != null) {
                        result = n1.property.getDisplayName().compareTo(n2.property.getDisplayName());
                    } else if (n1.category != null && n2.category != null) {
                        result = n1.category.compareTo(n2.category);
                    } else {
                        result = 0;
                    }
                    return result * mul;
                }
            });
        }
    }

    private class ActionResetProperty extends Action {
        private final TreeNode prop;
        private final boolean toDefault;

        public ActionResetProperty(TreeNode prop, boolean toDefault)
        {
            super("Reset value" + (!toDefault ? "" : " to default"));
            this.prop = prop;
            this.toDefault = toDefault;
        }

        @Override
        public void run() {
            if (toDefault && prop.propertySource instanceof IPropertySourceEx) {
                ((IPropertySourceEx)prop.propertySource).resetPropertyValueToDefault(prop.property.getId());
            } else {
                prop.propertySource.resetPropertyValue(prop.property.getId());
            }
            handlePropertyChange(prop);
            propsTree.update(prop, null);
            disposeOldEditor();
        }
    }

    class PaintListener implements Listener {

        public void handleEvent(Event event) {
            if (isDisposed()) {
                return;
            }
            switch(event.type) {
                case SWT.PaintItem: {
                    if (event.index == 1) {
                        final TreeNode node = (TreeNode)event.item.getData();
                        if (node != null && node.property != null) {
                            final Object propertyValue = node.propertySource.getPropertyValue(node.property.getId());
                            if (propertyValue instanceof Boolean) {
                                GC gc = event.gc;
                                final Tree tree = propsTree.getTree();
                                int columnWidth = tree.getColumn(1).getWidth();
                                Image image = (Boolean)propertyValue ? ImageUtils.getImageCheckboxEnabledOn() : ImageUtils.getImageCheckboxEnabledOff();
                                gc.drawImage(image, event.x + (columnWidth - image.getBounds().width) / 2, event.y);
                                event.doit = false;
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
}
