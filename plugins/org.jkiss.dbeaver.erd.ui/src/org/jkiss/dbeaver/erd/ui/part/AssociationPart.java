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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.erd.ui.part;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.erd.ui.ERDUIUtils;
import org.jkiss.dbeaver.erd.ui.connector.ERDConnection;
import org.jkiss.dbeaver.erd.ui.editor.*;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotation;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationDescriptor;
import org.jkiss.dbeaver.erd.ui.policy.AssociationBendEditPolicy;
import org.jkiss.dbeaver.erd.ui.policy.AssociationEditPolicy;
import org.jkiss.dbeaver.erd.ui.router.ERDConnectionRouter;
import org.jkiss.dbeaver.erd.ui.router.ERDConnectionRouterDescriptor;
import org.jkiss.dbeaver.erd.ui.router.shortpath.ShortPathRouting;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ListNode;
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
    private static final Log log = Log.getLog(AssociationPart.class);
    // Keep original line width to visualize selection
    private Integer oldLineWidth;

    private ERDHighlightingHandle associatedAttributesHighlighing = null;
    protected AccessibleGraphicalEditPart accPart;
    private final Color labelForegroundColor;

    public AssociationPart() {
        Color foreground = ERDThemeSettings.instance.attrForeground;
        final Color contrastColor = UIUtils.getContrastColor(foreground);
        final RGB labelForeground = UIUtils.blend(foreground.getRGB(), contrastColor.getRGB(), 60);
        labelForegroundColor = UIUtils.getSharedColor(labelForeground);
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
        if (!getDiagramPart().getEditor().isReadOnly()) {
            if (isEditEnabled()) {
                installEditPolicy(EditPolicy.COMPONENT_ROLE, new AssociationEditPolicy());
            }
            getDiagramPart().getDiagram().getModelAdapter().installPartEditPolicies(this);
        }
    }

    @Override
    protected IFigure createFigure() {
        DBRProgressMonitor monitor = getDiagramPart().getDiagram().getMonitor();
        PolylineConnection conn = createConnectionFigure(monitor);
        UIUtils.syncExec(() -> {
            setConnectionStyles(monitor, conn);
            setConnectionRouting(monitor, conn);
            setConnectionToolTip(monitor, conn);
        });
        return conn;
    }

    private PolylineConnection createConnectionFigure(@NotNull DBRProgressMonitor monitor) {
        ERDConnectionRouter router = getDiagramPart().getActiveRouter();
        PolylineConnection conn;
        if (router != null) {
            conn = router.getConnectionInstance();
        } else {
            conn = new ERDConnection();
        }
        conn.setForegroundColor(ERDThemeSettings.instance.linesForeground);
        if (monitor.isCanceled()) {
            return conn;
        }
        boolean showComments = getDiagramPart().getDiagram().hasAttributeStyle(ERDViewStyle.COMMENTS);
        if (showComments) {
            ERDAssociation association = getAssociation();
            if (association != null && association.getObject() != null && !CommonUtils.isEmpty(association.getObject().getDescription())) {
                ConnectionLocator descLabelLocator = new ConnectionLocator(conn, ConnectionLocator.MIDDLE);
                Label descLabel = new Label(association.getObject().getDescription());
                descLabel.setForegroundColor(ERDThemeSettings.instance.attrForeground);
                conn.add(descLabel, descLabelLocator);
            }
        }
        return conn;
    }

    protected void setConnectionRouting(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PolylineConnection conn
    ) {
        if (monitor.isCanceled()) {
            return;
        }
        if (getViewer() == null) {
            return;
        }
        ERDAssociation association = getAssociation();
        // Set router and initial bends
        ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
        ERDConnectionRouterDescriptor router = getDiagramPart().getEditor().getDiagramRouter();
        conn.setConnectionRouter(cLayer.getConnectionRouter());
        if (!CommonUtils.isEmpty(association.getInitBends())) {
            List<AbsoluteBendpoint> connBends = new ArrayList<>();
            for (int[] bend : association.getInitBends()) {
                connBends.add(new AbsoluteBendpoint(bend[0], bend[1]));
            }
            conn.setRoutingConstraint(connBends);
        } else if (association.getTargetEntity() != null && association.getTargetEntity() == association.getSourceEntity()) {
            EditPart entityPart = getSource();
            if (entityPart == null) {
                entityPart = getTarget();
            }
            if (entityPart instanceof GraphicalEditPart
                && !router.supportedAttributeAssociation()) {
                final IFigure entityFigure = ((GraphicalEditPart) entityPart).getFigure();
                final Dimension figureSize = entityFigure.getMinimumSize();
                int entityWidth = figureSize.width;
                int entityHeight = figureSize.height;
                List<RelativeBendpoint> bends = new ArrayList<>();
                int w2 = entityWidth / 2;
                int h2 = entityHeight / 2;
                RelativeBendpoint bp1 = new RelativeBendpoint(conn);
                bp1.setRelativeDimensions(new Dimension(entityWidth, h2), new Dimension(entityWidth / 2, -h2 + w2));
                bends.add(bp1);
                RelativeBendpoint bp2 = new RelativeBendpoint(conn);
                bp2.setRelativeDimensions(new Dimension(entityWidth, h2), new Dimension(entityWidth, -h2 + w2 / 2));
                bends.add(bp2);
                RelativeBendpoint bp3 = new RelativeBendpoint(conn);
                bp3.setRelativeDimensions(new Dimension(entityWidth, h2), new Dimension(entityWidth, -h2 - w2 / 2));
                bends.add(bp3);
                RelativeBendpoint bp4 = new RelativeBendpoint(conn);
                bp4.setRelativeDimensions(new Dimension(entityWidth, h2), new Dimension(entityWidth / 2, -h2 - w2));
                bends.add(bp4);
                conn.setRoutingConstraint(bends);
            }
        }
        if (cLayer.getConnectionRouter() instanceof ShortPathRouting) {
            ERDNotationDescriptor diagramNotationDescriptor = getDiagramPart().getEditor().getDiagramNotation();
            ((ShortPathRouting) cLayer.getConnectionRouter())
                .setIndentation(diagramNotationDescriptor.getNotation().getIndentation());
        }
    }

    protected void setConnectionStyles(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PolylineConnection conn
    ) {
        if (monitor.isCanceled()) {
            return;
        }
        ERDNotationDescriptor diagramNotationDescriptor = getDiagramPart().getEditor().getDiagramNotation();
        if (diagramNotationDescriptor == null) {
            log.error("ERD notation descriptor is not defined");
        }
        if (diagramNotationDescriptor != null) {
            Color background = getParent().getViewer().getControl().getBackground();
            ERDNotation notation = diagramNotationDescriptor.getNotation();
            if (notation != null) {
                notation.applyNotationForArrows(monitor, conn, getAssociation(), background, labelForegroundColor);
            } else {
                log.error("ERD notation instance not created for id: " + diagramNotationDescriptor.getId());
            }
        }
    }

    protected void setConnectionToolTip(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PolylineConnection conn
    ) {
        if (monitor.isCanceled()) {
            return;
        }
        Label toolTip = new Label(getAssociation().getObject().getName() + " [" + getAssociation().getObject().getConstraintType().getName() + "]");
        toolTip.setIcon(DBeaverIcons.getImage(DBIcon.TREE_FOREIGN_KEY));
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
            ((PolylineConnection) getFigure()).setLineWidth(oldLineWidth + 3);
        } else {
            ((PolylineConnection) getFigure()).setLineWidth(oldLineWidth);
        }
        if (getSource() == null || getTarget() == null) {
            // This part seems to be deleted
            return;
        }

        if (value != EditPart.SELECTED_NONE) {
            if (this.getViewer() instanceof ERDGraphicalViewer erdViewer && associatedAttributesHighlighing == null) {
                Color attributeColor = ERDThemeSettings.instance.fkHighlightColor;
                Color associationColor = ERDThemeSettings.instance.fkHighlightColor;
                ERDHighlightingManager highlightingManager = erdViewer.getEditor().getHighlightingManager();
                ListNode<ERDHighlightingHandle> nodes = highlightingManager.highlightRelatedAttributes(this, attributeColor);
                nodes = highlightingManager.highlightAssociation(nodes, this, associationColor);
                associatedAttributesHighlighing = highlightingManager.makeHighlightingGroupHandle(nodes);
            }
        } else if (associatedAttributesHighlighing != null) {
            associatedAttributesHighlighing.release();
            associatedAttributesHighlighing  = null;
        }
    }

    @Override
    public void performRequest(Request request) {
        if (request.getType() == RequestConstants.REQ_OPEN) {
            ERDUIUtils.openObjectEditor(getDiagramPart().getDiagram(), getAssociation());
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

        private int radius = 4;
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

    @Override
    protected AccessibleEditPart getAccessibleEditPart() {
        if (this.accPart == null) {
            this.accPart = new AccessibleGraphicalEditPart() {
                public void getName(AccessibleEvent e) {
                    ERDAssociation association = AssociationPart.this.getAssociation();
                    String result = "";
                    if (association.isLogical()) {
                        result += ERDUIMessages.erd_accessibility_association_part_logical;
                    }
                    StringBuilder sourceString = new StringBuilder();
                    for (ERDEntityAttribute sourceAttribute : association.getSourceAttributes()) {
                        sourceString.append(NLS.bind(ERDUIMessages.erd_accessibility_association_part_attribute,
                            sourceAttribute.getName()));
                    }
                    StringBuilder targetString = new StringBuilder();
                    for (ERDEntityAttribute targetAttribute : association.getTargetAttributes()) {
                        targetString.append(NLS.bind(
                            ERDUIMessages.erd_accessibility_association_part_attribute,
                            targetAttribute.getName()));
                    }
                    result += NLS.bind(ERDUIMessages.erd_accessibility_association_part, new Object[]{
                        association.getName(),
                        association.getSourceEntity().getName(),
                        sourceString.toString(),
                        association.getTargetEntity().getName(),
                        targetString.toString()
                    });
                    e.result = result;
                }
            };
        }

        return this.accPart;
    }
}