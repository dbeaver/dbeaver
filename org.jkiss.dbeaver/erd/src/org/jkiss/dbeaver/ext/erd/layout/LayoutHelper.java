// $Id: LayoutHelper.java 32 2006-07-04 14:50:54Z harrigan $
// Copyright (c) 2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.jkiss.dbeaver.ext.erd.layout;

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

