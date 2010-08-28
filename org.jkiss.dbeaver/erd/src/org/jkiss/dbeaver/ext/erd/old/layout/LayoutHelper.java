/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.layout;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Polygon;

/** LayoutHelper is a utility class which mainly returns various types
 *  of routing polygons for different kind of connection lines between
 *  two nodes. Specific layouters might use these methods to reuse certain
 *  kinds of diagram lines.
 *  @stereotype utility
*/
public class LayoutHelper {

    /**
     * A constant bitmask for a direction.
     */
    public static final int NORTH = 0;

    /**
     * A constant bitmask for a direction.
     */
    public static final int NORTHEAST = 1;

    /**
     * A constant bitmask for a direction.
     */
    public static final int EAST = 2;

    /**
     * A constant bitmask for a direction.
     */
    public static final int SOUTHEAST = 4;

    /**
     * A constant bitmask for a direction.
     */
    public static final int SOUTH = 8;

    /**
     * A constant bitmask for a direction.
     */
    public static final int SOUTHWEST = 16;

    /**
     * A constant bitmask for a direction.
     */
    public static final int WEST = 32;

    /**
     * A constant bitmask for a direction.
     */
    public static final int NORTHWEST = 64;

    /**
     * @param rect the rectangle
     * @param direction the direction
     * @return the point on the perimeter
     */
    public static Point getPointOnPerimeter(Rectangle rect, int direction) {
        return getPointOnPerimeter(rect, direction, 0, 0);
    }

    /**
     * @param rect the rectangle
     * @param direction the direction
     * @param xOff the x offset
     * @param yOff the y offset
     * @return the point on the perimeter
     */
    public static Point getPointOnPerimeter(Rectangle rect, int direction,
					    double xOff, double yOff) {
        double x = 0;
        double y = 0;
        if (direction == NORTH
                || direction == NORTHEAST
                || direction == NORTHWEST) {
            y = rect.getY();
        }
        if (direction == SOUTH
                || direction == SOUTHWEST
                || direction == SOUTHEAST) {
            y = rect.getY() + rect.getHeight();
        }
        if (direction == EAST
                || direction == WEST) {
            y = rect.getY() + rect.getHeight() / 2.0;
	}
        if (direction == NORTHWEST
                || direction == WEST
                || direction == SOUTHWEST) {
            x = rect.getX();
        }
        if (direction == NORTHEAST
                || direction == EAST
                || direction == SOUTHEAST) {
            x = rect.getX() + rect.getWidth();
        }
        if (direction == NORTH || direction == SOUTH) {
            x = rect.getX() + rect.getWidth() / 2.0;
	}

        x += xOff;
        y += yOff;
        return new Point((int) x, (int) y);
    }

    /**
     * Get a routing polygon for a straightline between two points.
     *
     * @param start start of the line
     * @param end end of the line
     * @return the routing polygon between start and end
     */
    public static Polygon getRoutingPolygonStraightLine(Point start, Point end)
    {
        return getRoutingPolygonStraightLineWithOffset(start, end, 0);
    }

    /**
     * Get a routing polygon with a horizontal offset from the two points.
     *
     * @param start start of the line
     * @param end end of the line
     * @param offset the given offset
     * @return the routing polygon between start and end
     */
    public static Polygon getRoutingPolygonStraightLineWithOffset(Point start,
                                                Point end, int offset) {
        Polygon newPoly = new Polygon();

        newPoly.addPoint((int) start.getX(), (int) start.getY());
        if (offset != 0) {
            double newY = 0.0;
            if (offset < 0) {
                newY =
                    Math.min(start.getY() + offset, end.getY() + offset);
            }
            if (offset > 0) {
                newY =
                    Math.max(start.getY() + offset, end.getY() + offset);
            }
            newPoly.addPoint((int) start.getX(), (int) newY);
            newPoly.addPoint((int) end.getX(), (int) newY);

        }
        newPoly.addPoint((int) end.getX(), (int) end.getY());
        return newPoly;
    }

}

