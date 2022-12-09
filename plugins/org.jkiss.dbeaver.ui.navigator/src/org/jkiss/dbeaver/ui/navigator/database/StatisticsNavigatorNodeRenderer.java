/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
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
import org.jkiss.dbeaver.utils.RuntimeUtils;
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

    private static final RGB HOST_NAME_FG_DARK = new RGB(140,140,140);
    private static final RGB HOST_NAME_FG_LIGHT = new RGB(105,105,105);

    // Disabled because of performance and a couple of glitches
    // Sometimes hover bg remains after mouse move
    private static final boolean PAINT_ACTION_HOVER = false;

    private final INavigatorModelView view;

    private static final ByteNumberFormat numberFormat = new ByteNumberFormat();

    private final Map<String, Format> classFormatMap = new HashMap<>();

    private static final Map<DBSObject, StatReadJob> statReaders = new IdentityHashMap<>();

    private Font fontItalic;

    public StatisticsNavigatorNodeRenderer(INavigatorModelView view) {
        this.view = view;
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

        boolean scrollEnabled = isHorizontalScrollbarEnabled(tree);
        Object element = event.item.getData();

        if (element instanceof DBNDatabaseNode) {
            final DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
            int widthOccupied = 0;
            if (element instanceof DBNDataSource) {
                if (!scrollEnabled && preferenceStore.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS)) {
                    widthOccupied += renderDataSourceNodeActions((DBNDatabaseNode) element, tree, gc, event);
                }
                if (preferenceStore.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME)) {
                    renderDataSourceHostName((DBNDataSource) element, tree, gc, event, widthOccupied);
                }
            }
            if (!scrollEnabled && preferenceStore.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO)) {
                widthOccupied += renderObjectStatistics((DBNDatabaseNode) element, tree, gc, event);
            }
            if (element instanceof DBNDatabaseItem && preferenceStore.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_OBJECTS_DESCRIPTION)) {
                renderObjectDescription((DBNDatabaseItem) element, tree, gc, event, widthOccupied);
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

    @Override
    public void performAction(DBNNode node, Tree tree, Event event, boolean defaultAction) {
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS)) {
            // Detect active action
            INavigatorNodeActionHandler overActionButton = getActionButtonFor(node, tree, event);
            if (overActionButton != null) {
                overActionButton.handleNodeAction(view, node, event, defaultAction);
            }
        }
    }

    @Override
    public void handleHover(DBNNode node, Tree tree, TreeItem item, Event event) {
        super.handleHover(node, tree, item, event);

        boolean scrollEnabled = isHorizontalScrollbarEnabled(tree);
        Object element = item.getData();

        if (element instanceof DBNDatabaseNode) {
            if (element instanceof DBNDataSource) {
                if (!scrollEnabled && DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS)) {
                    if (isOverActionButton((DBNDatabaseNode) element, tree, item, event.gc, event)) {
                        tree.setCursor(tree.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                        return;
                    }
                }
            }
        }
        tree.setCursor(null);
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
                    return DataSourceUtils.getDataSourceAddressText(((DBNDataSource) element).getDataSourceContainer());
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
        if (isHorizontalScrollbarEnabled(tree)) {
            return null;
        }
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

                if (event.x > tree.getClientArea().width - widthOccupied) {
                    return nah;
                }
            }
        }
        return null;
    }

    private void renderObjectDescription(@NotNull DBNDatabaseItem element, @NotNull Tree tree, @NotNull GC gc, @NotNull Event event, int widthOccupied) {
        final DBSObject object = element.getObject();
        if (object != null) {
            final String description = object.getDescription();
            if (!CommonUtils.isEmptyTrimmed(description)) {
                drawText(description, null, tree, gc, event, widthOccupied);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Host name

    private void renderDataSourceHostName(DBNDataSource element, Tree tree, GC gc, Event event, int widthOccupied) {
        final DBPDataSourceContainer container = element.getDataSourceContainer();
        final String text = DataSourceUtils.getDataSourceAddressText(container);
        final Color background = UIUtils.getConnectionColor(container.getConnectionConfiguration());
        drawText(text, background, tree, gc, event, widthOccupied);
    }

    private void drawText(@Nullable String text, @Nullable Color bgColor, Tree tree, GC gc, Event event, int widthOccupied) {
        if (!CommonUtils.isEmpty(text)) {
            Font oldFont = gc.getFont();

            Color hostNameColor = UIUtils.getSharedColor(
                (bgColor == null ? UIStyles.isDarkTheme() : UIUtils.isDark(bgColor.getRGB())) ?
                    HOST_NAME_FG_DARK : HOST_NAME_FG_LIGHT);
            gc.setForeground(hostNameColor);
            Font hostNameFont = getFontItalic(tree);
            gc.setFont(hostNameFont);
            Point hostTextSize = gc.stringExtent(text);

            int xOffset = RuntimeUtils.isLinux() ? 16 : 2;
            ScrollBar hSB = tree.getHorizontalBar();
            boolean scrollEnabled = (hSB != null && hSB.isVisible());

            if (!scrollEnabled) {
                // In case of visible scrollbar it must respect full scrollable area size
                int treeWidth = tree.getClientArea().width;

                gc.setClipping(
                    event.x + event.width + xOffset,
                    event.y + ((event.height - hostTextSize.y) / 2),
                    treeWidth - (event.x + event.width + xOffset + widthOccupied),
                    event.height
                );
            }
            gc.drawText(" - " + text,
                event.x + event.width + xOffset,
                event.y + ((event.height - hostTextSize.y) / 2),
                true);
            if (!scrollEnabled) {
                gc.setClipping((Rectangle) null);
            }
            gc.setFont(oldFont);
        }
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
                Point mousePos = tree.getDisplay().getCursorLocation();
                Point itemPos = tree.toDisplay(xPos, event.y + (event.height - imageSize) / 2);

                if (PAINT_ACTION_HOVER) {
                    if (mousePos.x >= itemPos.x - 1 && mousePos.x <= itemPos.x + imageBounds.width + 2 &&
                        mousePos.y > itemPos.y - 1 && mousePos.y < itemPos.y + imageBounds.height + 2) {
                        Color oldBackground = gc.getBackground();
                        Color overBG = UIUtils.getSharedColor(new RGB(200, 200, 255));
                        gc.setBackground(overBG);
                        gc.fillRoundRectangle(xPos - 1, event.y + (event.height - imageSize) / 2 - 1, imageBounds.width + 2, imageBounds.height + 2, 2, 2);
                        gc.setBackground(oldBackground);
                    }
                }
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

    private boolean isOverActionButton(DBNDatabaseNode element, Tree tree, TreeItem item, GC gc, Event event) {
        List<INavigatorNodeActionHandler> nodeActions = NavigatorExtensionsRegistry.getInstance().getNodeActions(getView(), element);
        int xPos = getTreeWidth(tree);
        for (INavigatorNodeActionHandler nah : nodeActions) {
            if (!nah.isSticky(view, element)) {
                continue;
            }
            DBPImage icon = nah.getNodeActionIcon(getView(), element);
            if (icon != null) {
                Image image = DBeaverIcons.getImage(icon);

                Rectangle imageBounds = image.getBounds();
                int imageSize = imageBounds.width;
                // event.height * 2 / 3;
                xPos -= imageSize;
                if (event.x >= xPos && event.x < xPos + imageSize) {
                    return true;
                }
            }
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////
    // Statistics renderer

    private int renderObjectStatistics(DBNDatabaseNode element, Tree tree, GC gc, Event event) {
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
                    return 0;
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
                        return 0;
                    }
                }
            }
            Point textSize = gc.stringExtent(sizeText);
            textSize.x += 4;

            //int caWidth = tree.getClientArea().width;
            int occupiedWidth = event.x + event.width + 4;
            int xWidth = getTreeWidth(tree);

            if (xWidth - occupiedWidth > Math.max(PERCENT_FILL_WIDTH, textSize.x)) {
                CTabFolder tabFolder = UIUtils.getParentOfType(tree, CTabFolder.class);
                Color fillColor = tabFolder == null ? UIStyles.getDefaultWidgetBackground() : tabFolder.getBackground();

                // Frame
                gc.setForeground(fillColor);
                gc.drawRectangle(xWidth - PERCENT_FILL_WIDTH - 2, event.y + 1, PERCENT_FILL_WIDTH, event.height - 3);

                // Bar
                final int width = Math.max((int) Math.ceil((PERCENT_FILL_WIDTH - 3) * percentFull / 100.0), 1);
                gc.setBackground(fillColor);
                gc.fillRectangle(xWidth - PERCENT_FILL_WIDTH, event.y + 3, width, event.height - 6);

                // Text
                gc.setForeground(tree.getForeground());
                gc.setFont(tree.getFont());
                gc.drawText(sizeText, xWidth - textSize.x, event.y + (event.height - textSize.y) / 2, true);

                return Math.max(PERCENT_FILL_WIDTH, textSize.x);
            }
        }

        return 0;
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

    private static boolean isHorizontalScrollbarEnabled(Tree tree) {
        ScrollBar horizontalBar = tree.getHorizontalBar();
        if (horizontalBar == null) {
            return false;
        }
        if (RuntimeUtils.isLinux()) {
            return tree.getClientArea().width != horizontalBar.getMaximum();
        }
        return horizontalBar.isVisible();
    }
}
