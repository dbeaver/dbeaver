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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ByteNumberFormat;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Default node renderer.
 * Draws connection type marker next to the item name.
 * Draws item statistics in the right part.
 */
public class DefaultNavigatorNodeRenderer implements DatabaseNavigatorItemRenderer {

    private static final Log log = Log.getLog(DefaultNavigatorNodeRenderer.class);
    public static final int PERCENT_FILL_WIDTH = 50;

    private final ByteNumberFormat numberFormat = new ByteNumberFormat();

    private static final Map<DBSObject, StatReadJob> statReaders = new IdentityHashMap<>();

    public void paintNodeDetails(DBNNode node, Tree tree, GC gc, Event event) {
        Color conColor = null;
        Object element = event.item.getData();
        if (element instanceof DBNDataSource) {
            DBPDataSourceContainer ds = ((DBNDataSource) element).getDataSourceContainer();
            conColor = UIUtils.getConnectionColor(ds.getConnectionConfiguration());
        }

        int treeWidth = tree.getClientArea().width;
        int occupiedWidth = 0;
        if (conColor != null) {
            int boxSize = event.height - 4;
            int x = event.x + event.width + 4;

            Point textSize = new Point(boxSize, boxSize);
            Color fg = tree.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
            gc.setForeground(fg);
            gc.setBackground(conColor);

            gc.fillRectangle(x, event.y + 2, textSize.x, textSize.y);
            gc.drawRectangle(x, event.y + 2, textSize.x - 1, textSize.y - 1);
            //gc.drawText(colorSettings, x, event.y);
            x += textSize.x + 4;
            occupiedWidth = x;
        } else {
            occupiedWidth = event.x + event.width + 4;
        }

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
                    if (((DBNDatabaseNode) element).getParentNode() instanceof DBNDatabaseNode) {
                        readObjectStatistics(
                            (DBNDatabaseNode) ((DBNDatabaseNode) element).getParentNode(),
                            ((TreeItem) event.item).getParentItem());
                    }
                }
                Point textSize = gc.stringExtent(sizeText);
                textSize.x += 4;
                if (treeWidth - occupiedWidth > Math.max(PERCENT_FILL_WIDTH, textSize.x)) {
                    int x = treeWidth - textSize.x - 2;
                    {
                        Color fillColor = UIStyles.getDefaultWidgetBackground();
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

    private void readObjectStatistics(DBNDatabaseNode parentNode, TreeItem parentItem) {
        DBSObject parentObject = parentNode.getObject();
        if (parentObject instanceof DBPObjectStatisticsCollector && !((DBPObjectStatisticsCollector) parentObject).isStatisticsCollected()) {
            synchronized (statReaders) {
                StatReadJob statReadJob = statReaders.get(parentObject);
                if (statReadJob == null) {
                    statReadJob = new StatReadJob(parentNode, parentObject, parentItem);
                    statReaders.put(parentObject, statReadJob);
                    statReadJob.schedule();
                }
            }
        }
    }

    private static class StatReadJob extends AbstractJob {

        private final DBNDatabaseNode node;
        private final DBSObject collector;
        private final TreeItem treeItem;

        StatReadJob(DBNDatabaseNode node, DBSObject collector, TreeItem treeItem) {
            super("Read statistics for " + DBUtils.getObjectFullName(collector, DBPEvaluationContext.UI));
            this.node = node;
            this.collector = collector;
            this.treeItem = treeItem;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                ((DBPObjectStatisticsCollector)collector).collectObjectStatistics(monitor, false, false);
                long maxStatSize = 0;
                if (node.getParentNode() != null) {
                    // Calculate max object size
                    DBNDatabaseNode[] children = node.getChildren(monitor);
                    if (children != null) {
                        for (DBNDatabaseNode childNode : children) {
                            if (childNode.getObject() instanceof DBPObjectStatistics) {
                                long statObjectSize = ((DBPObjectStatistics) childNode.getObject()).getStatObjectSize();
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
