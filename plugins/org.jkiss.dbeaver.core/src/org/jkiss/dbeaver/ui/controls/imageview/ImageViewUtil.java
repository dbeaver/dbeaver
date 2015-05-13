/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.controls.imageview;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;


/**
 * Utility for Java2d transform
 *
 * @author Chengdong Li: cli4@uky.edu
 */
class ImageViewUtil {

    /**
     * Given an arbitrary rectangle, get the rectangle with the given transform.
     * The result rectangle is positive width and positive height.
     *
     * @param af  AffineTransform
     * @param src source rectangle
     * @return rectangle after transform with positive width and height
     */
    public static Rectangle transformRect(AffineTransform af, Rectangle src) {
        Rectangle dest = new Rectangle(0, 0, 0, 0);
        src = absRect(src);
        Point p1 = new Point(src.x, src.y);
        p1 = transformPoint(af, p1);
        dest.x = p1.x;
        dest.y = p1.y;
        dest.width = (int) (src.width * af.getScaleX());
        dest.height = (int) (src.height * af.getScaleY());
        return dest;
    }

    /**
     * Given an arbitrary rectangle, get the rectangle with the inverse given transform.
     * The result rectangle is positive width and positive height.
     *
     * @param af  AffineTransform
     * @param src source rectangle
     * @return rectangle after transform with positive width and height
     */
    public static Rectangle inverseTransformRect(AffineTransform af, Rectangle src) {
        Rectangle dest = new Rectangle(0, 0, 0, 0);
        src = absRect(src);
        Point p1 = new Point(src.x, src.y);
        p1 = inverseTransformPoint(af, p1);
        dest.x = p1.x;
        dest.y = p1.y;
        dest.width = (int) (src.width / af.getScaleX());
        dest.height = (int) (src.height / af.getScaleY());
        return dest;
    }

    /**
     * Given an arbitrary point, get the point with the given transform.
     *
     * @param af affine transform
     * @param pt point to be transformed
     * @return point after tranform
     */
    public static Point transformPoint(AffineTransform af, Point pt) {
        Point2D src = new Point2D.Float(pt.x, pt.y);
        Point2D dest = af.transform(src, null);
        Point point = new Point((int) Math.floor(dest.getX()), (int) Math.floor(dest.getY()));
        return point;
    }

    /**
     * Given an arbitrary point, get the point with the inverse given transform.
     *
     * @param af AffineTransform
     * @param pt source point
     * @return point after transform
     */
    public static Point inverseTransformPoint(AffineTransform af, Point pt) {
        Point2D src = new Point2D.Float(pt.x, pt.y);
        try {
            Point2D dest = af.inverseTransform(src, null);
            return new Point((int) Math.floor(dest.getX()), (int) Math.floor(dest.getY()));
        } catch (Exception e) {
            e.printStackTrace();
            return new Point(0, 0);
        }
    }

    /**
     * Given arbitrary rectangle, return a rectangle with upper-left
     * start and positive width and height.
     *
     * @param src source rectangle
     * @return result rectangle with positive width and height
     */
    public static Rectangle absRect(Rectangle src) {
        Rectangle dest = new Rectangle(0, 0, 0, 0);
        if (src.width < 0) {
            dest.x = src.x + src.width + 1;
            dest.width = -src.width;
        } else {
            dest.x = src.x;
            dest.width = src.width;
        }
        if (src.height < 0) {
            dest.y = src.y + src.height + 1;
            dest.height=-src.height; }
		else{ dest.y=src.y; dest.height=src.height; }
		return dest;
	}
}
