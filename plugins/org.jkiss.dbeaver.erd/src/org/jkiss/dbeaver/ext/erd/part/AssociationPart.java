/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.utils.CommonUtils;
import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.*;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.policy.AssociationBendEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.AssociationEditPolicy;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the editable primary key/foreign key relationship
 *
 * @author Serge Rieder
 */
public class AssociationPart extends PropertyAwareConnectionPart {

    public AssociationPart()
    {
    }

    public ERDAssociation getAssociation()
    {
        return (ERDAssociation) getModel();
    }

    public void activate() {
        super.activate();
    }

    public void deactivate() {
        super.deactivate();
    }

    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE, new ConnectionEndpointEditPolicy());
        installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE, new AssociationBendEditPolicy());

        if (isEditEnabled()) {
            installEditPolicy(EditPolicy.COMPONENT_ROLE, new AssociationEditPolicy());
        }
    }

    protected IFigure createFigure() {
        ERDAssociation association = (ERDAssociation) getModel();

        PolylineConnection conn = (PolylineConnection) super.createFigure();
        //conn.setLineJoin(SWT.JOIN_ROUND);
        //conn.setConnectionRouter(new BendpointConnectionRouter());
        //conn.setConnectionRouter(new ShortestPathConnectionRouter(conn));
        //conn.setToolTip(new TextFlow(association.getObject().getName()));
        conn.setTargetDecoration(new PolygonDecoration());

        if (association.isLogical()) {
            conn.setLineStyle(SWT.LINE_CUSTOM);
            conn.setLineDash(new float[] {5} );
        }

        //ChopboxAnchor sourceAnchor = new ChopboxAnchor(classFigure);
        //ChopboxAnchor targetAnchor = new ChopboxAnchor(classFigure2);
        //conn.setSourceAnchor(sourceAnchor);
        //conn.setTargetAnchor(targetAnchor);

/*
        ConnectionEndpointLocator relationshipLocator = new ConnectionEndpointLocator(conn, true);
        //relationshipLocator.setUDistance(30);
        //relationshipLocator.setVDistance(-20);
        Label relationshipLabel = new Label(association.getObject().getName());
        conn.add(relationshipLabel, relationshipLocator);
*/

        // Set router and initial bends
        ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
        conn.setConnectionRouter(cLayer.getConnectionRouter());
        if (!CommonUtils.isEmpty(association.getInitBends())) {
            List<AbsoluteBendpoint> connBends = new ArrayList<AbsoluteBendpoint>();
            for (Point bend : association.getInitBends()) {
                connBends.add(new AbsoluteBendpoint(bend.x, bend.y));
            }
            conn.setRoutingConstraint(connBends);
        } else if (association.getPrimaryKeyTable() == association.getForeignKeyTable()) {
            // Self link
            final IFigure entityFigure = ((GraphicalEditPart) getSource()).getFigure();
            //EntityPart entity = (EntityPart) connEdge.source.getParent().data;
            //final Dimension entitySize = entity.getFigure().getSize();
            final Dimension figureSize = entityFigure.getMinimumSize();
            int entityWidth = figureSize.width;
            int entityHeight = figureSize.height;

            List<RelativeBendpoint> bends = new ArrayList<RelativeBendpoint>();
            {
                RelativeBendpoint bp1 = new RelativeBendpoint(conn);
                bp1.setRelativeDimensions(new Dimension(entityWidth, entityHeight / 2), new Dimension(entityWidth / 2, entityHeight / 2));
                bends.add(bp1);
            }
            {
                RelativeBendpoint bp2 = new RelativeBendpoint(conn);
                bp2.setRelativeDimensions(new Dimension(-entityWidth, entityHeight / 2), new Dimension(entityWidth, entityHeight));
                bends.add(bp2);
            }
            conn.setRoutingConstraint(bends);
        }

        // Set tool tip
        Label toolTip = new Label(getAssociation().getObject().getName() + " [" + getAssociation().getObject().getConstraintType().getName() + "]");
        toolTip.setIcon(DBIcon.TREE_FOREIGN_KEY.getImage());
        //toolTip.setTextPlacement(PositionConstants.SOUTH);
        //toolTip.setIconTextGap();
        conn.setToolTip(toolTip);

        return conn;
    }

    /**
     * Sets the width of the line when selected
     */
    public void setSelected(int value) {
        super.setSelected(value);
        if (value != EditPart.SELECTED_NONE) {
            ((PolylineConnection) getFigure()).setLineWidth(2);
        } else {
            ((PolylineConnection) getFigure()).setLineWidth(1);
        }
        if (getSource() == null || getTarget() == null) {
            // This part seems to be deleted
            return;
        }

        DBSEntityAssociation association = getAssociation().getObject();
        if (association instanceof DBSForeignKey) {
            List<AttributePart> sourceAttributes = getEntityAttributes(
                (EntityPart)getSource(),
                DBUtils.getTableColumns(VoidProgressMonitor.INSTANCE, ((DBSForeignKey) association).getReferencedKey()));
            List<AttributePart> targetAttributes = getEntityAttributes(
                (EntityPart)getTarget(),
                DBUtils.getTableColumns(VoidProgressMonitor.INSTANCE, (DBSForeignKey)association));
            Color columnColor = value != EditPart.SELECTED_NONE ? Display.getDefault().getSystemColor(SWT.COLOR_RED) : getViewer().getControl().getForeground();
            for (AttributePart attr : sourceAttributes) {
                attr.getFigure().setForegroundColor(columnColor);
            }
            for (AttributePart attr : targetAttributes) {
                attr.getFigure().setForegroundColor(columnColor);
            }
        }
    }

    private List<AttributePart> getEntityAttributes(EntityPart source, List<DBSTableColumn> columns)
    {
        List<AttributePart> erdColumns = new ArrayList<AttributePart>(source.getChildren());
        for (Iterator<AttributePart> iter = erdColumns.iterator(); iter.hasNext(); ) {
            if (!columns.contains(iter.next().getColumn().getObject())) {
                iter.remove();
            }
        }
        return erdColumns;
    }

    public void performRequest(Request request)
    {
        if (request.getType() == RequestConstants.REQ_OPEN) {
            getAssociation().openEditor();
        }
    }

    public void addBendpoint(int bendpointIndex, Point location) {
        Bendpoint bendpoint = new AbsoluteBendpoint(location);
        List<Bendpoint> bendpoints = getBendpointsCopy();
        bendpoints.add(bendpointIndex, bendpoint);
        updateBendpoints(bendpoints);
    }

    public void removeBendpoint(int bendpointIndex) {
        List<Bendpoint> bendpoints = getBendpointsCopy();
        if (bendpointIndex < bendpoints.size()) {
            bendpoints.remove(bendpointIndex);
            updateBendpoints(bendpoints);
        }
    }

    public void moveBendpoint(int bendpointIndex, Point location) {
        Bendpoint bendpoint = new AbsoluteBendpoint(location);
        List<Bendpoint> bendpoints = getBendpointsCopy();
        if (bendpointIndex < bendpoints.size()) {
            bendpoints.set(bendpointIndex, bendpoint);
            updateBendpoints(bendpoints);
        }
    }

    public List<Bendpoint> getBendpoints() {
        Object constraint = getConnectionFigure().getRoutingConstraint();
        if (constraint instanceof List) {
            // Make constraint copy
            return (List<Bendpoint>) constraint;
        } else {
            return Collections.emptyList();
        }
    }

    private List<Bendpoint> getBendpointsCopy() {
        Object constraint = getConnectionFigure().getRoutingConstraint();
        if (constraint instanceof List) {
            // Make constraint copy
            List<Bendpoint> curList = (List<Bendpoint>) constraint;
            return new ArrayList<Bendpoint>(curList);
        } else {
            return new ArrayList<Bendpoint>();
        }
    }

    private void updateBendpoints(List<Bendpoint> bendpoints) {
        getConnectionFigure().setRoutingConstraint(bendpoints);
    }

    @Override
    public String toString()
    {
        return getAssociation().getObject().getConstraintType().getName() + " " + getAssociation().getObject().getName();
    }
}