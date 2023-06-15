/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;

public class OrthogonalPath {
    private boolean isDirty;
    private PointList points;
    private PointList bendpoints;
    private Connection connection;
    private Direction forbiddenDirection;

    private boolean sourceIsChild;
    private boolean targetIsChild;

    Point start;
    Point end;

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public void reset() {
        sourceIsChild = false;
        targetIsChild = false;
        forbiddenDirection = null;
    }

    public void updateForbiddenDirection(Point origin) {
        if (start.x == origin.x) {
            if (start.y > origin.y) {
                forbiddenDirection = Direction.DOWN;
            } else {
                forbiddenDirection = Direction.UP;
            }
        }
        if (start.y == origin.y) {
            if (start.x > origin.x) {
                forbiddenDirection = Direction.LEFT;
            } else {
                forbiddenDirection = Direction.RIGHT;
            }
        }
    }

    public boolean isSourceIsChild() {
        return sourceIsChild;
    }

    public void setSourceIsChild(boolean child) {
        sourceIsChild = child;
    }

    public boolean isTargetIsChild() {
        return targetIsChild;
    }

    public void setTargetIsChild(boolean child) {
        targetIsChild = child;
    }

    public Direction getForbiddenDirection() {
        return forbiddenDirection;
    }

    public void setForbiddenDirection(Direction forbiddenDirection) {
        this.forbiddenDirection = forbiddenDirection;
    }

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    public PointList getPoints() {
        return points;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public OrthogonalPath(Connection conn) {
        setDirty(true);
        this.connection = conn;
    }

    public void setStartPoint(Point start) {
        if (!start.equals(this.start)) {
            this.start = start;
            this.isDirty = true;
        }

    }

    public void setEndPoint(Point end) {
        if (!end.equals(this.end)) {
            this.end = end;
            this.isDirty = true;
        }
    }

    public void setPoints(PointList points) {
        this.points = points;
    }

    public PointList getBendpoints() {
        return bendpoints;
    }

    public void setBendpoints(PointList bendpoints) {
        this.bendpoints = bendpoints;
        this.setDirty(true);
    }

    public enum Direction {
        UP,
        DOWN,
        RIGHT,
        LEFT
    }

}
