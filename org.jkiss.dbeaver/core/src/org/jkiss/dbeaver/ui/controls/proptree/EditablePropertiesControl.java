/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.proptree;

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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.prop.DBPProperty;
import org.jkiss.dbeaver.model.prop.DBPPropertyGroup;
import org.jkiss.dbeaver.registry.PropertyDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.text.Collator;
import java.util.*;
import java.util.List;

/**
 * Driver properties control
 */
public class EditablePropertiesControl extends Composite {

    private TreeViewer propsTree;
    private TreeEditor treeEditor;

    private Map<String, String> originalValues = new TreeMap<String, String>();
    private Map<String, String> propValues = new TreeMap<String, String>();

    private Font boldFont;
    //private Color colorBlue;
    private Clipboard clipboard;
    private Map<String,String> defaultValues = new TreeMap<String, String>();

    public EditablePropertiesControl(Composite parent, int style)
    {
        super(parent, style);

        //colorBlue = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
        clipboard = new Clipboard(getDisplay());

        this.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        this.setLayoutData(gd);

        setMarginVisible(true);
        initPropTree();
    }

    public void loadProperties(
        List<? extends DBPPropertyGroup> propertyGroups,
        Map<String, String> propertyValues)
    {
        loadProperties(propertyGroups, propertyValues, null);
    }

    public void loadProperties(
        List<? extends DBPPropertyGroup> propertyGroups,
        Map<String, String> propertyValues,
        Map<String, String> defaultValues)
    {
        this.propValues.clear();
        this.originalValues.clear();
        if (propertyValues != null) {
            this.propValues.putAll(propertyValues);
            this.originalValues.putAll(propertyValues);
        }
        this.defaultValues.clear();
        if (defaultValues != null) {
            // Set specified default values
            this.defaultValues.putAll(defaultValues);
        } else {
            // Collect default values from property model
            Object root = propsTree.getInput();
            if (root instanceof DBPPropertyGroup) {
                addDefaultValues((DBPPropertyGroup)root);
            } else if (root instanceof Collection) {
                for (Object group : (Collection)root) {
                    if (group instanceof DBPPropertyGroup) {
                        addDefaultValues((DBPPropertyGroup)group);
                    }
                }

            }
        }

        if (propsTree != null) {
            Object root;
            if (propertyGroups.size() == 1) {
                root = propertyGroups.get(0);
            } else {
                root = propertyGroups;
            }
            propsTree.setInput(root);
            propsTree.expandAll();
            UIUtils.packColumns(propsTree.getTree());
        }
        disposeOldEditor();
    }

    public void reloadDefaultValues(Map<String, String> defaultValues)
    {
        this.defaultValues.clear();
        if (defaultValues != null) {
            this.defaultValues.putAll(defaultValues);
        }
        disposeOldEditor();
        propsTree.refresh();
    }

    public Map<String, String> getProperties() {
        return propValues;
    }

    public Map<String, String> getPropertiesWithDefaults() {
        Map<String, String> allValues = new HashMap<String, String>(defaultValues);
        allValues.putAll(propValues);
        return allValues;
    }

    private String getDefaultValue(DBPProperty property)
    {
        String value = defaultValues.get(property.getId());
        if (value == null) {
            value = property.getDefaultValue();
        }
        return value;
    }

    private void addDefaultValues(DBPPropertyGroup propertyGroup)
    {
        for (DBPProperty property : propertyGroup.getProperties()) {
            String defaultValue = getDefaultValue(property);
            if (defaultValue != null) {
                defaultValues.put(property.getId(), defaultValue);
            }
        }
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

        treeControl.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    e.doit = false;
                } else if (e.keyCode == SWT.ESC) {
                    e.doit = false;
                }
            }
            public void keyReleased(KeyEvent e)
            {
            }
        });
        treeControl.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                UIUtils.packColumns(treeControl);
            }
        });

        Combo tmpCombo = new Combo(treeControl, SWT.READ_ONLY | SWT.DROP_DOWN);
        final int comboHeight = tmpCombo.getBounds().height;
        tmpCombo.dispose();

        treeControl.addListener(SWT.MeasureItem, new Listener() {
            public void handleEvent(Event event) {
                Rectangle rect = ((TreeItem) event.item).getBounds();
                event.width = rect.width;
                event.height = comboHeight;
            }
        });


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
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    private void registerEditor() {
        // Make an editor
        final Tree treeControl = propsTree.getTree();
        treeEditor = new TreeEditor(treeControl);
        treeEditor.horizontalAlignment = SWT.LEFT;
        treeEditor.verticalAlignment = SWT.TOP;
        treeEditor.grabHorizontal = true;
        treeEditor.minimumWidth = 50;

        treeControl.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                showEditor(e, true);
            }

            public void widgetSelected(SelectionEvent e) {
                showEditor(e, false);
            }

            public void showEditor(SelectionEvent e, boolean isDef) {
                // Clean up any previous editor control
                disposeOldEditor();

                // Identify the selected row
                TreeItem item = (TreeItem)e.item;
                if (item == null) {
                    return;
                }
                if (item.getData() instanceof DBPProperty) {
                    final DBPProperty prop = (DBPProperty)item.getData();
                    Object[] validValues = prop.getValidValues();
                    Control newEditor;
                    if (validValues == null) {
                        Text text = new Text(treeControl, SWT.BORDER);
                        text.setText(item.getText(1));
                        text.addModifyListener(new ModifyListener() {
                            public void modifyText(ModifyEvent e) {
                                Text text = (Text) treeEditor.getEditor();
                                changeProperty(prop, text.getText());
                                treeEditor.getItem().setText(1, text.getText());
                            }
                        });
                        text.selectAll();
                        newEditor = text;
                    } else {
                        Combo control = new Combo(treeControl, SWT.READ_ONLY | SWT.DROP_DOWN);
                        int selIndex = -1;
                        for (int i = 0; i < validValues.length; i++) {
                            String value =  String.valueOf(validValues[i]);
                            control.add(value);
                            if (value.equals(item.getText(1))) {
                                selIndex = i;
                            }
                        }
                        if (selIndex >= 0) {
                            control.select(selIndex);
                        }
                        control.addModifyListener(new ModifyListener() {
                            public void modifyText(ModifyEvent e) {
                                Combo combo = (Combo) treeEditor.getEditor();
                                changeProperty(prop, combo.getText());
                                treeEditor.getItem().setText(1, combo.getText());
                            }
                        });
                        newEditor = control;
                    }
                    if (isDef) {
                        // Selected by mouse
                        newEditor.setFocus();
                    }
                    treeEditor.setEditor(newEditor, item, 1);
                }
            }
        });
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
                    if (object instanceof DBPProperty) {
                        final DBPProperty prop = (DBPProperty)object;
                        final String propId = prop.getId();
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
                            final boolean isCustom = isCustomProperty(prop);
                            manager.add(new Action("Reset value") {
                                @Override
                                public void run() {
                                    if (originalValues.containsKey(propId)) {
                                        changeProperty(prop, originalValues.get(propId));
                                    } else if (!isCustom) {
                                        changeProperty(prop, null);
                                    }
                                    propsTree.update(prop, null);
                                    disposeOldEditor();
                                }
                            });
                            if (!isCustom) {
                                manager.add(new Action("Reset value to default") {
                                    @Override
                                    public void run() {
                                        changeProperty(prop, null);
                                        propsTree.update(prop, null);
                                        disposeOldEditor();
                                    }
                                });
                            }
                        }
                        manager.add(new Separator());
                    }
                    contributeContextMenu(manager, object);
                }
            });

            menuMgr.setRemoveAllWhenShown(true);
            Menu menu = menuMgr.createContextMenu(propsTree.getControl());

            propsTree.getControl().setMenu(menu);
        }
    }

    protected boolean isCustomProperty(DBPProperty property)
    {
        return false;
    }

    protected void contributeContextMenu(IMenuManager manager, Object selectedObject)
    {

    }

    private String getPropertyValue(DBPProperty prop)
    {
        Object propValue = propValues.get(prop.getId());
        if (propValue == null) {
            propValue = getDefaultValue(prop);
        }
        if (propValue != null) {
            return String.valueOf(propValue);
        } else {
            return "";
        }
    }

    private boolean isPropertyChanged(DBPProperty prop)
    {
        Object propValue = propValues.get(prop.getId());
        return propValue != null && !CommonUtils.equalObjects(propValue, getDefaultValue(prop));
    }

    private void changeProperty(DBPProperty prop, String text)
    {
        String propId = prop.getId();
        if (!originalValues.containsKey(propId) && propValues.containsKey(propId)) {
            originalValues.put(propId, propValues.get(propId));
        }
        if (text == null) {
            propValues.remove(propId);
        } else {
            propValues.put(propId, text);
        }
        propsTree.update(prop, null);

        // Send modify event
        Event event = new Event();
        event.data = prop;
        this.notifyListeners(SWT.Modify, event);
    }

    protected void handlePropertyCreate(PropertyDescriptor newProp, String newValue) {
        changeProperty(newProp, newValue);
        propsTree.refresh(newProp.getGroup());
        propsTree.expandToLevel(newProp.getGroup(), 1);
    }

    protected void handlePropertyRemove(DBPProperty prop) {
        changeProperty(prop, null);
        propsTree.refresh(prop.getGroup());
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

    public boolean isDirty()
    {
        return !propValues.isEmpty();
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
            if (child instanceof DBPPropertyGroup) {
                return propsTree.getInput();
            } else if (child instanceof DBPProperty) {
                return ((DBPProperty) child).getGroup();
            } else {
                return null;
            }
        }

        public Object[] getChildren(Object parent)
        {
            if (parent instanceof List) {
                // Add all available property groups
                return ((List) parent).toArray();
            } else if (parent instanceof DBPPropertyGroup) {
                // Sort props by name
                return ((DBPPropertyGroup) parent).getProperties().toArray();
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
            if (columnIndex == 0) {
                if (obj instanceof DBPPropertyGroup) {
                    return ((DBPPropertyGroup) obj).getName();
                } else if (obj instanceof DBPProperty) {
                    return ((DBPProperty) obj).getName();
                } else {
                    return obj.toString();
                }
            } else {
                if (obj instanceof DBPProperty) {
                    return getPropertyValue((DBPProperty) obj);
                } else {
                    return "";
                }
            }
        }

        public String getToolTipText(Object obj)
        {
            if (obj instanceof DBPPropertyGroup) {
                return ((DBPPropertyGroup) obj).getDescription();
            } else if (obj instanceof DBPProperty) {
                return ((DBPProperty) obj).getDescription();
            } else {
                return obj.toString();
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
            if (element instanceof DBPProperty) {
                changed = isPropertyChanged((DBPProperty)element);
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
                    if (e1 instanceof DBPProperty && e2 instanceof DBPProperty) {
                        result = ((DBPProperty)e1).getName().compareTo(((DBPProperty)e2).getName());
                    } else if (e1 instanceof DBPPropertyGroup && e2 instanceof DBPPropertyGroup) {
                        result = ((DBPPropertyGroup)e1).getName().compareTo(((DBPPropertyGroup)e2).getName());
                    } else {
                        result = 0;
                    }
                    return result * mul;
                }
            });
        }
    }

}
