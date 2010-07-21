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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
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
    private List<DBPDriverPropertyGroup> propertyGroups = null;
    private DBPDriverPropertyGroup driverProvidedProperties;
    private DBPDriverPropertyGroup customProperties;
    private boolean showDriverProperties = true;

    private Map<String, String> originalValues = new TreeMap<String, String>();
    private Map<String, String> propValues = new TreeMap<String, String>();

    private Font boldFont;
    private Color colorBlue;
    private Clipboard clipboard;

    public DriverPropertiesControl(Composite parent, int style)
    {
        super(parent, style);

        colorBlue = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
        clipboard = new Clipboard(getDisplay());

        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 5;
        gl.marginWidth = 5;
        this.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        this.setLayoutData(gd);

        initPropTree();
    }

    public List<DBPDriverPropertyGroup> getPropertyGroups()
    {
        return propertyGroups;
    }

    public void setPropertyGroups(List<DBPDriverPropertyGroup> propertyGroups)
    {
        this.propertyGroups = propertyGroups;
    }

    public boolean isShowDriverProperties()
    {
        return showDriverProperties;
    }

    public void setShowDriverProperties(boolean showDriverProperties)
    {
        this.showDriverProperties = showDriverProperties;
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

        treeControl.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                // Clean up any previous editor control
                Control oldEditor = treeEditor.getEditor();
                if (oldEditor != null) oldEditor.dispose();

                // Identify the selected row
                TreeItem item = (TreeItem)e.item;
                if (item == null) {
                    return;
                }
                if (item.getData() instanceof DBPDriverProperty) {
                    final DBPDriverProperty prop = (DBPDriverProperty)item.getData();
                    String[] validValues = prop.getValidValues();
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
                            String value =  validValues[i];
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
                    if (object instanceof DBPDriverProperty) {
                        final DBPDriverProperty prop = (DBPDriverProperty)object;
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
                            manager.add(new Action("Reset value") {
                                @Override
                                public void run() {
                                    if (originalValues.containsKey(propName)) {
                                        propValues.put(propName, originalValues.get(propName));
                                    } else {
                                        propValues.remove(propName);
                                    }
                                    propsTree.update(prop, null);
                                    Control oldEditor = treeEditor.getEditor();
                                    if (oldEditor != null) oldEditor.dispose();
                                }
                            });
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
                        manager.add(new Separator());
                    }

                    boolean isCustom = false;
                    if (object instanceof CustomPropertyGroup) {
                        isCustom = true;
                    } else if (object instanceof DBPDriverProperty && ((DBPDriverProperty)object).getGroup() instanceof CustomPropertyGroup) {
                        isCustom = true;
                    }
                    if (isCustom) {
                        manager.add(new Action("Add new property") {
                            @Override
                            public void run() {
                                addNewProperty(object);
                            }
                        });
                        if (object instanceof DBPDriverProperty) {
                            manager.add(new Action("Remove property") {
                                @Override
                                public void run() {
                                    removeProperty((DBPDriverProperty)object);
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
        if (parent instanceof DBPDriverProperty && ((DBPDriverProperty)parent).getGroup() instanceof CustomPropertyGroup) {
            customGroup = (CustomPropertyGroup)((DBPDriverProperty)parent).getGroup();
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

    private void removeProperty(DBPDriverProperty prop) {
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

    private List<DBPDriverPropertyGroup> getAllPropertyGroups(DBPDriver driver, boolean includeCustom) {
        List<DBPDriverPropertyGroup> groups = new ArrayList<DBPDriverPropertyGroup>();
        if (propertyGroups != null) {
            groups.addAll(propertyGroups);
        }
        if (showDriverProperties) {
            groups.addAll(driver.getPropertyGroups());
        }
        if (driverProvidedProperties != null) {
            groups.add(driverProvidedProperties);
        }
        if (includeCustom && customProperties != null) {
            groups.add(customProperties);
        }
        return groups;
    }

    private String getPropertyValue(DBPDriverProperty prop)
    {
        Object propValue = propValues.get(prop.getName());
        if (propValue != null) {
            return propValue.toString();
        }
        return prop.getDefaultValue();
    }

    private boolean isPropertyChanged(DBPDriverProperty prop)
    {
        Object propValue = propValues.get(prop.getName());
        if (propValue != null && !CommonUtils.equalObjects(propValue, prop.getDefaultValue())) {
            return true;
        }
        return false;
    }

    private void changeProperty(DBPDriverProperty prop, String text)
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
            List<DBPConnectionProperty> propInfos = driver.getDataSourceProvider().getConnectionProperties(driver, connectionInfo);
            if (!CommonUtils.isEmpty(propInfos)) {
                driverProvidedProperties = new DriverProvidedPropertyGroup(driver, propInfos);
            }
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
        List<DBPDriverPropertyGroup> allGroups = getAllPropertyGroups(driver, false);
        for (DBPDriverPropertyGroup group : allGroups) {
            for (DBPDriverProperty prop : group.getProperties()) {
                propNames.add(prop.getName());
            }
        }

        // Find prop values which are not from driver
        for (Object propName : propValues.keySet()) {
            if (!propNames.contains(propName.toString())) {
                customNames.add(propName.toString());
            }
        }
        customProperties = new CustomPropertyGroup(driver, customNames);
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
            if (child instanceof DBPDriverPropertyGroup) {
                return ((DBPDriverPropertyGroup) child).getDriver();
            } else if (child instanceof DBPDriverProperty) {
                return ((DBPDriverProperty) child).getGroup();
            } else {
                return null;
            }
        }

        public Object[] getChildren(Object parent)
        {
            if (parent instanceof DBPDriver) {
                // Add all available property groups
                return getAllPropertyGroups((DBPDriver) parent, true).toArray();
            } else if (parent instanceof DBPDriverPropertyGroup) {
                // Sort props by name
                List<? extends DBPDriverProperty> props = ((DBPDriverPropertyGroup) parent).getProperties();
                Collections.sort(props, new Comparator<DBPDriverProperty>() {
                    public int compare(DBPDriverProperty o1, DBPDriverProperty o2)
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
                if (obj instanceof DBPDriverPropertyGroup) {
                    return ((DBPDriverPropertyGroup) obj).getName();
                } else if (obj instanceof DBPDriverProperty) {
                    return ((DBPDriverProperty) obj).getName();
                } else {
                    return obj.toString();
                }
            } else {
                if (obj instanceof DBPDriverProperty) {
                    return getPropertyValue((DBPDriverProperty) obj);
                } else {
                    return "";
                }
            }
        }

        public String getToolTipText(Object obj)
        {
            if (obj instanceof DBPDriverPropertyGroup) {
                return ((DBPDriverPropertyGroup) obj).getDescription();
            } else if (obj instanceof DBPDriverProperty) {
                return ((DBPDriverProperty) obj).getDescription();
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
            if (element instanceof DBPDriverProperty) {
                changed = isPropertyChanged((DBPDriverProperty)element);
                custom = ((DBPDriverProperty)element).getGroup() instanceof CustomPropertyGroup;
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

    private class DriverPropertyImpl implements DBPDriverProperty {
        private final DBPDriverPropertyGroup group;
        private DBPConnectionProperty propInfo;
        private String name;

        public DriverPropertyImpl(DBPDriverPropertyGroup group, DBPConnectionProperty propInfo) {
            this.group = group;
            this.propInfo = propInfo;
        }

        private DriverPropertyImpl(DBPDriverPropertyGroup group, String name) {
            this.group = group;
            this.name = name;
        }

        public DBPDriverPropertyGroup getGroup()
        {
            return group;
        }

        public String getName()
        {
            return propInfo == null ? name : propInfo.getName();
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getDescription()
        {
            return propInfo == null ? null : propInfo.getDescription();
        }

        public String getDefaultValue()
        {
            return propInfo == null ? null : propInfo.getValue();
        }

        public PropertyType getType()
        {
            return PropertyType.STRING;
        }

        public String[] getValidValues()
        {
            return propInfo == null ? null : propInfo.getChoices();
        }
    }

    private class DriverProvidedPropertyGroup implements DBPDriverPropertyGroup
    {
        private DBPDriver driver;
        private final List<DBPConnectionProperty> propInfos;
        private List<DBPDriverProperty> propList;

        public DriverProvidedPropertyGroup(DBPDriver driver, List<DBPConnectionProperty> propInfos)
        {
            this.driver = driver;
            this.propInfos = propInfos;
            this.propList = null;
        }

        public DBPDriver getDriver()
        {
            return driver;
        }

        public String getName()
        {
            return "Driver Properties";
        }

        public String getDescription()
        {
            return "Driver Properties";
        }

        public List<? extends DBPDriverProperty> getProperties()
        {
            if (propList == null) {
                propList = new ArrayList<DBPDriverProperty>();
                for (final DBPConnectionProperty propInfo : propInfos) {
                    if (JDBCConstants.PROPERTY_USER.equals(propInfo.getName()) || JDBCConstants.PROPERTY_PASSWORD.equals(propInfo.getName())) {
                        continue;
                    }
                    DBPDriverProperty prop = new DriverPropertyImpl(this, propInfo);
                    propList.add(prop);
                }
            }
            return propList;
        }
    }

    private class CustomPropertyGroup implements DBPDriverPropertyGroup
    {
        private DBPDriver driver;
        private List<DBPDriverProperty> propList;

        public CustomPropertyGroup(DBPDriver driver, Set<String> propNames)
        {
            this.driver = driver;
            this.propList = new ArrayList<DBPDriverProperty>();
            if (propNames != null) {
                for (String name : propNames) {
                    propList.add(new DriverPropertyImpl(this, name));
                }
            }
        }

        public DBPDriver getDriver()
        {
            return driver;
        }

        public String getName()
        {
            return "User Properties";
        }

        public String getDescription()
        {
            return "User Properties";
        }

        public List<? extends DBPDriverProperty> getProperties()
        {
            return propList;
        }

        public void addProperty(String name) {
            propList.add(new DriverPropertyImpl(this, name));
        }

        public void removeProperty(DBPDriverProperty prop) {
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
