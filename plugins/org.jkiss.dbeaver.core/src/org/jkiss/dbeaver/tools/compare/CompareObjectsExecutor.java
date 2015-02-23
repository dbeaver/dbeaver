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
package org.jkiss.dbeaver.tools.compare;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.properties.DataSourcePropertyFilter;
import org.jkiss.dbeaver.ui.properties.ILazyPropertyLoadListener;
import org.jkiss.dbeaver.ui.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.editors.entity.properties.PropertiesContributor;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class CompareObjectsExecutor {

    private final Object PROPS_LOCK = new Object();
    private final static Object LAZY_VALUE = new Object();

    private CompareObjectsSettings settings;

    private final List<DBNDatabaseNode> rootNodes;
    private final Map<DBPDataSource, DataSourcePropertyFilter> dataSourceFilters = new IdentityHashMap<DBPDataSource, DataSourcePropertyFilter>();

    private final DBRProcessListener initializeFinisher;
    private final ILazyPropertyLoadListener lazyPropertyLoadListener;

    private volatile int initializedCount = 0;
    private volatile IStatus initializeError;
    private final Map<Object, Map<IPropertyDescriptor, Object>> propertyValues = new IdentityHashMap<Object, Map<IPropertyDescriptor, Object>>();

    private final List<CompareReportLine> reportLines = new ArrayList<CompareReportLine>();
    private int reportDepth = 0;
    private CompareReportLine lastLine;

    private void reportObjectsCompareBegin(List<DBNDatabaseNode> objects)
    {
        reportDepth++;
        lastLine = new CompareReportLine();
        lastLine.depth = reportDepth;
        lastLine.structure = objects.get(0);
        lastLine.nodes = new DBNDatabaseNode[rootNodes.size()];
        for (int i = 0; i < rootNodes.size(); i++) {
            for (DBNDatabaseNode node : objects) {
                if (node == rootNodes.get(i) || node.isChildOf(rootNodes.get(i))) {
                    lastLine.nodes[i] = node;
                    break;
                }
            }
        }
        for (DBNDatabaseNode node : lastLine.nodes) {
            if (node == null) {
                lastLine.hasDifference = true;
                break;
            }
        }
        reportLines.add(lastLine);
    }

    private void reportPropertyCompare(ObjectPropertyDescriptor property)
    {
        CompareReportProperty reportProperty = new CompareReportProperty(property);
        reportProperty.values = new Object[rootNodes.size()];
        for (int i = 0; i < lastLine.nodes.length; i++) {
            DBNDatabaseNode node = lastLine.nodes[i];
            if (node == null) {
                continue;
            }
            Map<IPropertyDescriptor, Object> valueMap = propertyValues.get(node.getObject());
            if (valueMap != null) {
                reportProperty.values[i] = valueMap.get(property);
            }
        }
        if (lastLine.properties == null) {
            lastLine.properties = new ArrayList<CompareReportProperty>();
        }
        lastLine.properties.add(reportProperty);

        Object firstValue = reportProperty.values[0];
        for (int i = 1; i < rootNodes.size(); i++) {
            if (!CompareUtils.equalPropertyValues(reportProperty.values[i], firstValue)) {
                lastLine.hasDifference = true;
                break;
            }
        }
    }

    private void reportObjectsCompareEnd()
    {
        reportDepth--;
    }

    public CompareObjectsExecutor(CompareObjectsSettings settings)
    {
        this.settings = settings;
        this.rootNodes = settings.getNodes();

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

    IStatus getInitializeError()
    {
        return initializeError;
    }

    void dispose()
    {
        PropertiesContributor.getInstance().removeLazyListener(lazyPropertyLoadListener);
    }

    public CompareReport compareObjects(DBRProgressMonitor monitor, List<DBNDatabaseNode> nodes)
        throws DBException, InterruptedException
    {
        reportLines.clear();
        lastLine = null;

        compareNodes(monitor, nodes);
        return new CompareReport(rootNodes, reportLines);
    }

    private void compareNodes(DBRProgressMonitor monitor, List<DBNDatabaseNode> nodes)
        throws DBException, InterruptedException
    {
        reportObjectsCompareBegin(nodes);

        try {
            if (nodes.size() > 1) {
                // Go deeper only if we have more than one node
                if (!settings.isCompareOnlyStructure() && !(nodes.get(0) instanceof DBNDatabaseFolder)) {
                    compareProperties(monitor, nodes);
                }

                compareChildren(monitor, nodes);
            }
        } finally {
            reportObjectsCompareEnd();
        }
    }

    private void compareProperties(DBRProgressMonitor monitor, List<DBNDatabaseNode> nodes) throws DBException, InterruptedException
    {
        // Clear compare singletons
        this.initializedCount = 0;
        this.initializeError = null;
        this.propertyValues.clear();

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
        List<ObjectPropertyDescriptor> properties = ObjectPropertyDescriptor.extractAnnotations(
            null,
            firstNode.getObject().getClass(),
            getDataSourceFilter(firstNode));
        for (ObjectPropertyDescriptor prop : properties) {
            if (prop.isLazy()) {
                compareLazyProperties = true;
                break;
            }
        }
        compareLazyProperties = compareLazyProperties && settings.isCompareLazyProperties();

        // Check should we read lazy properties
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
            reportPropertyCompare(prop);
        }
    }

    private void compareChildren(DBRProgressMonitor monitor, List<DBNDatabaseNode> nodes) throws DBException, InterruptedException
    {
        // Compare children
        int nodeCount = nodes.size();
        List<List<DBNDatabaseNode>> allChildren = new ArrayList<List<DBNDatabaseNode>>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            DBNDatabaseNode node = nodes.get(i);
            // Cache structure if possible
            if (node.getObject() instanceof DBSObjectContainer) {
                ((DBSObjectContainer) node.getObject()).cacheStructure(monitor, DBSObjectContainer.STRUCT_ALL);
            }
            allChildren.add(CommonUtils.safeList(node.getChildren(monitor)));
        }

        Set<String> allChildNames = new LinkedHashSet<String>();
        for (List<DBNDatabaseNode> childList : allChildren) {
            for (DBNDatabaseNode child : childList) {
                DBXTreeNode meta = child.getMeta();
                if (meta.isVirtual()) {
                    // Skip virtual nodes
                    continue;
                }
                if (settings.isSkipSystemObjects() && child.getObject() instanceof DBPSystemObject && ((DBPSystemObject) child.getObject()).isSystem()) {
                    // Skip system objects
                    continue;
                }
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
            // Compare children recursively
            compareNodes(monitor, nodesToCompare);
        }
    }


    private DataSourcePropertyFilter getDataSourceFilter(DBNDatabaseNode node)
    {
        DBPDataSource dataSource = node.getDataSourceContainer().getDataSource();
        if (dataSource == null) {
            return null;
        }
        DataSourcePropertyFilter filter = dataSourceFilters.get(dataSource);
        if (filter == null) {
            filter = new DataSourcePropertyFilter(dataSource);
            dataSourceFilters.put(dataSource, filter);
        }
        return filter;
    }

}