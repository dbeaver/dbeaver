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
package org.jkiss.dbeaver.tools.compare.simple;

import org.eclipse.core.runtime.IStatus;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.SubTaskProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.properties.*;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class CompareObjectsExecutor {

    private static final Log log = Log.getLog(CompareObjectsExecutor.class);

    private final Object PROPS_LOCK = new Object();

    private CompareObjectsSettings settings;

    private final List<DBNDatabaseNode> rootNodes;
    private final Map<DBPDataSource, DataSourcePropertyFilter> dataSourceFilters = new IdentityHashMap<>();

    private final DBRProgressListener initializeFinisher;
    private final ILazyPropertyLoadListener lazyPropertyLoadListener;

    private volatile int initializedCount = 0;
    private volatile IStatus initializeError;
    private final Map<Object, Map<DBPPropertyDescriptor, Object>> propertyValues = new IdentityHashMap<>();

    private final List<CompareReportLine> reportLines = new ArrayList<>();
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
            Map<DBPPropertyDescriptor, Object> valueMap = propertyValues.get(node.getObject());
            if (valueMap != null) {
                reportProperty.values[i] = valueMap.get(property);
            }
        }
        if (lastLine.properties == null) {
            lastLine.properties = new ArrayList<>();
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

        initializeFinisher = new DBRProgressListener() {
            @Override
            public void onTaskFinished(IStatus status)
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
            public void handlePropertyLoad(Object object, DBPPropertyDescriptor property, Object propertyValue, boolean completed)
            {
                synchronized (propertyValues) {
                    Map<DBPPropertyDescriptor, Object> objectProps = propertyValues.get(object);
                    if (objectProps != null) {
                        objectProps.put(property, propertyValue);
                    }
                }
            }
        };
        PropertiesContributor.getInstance().addLazyListener(lazyPropertyLoadListener);
    }

    public IStatus getInitializeError()
    {
        return initializeError;
    }

    public void dispose()
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
                if (!(nodes.get(0) instanceof DBNDatabaseFolder)) {
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
        boolean onlyStruct = settings.isCompareOnlyStructure();

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
                Thread.sleep(50);
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
            ObjectPropertyDescriptor.getObjectClass(firstNode.getObject()),
            getDataSourceFilter(firstNode), null);
        for (ObjectPropertyDescriptor prop : properties) {
            if (prop.isLazy()) {
                compareLazyProperties = true;
                break;
            }
        }

        boolean compareScripts = compareLazyProperties && settings.isCompareScripts();
        compareLazyProperties = compareLazyProperties && settings.isCompareLazyProperties();

        if (onlyStruct && !compareScripts) {
            return;
        }
        
        boolean useExternalTool = settings.isUseExternalTool();
        String externalToolPath = settings.getExternalToolPath();

        // Objects for external compare
        Map<DBNDatabaseNode, DBPScriptObject> scriptObjects = new HashMap<>();         
        // Load all properties        
        for (DBNDatabaseNode node : nodes) {
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            DBSObject databaseObject = node.getObject();
            if ((useExternalTool) && (databaseObject instanceof DBPScriptObject)) {
                scriptObjects.put(node, (DBPScriptObject) databaseObject);
            }
            Map<DBPPropertyDescriptor, Object> nodeProperties = propertyValues.get(databaseObject);
            if (nodeProperties == null) {
                nodeProperties = new IdentityHashMap<>();
                propertyValues.put(databaseObject, nodeProperties);
            }
            PropertyCollector propertySource = new PropertyCollector(databaseObject, compareLazyProperties || compareScripts);
            for (ObjectPropertyDescriptor prop : properties) {
                boolean isScriptProperty = prop.getId().equals(DBConstants.PARAM_OBJECT_DEFINITION_TEXT) || prop.getId().equals(DBConstants.PARAM_EXTENDED_DEFINITION_TEXT);
                if (prop.isLazy()) {
                    if (!compareLazyProperties) {
                        if (compareScripts) {
                            // Only DBPScriptObject methods
                            if (!isScriptProperty) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                } else {
                    if (prop.isHidden()) {
                        continue;
                    }
                }
                if (onlyStruct && !isScriptProperty) {
                    continue;
                }
                Object propertyValue = propertySource.getPropertyValue(monitor, databaseObject, prop, true);
                synchronized (PROPS_LOCK) {
                    if (propertyValue instanceof DBPNamedObject) {
                        // Compare just object names
                        propertyValue = ((DBPNamedObject) propertyValue).getName();
                    }
                    nodeProperties.put(prop, propertyValue);
                }
            }
            monitor.worked(1);
        }

        // Compare properties
        for (ObjectPropertyDescriptor prop : properties) {
            reportPropertyCompare(prop);
        }
        // Call external compare
        if (useExternalTool) {
            ArrayList<String> filesToCompare = new ArrayList<String>();
            for (Map.Entry<DBNDatabaseNode, DBPScriptObject> entry : scriptObjects.entrySet()) {
                try {
                    DBNDatabaseNode node = entry.getKey();
                    DBPScriptObject sobj = entry.getValue();
                    StringBuilder fileName = new StringBuilder("obj");
                    fileName.append("-").append(CommonUtils.escapeIdentifier(node.getNodeFullName()));
                    fileName.append("-ddl-");
                    File objectFile = File.createTempFile(fileName.toString(), ".sql");                
                    HashMap<String, Object> hmapOptions = new HashMap<>();
                    hmapOptions.put(DBPScriptObject.OPTION_DDL_SEPARATE_FOREIGN_KEYS_STATEMENTS, true);
                    hmapOptions.put(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS, true);
                    hmapOptions.put(DBPScriptObject.OPTION_INCLUDE_COMMENTS, true);
                    hmapOptions.put(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS, true);
                    hmapOptions.put(DBPScriptObject.OPTION_INCLUDE_PARTITIONS, true);
                    hmapOptions.put("ddl.includePermissions", true);
                    String sqlText = sobj.getObjectDefinitionText(monitor, hmapOptions);
                    try (OutputStream outputStream = new FileOutputStream(objectFile)) {
                        outputStream.write(sqlText.getBytes());
                    }
                    filesToCompare.add(objectFile.getAbsolutePath());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (filesToCompare.size() > 1) {
                String[] params;
                //new String[] {"C:\\Program Files\\WinMerge\\WinMergeU.exe",
                if (filesToCompare.size() > 2) {
                    params = new String[] {
                            //"C:\\Program Files\\WinMerge\\WinMergeU.exe",
                            //"c:\\Program Files\\KDiff3\\kdiff3.exe",
                            externalToolPath,
                            filesToCompare.get(0), filesToCompare.get(1), filesToCompare.get(2)};
                } else {
                    params = new String[] {
                            externalToolPath,
                            filesToCompare.get(0), filesToCompare.get(1)};
                }
                try {
                    Runtime.getRuntime().exec(params);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }    
    }

    private void compareChildren(DBRProgressMonitor monitor, List<DBNDatabaseNode> nodes) throws DBException, InterruptedException
    {
        // Compare children
        int nodeCount = nodes.size();
        List<DBNDatabaseNode[]> allChildren = new ArrayList<>(nodeCount);
        // Use submonitor to avoid huge number of tasks
        DBRProgressMonitor subMonitor = new SubTaskProgressMonitor(monitor);
        for (int i = 0; i < nodeCount; i++) {
            DBNDatabaseNode node = nodes.get(i);
            // Cache structure if possible
            if (node.getObject() instanceof DBSObjectContainer) {
                ((DBSObjectContainer) node.getObject()).cacheStructure(subMonitor, DBSObjectContainer.STRUCT_ALL);
            }
            try {
                DBNDatabaseNode[] children = node.getChildren(subMonitor);
                allChildren.add(children);
            } catch (Exception e) {
                log.warn("Error reading child nodes for compare", e);
                allChildren.add(null);
            }
        }

        Set<String> allChildNames = new LinkedHashSet<>();
        for (DBNDatabaseNode[] childList : allChildren) {
            if (childList == null) continue;
            for (DBNDatabaseNode child : childList) {
                DBXTreeNode meta = child.getMeta();
                if (meta.isVirtual()) {
                    // Skip virtual nodes
                    continue;
                }
                if (settings.isSkipSystemObjects() && DBUtils.isSystemObject(child.getObject())) {
                    // Skip system objects
                    continue;
                }
                allChildNames.add(child.getNodeDisplayName());
            }
        }

        for (String childName : allChildNames) {
            int[] childIndexes = new int[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                childIndexes[i] = -1;
                DBNDatabaseNode[] childList = allChildren.get(i);
                if (childList == null) continue;
                for (int k = 0; k < childList.length; k++) {
                    DBNDatabaseNode child = childList[k];
                    if (child.getNodeDisplayName().equals(childName)) {
                        childIndexes[i] = k;
                        break;
                    }
                }
            }

            List<DBNDatabaseNode> nodesToCompare = new ArrayList<>(nodeCount);
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
                    final DBNDatabaseNode[] childList = allChildren.get(i);
                    if (childList != null) {
                        nodesToCompare.add(childList[childIndexes[i]]);
                    }
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