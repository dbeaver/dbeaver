/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.registry.NavigatorExtensionsRegistry;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.INavigatorNodeActionHandler;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.text.Format;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistics node renderer.
 * Draws connection type marker next to the item name.
 * Draws item statistics in the right part.
 */
public class StatisticsNavigatorNodeRenderer extends DefaultNavigatorNodeRenderer {

    private static final Log log = Log.getLog(StatisticsNavigatorNodeRenderer.class);
    private static final int PERCENT_FILL_WIDTH = 50;
    //public static final String ITEM_WIDTH_ATTR = "item.width";

    private final INavigatorModelView view;

    private static final ByteNumberFormat numberFormat = new ByteNumberFormat();

    private final Map<String, Format> classFormatMap = new HashMap<>();

    private static final Map<DBSObject, StatReadJob> statReaders = new IdentityHashMap<>();

    private Font fontItalic;
    private boolean isLinux;

    public StatisticsNavigatorNodeRenderer(INavigatorModelView view) {
        this.view = view;
        this.isLinux = !GeneralUtils.isWindows() && !GeneralUtils.isMacOS();
    }

    public INavigatorModelView getView() {
        return view;
    }

    public Font getFontItalic(Tree tree) {
        if (fontItalic == null) {
            fontItalic = UIUtils.modifyFont(tree.getFont(), SWT.ITALIC);
            tree.addDisposeListener(e -> UIUtils.dispose(fontItalic));
        }
        return fontItalic;
    }

    public void paintNodeDetails(DBNNode node, Tree tree, GC gc, Event event) {
        super.paintNodeDetails(node, tree, gc, event);

        Object element = event.item.getData();

        if (element instanceof DBNDatabaseNode) {
            if (element instanceof DBNDataSource) {
                int widthOccupied = 0;
                if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS)) {
                    widthOccupied += renderDataSourceNodeActions((DBNDatabaseNode) element, tree, gc, event);
                }
                if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME)) {
                    renderDataSourceHostName((DBNDataSource) element, tree, gc, event, widthOccupied);
                }
            }

            if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO)) {
                renderObjectStatistics((DBNDatabaseNode) element, tree, gc, event);
            }
        }
    }

    @Override
    public void showDetailsToolTip(DBNNode node, Tree tree, Event event) {
        String detailsTip = getDetailsTipText(node, tree, event);
        if (detailsTip != null) {
            tree.setToolTipText(detailsTip);
        } else {
            tree.setToolTipText(null);
        }
    }

    private String getDetailsTipText(DBNNode element, Tree tree, Event event) {
        if (element instanceof DBNDatabaseNode) {
            if (element instanceof DBNDataSource) {
                if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS)) {
                    // Detect active action
                    INavigatorNodeActionHandler overActionButton = getActionButtonFor(element, tree, event);
                    if (overActionButton != null) {
                        return overActionButton.getNodeActionToolTip(view, element);
                    }
                }
                if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME)) {
                    return getDataSourceHostText(((DBNDataSource) element).getDataSourceContainer().getConnectionConfiguration());
                }
                return null;
            }

            if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO)) {
                if (event.x > getTreeWidth(tree) - PERCENT_FILL_WIDTH) {
                    DBSObject object = ((DBNDatabaseNode) element).getObject();
                    if (object instanceof DBPObjectStatistics && ((DBPObjectStatistics) object).hasStatistics()) {
                        long statObjectSize = ((DBPObjectStatistics) object).getStatObjectSize();
                        if (statObjectSize > 0) {
                            String formattedSize;
                            try {
                                DBDDataFormatter formatter = object.getDataSource().getContainer().getDataFormatterProfile().createFormatter(DBDDataFormatter.TYPE_NAME_NUMBER, null);
                                formattedSize = formatter.formatValue(statObjectSize);
                            } catch (Exception e) {
                                formattedSize = String.valueOf(statObjectSize);
                            }
                            return "Object size on disk: " + formattedSize + " bytes";
                        }
                    }
                }
                //renderObjectStatistics((DBNDatabaseNode) element, tree, gc, event);
            }
        }
        return null;
    }

    private INavigatorNodeActionHandler getActionButtonFor(DBNNode element, Tree tree, Event event) {
        List<INavigatorNodeActionHandler> nodeActions = NavigatorExtensionsRegistry.getInstance().getNodeActions(getView(), element);
        int treeWidth = getTreeWidth(tree);
        int widthOccupied = 0;
        for (INavigatorNodeActionHandler nah : nodeActions) {
            if (!nah.isSticky(view, element)) {
                // Non-sticky buttons are active only for selected or hovered items
                boolean isSelected = (event.stateMask & SWT.SELECTED) != 0;
                boolean isHover = false;
                if (!isSelected && !isHover) {
                    return null;
                }
            }
            widthOccupied += 2; // Margin

            DBPImage icon = nah.getNodeActionIcon(getView(), element);
            if (icon != null) {
                Image image = DBeaverIcons.getImage(icon);

                Rectangle imageBounds = image.getBounds();
                int imageSize = imageBounds.height;
                widthOccupied += imageSize;

                if (event.x > treeWidth - widthOccupied) {
                    return nah;
                }
            }
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////
    // Host name

    private void renderDataSourceHostName(DBNDataSource element, Tree tree, GC gc, Event event, int widthOccupied) {
        DBPDataSourceContainer dataSourceContainer = element.getDataSourceContainer();
        DBPConnectionConfiguration configuration = dataSourceContainer.getConnectionConfiguration();
        if (!CommonUtils.isEmpty(configuration.getHostName())) {
            Font oldFont = gc.getFont();
            String hostText = getDataSourceHostText(configuration);
            if (CommonUtils.isEmpty(hostText)) {
                return;
            }
            DBPDataSourceContainer ds = element.getDataSourceContainer();
            Color bgColor = UIUtils.getConnectionColor(ds.getConnectionConfiguration());

            Color hostNameColor = tree.getDisplay().getSystemColor(
                (bgColor == null ? UIStyles.isDarkTheme() : UIUtils.isDark(bgColor.getRGB())) ?
                    SWT.COLOR_WIDGET_NORMAL_SHADOW : SWT.COLOR_WIDGET_DARK_SHADOW);
            gc.setForeground(hostNameColor);
            Font hostNameFont = getFontItalic(tree);
            gc.setFont(hostNameFont);
            Point hostTextSize = gc.stringExtent(hostText);

            int xOffset = isLinux ? 16 : 2;
            int treeWidth = getTreeWidth(tree);

            gc.setClipping(
                event.x + event.width + xOffset,
                event.y + ((event.height - hostTextSize.y) / 2),
                treeWidth - (event.x + event.width + xOffset + widthOccupied),
                event.height
            );
            gc.drawText(" - " + hostText,
                event.x + event.width + xOffset,
                event.y + ((event.height - hostTextSize.y) / 2),
                true);
            gc.setFont(oldFont);
        }
    }

    @NotNull
    private String getDataSourceHostText(DBPConnectionConfiguration configuration) {
        String hostText = configuration.getHostName();
        // For localhost ry to get real host name from tunnel configuration
        if (CommonUtils.isEmpty(hostText) || hostText.equals("localhost") || hostText.equals("127.0.0.1")) {
            for (DBWHandlerConfiguration hc : configuration.getHandlers()) {
                if (hc.isEnabled() && hc.getType() == DBWHandlerType.TUNNEL) {
                    String tunnelHost = hc.getStringProperty(DBWHandlerConfiguration.PROP_HOST);
                    if (!CommonUtils.isEmpty(tunnelHost)) {
                        hostText = tunnelHost;
                        break;
                    }
                }
            }
        }
        return hostText;
    }

    ///////////////////////////////////////////////////////////////////
    // Node actions

    private int renderDataSourceNodeActions(DBNDatabaseNode element, Tree tree, GC gc, Event event) {
        List<INavigatorNodeActionHandler> nodeActions = NavigatorExtensionsRegistry.getInstance().getNodeActions(getView(), element);

        int xWidth = getTreeWidth(tree);
        int xPos = xWidth;
        int widthOccupied = 0;
        for (INavigatorNodeActionHandler nah : nodeActions) {
            if (!nah.isSticky(view, element)) {
                // Non-sticky buttons are active only for selected or hovered items
                boolean isSelected = (event.stateMask & SWT.SELECTED) != 0;
                boolean isHover = false;
                if (!isSelected && !isHover) {
                    return widthOccupied;
                }
            }
            widthOccupied += 2; // Margin

            DBPImage icon = nah.getNodeActionIcon(getView(), element);
            if (icon != null) {
                Image image = DBeaverIcons.getImage(icon);

                Rectangle imageBounds = image.getBounds();
                int imageSize = imageBounds.height;
                    // event.height * 2 / 3;
                xPos -= imageSize;
                widthOccupied += imageSize;
                //gc.fillRectangle(xPos, event.y + (event.height - imageSize) / 2, imageBounds.width, imageBounds.height);
                gc.drawImage(image, xPos, event.y + (event.height - imageSize) / 2);
//                gc.drawImage(image,
//                    0, 0, imageBounds.width, imageBounds.height,
//                    xPos, event.y + (event.height - imageSize) / 2, imageBounds.width, imageBounds.height);
            }
//            gc.setForeground(tree.getForeground());
//            int x = xWidth - textSize.x - 2;
//            gc.drawText(sizeText, x + 2, event.y, true);

        }
        //System.out.println(nodeActions);
        return widthOccupied;
    }

    ///////////////////////////////////////////////////////////////////
    // Statistics renderer

    private void renderObjectStatistics(DBNDatabaseNode element, Tree tree, GC gc, Event event) {
        DBSObject object = element.getObject();
        if (object instanceof DBPObjectStatistics) {
            String sizeText;
            int percentFull;
            boolean statsWasRead = false;
            DBNNode parentNode = getParentItem(element);
            DBSObject parentObject = parentNode instanceof DBNDatabaseNode ? DBUtils.getPublicObject(((DBNDatabaseNode) parentNode).getObject()) : null;
            if (parentObject instanceof DBPObjectStatisticsCollector) { // && !((DBPObjectStatisticsCollector) parentObject).isStatisticsCollected()
                statsWasRead = ((DBPObjectStatisticsCollector) parentObject).isStatisticsCollected();
            } else {
                // If there is no stats collector then do not check for stats presence
                // Because it will trigger stats read job which won't read any statistics (as there is no way to load it for individual object).
                statsWasRead = true;//((DBPObjectStatistics) object).hasStatistics();
            }

            long maxObjectSize = statsWasRead ? getMaxObjectSize((TreeItem) event.item) : -1;
            if (statsWasRead && maxObjectSize >= 0) {
                long statObjectSize = ((DBPObjectStatistics) object).getStatObjectSize();
                if (statObjectSize <= 0) {
                    // Empty or no size - nothing to show
                    return;
                }
                percentFull = maxObjectSize == 0 ? 0 : (int) (statObjectSize * 100 / maxObjectSize);
                if (percentFull < 0 || percentFull > 100) {
                    log.debug("Object stat > 100%!");
                    percentFull = 100;
                }
                Format format;
                synchronized (classFormatMap) {
                    format = classFormatMap.get(object.getClass().getName());
                    if (format == null) {
                        try {
                            Method getStatObjectSizeMethod = object.getClass().getMethod("getStatObjectSize");
                            Property propAnnotation = getStatObjectSizeMethod.getAnnotation(Property.class);
                            if (propAnnotation != null) {
                                Class<? extends Format> formatterClass = propAnnotation.formatter();
                                if (formatterClass != Format.class) {
                                    format = formatterClass.getConstructor().newInstance();
                                }
                            }
                        } catch (Exception e) {
                            log.debug(e);
                        }
                        if (format == null) {
                            format = numberFormat;
                        }
                        classFormatMap.put(object.getClass().getName(), format);
                    }
                }
                sizeText = format.format(statObjectSize);
            } else {
                sizeText = "...";
                percentFull = 0;
                if (parentNode instanceof DBNDatabaseNode) {
                    DBSObject realParentObject = DBUtils.getPublicObject(((DBNDatabaseNode)parentNode).getObject());
                    if (!readObjectStatistics(
                        element.getParentNode(),
                        realParentObject,
                        ((TreeItem) event.item).getParentItem())) {
                        return;
                    }
                }
            }
            Point textSize = gc.stringExtent(sizeText);
            textSize.x += 4;

            //int caWidth = tree.getClientArea().width;
            int occupiedWidth = event.x + event.width + 4;
            int xWidth = getTreeWidth(tree);

            if (xWidth - occupiedWidth > Math.max(PERCENT_FILL_WIDTH, textSize.x)) {
                {
                    CTabFolder tabFolder = UIUtils.getParentOfType(tree, CTabFolder.class);
                    Color fillColor = tabFolder == null ? UIStyles.getDefaultWidgetBackground() : tabFolder.getBackground();
                    gc.setBackground(fillColor);
                    int fillWidth = PERCENT_FILL_WIDTH * percentFull / 100 + 1;
                    int x = xWidth - fillWidth - 2;
                    gc.fillRectangle(x, event.y + 2, fillWidth, event.height - 4);
                }

                gc.setForeground(tree.getForeground());
                int x = xWidth - textSize.x - 2;
                gc.drawText(sizeText, x + 2, event.y + (event.height - textSize.y) / 2, true);
            }
        }
    }

    private DBNNode getParentItem(DBNDatabaseNode element) {
        DBNNode parentNode = element.getParentNode();
        while (parentNode instanceof DBNDatabaseFolder) {
            parentNode = parentNode.getParentNode();
        }
        return parentNode;
    }

    private long getMaxObjectSize(TreeItem item) {
        TreeItem parentItem = item.getParentItem();
        Object maxSize = parentItem.getData(DatabaseNavigatorTree.TREE_DATA_STAT_MAX_SIZE);
        if (maxSize instanceof Number) {
            return ((Number) maxSize).longValue();
        }
        return -1;
    }

    private boolean readObjectStatistics(DBNNode parentNode, DBSObject parentObject, TreeItem parentItem) {
        // Read stats always event if it is already collected.
        // Because we need to calc max object size anyway
        synchronized (statReaders) {
            StatReadJob statReadJob = statReaders.get(parentObject);
            if (statReadJob == null) {
                statReadJob = new StatReadJob(parentNode, parentObject, parentItem);
                statReaders.put(parentObject, statReadJob);
                statReadJob.schedule();
            }
        }
        return true;
    }

    private static class StatReadJob extends AbstractJob {

        private final DBNNode parentNode;
        private final DBSObject collector;
        private final TreeItem treeItem;

        StatReadJob(DBNNode parentNode, DBSObject collector, TreeItem treeItem) {
            super("Read statistics for " + DBUtils.getObjectFullName(collector, DBPEvaluationContext.UI));
            this.parentNode = parentNode;
            this.collector = collector;
            this.treeItem = treeItem;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                monitor.beginTask("Collect database statistics", 1);
                if (collector instanceof DBPObjectStatisticsCollector) {
                    // Parent object is not necessary is stats collector.
                    // E.g. table partition parent is table while stats collector is schema
                    ((DBPObjectStatisticsCollector) collector).collectObjectStatistics(monitor, false, false);
                }
                long maxStatSize = 0;

                if (parentNode instanceof DBNDatabaseNode) {
                    // Calculate max object size
                    DBNDatabaseNode[] children = ((DBNDatabaseNode)parentNode).getChildren(monitor);
                    if (children != null) {
                        for (DBNDatabaseNode childNode : children) {
                            DBSObject child = childNode.getObject();
                            if (child instanceof DBPObjectStatistics) {
                                long statObjectSize = ((DBPObjectStatistics) child).getStatObjectSize();
                                maxStatSize = Math.max(maxStatSize, statObjectSize);
                            }
                        }
                    }
                }

                long finalMaxStatSize = maxStatSize;
                UIUtils.asyncExec(() -> {
                    try {
                        if (!treeItem.isDisposed()) {
                            Object prevValue = treeItem.getData(DatabaseNavigatorTree.TREE_DATA_STAT_MAX_SIZE);
                            if (!CommonUtils.equalObjects(finalMaxStatSize, prevValue)) {
                                treeItem.setData(DatabaseNavigatorTree.TREE_DATA_STAT_MAX_SIZE, finalMaxStatSize);
                                treeItem.getParent().redraw();
                            }
                        }
                    } finally {
                        synchronized (statReaders) {
                            statReaders.remove(collector);
                        }
                    }
                });
            } catch (DBException e) {
                log.error(e);
            } finally {
                monitor.done();
            }
            return Status.OK_STATUS;
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Utils

    private int getTreeWidth(Tree tree) {
        int treeWidth;
        int xShift;
        ScrollBar hSB = tree.getHorizontalBar();
        if (hSB == null || !hSB.isVisible()) {
            treeWidth = tree.getClientArea().width;
            xShift = 0;
        } else {
            treeWidth = hSB.getMaximum();
            xShift = hSB.getSelection();
        }

        return treeWidth - xShift;
    }

}
