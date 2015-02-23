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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.policy.DiagramXYLayoutPolicy;

/**
 * Used to delegate between the GraphLayoutAuto and the GraphLayoutXY classes
 *
 * @author Serge Rieder
 */
public class DelegatingLayoutManager implements LayoutManager {

    private DiagramPart diagram;

    private LayoutManager activeLayoutManager;
    private GraphLayoutAuto graphLayoutManager;
    private GraphLayoutXY xyLayoutManager;

    public DelegatingLayoutManager(DiagramPart diagram)
    {
        this.diagram = diagram;
        this.graphLayoutManager = new GraphLayoutAuto(diagram);
        this.xyLayoutManager = new GraphLayoutXY(diagram);

        //use the graph layout manager as the initial delegate
        this.activeLayoutManager = this.graphLayoutManager;
    }

    //********************* layout manager methods methods
    // ****************************/

    public void rearrange(IFigure container)
    {
        graphLayoutManager.layout(container);
        xyLayoutManager.cleanupConstraints();
    }

    @Override
    public void layout(IFigure container)
    {
        EntityDiagram entityDiagram = diagram.getDiagram();

        try {
            if (entityDiagram.isLayoutManualDesired()) {

                if (activeLayoutManager != xyLayoutManager) {

                    if (entityDiagram.isLayoutManualAllowed() && !entityDiagram.isNeedsAutoLayout()) {

                        //	yes we are okay to start populating the table bounds
                        setLayoutManager(container, xyLayoutManager);
                        activeLayoutManager.layout(container);

                    } else {

                        // we first have to set the constraint data
                        if (diagram.setTableFigureBounds(true)) {
                            //we successfully set bounds for all the existing
                            // tables so we can start using xyLayout immediately
                            setLayoutManager(container, xyLayoutManager);
                            activeLayoutManager.layout(container);
                        } else {
                            //we did not - we still need to run autolayout once
                            // before we can set xyLayout
                            activeLayoutManager.layout(container);

                            //run this again so that it will work again next time
                            setLayoutManager(container, xyLayoutManager);
                        }

                    }

                } else {
                    setLayoutManager(container, xyLayoutManager);
                    activeLayoutManager.layout(container);
                }
            } else {
                setLayoutManager(container, graphLayoutManager);
                activeLayoutManager.layout(container);
            }
        }
        finally {
            if (!diagram.getChildren().isEmpty()) {
                entityDiagram.setNeedsAutoLayout(false);
            }
        }
    }

    @Override
    public Object getConstraint(IFigure child)
    {
        return activeLayoutManager.getConstraint(child);
    }

    @Override
    public Dimension getMinimumSize(IFigure container, int wHint, int hHint)
    {
        return activeLayoutManager.getMinimumSize(container, wHint, hHint);
    }

    @Override
    public Dimension getPreferredSize(IFigure container, int wHint, int hHint)
    {
        return activeLayoutManager.getPreferredSize(container, wHint, hHint);
    }

    @Override
    public void invalidate()
    {
        activeLayoutManager.invalidate();
    }

    @Override
    public void remove(IFigure child)
    {
        activeLayoutManager.remove(child);
    }

    @Override
    public void setConstraint(IFigure child, Object constraint)
    {
        activeLayoutManager.setConstraint(child, constraint);
    }

    public void setXYLayoutConstraint(IFigure child, Rectangle constraint)
    {
        xyLayoutManager.setConstraint(child, constraint);
    }

    //********************* protected and private methods
    // ****************************/

    /**
     * Sets the current active layout manager
     */
    private void setLayoutManager(IFigure container, LayoutManager layoutManager)
    {
        container.setLayoutManager(layoutManager);
        this.activeLayoutManager = layoutManager;
        if (layoutManager == xyLayoutManager) {
            diagram.installEditPolicy(EditPolicy.LAYOUT_ROLE, new DiagramXYLayoutPolicy());
        } else {
            diagram.installEditPolicy(EditPolicy.LAYOUT_ROLE, null);
        }
    }

    public LayoutManager getActiveLayoutManager()
    {
        return activeLayoutManager;
    }

}