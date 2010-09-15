/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.proptree;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.prop.DBPProperty;
import org.jkiss.dbeaver.model.prop.DBPPropertyGroup;
import org.jkiss.dbeaver.registry.PropertyDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.*;
import java.util.List;

/**
 * Driver properties control
 */
public class DriverPropertiesControl extends Composite {

    static final Log log = LogFactory.getLog(DriverPropertiesControl.class);

    private TreeViewer propsTree;
    private TreeEditor treeEditor;
    private List<DBPPropertyGroup> propertyGroups = null;
    private DBPPropertyGroup driverProvidedProperties;
    private DBPPropertyGroup customProperties;

    private Map<String, String> originalValues = new TreeMap<String, String>();
    private Map<String, String> propValues = new TreeMap<String, String>();

    private Font boldFont;
    //private Color colorBlue;
    private Clipboard clipboard;

    public DriverPropertiesControl(Composite parent, int style)
    {
        super(parent, style);

        //colorBlue = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
        clipboard = new Clipboard(getDisplay());

        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 5;
        gl.marginWidth = 5;
        this.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        this.setLayoutData(gd);

        initPropTree();
    }

    public void loadProperties(DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        propValues.clear();
        originalValues.clear();
        Map<String, String> connectionProps = connectionInfo.getProperties();
        if (connectionProps != null) {
            propValues.putAll(connectionProps);
            originalValues.putAll(connectionProps);
        }
        driverProvidedProperties = null;
        customProperties = null;

        loadDriverProperties(driver, connectionInfo);
        loadCustomProperties(driver);
        if (propsTree != null) {
            propsTree.setInput(driver);
            propsTree.expandAll();
            for (TreeColumn column : propsTree.getTree().getColumns()) {
                column.pack();
            }
        }
    }

    public Map<String, String> getProperties() {
        return propValues;
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


        column = new TreeViewerColumn(propsTree, SWT.NONE);
        column.getColumn().setWidth(120);
        column.getColumn().setMoveable(true);
        column.getColumn().setText("Value");
        column.setLabelProvider(labelProvider);

        registerEditor();
        registerContextMenu();
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
                Control oldEditor = treeEditor.getEditor();
                if (oldEditor != null) oldEditor.dispose();

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
                        final String propName = prop.getName();
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
                            final boolean isCustom = prop.getGroup() instanceof CustomPropertyGroup;
                            manager.add(new Action("Reset value") {
                                @Override
                                public void run() {
                                    if (originalValues.containsKey(propName)) {
                                        propValues.put(propName, originalValues.get(propName));
                                    } else if (!isCustom) {
                                        propValues.remove(propName);
                                    }
                                    propsTree.update(prop, null);
                                    Control oldEditor = treeEditor.getEditor();
                                    if (oldEditor != null) oldEditor.dispose();
                                }
                            });
                            if (!isCustom) {
                                manager.add(new Action("Reset value to default") {
                                    @Override
                                    public void run() {
                                        propValues.remove(propName);
                                        propsTree.update(prop, null);
                                        Control oldEditor = treeEditor.getEditor();
                                        if (oldEditor != null) oldEditor.dispose();
                                    }
                                });
                            }
                        }
                        manager.add(new Separator());
                    }

                    boolean isCustom = false;
                    if (object instanceof CustomPropertyGroup) {
                        isCustom = true;
                    } else if (object instanceof DBPProperty && ((DBPProperty)object).getGroup() instanceof CustomPropertyGroup) {
                        isCustom = true;
                    }
                    if (isCustom) {
                        manager.add(new Action("Add new property") {
                            @Override
                            public void run() {
                                addNewProperty(object);
                            }
                        });
                        if (object instanceof DBPProperty) {
                            manager.add(new Action("Remove property") {
                                @Override
                                public void run() {
                                    removeProperty((DBPProperty)object);
                                }
                            });
                        }
                    }
                }
            });

            menuMgr.setRemoveAllWhenShown(true);
            Menu menu = menuMgr.createContextMenu(propsTree.getControl());

            propsTree.getControl().setMenu(menu);
        }
    }

    private void addNewProperty(Object parent) {
        CustomPropertyGroup customGroup = null;
        if (parent instanceof DBPProperty && ((DBPProperty)parent).getGroup() instanceof CustomPropertyGroup) {
            customGroup = (CustomPropertyGroup)((DBPProperty)parent).getGroup();
        } else if (parent instanceof CustomPropertyGroup) {
            customGroup = (CustomPropertyGroup)parent;
        }
        if (customGroup != null) {
            // Ask user for new property name
            PropertyNameDialog dialog = new PropertyNameDialog(getShell());
            if (dialog.open() == IDialogConstants.OK_ID) {
                String propName = dialog.getPropName();
                // Check property name (must be unique
                customGroup.addProperty(propName);
                propValues.put(propName, "");
                propsTree.refresh(customGroup);
                propsTree.expandToLevel(customGroup, 1);
            }
        }
    }

    private void removeProperty(DBPProperty prop) {
        CustomPropertyGroup customGroup = null;
        if (prop.getGroup() instanceof CustomPropertyGroup) {
            customGroup = (CustomPropertyGroup)prop.getGroup();
        }
        if (customGroup != null) {
            customGroup.removeProperty(prop);
            propValues.remove(prop.getName());
            propsTree.refresh(customGroup);
        }

    }

    private List<DBPPropertyGroup> getAllPropertyGroups(DBPDriver driver, boolean includeCustom) {
        List<DBPPropertyGroup> groups = new ArrayList<DBPPropertyGroup>();
        if (propertyGroups != null) {
            groups.addAll(propertyGroups);
        }
        groups.addAll(driver.getPropertyGroups());
        if (driverProvidedProperties != null) {
            groups.add(driverProvidedProperties);
        }
        if (includeCustom && customProperties != null) {
            groups.add(customProperties);
        }
        return groups;
    }

    private String getPropertyValue(DBPProperty prop)
    {
        Object propValue = propValues.get(prop.getName());
        if (propValue != null) {
            return propValue.toString();
        }
        return String.valueOf(prop.getDefaultValue());
    }

    private boolean isPropertyChanged(DBPProperty prop)
    {
        Object propValue = propValues.get(prop.getName());
        return propValue != null && !CommonUtils.equalObjects(propValue, prop.getDefaultValue());
    }

    private void changeProperty(DBPProperty prop, String text)
    {
        String propName = prop.getName();
        if (!originalValues.containsKey(propName) && propValues.containsKey(propName)) {
            originalValues.put(propName, propValues.get(propName));
        }
        propValues.put(propName, text);
        propsTree.update(prop, null);
    }

    private void loadDriverProperties(DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        try {
            driverProvidedProperties = driver.getDataSourceProvider().getConnectionProperties(driver, connectionInfo);
        } catch (DBException e) {
            log.warn("Can't load driver properties", e);
        }
    }

    private void loadCustomProperties(DBPDriver driver)
    {
        // Custom properties are properties which are came not from driver and not from
        Set<String> customNames = new TreeSet<String>();

        // Collect all driver (and all other) properties
        Set<String> propNames = new TreeSet<String>();
        List<DBPPropertyGroup> allGroups = getAllPropertyGroups(driver, false);
        for (DBPPropertyGroup group : allGroups) {
            for (DBPProperty prop : group.getProperties()) {
                propNames.add(prop.getName());
            }
        }

        // Find prop values which are not from driver
        for (Object propName : propValues.keySet()) {
            if (!propNames.contains(propName.toString())) {
                customNames.add(propName.toString());
            }
        }
        customProperties = new CustomPropertyGroup(customNames);
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
            if (parent instanceof DBPDriver) {
                // Add all available property groups
                return getAllPropertyGroups((DBPDriver) parent, true).toArray();
            } else if (parent instanceof DBPPropertyGroup) {
                // Sort props by name
                List<? extends DBPProperty> props = ((DBPPropertyGroup) parent).getProperties();
                Collections.sort(props, new Comparator<DBPProperty>() {
                    public int compare(DBPProperty o1, DBPProperty o2)
                    {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                return props.toArray();
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
            boolean custom = false;
            if (element instanceof DBPProperty) {
                changed = isPropertyChanged((DBPProperty)element);
                custom = ((DBPProperty)element).getGroup() instanceof CustomPropertyGroup;
            }
            if (changed) {
                cell.setFont(boldFont);
            } else {
                cell.setFont(null);
            }
            if (custom) {
                //cell.setForeground(colorBlue);
            }
        }

    }

    private class CustomPropertyGroup implements DBPPropertyGroup
    {
        private List<DBPProperty> propList;

        public CustomPropertyGroup(Set<String> propNames)
        {
            this.propList = new ArrayList<DBPProperty>();
            if (propNames != null) {
                for (String name : propNames) {
                    propList.add(new PropertyDescriptor(this, name, null, DBPProperty.PropertyType.STRING, false, null, null));
                }
            }
        }

        public String getName()
        {
            return "User Properties";
        }

        public String getDescription()
        {
            return "User Properties";
        }

        public List<? extends DBPProperty> getProperties()
        {
            return propList;
        }

        public void addProperty(String name) {
            propList.add(new PropertyDescriptor(this, name, null, DBPProperty.PropertyType.STRING, false, null, null));
        }

        public void removeProperty(DBPProperty prop) {
            propList.remove(prop);
        }
    }

    static class PropertyNameDialog extends Dialog
    {
        private Text propNameText;

        private String propName;

        public PropertyNameDialog(Shell parentShell)
        {
            super(parentShell);
        }

        public String getPropName() {
            return propName;
        }

        protected Control createDialogArea(Composite parent)
        {
            Composite propGroup = new Composite(parent, SWT.NONE);
            GridLayout gl = new GridLayout(1, false);
            gl.marginHeight = 10;
            gl.marginWidth = 10;
            propGroup.setLayout(gl);
            GridData gd = new GridData(GridData.FILL_BOTH);
            propGroup.setLayoutData(gd);

            propNameText = UIUtils.createLabelText(propGroup, "Property Name", "");

            return parent;
        }

        protected void okPressed() {
            propName = propNameText.getText();

            super.okPressed();
        }

    }

}
