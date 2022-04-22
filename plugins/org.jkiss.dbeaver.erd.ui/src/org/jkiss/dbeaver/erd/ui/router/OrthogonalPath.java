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

import org.eclipse.draw2dl.Connection;
import org.eclipse.draw2dl.geometry.Point;
import org.eclipse.draw2dl.geometry.PointList;

public class OrthogonalPath {
    private boolean isDirty;
    private PointList bendPoints;
    private Connection connection;
    Point start;
    Point end;

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    public PointList getBendPoints() {
        return bendPoints;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public OrthogonalPath(Connection conn) {
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

    public void setBendPoints(PointList points) {
        this.bendPoints = points;
        this.isDirty = true;
    }
}
