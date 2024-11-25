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

package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.registry.NavigatorExtensionsRegistry;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.INavigatorNodeActionHandler;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.text.Format;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Statistics node renderer.
 * Draws connection type marker next to the item name.
 * Draws item statistics in the right part.
 */
public class StatisticsNavigatorNodeRenderer extends DefaultNavigatorNodeRenderer {
    private static final Log log = Log.getLog(StatisticsNavigatorNodeRenderer.class);

    private static final int ELEMENT_MARGIN = 3;
    private static final int PERCENT_FILL_WIDTH = 50;

    private static final String HOST_NAME_FOREGROUND_COLOR = "org.jkiss.dbeaver.ui.navigator.node.foreground";
    private static final String TABLE_STATISTICS_BACKGROUND_COLOR = "org.jkiss.dbeaver.ui.navigator.node.statistics.background";

    // Disabled because of performance and a couple of glitches
    // Sometimes hover bg remains after mouse move
    private static final boolean PAINT_ACTION_HOVER = false;

    private final INavigatorModelView view;

    private static final ByteNumberFormat numberFormat = new ByteNumberFormat();

    private final Map<String, Format> classFormatMap = new HashMap<>();

    private static final Map<DBSObject, StatReadJob> statReaders = new IdentityHashMap<>();

    private final IPropertyChangeListener themeChangeListener;
    private Font fontItalic;
    private Color hostNameColor;
    private Color statisticsFrameColor;

    public StatisticsNavigatorNodeRenderer(INavigatorModelView view) {
        this.view = view;
        this.themeChangeListener = e -> {
            final ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
            fontItalic = theme.getFontRegistry().getItalic(DatabaseNavigatorLabelProvider.TREE_TABLE_FONT);
            hostNameColor = theme.getColorRegistry().get(HOST_NAME_FOREGROUND_COLOR);
            statisticsFrameColor = theme.getColorRegistry().get(TABLE_STATISTICS_BACKGROUND_COLOR);
        };
        this.themeChangeListener.propertyChange(null);

        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeChangeListener);

        view.getNavigatorViewer().getControl().addDisposeListener(e -> {
            PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeChangeListener);
        });
    }

    public INavigatorModelView getView() {
        return view;
    }

    @Override
    public void paintNodeDetails(DBNNode node, Tree tree, GC gc, Event event) {
        super.paintNodeDetails(node, tree, gc, event);

        if (!(node instanceof DBNDatabaseNode databaseNode)) {
            return;
        }

        Rectangle client = getClientArea(tree);
        Rectangle item = getItemBounds(client, (TreeItem) event.item);

        boolean hovering = (event.detail & SWT.HOT) != 0;

        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        if (node instanceof DBNDataSource dataSourceNode) {
            if (store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS)) {
                drawDataSourceActions(gc, dataSourceNode, item, client, hovering);
            }
            if (store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME)) {
                drawDataSourceAddress(gc, dataSourceNode, item);
            }
        } else {
            if (store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO)) {
                drawObjectStatistics(gc, databaseNode, item, event);
            }
            if (node instanceof DBNDatabaseItem && store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_OBJECTS_DESCRIPTION)) {
                drawObjectDescription(gc, databaseNode, item);
            }
        }
    }

    private void drawDataSourceAddress(@NotNull GC gc, @NotNull DBNDataSource node, @NotNull Rectangle bounds) {
        drawText(gc, DataSourceUtils.getDataSourceAddressText(node.getDataSourceContainer()), bounds);
    }

    private void drawDataSourceActions(
        @NotNull GC gc,
        @NotNull DBNDataSource node,
        @NotNull Rectangle item,
        @NotNull Rectangle client,
        boolean hovering
    ) {
        var actions = NavigatorExtensionsRegistry.getInstance().getNodeActions(getView(), node);
        if (actions.isEmpty()) {
            return;
        }

        // Compute width required to draw all actions
        int width = (actions.size() - 1) * ELEMENT_MARGIN;
        for (INavigatorNodeActionHandler action : actions) {
            Image image = DBeaverIcons.getImage(action.getNodeActionIcon(getView(), node));
            Rectangle size = image.getBounds();
            width += size.width;
        }

        // Allow to show action button even if it does not fit into the bounds
        boolean overdraw = hovering && item.width < width;
        Rectangle bounds = item;
        if (overdraw) {
            bounds = new Rectangle(client.x, bounds.y, client.width, bounds.height);
        }

        // Draw actions
        for (int i = actions.size() - 1; i >= 0; i--) {
            INavigatorNodeActionHandler action = actions.get(i);
            Image image = DBeaverIcons.getImage(action.getNodeActionIcon(getView(), node));
            Rectangle size = image.getBounds();

            if (bounds.width < size.width) {
                return;
            }

            if (overdraw) {
                gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
                gc.fillRectangle(bounds.x + bounds.width - size.width - ELEMENT_MARGIN, bounds.y, size.width + ELEMENT_MARGIN * 2, bounds.height);
            }

            gc.drawImage(image, bounds.x + bounds.width - size.width, bounds.y + (bounds.height - size.height) / 2);
            bounds.width -= size.width + ELEMENT_MARGIN;
        }

        // Draw delimiter between whatever is left from the tree item and the actions
        if (overdraw) {
            gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
            gc.fillRectangle(bounds.x + bounds.width - 1, bounds.y, 1, bounds.height);
            item.width -= client.width - bounds.width;
        }
    }

    private void drawObjectDescription(@NotNull GC gc, @NotNull DBNDatabaseNode node, @NotNull Rectangle bounds) {
        DBSObject object = node.getObject();
        if (object != null) {
            String description = object.getDescription();
            if (!CommonUtils.isEmptyTrimmed(description)) {
                drawText(gc, CommonUtils.getSingleLineString(description), bounds);
            }
        }
    }

    private void drawObjectStatistics(@NotNull GC gc, @NotNull DBNDatabaseNode node, @NotNull Rectangle bounds, @NotNull Event event) {
        if (bounds.width < PERCENT_FILL_WIDTH) {
            return;
        }

        ObjectStatistics statistics = getObjectStatistics(node, event);
        if (statistics == null) {
            return;
        }

        String text;
        int percentFull;

        if (statistics instanceof ObjectStatistics.Known known) {
            text = known.format.format(known.statObjectSize);
            percentFull = known.maxObjectSize == 0 ? 0 : (int) (known.statObjectSize * 100 / known.maxObjectSize);
        } else {
            text = "...";
            percentFull = 0;
        }

        Point textSize = gc.textExtent(text);
        Tree tree = (Tree) event.widget;

        // Frame
        gc.setForeground(statisticsFrameColor);
        gc.drawRectangle(bounds.x + bounds.width - PERCENT_FILL_WIDTH, bounds.y + 1, PERCENT_FILL_WIDTH, bounds.height - 3);

        // Bar
        int width = Math.max((int) Math.ceil((PERCENT_FILL_WIDTH - 3) * percentFull / 100.0), 1);
        gc.setBackground(statisticsFrameColor);
        gc.fillRectangle(bounds.x + bounds.width - PERCENT_FILL_WIDTH, bounds.y + 3, width, bounds.height - 6);

        // Text
        if (UIStyles.isDarkHighContrastTheme() && PERCENT_FILL_WIDTH - width < PERCENT_FILL_WIDTH / 2) {
            gc.setForeground(tree.getBackground());
        } else {
            gc.setForeground(tree.getForeground());
        }
        gc.setFont(tree.getFont());
        gc.drawText(text, bounds.x + bounds.width - textSize.x, bounds.y + (bounds.height - textSize.y) / 2, true);

        bounds.width -= PERCENT_FILL_WIDTH + ELEMENT_MARGIN;
    }

    private void drawText(@NotNull GC gc, @NotNull String text, @NotNull Rectangle bounds) {
        if (text.isEmpty()) {
            return;
        }

        Color foreground = gc.getForeground();
        Font font = gc.getFont();

        try {
            gc.setForeground(hostNameColor);
            gc.setFont(fontItalic);

            drawTextClipped(gc, text, bounds);
        } finally {
            gc.setFont(font);
            gc.setForeground(foreground);
        }
    }

    private static void drawTextClipped(@NotNull GC gc, @NotNull String text, @NotNull Rectangle bounds) {
        Point extent = gc.textExtent(text);

        if (extent.x > bounds.width) {
            int low = 0;
            int high = text.length();
            String clipped = text;

            while (low <= high) {
                int mid = (low + high) >>> 1;
                clipped = text.substring(0, mid);
                int ext = gc.textExtent(clipped + "...").x;

                if (ext < bounds.width) {
                    low = mid + 1;
                } else if (ext > bounds.width) {
                    high = mid - 1;
                } else {
                    break;
                }
            }

            if (clipped.isEmpty()) {
                return;
            }

            drawTextSegment(gc, clipped, bounds);
            drawTextSegment(gc, "...", bounds);
        } else {
            drawTextSegment(gc, text, bounds);
        }
    }

    private static void drawTextSegment(@NotNull GC gc, @NotNull String text, @NotNull Rectangle bounds) {
        Point extent = gc.textExtent(text);
        gc.drawText(text, bounds.x, bounds.y + (bounds.height - extent.y) / 2, true);
        bounds.x += extent.x;
        bounds.width -= extent.x;
    }

    @NotNull
    private static Rectangle getClientArea(@NotNull Tree tree) {
        Rectangle client = tree.getClientArea();
        client.x += ELEMENT_MARGIN;
        client.width -= ELEMENT_MARGIN * 2;
        return client;
    }

    @NotNull
    private static Rectangle getItemBounds(@NotNull Rectangle client, @NotNull TreeItem item) {
        Rectangle bounds = item.getBounds();
        return new Rectangle(
            client.x + bounds.x + bounds.width,
            bounds.y,
            client.width - bounds.x - bounds.width,
            bounds.height
        );
    }

    @Nullable
    @Override
    public String getToolTipText(@NotNull DBNNode node, @NotNull Tree tree, @NotNull Event event) {
        if (node instanceof DBNDatabaseNode node1) {
            if (node instanceof DBNDataSource dataSource) {
                INavigatorNodeActionHandler overActionButton = getActionButton(node, tree, event);
                if (overActionButton != null) {
                    return overActionButton.getNodeActionToolTip(view, node);
                }
                if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME)) {
                    return DataSourceUtils.getDataSourceAddressText(dataSource.getDataSourceContainer());
                }
                return null;
            }

            if (isOverObjectStatistics(node1, tree, event)) {
                DBSObject object = node1.getObject();
                if (object instanceof DBPObjectStatistics statistics && statistics.hasStatistics()) {
                    long statObjectSize = statistics.getStatObjectSize();
                    if (statObjectSize > 0) {
                        String formattedSize;
                        try {
                            DBDDataFormatterProfile profile = object.getDataSource().getContainer().getDataFormatterProfile();
                            DBDDataFormatter formatter = profile.createFormatter(DBDDataFormatter.TYPE_NAME_NUMBER, null);
                            formattedSize = formatter.formatValue(statObjectSize);
                        } catch (Exception e) {
                            formattedSize = String.valueOf(statObjectSize);
                        }
                        return NLS.bind("Object size on disk: {0} bytes", formattedSize);
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void performAction(DBNNode node, Tree tree, Event event, boolean defaultAction) {
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS)) {
            // Detect active action
            INavigatorNodeActionHandler overActionButton = getActionButton(node, tree, event);
            if (overActionButton != null) {
                overActionButton.handleNodeAction(view, node, event, defaultAction);
            }
        }
    }

    @Nullable
    @Override
    public Cursor getCursor(@NotNull DBNNode node, @NotNull Tree tree, @NotNull Event event) {
        if (node instanceof DBNDataSource n && isOverActionButton(n, tree, event)) {
            return tree.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
        } else if (node instanceof DBNDatabaseNode n && isOverObjectStatistics(n, tree, event)) {
            return tree.getDisplay().getSystemCursor(SWT.CURSOR_HELP);
        } else {
            return null;
        }
    }

    private boolean isOverActionButton(@NotNull DBNNode node, @NotNull Tree tree, @NotNull Event event) {
        return getActionButton(node, tree, event) != null;
    }

    @Nullable
    private INavigatorNodeActionHandler getActionButton(@NotNull DBNNode node, @NotNull Tree tree, @NotNull Event event) {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS)) {
            return null;
        }

        var actions = NavigatorExtensionsRegistry.getInstance().getNodeActions(getView(), node);
        if (actions.isEmpty()) {
            return null;
        }

        var item = tree.getItem(new Point(event.x, event.y));
        if (item == null) {
            return null;
        }

        Rectangle bounds = item.getBounds();
        Rectangle client = getClientArea(tree);
        client.y = bounds.y;
        client.height = bounds.height;

        for (int i = actions.size() - 1; i >= 0; i--) {
            INavigatorNodeActionHandler action = actions.get(i);
            Image image = DBeaverIcons.getImage(action.getNodeActionIcon(getView(), node));
            Rectangle size = image.getBounds();

            if (client.width < size.width || event.y < client.y || event.y >= client.y + client.height) {
                return null;
            }

            if (event.x >= client.x + client.width - size.width && event.x < client.x + client.width) {
                return action;
            }

            client.width -= size.width + ELEMENT_MARGIN;
        }

        return null;
    }

    private boolean isOverObjectStatistics(@NotNull DBNDatabaseNode node, @NotNull Tree tree, @NotNull Event event) {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO)) {
            return false;
        }

        if (!(node.getObject() instanceof DBPObjectStatistics statistics) || !statistics.hasStatistics() || statistics.getStatObjectSize() <= 0) {
            return false;
        }

        var treeItem = tree.getItem(new Point(event.x, event.y));
        if (treeItem == null) {
            return false;
        }

        Rectangle client = getClientArea(tree);
        Rectangle item = getItemBounds(client, treeItem);

        return event.x < item.x + item.width
            && event.x >= item.x + item.width - PERCENT_FILL_WIDTH
            && item.width >= PERCENT_FILL_WIDTH;
    }

    @Nullable
    private ObjectStatistics getObjectStatistics(@NotNull DBNDatabaseNode node, @NotNull Event event) {
        DBSObject object = node.getObject();
        if (!(object instanceof DBPObjectStatistics statistics)) {
            return null;
        }

        boolean statsWasRead;
        DBNNode parentNode = getParentItem(node);
        DBSObject parentObject = parentNode instanceof DBNDatabaseNode pn ? DBUtils.getPublicObject(pn.getObject()) : null;
        if (parentObject instanceof DBPObjectStatisticsCollector) { // && !((DBPObjectStatisticsCollector) parentObject).isStatisticsCollected()
            statsWasRead = ((DBPObjectStatisticsCollector) parentObject).isStatisticsCollected();
        } else {
            // If there is no stats collector then do not check for stats presence
            // Because it will trigger stats read job which won't read any statistics (as there is no way to load it for individual object).
            statsWasRead = true;// statistics.hasStatistics();
        }

        long maxObjectSize = statsWasRead ? getMaxObjectSize((TreeItem) event.item) : -1;
        if (statsWasRead && maxObjectSize >= 0) {
            long statObjectSize = statistics.getStatObjectSize();
            if (statObjectSize <= 0) {
                // Empty or no size - nothing to show
                return null;
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
            return new ObjectStatistics.Known(statObjectSize, maxObjectSize, format);
        } else if (parentNode instanceof DBNDatabaseNode) {
            DBSObject realParentObject = DBUtils.getPublicObject(((DBNDatabaseNode) parentNode).getObject());
            TreeItem parentItem = ((TreeItem) event.item).getParentItem();
            getObjectStatistics(node.getParentNode(), realParentObject, parentItem);
        }

        return new ObjectStatistics.Unknown();
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
        if (parentItem != null) {
            Object maxSize = parentItem.getData(DatabaseNavigatorTree.TREE_DATA_STAT_MAX_SIZE);
            if (maxSize instanceof Number) {
                return ((Number) maxSize).longValue();
            }
        }
        return -1;
    }

    private void getObjectStatistics(DBNNode parentNode, DBSObject parentObject, TreeItem parentItem) {
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
    }

    private sealed interface ObjectStatistics {
        record Unknown() implements ObjectStatistics {
        }

        record Known(long statObjectSize, long maxObjectSize, @NotNull Format format) implements ObjectStatistics {
        }
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
                        if (treeItem != null && !treeItem.isDisposed()) {
                            Object prevValue = treeItem.getData(DatabaseNavigatorTree.TREE_DATA_STAT_MAX_SIZE);
                            /*if (!CommonUtils.equalObjects(finalMaxStatSize, prevValue)) */{
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
                log.debug(e);
            } finally {
                monitor.done();
            }
            return Status.OK_STATUS;
        }
    }
}
