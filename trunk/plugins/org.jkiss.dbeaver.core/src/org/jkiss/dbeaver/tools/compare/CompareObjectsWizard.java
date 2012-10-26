/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.tools.compare;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.DataSourcePropertyFilter;
import org.jkiss.dbeaver.ui.properties.ILazyPropertyLoadListener;
import org.jkiss.dbeaver.ui.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CompareObjectsWizard extends Wizard implements IExportWizard {

    static final Log log = LogFactory.getLog(CompareObjectsWizard.class);

    private static final String RS_COMPARE_WIZARD_DIALOG_SETTINGS = "CompareWizard";//$NON-NLS-1$

    private final Object PROPS_LOCK = new Object();
    private final static Object LAZY_VALUE = new Object();

    private CompareObjectsSettings settings;
    private Map<DBPDataSource, IFilter> dataSourceFilters = new IdentityHashMap<DBPDataSource, IFilter>();

    private final DBRProcessListener initializeFinisher;
    private final ILazyPropertyLoadListener lazyPropertyLoadListener;

    private volatile int initializedCount = 0;
    private volatile IStatus initializeError;
    private final Map<Object, Map<IPropertyDescriptor, Object>> propertyValues = new IdentityHashMap<Object, Map<IPropertyDescriptor, Object>>();
    private final List<ObjectPropertyDescriptor> differentProps = new ArrayList<ObjectPropertyDescriptor>();

    public CompareObjectsWizard(List<DBNDatabaseNode> nodes)
    {
        this.settings = new CompareObjectsSettings(nodes);
        IDialogSettings section = UIUtils.getDialogSettings(RS_COMPARE_WIZARD_DIALOG_SETTINGS);
        setDialogSettings(section);

        settings.loadFrom(section);
        initializeFinisher = new DBRProcessListener() {
            @Override
            public void onProcessFinish(IStatus status)
            {
                if (!status.isOK()) {
                    initializeError = status;
                } else {
                    initializedCount++;
                }
            }
        };
        lazyPropertyLoadListener = new ILazyPropertyLoadListener() {
            @Override
            public void handlePropertyLoad(Object object, IPropertyDescriptor property, Object propertyValue, boolean completed)
            {
                synchronized (propertyValues) {
                    Map<IPropertyDescriptor, Object> objectProps = propertyValues.get(object);
                    if (objectProps != null) {
                        objectProps.put(property, propertyValue);
                    }
                }
            }
        };
        PropertiesContributor.getInstance().addLazyListener(lazyPropertyLoadListener);
    }

    @Override
    public void dispose()
    {
        PropertiesContributor.getInstance().removeLazyListener(lazyPropertyLoadListener);
        super.dispose();
    }

    public CompareObjectsSettings getSettings()
    {
        return settings;
    }

    @Override
    public void addPages()
    {
        super.addPages();
        addPage(new CompareObjectsPageSettings());
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection)
    {
        setWindowTitle("Export data");
        setNeedsProgressMonitor(true);
    }

    private void showError(String error)
    {
        ((WizardPage)getContainer().getCurrentPage()).setErrorMessage(error);
    }

    @Override
    public boolean performFinish()
    {
        // Save settings
        getSettings().saveTo(getDialogSettings());
        dataSourceFilters.clear();
        showError(null);

        // Compare
        try {
            RuntimeUtils.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        monitor.beginTask("Compare objects", 100);
                        compareNodes(monitor, getSettings().getNodes());
                        monitor.done();
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
            UIUtils.showMessageBox(getShell(), "Objects compare", "Objects compare finished", SWT.ICON_INFORMATION);
        } catch (InvocationTargetException e) {
            if (initializeError != null) {
                showError(initializeError.getMessage());
            } else {
                log.error(e.getTargetException());
                showError(e.getTargetException().getMessage());
            }
            return false;
        } catch (InterruptedException e) {
            showError("Compare interrupted");
            return false;
        }

        // Done
        return true;
    }

    private void compareNodes(DBRProgressMonitor monitor, List<DBNDatabaseNode> nodes)
        throws DBException, InterruptedException
    {
        // Clear compare singletons
        this.initializedCount = 0;
        this.initializeError = null;
        this.propertyValues.clear();
        this.differentProps.clear();

        StringBuilder title = new StringBuilder();
        // Initialize nodes
        {
            monitor.subTask("Initialize nodes");
            for (DBNDatabaseNode node : nodes) {
                if (title.length() > 0) title.append(", ");
                title.append(node.getNodeFullName());
                node.initializeNode(null, initializeFinisher);
                monitor.worked(1);
            }
            while (initializedCount != nodes.size()) {
                if (initializeError != null) {
                    throw new DBException(initializeError.getMessage());
                }
                Thread.sleep(100);
                if (monitor.isCanceled()) {
                    throw new InterruptedException();
                }
            }
        }

        monitor.subTask("Compare " + title.toString());
        boolean compareLazyProperties = false;

        DBNDatabaseNode firstNode = nodes.get(0);
        List<ObjectPropertyDescriptor> properties = ObjectPropertyDescriptor.extractAnnotations(null, firstNode.getObject().getClass(), getDataSourceFilter(firstNode));
        for (ObjectPropertyDescriptor prop : properties) {
            if (prop.isLazy()) {
                compareLazyProperties = true;
                break;
            }
        }
        compareLazyProperties = compareLazyProperties && getSettings().isCompareLazyProperties();

        // Check should we read lazy properties
        try {
            boolean hasLazy = false;
            // Load all properties
            for (DBNDatabaseNode node : nodes) {
                if (monitor.isCanceled()) {
                    throw new InterruptedException();
                }
                DBSObject databaseObject = node.getObject();
                Map<IPropertyDescriptor, Object> nodeProperties = propertyValues.get(databaseObject);
                if (nodeProperties == null) {
                    nodeProperties = new IdentityHashMap<IPropertyDescriptor, Object>();
                    propertyValues.put(databaseObject, nodeProperties);
                }
                PropertyCollector propertySource = new PropertyCollector(databaseObject, compareLazyProperties);
                for (ObjectPropertyDescriptor prop : properties) {
                    if (prop.isLazy(databaseObject, true)) {
                        if (compareLazyProperties) {
                            synchronized (PROPS_LOCK) {
                                nodeProperties.put(prop, LAZY_VALUE);
                            }
                            // Initiate lazy value read
                            propertySource.getPropertyValue(databaseObject, prop);
                            hasLazy = true;
                        }
                    } else {
                        Object propertyValue = propertySource.getPropertyValue(databaseObject, prop);
                        synchronized (PROPS_LOCK) {
                            nodeProperties.put(prop, propertyValue);
                        }
                    }
                }
                monitor.worked(1);
            }

            // Wait for all lazy properties to load
            while (hasLazy) {
                if (monitor.isCanceled()) {
                    throw new InterruptedException();
                }

                Thread.sleep(50);
                synchronized (PROPS_LOCK) {
                    hasLazy = false;
                    for (Map<IPropertyDescriptor, Object> objectProps : propertyValues.values()) {
                        if (objectProps.values().contains(LAZY_VALUE)) {
                            hasLazy = true;
                            break;
                        }
                    }
                }
            }

            // Compare properties
            for (ObjectPropertyDescriptor prop : properties) {
                Object firstValue = null;
                for (DBNDatabaseNode node : nodes) {
                    DBSObject object = node.getObject();
                    Object value = propertyValues.get(object).get(prop);
                    if (firstValue == null) {
                        firstValue = value;
                    } else if (!CommonUtils.equalObjects(value, firstValue)) {
                        differentProps.add(prop);
                        break;
                    }
                }
            }
            if (!differentProps.isEmpty()) {
                // Add difference in report
            }

            compareChildren(monitor, nodes);


        } finally {
            if (lazyPropertyLoadListener != null) {
                PropertiesContributor.getInstance().removeLazyListener(lazyPropertyLoadListener);
            }
        }
    }

    private void compareChildren(DBRProgressMonitor monitor, List<DBNDatabaseNode> nodes) throws DBException, InterruptedException
    {
        // Compare children
        int nodeCount = nodes.size();
        List<List<DBNDatabaseNode>> allChildren = new ArrayList<List<DBNDatabaseNode>>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            DBNDatabaseNode node = nodes.get(i);
            allChildren.add(CommonUtils.safeList(node.getChildren(monitor)));
        }

        Set<String> allChildNames = new LinkedHashSet<String>();
        for (List<DBNDatabaseNode> childList : allChildren) {
            for (DBNDatabaseNode child : childList) {
                allChildNames.add(child.getNodeName());
            }
        }

        for (String childName : allChildNames) {
            int[] childIndexes = new int[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                childIndexes[i] = -1;
                List<DBNDatabaseNode> childList = allChildren.get(i);
                for (int k = 0; k < childList.size(); k++) {
                    DBNDatabaseNode child = childList.get(k);
                    if (child.getNodeName().equals(childName)) {
                        childIndexes[i] = k;
                        break;
                    }
                }
            }

            List<DBNDatabaseNode> nodesToCompare = new ArrayList<DBNDatabaseNode>(nodeCount);
            for (int i = 0; i < nodeCount; i++) {
                if (childIndexes[i] == -1) {
                    // Missing
                } else {
                    for (int k = 0; k < nodeCount; k++) {
                        if (k != i && childIndexes[k] != childIndexes[i]) {
                            // Wrong index - add to report
                            break;
                        }
                    }
                    nodesToCompare.add(allChildren.get(i).get(childIndexes[i]));
                }
            }
            if (nodesToCompare.size() > 1) {
                // Compare children recursively
                compareNodes(monitor, nodesToCompare);
            }
        }
    }


    private IFilter getDataSourceFilter(DBNDatabaseNode node)
    {
        DBPDataSource dataSource = node.getDataSourceContainer().getDataSource();
        if (dataSource == null) {
            return null;
        }
        IFilter filter = dataSourceFilters.get(dataSource);
        if (filter == null) {
            filter = new DataSourcePropertyFilter(dataSource);
            dataSourceFilters.put(dataSource, filter);
        }
        return filter;
    }

}