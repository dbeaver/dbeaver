/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.views;

import org.jgraph.graph.CellViewRenderer;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.VertexRenderer;
import org.jgraph.graph.VertexView;
import org.jkiss.dbeaver.ext.erd.old.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.old.model.ERDTableColumn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.List;

/**
 * ERDEntityView
 */
public class ERDEntityView extends VertexView {

    private static transient EntityRenderer renderer = new EntityRenderer();
    private static java.awt.Color entityBackground;

    private static Stroke LINE_STROKE = new BasicStroke(1);

    private ERDTable table;

    public ERDEntityView(ERDTable table)
    {
        super(table);
        this.table = table;
    }

    public ERDTable getTable()
    {
        return table;
    }

    public CellViewRenderer getRenderer() {
        return renderer;
    }

    public static class EntityRenderer extends VertexRenderer { //JLabel implements CellViewRenderer {

        //transient protected boolean hasFocus, selected, preview, childrenSelected;
        //transient ERDEntityView view;

/*
        public Component getRendererComponent(
            JGraph graph,
            CellView view,
            boolean sel,
            boolean focus,
            boolean preview)
        {
            this.view = (ERDEntityView) view;
            this.hasFocus = focus;
            this.selected = sel;
            this.childrenSelected = graph.getSelectionModel().isChildrenSelected(view.getCell());
            this.preview = preview;
            setComponentOrientation(graph.getComponentOrientation());
            setOpaque(true);
            return this;
        }
*/

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width += d.height / 5;
            return d;
        }

        public ERDEntityView getView()
        {
            return (ERDEntityView) super.view;
        }

        public void paint(Graphics g) {
            int bw = 1;
            Graphics2D g2 = (Graphics2D) g;
            Dimension d = getSize();
            int roundRectArc = 20 - bw;
            int framePos = g.getFontMetrics().getHeight();
            int boxWidth = d.width - 1;
            int boxHeight = d.height - framePos - 1;
            int attrHeight = g.getFontMetrics().getHeight();
            ERDTable erdTable = getView().getTable();

            boolean isRect = erdTable.isIndependent();
            // Fill background
            if (super.isOpaque()) {
                if (entityBackground == null) {
                    //org.eclipse.swt.graphics.Color swtColor = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
                    // Use light yellow color to draw entities
                    entityBackground = new java.awt.Color(255, 255, 225);
                }
                g.setColor(entityBackground);
                if (isRect) {
                    g.fillRect(0, framePos, boxWidth, boxHeight);
                } else {
                    g.fillRoundRect(0, framePos, boxWidth, boxHeight, roundRectArc, roundRectArc);
                }
            }

            // Draw border
            {
                g.setColor(Color.BLACK);
                g2.setStroke(LINE_STROKE);
                if (isRect) {
                    g.drawRect(0, framePos, boxWidth, boxHeight);
                } else {
                    g.drawRoundRect(0, framePos, boxWidth, boxHeight, roundRectArc, roundRectArc);
                }
            }
            
            // Draw selection stroke
            if (selected) {
                g.setColor(Color.GRAY);
                g2.setStroke(GraphConstants.SELECTION_STROKE);
                if (isRect) {
                    g.drawRect(0, framePos, boxWidth, boxHeight);
                } else {
                    g.drawRoundRect(0, framePos, boxWidth, boxHeight, roundRectArc, roundRectArc);
                }
            }

            // Print entity name
            g.setColor(Color.BLACK);
            //g.setFont(ERDConstants.ENTITY_NAME_FONT);
            g.drawString(erdTable.getName(), 4, framePos - 4);

            // Print attributes and divider
            //g.setFont(ERDConstants.ENTITY_ATTR_FONT);
            List<ERDTableColumn> tableColumns = erdTable.getColumns();
            int attrPos = framePos + framePos - 4;
            boolean lastKey = true;
            if (tableColumns.isEmpty()) {
                // Just draw divider in the center
                g2.setStroke(LINE_STROKE);
                g2.drawLine(0, framePos + boxHeight / 2, boxWidth, framePos + boxHeight / 2);
            } else {
                // Print column names and divider between keys and non-key columns
                if (!tableColumns.get(0).isKey()) {
                    // No key columns - skip first line
                    attrPos += attrHeight;
                }
                for (ERDTableColumn tableColumn : tableColumns) {
                    if (lastKey && !tableColumn.isKey()) {
                        // Draw divider
                        g2.setStroke(LINE_STROKE);
                        g2.drawLine(0, attrPos - framePos + 8, boxWidth, attrPos - framePos + 8);
                        attrPos += 4;
                    }
                    g.drawString(tableColumn.getName(), 4, attrPos);
                    attrPos += attrHeight;
                    lastKey = tableColumn.isKey();
                }
            }

        }

    }
}
