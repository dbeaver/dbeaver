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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
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

import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class CompareObjectsWizard extends Wizard implements IExportWizard {

    private static final String RS_COMPARE_WIZARD_DIALOG_SETTINGS = "CompareWizard";//$NON-NLS-1$

    private CompareObjectsSettings settings;
    private Map<DBPDataSource, IFilter> dataSourceFilters = new IdentityHashMap<DBPDataSource, IFilter>();
    private DBRProcessListener initializeFinisher;
    private volatile int initializedCount = 0;
    private volatile IStatus initializeError;
    private final Object PROPS_LOCK = new Object();

    private final static Object LAZY_VALUE = new Object();

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
        } catch (InvocationTargetException e) {
            if (initializeError != null) {
                showError(initializeError.getMessage());
            } else {
                showError(e.getTargetException().getMessage());
            }
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        // Done
        return true;
    }

    private void compareNodes(DBRProgressMonitor monitor, List<DBNDatabaseNode> nodes)
        throws DBException, InterruptedException
    {
        StringBuilder title = new StringBuilder();
        // Initialize nodes
        {
            monitor.subTask("Initialize nodes");
            this.initializedCount = 0;
            this.initializeError = null;
            for (DBNDatabaseNode node : nodes) {
                if (title.length() > 0) title.append(", ");
                title.append(node.getNodeName());
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

        final Map<Object, Map<IPropertyDescriptor, Object>> propertyValues = new IdentityHashMap<Object, Map<IPropertyDescriptor, Object>>();
        // Check should we read lazy properties
        ILazyPropertyLoadListener lazyPropertyLoadListener = null;
        if (compareLazyProperties) {
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

            }

            // Compare children

        } finally {
            if (lazyPropertyLoadListener != null) {
                PropertiesContributor.getInstance().removeLazyListener(lazyPropertyLoadListener);
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