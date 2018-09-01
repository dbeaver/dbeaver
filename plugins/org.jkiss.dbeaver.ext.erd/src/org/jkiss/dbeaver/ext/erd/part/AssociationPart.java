/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.editor.ERDViewStyle;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.ext.erd.model.ERDUtils;
import org.jkiss.dbeaver.ext.erd.policy.AssociationBendEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.AssociationEditPolicy;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the editable primary key/foreign key relationship
 *
 * @author Serge Rider
 */
public class AssociationPart extends PropertyAwareConnectionPart {

    // Keep original line width to visualize selection
    private Integer oldLineWidth;

    public AssociationPart() {
    }

    public ERDAssociation getAssociation() {
        return (ERDAssociation) getModel();
    }

    @Override
    public void activate() {
        super.activate();
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE, new ConnectionEndpointEditPolicy());
        installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE, new AssociationBendEditPolicy());

        if (isEditEnabled()) {
            installEditPolicy(EditPolicy.COMPONENT_ROLE, new AssociationEditPolicy());
        }
    }

    @Override
    protected IFigure createFigure() {
        PolylineConnection conn = new PolylineConnection();

        conn.setForegroundColor(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_LINES_FOREGROUND));

        boolean showComments = getDiagramPart().getDiagram().hasAttributeStyle(ERDViewStyle.COMMENTS);
        if (showComments) {
            ERDAssociation association = getAssociation();
            if (association != null && association.getObject() != null && !CommonUtils.isEmpty(association.getObject().getDescription())) {
                ConnectionLocator descLabelLocator = new ConnectionLocator(conn, ConnectionLocator.MIDDLE);
                //descLabelLocator.setRelativePosition(50);
                //descLabelLocator.setGap(50);
                Label descLabel = new Label(association.getObject().getDescription());
                descLabel.setForegroundColor(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_ATTR_FOREGROUND));
//                Border border = new MarginBorder(20, 0, 0, 0);
//                descLabel.setBorder(border);
                conn.add(descLabel, descLabelLocator);
            }
        }

        setConnectionStyles(conn);
        setConnectionRouting(conn);
        setConnectionToolTip(conn);

        return conn;
    }

    protected void setConnectionRouting(PolylineConnection conn) {
        ERDAssociation association = getAssociation();
        // Set router and initial bends
        ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
        conn.setConnectionRouter(cLayer.getConnectionRouter());
        if (!CommonUtils.isEmpty(association.getInitBends())) {
            List<AbsoluteBendpoint> connBends = new ArrayList<>();
            for (Point bend : association.getInitBends()) {
                connBends.add(new AbsoluteBendpoint(bend.x, bend.y));
            }
            conn.setRoutingConstraint(connBends);
        } else if (association.getTargetEntity() != null && association.getTargetEntity() == association.getSourceEntity()) {
            EditPart entityPart = getSource();
            if (entityPart == null) {
                entityPart = getTarget();
            }
            if (entityPart instanceof GraphicalEditPart) {
                // Self link
                final IFigure entityFigure = ((GraphicalEditPart) entityPart).getFigure();
                //EntityPart entity = (EntityPart) connEdge.source.getParent().data;
                //final Dimension entitySize = entity.getFigure().getSize();
                final Dimension figureSize = entityFigure.getMinimumSize();
                int entityWidth = figureSize.width;
                int entityHeight = figureSize.height;

                List<RelativeBendpoint> bends = new ArrayList<>();
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
        }
    }

    protected void setConnectionStyles(PolylineConnection conn) {

        ERDAssociation association = getAssociation();
        boolean identifying = ERDUtils.isIdentifyingAssociation(association);

        if (association.getObject().getConstraintType() == DBSEntityConstraintType.INHERITANCE) {
            final PolygonDecoration srcDec = new PolygonDecoration();
            srcDec.setTemplate(PolygonDecoration.TRIANGLE_TIP);
            srcDec.setFill(true);
            srcDec.setBackgroundColor(getParent().getViewer().getControl().getBackground());
            srcDec.setScale(10, 6);
            conn.setTargetDecoration(srcDec);
        }
        if (association.getObject().getConstraintType() == DBSEntityConstraintType.FOREIGN_KEY) {
            final CircleDecoration targetDecor = new CircleDecoration();
            targetDecor.setRadius(3);
            targetDecor.setFill(true);
            targetDecor.setBackgroundColor(getParent().getViewer().getControl().getForeground());
            //dec.setBackgroundColor(getParent().getViewer().getControl().getBackground());
            conn.setTargetDecoration(targetDecor);
            if (!identifying) {
                final RhombusDecoration sourceDecor = new RhombusDecoration();
                sourceDecor.setBackgroundColor(getParent().getViewer().getControl().getBackground());
                //dec.setBackgroundColor(getParent().getViewer().getControl().getBackground());
                conn.setSourceDecoration(sourceDecor);
            }
        }

        if (!identifying || association.isLogical()) {
            conn.setLineStyle(SWT.LINE_CUSTOM);
            conn.setLineDash(new float[]{5});
        }
    }

    protected void setConnectionToolTip(PolylineConnection conn) {
        // Set tool tip
        Label toolTip = new Label(getAssociation().getObject().getName() + " [" + getAssociation().getObject().getConstraintType().getName() + "]");
        toolTip.setIcon(DBeaverIcons.getImage(DBIcon.TREE_FOREIGN_KEY));
        //toolTip.setTextPlacement(PositionConstants.SOUTH);
        //toolTip.setIconTextGap();
        conn.setToolTip(toolTip);
    }

    /**
     * Sets the width of the line when selected
     */
    @Override
    public void setSelected(int value) {
        if (getSelected() == value) {
            return;
        }
        super.setSelected(value);

        if (oldLineWidth == null) {
            oldLineWidth = ((PolylineConnection) getFigure()).getLineWidth();
        }

        if (value != EditPart.SELECTED_NONE) {
            ((PolylineConnection) getFigure()).setLineWidth(oldLineWidth + 1);
        } else {
            ((PolylineConnection) getFigure()).setLineWidth(oldLineWidth);
        }
        if (getSource() == null || getTarget() == null) {
            // This part seems to be deleted
            return;
        }

        markAssociatedAttributes(value);
    }

    public void markAssociatedAttributes(int value) {
        //Color columnColor = value != EditPart.SELECTED_NONE ? Display.getDefault().getSystemColor(SWT.COLOR_RED) : getViewer().getControl().getForeground();
        boolean isSelected = value != EditPart.SELECTED_NONE;
        if (getSource() != null) {
            for (AttributePart attrPart : getEntityAttributes((EntityPart) getSource(), getAssociation().getSourceAttributes())) {
                //attrPart.getFigure().setForegroundColor(columnColor);
                attrPart.setSelected(value);
            }
        }
        if (getTarget() != null) {
            for (AttributePart attrPart : getEntityAttributes((EntityPart) getTarget(), getAssociation().getTargetAttributes())) {
                //attrPart.getFigure().setForegroundColor(columnColor);
                attrPart.setSelected(value);
            }
        }
    }

    private List<AttributePart> getEntityAttributes(EntityPart source, List<ERDEntityAttribute> columns) {
        List<AttributePart> result = new ArrayList<>();
        for (AttributePart attrPart : (List<AttributePart>) source.getChildren()) {
            if (columns.contains(attrPart.getAttribute())) {
                result.add(attrPart);
            }
        }
        return result;
    }

    @Override
    public void performRequest(Request request) {
        if (request.getType() == RequestConstants.REQ_OPEN) {
            ERDUtils.openObjectEditor(getAssociation());
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
            return new ArrayList<>(curList);
        } else {
            return new ArrayList<>();
        }
    }

    private void updateBendpoints(List<Bendpoint> bendpoints) {
        getConnectionFigure().setRoutingConstraint(bendpoints);
    }

    @Override
    public String toString() {
        return getAssociation().getObject().getConstraintType().getName() + " " + getAssociation().getObject().getName();
    }

    public static class CircleDecoration extends Ellipse implements RotatableDecoration {

        private int radius = 5;
        private Point location = new Point();

        public CircleDecoration() {
            super();
        }

        public void setRadius(int radius) {
            this.radius = radius;
        }

        @Override
        public void setLocation(Point p) {
            location = p;
            Rectangle bounds = new Rectangle(location.x - radius, location.y - radius, radius * 2, radius * 2);
            setBounds(bounds);
        }

        @Override
        public void setReferencePoint(Point p) {
            // length of line between reference point and location
            double d = Math.sqrt(Math.pow((location.x - p.x), 2) + Math.pow(location.y - p.y, 2));

            // do nothing if link is too short.
            if (d < radius)
                return;

            //
            // using pythagoras theorem, we have a triangle like this:
            //
            //      |       figure       |
            //      |                    |
            //      |_____(l.x,l.y)______|
            //                (\)
            //                | \(r.x,r.y)
            //                | |\
            //                | | \
            //                | |  \
            //                | |   \
            //                |_|(p.x,p.y)
            //
            // We want to find a point that at radius distance from l (location) on the line between l and p
            // and center our circle at this point.
            //
            // I you remember your school math, let the distance between l and p (calculated
            // using pythagoras theorem) be defined as d. We want to find point r where the
            // distance between r and p is d-radius (the same as saying that the distance
            // between l and r is radius). We can do this using triangle identities.
            //     |px-rx|/|px-lx|=|py-ry|/|py-ly|=|d-radius|/d
            //
            // we use
            //  k = |d-radius|/d
            //  longx = |px-lx|
            //  longy = |py-xy|
            //
            // remember that d > radius.
            //
            double k = (d - radius) / d;
            double longx = Math.abs(p.x - location.x);
            double longy = Math.abs(p.y - location.y);

            double shortx = k * longx;
            double shorty = k * longy;

            // now create locate the new point using the distances depending on the location of the original points.
            int rx, ry;
            if (location.x < p.x) {
                rx = p.x - (int) shortx;
            } else {
                rx = p.x + (int) shortx;
            }
            if (location.y > p.y) {
                ry = p.y + (int) shorty;
            } else {
                ry = p.y - (int) shorty;
            }

            // For reasons that are still unknown to me, I had to increase the radius
            // of the circle for the graphics to look right.
            setBounds(new Rectangle(rx - radius, ry - radius, (int) (radius * 2.5), (int) (radius * 2.5)));
        }
    }

    public static class RhombusDecoration extends PolygonDecoration {
        private static PointList GEOMETRY = new PointList();

        static {
            GEOMETRY.addPoint(0, 0);
            GEOMETRY.addPoint(-1, 1);
            GEOMETRY.addPoint(-2, 0);
            GEOMETRY.addPoint(-1, -1);
        }

        public RhombusDecoration() {
            setTemplate(GEOMETRY);
            setFill(true);
            setScale(5, 5);
        }
    }

}