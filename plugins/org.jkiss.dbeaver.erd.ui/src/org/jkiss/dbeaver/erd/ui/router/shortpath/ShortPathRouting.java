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
package org.jkiss.dbeaver.erd.ui.router.shortpath;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.Path;
import org.eclipse.draw2d.graph.ShortestPathRouter;
import org.jkiss.dbeaver.erd.ui.figures.EntityFigure;
import org.jkiss.dbeaver.erd.ui.router.ERDConnectionRouter;

import java.util.*;
import java.util.Map.Entry;

public class ShortPathRouting extends ERDConnectionRouter {

    private static final int POINT_DISTANCE = 7;
    private double indentation = 30.0;
    private static final int RIGHT = 180;
    private static final int LEFT = 0;
    private static final int UP = 90;
    private static final int DOWN = -90;
    private final Map<Connection, Object> constraintMap = new HashMap<>();
    private Map<IFigure, Rectangle> figuresToBounds;
    private Map<Connection, Path> connectionToPaths;
    private boolean isDirty;
    private final ShortestPathRouter algorithm = new ShortestPathRouter();
    private final Set<Connection> staleConnections = new HashSet<>();
    private final LayoutListener listener = new LayoutTracker();
    private boolean ignoreInvalidate;

    private final FigureListener figureListener = source -> {
        Rectangle newBounds = source.getBounds().getCopy();
        if (algorithm.updateObstacle(figuresToBounds.get(source), newBounds)) {
            queueSomeRouting();
            isDirty = true;
        }

        figuresToBounds.put(source, newBounds);
    };

    void addChild(IFigure child) {
        if (connectionToPaths == null) {
            return;
        }
        if (figuresToBounds.containsKey(child)) {
            return;
        }
        Rectangle bounds = child.getBounds().getCopy();
        algorithm.addObstacle(bounds);
        figuresToBounds.put(child, bounds);
        child.addFigureListener(figureListener);
        isDirty = true;
    }

    private void hookAll() {
        figuresToBounds = new HashMap<>();
        getContainer().getChildren().forEach(this::addChild);
        getContainer().addLayoutListener(listener);
    }

    private void unhookAll() {
        getContainer().removeLayoutListener(listener);
        if (figuresToBounds != null) {
            Iterator<IFigure> figureItr = figuresToBounds.keySet().iterator();
            while (figureItr.hasNext()) {
                // Must use iterator's remove to avoid concurrent modification
                IFigure child = figureItr.next();
                figureItr.remove();
                removeChild(child);
            }
            figuresToBounds = null;
        }
    }

    /**
     * Gets the constraint for the given {@link Connection}. The constraint is the
     * paths list of bend points for this connection.
     */
    @Override
    public Object getConstraint(Connection connection) {
        return constraintMap.get(connection);
    }

    /**
     * Returns the default spacing maintained on either side of a connection. The
     * default value is 4.
     */
    public int getSpacing() {
        return algorithm.getSpacing();
    }

    /**
     * ConnectionRouter#invalidate(Connection)
     */
    @Override
    public void invalidate(Connection connection) {
        if (ignoreInvalidate) {
            return;
        }
        staleConnections.add(connection);
        getConnectionPoints().put(connection, new PointList());
        isDirty = true;
    }

    private void processStaleConnections() {
        Iterator<Connection> iter = staleConnections.iterator();
        if (iter.hasNext() && connectionToPaths == null) {
            connectionToPaths = new HashMap<>();
            hookAll();
        }

        while (iter.hasNext()) {
            Connection conn = iter.next();

            Path path = connectionToPaths.get(conn);
            if (path == null) {
                path = new Path(conn);
                connectionToPaths.put(conn, path);
                algorithm.addPath(path);
            }
            @SuppressWarnings("unchecked")
            List<Object> constraint = (List<Object>) getConstraint(conn);
            if (constraint == null) {
                constraint = Collections.emptyList();
            }

            Point start = conn.getSourceAnchor().getReferencePoint().getCopy();
            Point end = conn.getTargetAnchor().getReferencePoint().getCopy();

            getContainer().translateToRelative(start);
            getContainer().translateToRelative(end);

            path.setStartPoint(start);
            path.setEndPoint(end);

            if (!constraint.isEmpty()) {
                PointList bends = new PointList(constraint.size());
                for (Object element : constraint) {
                    Bendpoint bp = (Bendpoint) element;
                    bends.addPoint(bp.getLocation());
                }
                path.setBendPoints(bends);
            } else {
                path.setBendPoints(null);
            }

            isDirty |= path.isDirty;
        }
        staleConnections.clear();
    }

    void queueSomeRouting() {
        if (connectionToPaths == null || connectionToPaths.isEmpty()) {
            return;
        }
        try {
            ignoreInvalidate = true;
            connectionToPaths.keySet().iterator().next().revalidate();
        } finally {
            ignoreInvalidate = false;
        }
    }

    /**
     * ConnectionRouter#remove(Connection)
     */
    @Override
    public void remove(Connection connection) {
        staleConnections.remove(connection);
        constraintMap.remove(connection);
        if (connectionToPaths == null) {
            return;
        }
        Path path = connectionToPaths.remove(connection);
        algorithm.removePath(path);
        isDirty = true;
        if (connectionToPaths.isEmpty()) {
            unhookAll();
            connectionToPaths = null;
        } else {
            // Make sure one of the remaining is revalidated so that we can
            // re-route again.
            queueSomeRouting();
        }
    }

    void removeChild(IFigure child) {
        if (connectionToPaths == null) {
            return;
        }
        Rectangle bounds = child.getBounds().getCopy();
        boolean change = algorithm.removeObstacle(bounds);
        figuresToBounds.remove(child);
        child.removeFigureListener(figureListener);
        if (change) {
            isDirty = true;
            queueSomeRouting();
        }
    }

    /**
     * ConnectionRouter#route(Connection)
     */
    @Override
    public void route(Connection conn) {
        if (isDirty) {
            ignoreInvalidate = true;
            processStaleConnections();
            isDirty = false;
            List<Path> paths = computePaths();
            Connection current;
            for (Path path : paths) {
                current = (Connection) path.data;
                current.revalidate();
                PointList points = path.getPoints().getCopy();
                Point ref1 = new PrecisionPoint(points.getPoint(1));
                Point ref2 = new PrecisionPoint(points.getPoint(points.size() - 2));
                current.translateToAbsolute(ref1);
                current.translateToAbsolute(ref2);
                Point start = current.getSourceAnchor().getLocation(ref1).getCopy();
                Point end = current.getTargetAnchor().getLocation(ref2).getCopy();
                current.translateToRelative(start);
                current.translateToRelative(end);
                points.setPoint(start, 0);
                points.setPoint(end, points.size() - 1);
               
                int srcTrgAngel = 0;
                int trgSrcAngel = 0;
                if (current.getSourceAnchor().getOwner() instanceof EntityFigure) {
                    Rectangle bounds = ((EntityFigure) current.getSourceAnchor().getOwner()).getBounds().getCopy();
                    srcTrgAngel = 90 - getDirection(bounds, points.getPoint(0));
                }

                if (current.getTargetAnchor().getOwner() instanceof EntityFigure) {
                    Rectangle bounds = ((EntityFigure) current.getTargetAnchor().getOwner()).getBounds().getCopy();
                    trgSrcAngel = -90 + getDirection(bounds, points.getPoint(points.size() - 1));
                }
                int dxSrcTrg = (int) (Math.cos(Math.toRadians(srcTrgAngel)) * indentation);
                int dySrcTrg = (int) (Math.sin(Math.toRadians(srcTrgAngel)) * indentation);
                int dxTrgSrc = (int) (Math.cos(Math.toRadians(trgSrcAngel)) * indentation);
                int dyTrgSrc = (int) (Math.sin(Math.toRadians(trgSrcAngel)) * indentation);

                for (Entry<Connection, PointList> entry : getConnectionPoints().entrySet()) {
                    if (entry.getKey().equals(current)) {
                        continue;
                    }
                    for (int i = 0; i < entry.getKey().getPoints().size(); i++) {
                        Point p = entry.getKey().getPoints().getPoint(i);
                        int dxStart = Math.abs(start.x - p.x);
                        int dyStart = Math.abs(start.y - p.y);
                        int dxEnd = Math.abs(end.x - p.x);
                        int dyEnd = Math.abs(end.y - p.y);
                        if (dxStart == 0 && dyStart < POINT_DISTANCE) {
                            start = new Point(start.x + dxSrcTrg, start.y - dySrcTrg);
                            Point firstPoint = points.getPoint(0);
                            firstPoint = new Point(firstPoint.x + dxSrcTrg, firstPoint.y - dySrcTrg);
                            points.setPoint(firstPoint, 0);
                        }
                        if (dxEnd == 0 && dyEnd < POINT_DISTANCE) {
                            end = new Point(end.x - dxTrgSrc, end.y - dyTrgSrc);
                            Point endPoint = points.getPoint(points.size() - 1);
                            endPoint = new Point(endPoint.x - dxTrgSrc, endPoint.y - dyTrgSrc);
                            points.setPoint(endPoint, points.size() - 1);
                        }
                    }
                }
                // first
                PointList modifiedPoints = new PointList();
                modifiedPoints.addPoint(points.getFirstPoint());
                int direction1 = 0;
                int direction2 = 0;
                IFigure srcOwner = current.getSourceAnchor().getOwner();
                if (srcOwner instanceof EntityFigure) {
                    Rectangle bounds = ((EntityFigure) srcOwner).getBounds().getCopy();
                    direction1 = 180 - getDirection(bounds, points.getPoint(0).getCopy());
                }
                IFigure targetOwner = current.getTargetAnchor().getOwner();
                if (targetOwner instanceof EntityFigure) {
                    Rectangle bounds = ((EntityFigure) targetOwner).getBounds().getCopy();
                    direction2 = getDirection(bounds, points.getPoint(points.size() - 1).getCopy());
                }
                // from 1-->>2
                int dx1 = (int) (Math.cos(Math.toRadians(direction1)) * indentation);
                int dy1 = (int) (Math.sin(Math.toRadians(direction1)) * indentation);
                // from 1<<--2
                int dx2 = (int) (Math.cos(Math.toRadians(direction2)) * indentation);
                int dy2 = (int) (Math.sin(Math.toRadians(direction2)) * indentation);
                Point p1 = new Point(start.x + dx1, start.y - dy1);
                modifiedPoints.addPoint(p1);
                // add other middle points
                for (int i = 1; i < points.size() - 1; i++) {
                    modifiedPoints.addPoint(points.getPoint(i));
                }
                // before end
                Point p2 = new Point(end.x - dx2, end.y - dy2);
                modifiedPoints.addPoint(p2);
                // end
                modifiedPoints.addPoint(points.getLastPoint());
                current.setPoints(modifiedPoints);
            }
            ignoreInvalidate = false;
        }
    }

    private List<Path> computePaths() {
        // this is a way to get List<Path> from algorithm
        List<Path> paths = algorithm.solve();
        for (Path path : paths) {
            removeOverlappingBendPoints(path);
        }
        // require to solve for new route calculation
        paths = algorithm.solve();
        return paths;
    }

    /**
     * This method checks and remove bend point if it overlap entity
     *
     * @param path - path
     */
    private void removeOverlappingBendPoints(Path path) {
        PointList bendPoints = path.getBendPoints();
        if (bendPoints != null) {
            PointList actualBendPoints = new PointList(bendPoints.size());
            for (int index = 0; index < bendPoints.size(); index++) {
                Point bp = bendPoints.getPoint(index);
                boolean requireToSkipp = false;
                for (Entry<IFigure, Rectangle> entry : figuresToBounds.entrySet()) {
                    Rectangle rectangle = entry.getValue();
                    if (rectangle.contains(bp)) {
                        requireToSkipp = true;
                        break;
                    }
                }
                if (requireToSkipp) {
                    continue;
                }
                actualBendPoints.addPoint(bp);
            }
            path.setBendPoints(actualBendPoints);
        }
    }

    protected int getDirection(Rectangle r, Point p) {
        int i = 0;
        int direction = LEFT;
        int distance = Math.abs(r.x - p.x);
        i = Math.abs(r.y - p.y);
        if (i <= distance) {
            distance = i;
            direction = UP;
        }
        i = Math.abs(r.bottom() - p.y);
        if (i <= distance) {
            distance = i;
            direction = DOWN;
        }
        i = Math.abs(r.right() - p.x);
        if (i < distance) {
            direction = RIGHT;
        }
        return direction;
    }

    /**
     * All connection paths after routing dirty paths. Some of the paths that were
     * not dirty may change as well, as a consequence of new routings.
     * 
     */
    public List<?> getPathsAfterRouting() {
        if (isDirty) {
            processStaleConnections();
            isDirty = false;
            return algorithm.solve();

        }
        return Collections.emptyList();
    }

    @Override
    public void setConstraint(Connection connection, Object constraint) {
        staleConnections.add(connection);
        constraintMap.put(connection, constraint);
        isDirty = true;
    }

    /**
     * Sets the default space that should be maintained on either side of a
     * connection. This causes the connections to be separated from each other and
     * from the obstacles. The default value is 4.
     */
    public void setSpacing(int spacing) {
        algorithm.setSpacing(spacing);
    }

    /**
     * Checks multiple connections
     */
    public boolean hasMoreConnections() {
        return connectionToPaths != null && !connectionToPaths.isEmpty();
    }

    /**
     * Sets the value indicating if connection invalidation should be ignored.
     */
    public void setIgnoreInvalidate(boolean b) {
        ignoreInvalidate = b;
    }

    /**
     * Returns the value indicating if connection invalidation should be ignored.
     */
    public boolean shouldIgnoreInvalidate() {
        return ignoreInvalidate;
    }

    /**
     * Returns the value indicating if the router is dirty, i.e. if there are any
     * outstanding connections that need to be routed
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Returns true if the given connection is routed by this router, false
     * otherwise
     */
    public boolean containsConnection(Connection conn) {
        return connectionToPaths != null && connectionToPaths.containsKey(conn);
    }

    private class LayoutTracker extends LayoutListener.Stub {
        @Override
        public void postLayout(IFigure container) {
            if (staleConnections.isEmpty()) {
                return;
            }
            staleConnections.iterator().next().revalidate();
        }

        @Override
        public void remove(IFigure child) {
            removeChild(child);
        }

        @Override
        public void setConstraint(IFigure child, Object constraint) {
            addChild(child);
        }
    }

    /**
     * The method returns indentation value
     */
    public double getIndentation() {
        return indentation;
    }

    /**
     * Method design to specify indentation value
     */
    public void setIndentation(double indentation) {
        this.indentation = indentation;
    }

}