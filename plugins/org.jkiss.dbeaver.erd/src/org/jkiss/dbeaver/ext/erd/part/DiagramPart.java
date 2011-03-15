/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ext.erd.figures.EntityDiagramFigure;
import org.jkiss.dbeaver.ext.erd.layout.DelegatingLayoutManager;
import org.jkiss.dbeaver.ext.erd.layout.GraphAnimation;
import org.jkiss.dbeaver.ext.erd.layout.GraphLayoutAuto;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.policy.DiagramContainerEditPolicy;
import org.jkiss.dbeaver.ui.UIUtils;

import java.beans.PropertyChangeEvent;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

/**
 * Edit part for Schema object, and uses a SchemaDiagram figure as
 * the container for all graphical objects
 *
 * @author Serge Rieder
 */
public class DiagramPart extends PropertyAwarePart {

    CommandStackListener stackListener = new CommandStackListener() {

        public void commandStackChanged(EventObject event)
        {
            if (delegatingLayoutManager.getActiveLayoutManager() instanceof GraphLayoutAuto) {
                if (!GraphAnimation.captureLayout(getFigure())) {
                    return;
                }
                while (GraphAnimation.step())
                    getFigure().getUpdateManager().performUpdate();
                GraphAnimation.end();
            } else {
                getFigure().getUpdateManager().performUpdate();
            }
        }
    };
    private DelegatingLayoutManager delegatingLayoutManager;
    private Font normalFont, boldFont, italicFont, boldItalicFont;


    /**
     * Adds this EditPart as a command stack listener, which can be used to call
     * performUpdate() when it changes
     */
    public void activate()
    {
        super.activate();
        getViewer().getEditDomain().getCommandStack().addCommandStackListener(stackListener);
    }

    /**
     * Removes this EditPart as a command stack listener
     */
    public void deactivate()
    {
        resetFonts();
        getViewer().getEditDomain().getCommandStack().removeCommandStackListener(stackListener);
        super.deactivate();
    }

    public void resetFonts()
    {
        UIUtils.dispose(boldFont);
        UIUtils.dispose(italicFont);
        UIUtils.dispose(boldItalicFont);
        normalFont = null;
        boldFont = null;
        italicFont = null;
        boldItalicFont = null;
    }

    protected IFigure createFigure()
    {
        Figure figure = new EntityDiagramFigure();
        delegatingLayoutManager = new DelegatingLayoutManager(this);
        figure.setLayoutManager(delegatingLayoutManager);

/*
        ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
        ViewportAwareConnectionLayerClippingStrategy clippingStrategy = new ViewportAwareConnectionLayerClippingStrategy(cLayer);
        figure.setClippingStrategy(clippingStrategy);
*/
        Control control = getViewer().getControl();
        ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
        if ((control.getStyle() & SWT.MIRRORED) == 0) {
            cLayer.setAntialias(SWT.ON);
        }

        FanRouter router = new FanRouter();
        router.setSeparation(15);
        //router.setNextRouter(new BendpointConnectionRouter());
        router.setNextRouter(new ShortestPathConnectionRouter(figure));
        //router.setNextRouter(new ManhattanConnectionRouter());
        //router.setNextRouter(new BendpointConnectionRouter());
        cLayer.setConnectionRouter(router);


        return figure;
    }

    public EntityDiagram getDiagram()
    {
        return (EntityDiagram) getModel();
    }

    public Font getNormalFont()
    {
        if (normalFont == null) {
            normalFont = getViewer().getControl().getFont();
        }
        return normalFont;
    }

    public Font getBoldFont()
    {
        if (boldFont == null) {
            boldFont = UIUtils.makeBoldFont(getNormalFont());
        }
        return boldFont;
    }

    public Font getItalicFont()
    {
        if (italicFont == null) {
            italicFont = UIUtils.modifyFont(getNormalFont(), SWT.ITALIC);
        }
        return italicFont;
    }

    public Font getBoldItalicFont()
    {
        if (boldItalicFont == null) {
            boldItalicFont = UIUtils.modifyFont(getNormalFont(), SWT.BOLD | SWT.ITALIC);
        }
        return boldItalicFont;
    }

    public void rearrangeDiagram()
    {
        //delegatingLayoutManager.set
        delegatingLayoutManager.rearrange(getFigure());

        //getFigure().setLayoutManager(delegatingLayoutManager);
        //getFigure().getLayoutManager().layout(getFigure());
        getFigure().repaint();
    }

    /**
     * @return the children Model objects as a new ArrayList
     */
    protected List<?> getModelChildren()
    {
        return getDiagram().getTables();
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#isSelectable()
     */
    public boolean isSelectable()
    {
        return false;
    }

    /**
     * Creates EditPolicy objects for the EditPart. The LAYOUT_ROLE policy is
     * left to the delegating layout manager
     */
    protected void createEditPolicies()
    {
        installEditPolicy(EditPolicy.CONTAINER_ROLE, new DiagramContainerEditPolicy());
        installEditPolicy(EditPolicy.LAYOUT_ROLE, null);
    }

    /**
     * Updates the table bounds in the model so that the same bounds can be
     * restored after saving
     *
     * @return whether the procedure execute successfully without any omissions.
     *         The latter occurs if any EntityFigure has no bounds set for any of
     *         the Table model objects
     */
    public boolean setTableModelBounds()
    {

        List<?> entityParts = getChildren();

        for (Iterator<?> iter = entityParts.iterator(); iter.hasNext();) {
            EntityPart entityPart = (EntityPart) iter.next();
            IFigure entityFigure = entityPart.getFigure();

            //if we don't find a node for one of the children then we should
            // continue
            if (entityFigure == null) {
                continue;
            }

            Rectangle bounds = entityFigure.getBounds().getCopy();
            entityPart.setBounds(bounds);
        }

        return true;

    }

    /**
     * Updates the bounds of the table figure (without invoking any event
     * handling), and sets layout constraint data
     *
     * @return whether the procedure execute successfully without any omissions.
     *         The latter occurs if any Table objects have no bounds set or if
     *         no figure is available for the EntityPart
     */
    public boolean setTableFigureBounds(boolean updateConstraint)
    {
        List<?> tableParts = getChildren();

        for (Iterator<?> iter = tableParts.iterator(); iter.hasNext();) {
            EntityPart entityPart = (EntityPart) iter.next();

            //now check whether we can find an entry in the tableToNodesMap
            Rectangle bounds = entityPart.getBounds();
            if (bounds == null) {
                //TODO handle this better
                return false;
            } else {
                IFigure entityFigure = entityPart.getFigure();
                if (entityFigure == null) {
                    return false;
                } else {
                    if (updateConstraint) {
                        //pass the constraint information to the xy layout
                        //setting the width and height so that the preferred size will be applied
                        delegatingLayoutManager.setXYLayoutConstraint(entityFigure, new Rectangle(bounds.x, bounds.y, -1, -1));
                    }
                }
            }

        }
        return true;

    }

    public void changeLayout()
    {
        //Boolean layoutType = (Boolean) evt.getNewValue();
        //boolean isManualLayoutDesired = layoutType.booleanValue();
        getFigure().setLayoutManager(delegatingLayoutManager);
    }

    /**
     * Passes on to the delegating layout manager that the layout type has
     * changed. The delegating layout manager will then decide whether to
     * delegate layout to the XY or Graph layout
     */
    /**
     * Sets layout constraint only if XYLayout is active
     */
    public void setLayoutConstraint(EditPart child, IFigure childFigure, Object constraint)
    {
        super.setLayoutConstraint(child, childFigure, constraint);
    }

    /**
     * Passes on to the delegating layout manager that the layout type has
     * changed. The delegating layout manager will then decide whether to
     * delegate layout to the XY or Graph layout
     */
    protected void handleChildChange(PropertyChangeEvent evt)
    {
        super.handleChildChange(evt);
    }


}