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
package org.jkiss.dbeaver.erd.ui.notation.crowsfoot;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.jkiss.dbeaver.erd.ui.notations.ERDAssociationType;

public class CrowsFootPolylineDecoration extends PolylineDecoration {
    private static PointList geometryList = new PointList();
    private ERDAssociationType association;
    private static final int DIAMETER = 8;

    static {
        // points: crowsfoot
        geometryList.addPoint(0, -1);
        geometryList.addPoint(-2, 0);
        geometryList.addPoint(0, 1);
        // points: dash 1
        geometryList.addPoint(-2, -1);
        geometryList.addPoint(-2, 1);
        // points: dash 2
        geometryList.addPoint(-3, -1);
        geometryList.addPoint(-3, 1);
        // points: circle
        geometryList.addPoint(-3, 0);
        // top
        geometryList.addPoint(-6, 0);
    }

    public CrowsFootPolylineDecoration(ERDAssociationType association) {
        setTemplate(geometryList);
        setScale(5, 5);
        this.association = association;
    }

    @Override
    protected void outlineShape(Graphics g) {
        PointList crowsFootPoints = getPoints();
        Point point;
        int radius = DIAMETER / 2;
        switch (association) {
            case ZERO_OR_ONE:
                g.drawLine(crowsFootPoints.getPoint(3), crowsFootPoints.getPoint(4));
                point = crowsFootPoints.getPoint(7);
                g.fillOval(point.x - 4, point.y - 4, 8, 8);
                g.drawOval(point.x - 4, point.y - 4, 8, 8);
                break;
            case ONE_ONLY:
                g.drawLine(crowsFootPoints.getPoint(3), crowsFootPoints.getPoint(4));
                g.drawLine(crowsFootPoints.getPoint(5), crowsFootPoints.getPoint(6));
                break;
            case ZERO:
                point = crowsFootPoints.getPoint(7);
                g.fillOval(point.x - 4, point.y - 4, 8, 8);
                g.drawOval(point.x - 4, point.y - 4, 8, 8);
                break;
            case MANY:
                g.drawLine(crowsFootPoints.getPoint(0), crowsFootPoints.getPoint(1));
                g.drawLine(crowsFootPoints.getPoint(1), crowsFootPoints.getPoint(2));
                break;
            case ONE_OR_MANY:
                g.drawLine(crowsFootPoints.getPoint(0), crowsFootPoints.getPoint(1));
                g.drawLine(crowsFootPoints.getPoint(1), crowsFootPoints.getPoint(2));
                g.drawLine(crowsFootPoints.getPoint(3), crowsFootPoints.getPoint(4));
                break;
            case ZERO_OR_MANY:
                g.drawLine(crowsFootPoints.getPoint(0), crowsFootPoints.getPoint(1));
                g.drawLine(crowsFootPoints.getPoint(1), crowsFootPoints.getPoint(2));
                point = crowsFootPoints.getPoint(7);
                g.fillOval(point.x - radius, point.y - radius, DIAMETER, DIAMETER);
                g.drawOval(point.x - radius, point.y - radius, DIAMETER, DIAMETER);
                break;
            default:
                // no default behavior
                break;
        }
    }
}
