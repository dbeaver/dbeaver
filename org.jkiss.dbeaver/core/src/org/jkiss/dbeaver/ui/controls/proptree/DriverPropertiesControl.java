/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.proptree;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.DBPDriverProperty;
import org.jkiss.dbeaver.model.DBPDriverPropertyGroup;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;

import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

/**
 * Driver properties control
 */
public class DriverPropertiesControl extends Composite {

    static Log log = LogFactory.getLog(DriverPropertiesControl.class);

    private TreeViewer propsTree;
    private List<DBPDriverPropertyGroup> propertyGroups = null;
    private DBPDriverPropertyGroup driverProvidedProperties;
    private boolean showDriverProperties = true;

    private Map<String, String> originalValues = new HashMap<String, String>();
    private Map<String, String> propValues = new HashMap<String, String>();

    public DriverPropertiesControl(Composite parent, int style)
    {
        super(parent, style);

        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
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

    private void initPropTree()
    {
        propsTree = new TreeViewer(this, SWT.BORDER | SWT.FULL_SELECTION);
        propsTree.setContentProvider(new PropsContentProvider());
        propsTree.setLabelProvider(new PropsLabelProvider());
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

        ColumnViewerToolTipSupport.enableFor(propsTree, ToolTip.NO_RECREATE);

        TreeViewerColumn column = new TreeViewerColumn(propsTree, SWT.NONE);
        column.getColumn().setWidth(200);
        column.getColumn().setMoveable(true);
        column.getColumn().setText("Name");
        column.setLabelProvider(new PropsLabelProvider());

        column = new TreeViewerColumn(propsTree, SWT.NONE);
        column.getColumn().setWidth(120);
        column.getColumn().setMoveable(true);
        column.getColumn().setText("Value");
        column.setLabelProvider(new PropsValueProvider());

        // Make an editor
        final TreeEditor editor = new TreeEditor(treeControl);
        editor.horizontalAlignment = SWT.LEFT;
        editor.verticalAlignment = SWT.TOP;
        editor.grabHorizontal = true;
        editor.minimumWidth = 50;

        treeControl.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                // Clean up any previous editor control
                Control oldEditor = editor.getEditor();
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
                                Text text = (Text)editor.getEditor();
                                changeProperty(prop, text.getText());
                                editor.getItem().setText(1, text.getText());
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
                                Combo combo = (Combo)editor.getEditor();
                                changeProperty(prop, combo.getText());
                                editor.getItem().setText(1, combo.getText());
                            }
                        });
                        newEditor = control;
                    }
                    editor.setEditor(newEditor, item, 1);
                    propsTree.update(prop, null);
                }
            }
        });
    }

    private void changeProperty(DBPDriverProperty prop, String text)
    {
        String propName = prop.getName();
        if (!originalValues.containsKey(propName)) {
            originalValues.put(propName, propValues.get(propName));
        }
        propValues.put(propName, text);
    }

    public void loadProperties(DBPDriver driver, String url, Properties connectionProps)
    {
        loadDriverProperties(driver, url, connectionProps);
        if (propsTree != null) {
            propsTree.setInput(driver);
            propsTree.expandAll();
            for (TreeColumn column : propsTree.getTree().getColumns()) {
                column.pack();
            }
        }
    }

    private void loadDriverProperties(DBPDriver driver, String url, Properties connectionProps)
    {
        if (driver.supportsDriverProperties()) {
            try {
                Object driverInstance = driver.getDriverInstance();
                if (driverInstance instanceof java.sql.Driver) {
                    final DriverPropertyInfo[] propInfos = ((java.sql.Driver)driverInstance).getPropertyInfo(
                        url,
                        connectionProps);
                    if (!CommonUtils.isEmpty(propInfos)) {
                        driverProvidedProperties = new DriverProvidedPropertyGroup(driver, propInfos);
                    }
                }

            } catch (SQLException ex) {
                //log.warn("Can't read driver properties", ex);

            } catch (DBException e) {
                log.warn("Can't obtain driver instance", e);
            }
        } else {
            driverProvidedProperties = new DriverProvidedPropertyGroup(driver, new DriverPropertyInfo[0]);
        }
    }

    class PropsContentProvider implements IStructuredContentProvider,
                                          ITreeContentProvider
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
                List<DBPDriverPropertyGroup> groups = new ArrayList<DBPDriverPropertyGroup>();
                if (propertyGroups != null) {
                    groups.addAll(propertyGroups);
                }
                if (showDriverProperties) {
                    groups.addAll(((DBPDriver)parent).getPropertyGroups());
                }
                if (driverProvidedProperties != null) {
                    groups.add(driverProvidedProperties);
                }
                return groups.toArray();
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

    private static class PropsLabelProvider extends CellLabelProvider
    {
        public String getText(Object obj)
        {
            if (obj instanceof DBPDriverPropertyGroup) {
                return ((DBPDriverPropertyGroup) obj).getName();
            } else if (obj instanceof DBPDriverProperty) {
                return ((DBPDriverProperty) obj).getName();
            } else {
                return obj.toString();
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
            cell.setText(getText(cell.getElement()));
        }

    }

    private class PropsValueProvider extends CellLabelProvider
    {
        public String getText(Object obj)
        {
            if (obj instanceof DBPDriverProperty) {
                return ((DBPDriverProperty) obj).getDefaultValue();
            } else {
                return "";
            }
        }

        public String getToolTipText(Object obj)
        {
            return null;
        }

        public Point getToolTipShift(Object object)
        {
            return new Point(5, 5);
        }

        public void update(ViewerCell cell)
        {
            Object element = cell.getElement();
            cell.setText(getText(element));
            if (element instanceof DBPDriverProperty && originalValues.containsKey(((DBPDriverProperty)element).getName())) {
                cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
            }
        }

    }

    private class DriverProvidedPropertyGroup implements DBPDriverPropertyGroup
    {
        private DBPDriver driver;
        private final DriverPropertyInfo[] propInfos;
        private List<DBPDriverProperty> propList;

        public DriverProvidedPropertyGroup(DBPDriver driver, DriverPropertyInfo[] propInfos)
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
                for (final DriverPropertyInfo propInfo : propInfos) {
                    if (JDBCConstants.PROPERTY_USER.equals(propInfo.name) || JDBCConstants.PROPERTY_PASSWORD.equals(propInfo.name)) {
                        continue;
                    }
                    DBPDriverProperty prop = new DBPDriverProperty()
                    {
                        public DBPDriverPropertyGroup getGroup()
                        {
                            return DriverProvidedPropertyGroup.this;
                        }

                        public String getName()
                        {
                            return propInfo.name;
                        }

                        public String getDescription()
                        {
                            return propInfo.description;
                        }

                        public String getDefaultValue()
                        {
                            return propInfo.value;
                        }

                        public PropertyType getType()
                        {
                            return PropertyType.STRING;
                        }

                        public String[] getValidValues()
                        {
                            return propInfo.choices;
                        }
                    };
                    propList.add(prop);
                }
            }
            return propList;
        }
    }

}
