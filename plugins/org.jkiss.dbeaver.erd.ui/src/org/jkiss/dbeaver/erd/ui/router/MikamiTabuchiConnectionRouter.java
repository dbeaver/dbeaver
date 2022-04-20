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
package org.jkiss.dbeaver.erd.ui.router;


import org.eclipse.draw2dl.*;
import org.eclipse.draw2dl.LayoutListener.Stub;
import org.eclipse.draw2dl.geometry.Point;
import org.eclipse.draw2dl.geometry.PointList;
import org.eclipse.draw2dl.geometry.Rectangle;
import java.util.*;


public class MikamiTabuchiConnectionRouter extends AbstractRouter {
    private boolean isDirty = false;
    private final int maxWidth;
    private final int maxHeight;
    private Map<IFigure, Rectangle> figuresToBounds;
    private Map<Connection, List<Point>> connectionToPathPoints;
    private final IFigure container;
    private final MikamiTabuchiRouter algorithm = new MikamiTabuchiRouter();
    private Set<Connection> staleConnections = new HashSet<>();
    private boolean ignoreInvalidate;
    private Map constraintMap = new HashMap();
    private Map connectionToPaths = new HashMap<Connection, OrthogonalPath>();
    private final LayoutListener listener = new MikamiTabuchiConnectionRouter.LayoutTracker();
    private final FigureListener figureListener = source -> {
        Rectangle newBounds = source.getBounds().getCopy();
        if (MikamiTabuchiConnectionRouter.this.algorithm.updateObstacle(MikamiTabuchiConnectionRouter.this.figuresToBounds.get(source), newBounds)) {
            MikamiTabuchiConnectionRouter.this.queueRerouting();
            MikamiTabuchiConnectionRouter.this.isDirty = true;
        }

        MikamiTabuchiConnectionRouter.this.figuresToBounds.put(source, newBounds);
    };




    public void invalidate(Connection connection) {
        if (!this.ignoreInvalidate) {
            this.staleConnections.add(connection);
            this.isDirty = true;
        }
    }

    private void queueRerouting() {
    }

    void addChild(IFigure child) {
        if (this.connectionToPathPoints != null) {
            if (!this.figuresToBounds.containsKey(child)) {
                Rectangle bounds = child.getBounds().getCopy();
                this.algorithm.addObstacle(bounds);
                this.figuresToBounds.put(child, bounds);
                child.addFigureListener(this.figureListener);
                this.isDirty = true;
            }
        }
    }

    public MikamiTabuchiConnectionRouter(IFigure container) {
        this.container = container;
        maxWidth = container.getClientArea().width;
        maxHeight = container.getClientArea().height;
    }

//    private void processStaleConnections() {
//        Iterator iter = this.staleConnections.iterator();
//        if (iter.hasNext() && this.connectionToPathPoints == null) {
//            this.connectionToPathPoints = new HashMap();
//            this.hookAll();
//        }
//
//        Path path;
//        for(; iter.hasNext(); this.isDirty |= path.isDirty) {
//            Connection conn = (Connection)iter.next();
//            path = this.connectionToPathPoints.get(conn);
//            if (path == null) {
//                path = new Path(conn);
//                this.connectionToPathPoints.put(conn, path);
////                this.algorithm.addPath(path);
//            }
//
//            List constraint = (List)this.getConstraint(conn);
//            if (constraint == null) {
//                constraint = Collections.EMPTY_LIST;
//            }
//
//            Point start = conn.getSourceAnchor().getReferencePoint().getCopy();
//            Point end = conn.getTargetAnchor().getReferencePoint().getCopy();
//            this.container.translateToRelative(start);
//            this.container.translateToRelative(end);
//            path.setStartPoint(start);
//            path.setEndPoint(end);
//            if (constraint.isEmpty()) {
//                path.setBendPoints(null);
//            } else {
//                PointList bends = new PointList(constraint.size());
//
//                for (Object o : constraint) {
//                    Bendpoint bp = (Bendpoint) o;
//                    bends.addPoint(bp.getLocation());
//                }
//
//                path.setBendPoints(bends);
//            }
//        }
//
//        this.staleConnections.clear();
//    }

    private void hookAll() {
        this.figuresToBounds = new HashMap<>();

        for(int i = 0; i < this.container.getChildren().size(); ++i) {
            this.addChild(this.container.getChildren().get(i));
        }

        this.container.addLayoutListener(this.listener);
    }

    @Override
    public void route(Connection connection) {
        if (this.isDirty) {
            this.ignoreInvalidate = true;
            this.hookAll();
            this.isDirty = false;
            this.algorithm.setClientArea(new Rectangle(0, 0, 10000, 10000));
//        this.algorithm.setObstacles(new ArrayList<>(figuresToBounds.values()));
            PointList updated = this.algorithm.solve(connection.getSourceAnchor().getReferencePoint(), connection.getTargetAnchor().getReferencePoint());
            if (updated != null) {
                connection.setPoints(updated);
            }
        }
    }

    private void unhookAll() {
        this.container.removeLayoutListener(this.listener);
        if (this.figuresToBounds != null) {
            Iterator figureItr = this.figuresToBounds.keySet().iterator();

            while(figureItr.hasNext()) {
                IFigure child = (IFigure)figureItr.next();
                figureItr.remove();
                this.removeChild(child);
            }

            this.figuresToBounds = null;
        }

    }

    public void remove(Connection connection) {
        this.staleConnections.remove(connection);
        this.constraintMap.remove(connection);
        if (this.connectionToPaths != null) {
            OrthogonalPath path = (OrthogonalPath)this.connectionToPaths.remove(connection);
            this.algorithm.removePath(path);
            this.isDirty = true;
            if (this.connectionToPaths.isEmpty()) {
                this.unhookAll();
                this.connectionToPaths = null;
            } else {
                this.queueRerouting();
            }

        }
    }

    void removeChild(IFigure child) {
        if (this.connectionToPathPoints != null) {
            Rectangle bounds = child.getBounds().getCopy();
            boolean change = this.algorithm.removeObstacle(bounds);
            this.figuresToBounds.remove(child);
            child.removeFigureListener(this.figureListener);
            if (change) {
                this.isDirty = true;
                this.queueRerouting();
            }

        }
    }

    public void setSpacing(int spacing) {
        this.algorithm.setSpacing(spacing);
    }

    public boolean hasMoreConnections() {
        return this.connectionToPathPoints != null && !this.connectionToPathPoints.isEmpty();
    }

    public IFigure getContainer() {
        return this.container;
    }

    public void setIgnoreInvalidate(boolean b) {
        this.ignoreInvalidate = b;
    }

    public boolean shouldIgnoreInvalidate() {
        return this.ignoreInvalidate;
    }

    public boolean isDirty() {
        return this.isDirty;
    }

    public boolean containsConnection(Connection conn) {
        return this.connectionToPathPoints != null && this.connectionToPathPoints.containsKey(conn);
    }


    @Override
    public void setConstraint(Connection connection, Object constraint) {
        this.staleConnections.add(connection);
        this.constraintMap.put(connection, constraint);
        this.isDirty = true;
    }

    private class LayoutTracker extends Stub {
        private LayoutTracker() {
        }

        public void postLayout(IFigure container) {
            MikamiTabuchiConnectionRouter.this.processLayout();
        }

        public void remove(IFigure child) {
            MikamiTabuchiConnectionRouter.this.removeChild(child);
        }

        public void setConstraint(IFigure child, Object constraint) {
            MikamiTabuchiConnectionRouter.this.addChild(child);
        }
    }

    private void processLayout() {
        if (!this.staleConnections.isEmpty()) {
            this.staleConnections.iterator().next().revalidate();
        }
    }
}
