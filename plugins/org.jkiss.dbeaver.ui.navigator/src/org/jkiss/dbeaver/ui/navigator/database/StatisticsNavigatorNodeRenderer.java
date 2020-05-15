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
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.utils.ByteNumberFormat;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Statistics node renderer.
 * Draws connection type marker next to the item name.
 * Draws item statistics in the right part.
 */
public class StatisticsNavigatorNodeRenderer extends DefaultNavigatorNodeRenderer {

    private static final Log log = Log.getLog(StatisticsNavigatorNodeRenderer.class);
    private static final int PERCENT_FILL_WIDTH = 50;

    private final ByteNumberFormat numberFormat = new ByteNumberFormat();

    private static final Map<DBSObject, StatReadJob> statReaders = new IdentityHashMap<>();

    public void paintNodeDetails(DBNNode node, Tree tree, GC gc, Event event) {
        super.paintNodeDetails(node, tree, gc, event);
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO)) {
            return;
        }

        Object element = event.item.getData();

        if (element instanceof DBNDatabaseNode) {
            DBSObject object = ((DBNDatabaseNode) element).getObject();
            if (object instanceof DBPObjectStatistics) {
                String sizeText;
                int percentFull;
                if (((DBPObjectStatistics) object).hasStatistics()) {
                    // Draw object size
                    long maxObjectSize = getMaxObjectSize((TreeItem)event.item);
                    long statObjectSize = ((DBPObjectStatistics) object).getStatObjectSize();
                    percentFull = maxObjectSize == 0 ? 0 : (int) (statObjectSize * 100 / maxObjectSize);
                    if (percentFull > 100) {
                        log.debug("Object stat > 100%!");
                        percentFull = 100;
                    }
                    sizeText = numberFormat.format(statObjectSize);
                } else {
                    sizeText = "...";
                    percentFull = 0;
                    DBNNode parentNode = ((DBNDatabaseNode) element).getParentNode();
                    while (parentNode instanceof DBNDatabaseFolder) {
                        parentNode = parentNode.getParentNode();
                    }
                    if (parentNode instanceof DBNDatabaseNode) {
                        if (!readObjectStatistics(
                            (DBNDatabaseNode) parentNode,
                            ((TreeItem) event.item).getParentItem())) {
                            return;
                        }
                    }
                }
                Point textSize = gc.stringExtent(sizeText);
                textSize.x += 4;

                int treeWidth = tree.getClientArea().width;
                int occupiedWidth = event.x + event.width + 4;

                if (treeWidth - occupiedWidth > Math.max(PERCENT_FILL_WIDTH, textSize.x)) {
                    int x = treeWidth - textSize.x - 2;
                    {
                        CTabFolder tabFolder = UIUtils.getParentOfType(tree, CTabFolder.class);
                        Color fillColor = tabFolder == null ? UIStyles.getDefaultWidgetBackground() : tabFolder.getBackground();
                        gc.setBackground(fillColor);
                        int fillWidth = PERCENT_FILL_WIDTH * percentFull / 100 + 1;
                        gc.fillRectangle(treeWidth - fillWidth - 2, event.y + 2, fillWidth, event.height - 4);
                    }

                    gc.setForeground(tree.getForeground());
                    gc.drawText(sizeText, x + 2, event.y, true);
                }
            }
        }
    }

    private long getMaxObjectSize(TreeItem item) {
        TreeItem parentItem = item.getParentItem();
        Object maxSize = parentItem.getData("nav.stat.maxSize");
        if (maxSize instanceof Number) {
            return ((Number) maxSize).longValue();
        }
        return 0;
    }

    private boolean readObjectStatistics(DBNDatabaseNode parentNode, TreeItem parentItem) {
        DBSObject parentObject = DBUtils.getPublicObject(parentNode.getObject());
        if (parentObject instanceof DBPObjectStatisticsCollector && !((DBPObjectStatisticsCollector) parentObject).isStatisticsCollected()) {
            synchronized (statReaders) {
                StatReadJob statReadJob = statReaders.get(parentObject);
                if (statReadJob == null) {
                    statReadJob = new StatReadJob(parentObject, parentItem);
                    statReaders.put(parentObject, statReadJob);
                    statReadJob.schedule();
                }
            }
            return true;
        }
        return false;
    }

    private static class StatReadJob extends AbstractJob {

        private final DBSObject collector;
        private final TreeItem treeItem;

        StatReadJob(DBSObject collector, TreeItem treeItem) {
            super("Read statistics for " + DBUtils.getObjectFullName(collector, DBPEvaluationContext.UI));
            this.collector = collector;
            this.treeItem = treeItem;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                ((DBPObjectStatisticsCollector)collector).collectObjectStatistics(monitor, false, false);
                long maxStatSize = 0;

                if (collector instanceof DBSObjectContainer) {
                    // Calculate max object size
                    Collection<? extends DBSObject> children = ((DBSObjectContainer) collector).getChildren(monitor);
                    if (children != null) {
                        for (DBSObject child : children) {
                            if (child instanceof DBPObjectStatistics) {
                                long statObjectSize = ((DBPObjectStatistics) child).getStatObjectSize();
                                maxStatSize = Math.max(maxStatSize, statObjectSize);
                            }
                        }
                    }
                }

                long finalMaxStatSize = maxStatSize;
                UIUtils.asyncExec(() -> {
                    treeItem.setData("nav.stat.maxSize", finalMaxStatSize);
                    treeItem.getParent().redraw();
                });
            } catch (DBException e) {
                log.error(e);
            } finally {
                synchronized (statReaders) {
                    statReaders.remove(collector);
                }
            }
            return Status.OK_STATUS;
        }
    }

}
