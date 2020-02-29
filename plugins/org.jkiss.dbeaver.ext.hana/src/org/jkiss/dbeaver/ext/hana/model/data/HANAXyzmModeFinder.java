/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
 *
 * Contributors:
 *    Stefan Uhrig - initial implementation
 */
package org.jkiss.dbeaver.ext.hana.model.data;

import org.jkiss.dbeaver.ext.hana.model.data.wkb.XyzmMode;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Find the XYZM mode of a geometry instance.
 *
 * JTS coordinate sequences are often three-dimensional, but their coordinates
 * don't have a third ordinate (i.e. it is NaN). This class checks the
 * coordinate sequences and checks if all points have a z-coordinate and if any
 * point has a m-coordinate to find the proper XYZM mode.
 */
public class HANAXyzmModeFinder {

    private boolean allHaveZ = true;

    private boolean someHaveM = false;

    /**
     * Finds the XYZM mode of a geometry.
     *
     * @param g
     *            A geometry. Must not be null.
     * @return Returns the appropriate XYZM mode.
     */
    public static XyzmMode findXyzmMode(Geometry g) {
        HANAXyzmModeFinder instance = new HANAXyzmModeFinder();
        return instance.inspect(g);
    }

    private HANAXyzmModeFinder() {
    }

    private XyzmMode inspect(Geometry g) {
        inspectGeometry(g);
        if (allHaveZ && someHaveM) {
            return XyzmMode.XYZM;
        } else if (allHaveZ) {
            return XyzmMode.XYZ;
        } else if (someHaveM) {
            return XyzmMode.XYM;
        } else {
            return XyzmMode.XY;
        }
    }

    private void inspectGeometry(Geometry g) {
        if (g instanceof Point) {
            inspectPoint((Point) g);
        } else if (g instanceof LineString) {
            inspectLineString((LineString) g);
        } else if (g instanceof Polygon) {
            inspectPolygon((Polygon) g);
        } else if (g instanceof GeometryCollection) {
            inspectCollection((GeometryCollection) g);
        } else {
            throw new AssertionError("Unknown geometry type " + g.getClass());
        }
    }

    private void inspectPoint(Point p) {
        inspectSequence(p.getCoordinateSequence());
    }

    private void inspectLineString(LineString ls) {
        inspectSequence(ls.getCoordinateSequence());
    }

    private void inspectPolygon(Polygon pg) {
        inspectLineString(pg.getExteriorRing());
        int numHoles = pg.getNumInteriorRing();
        for (int i = 0; i < numHoles; ++i) {
            inspectLineString(pg.getInteriorRingN(i));
        }
    }

    private void inspectCollection(GeometryCollection gc) {
        int numGeometries = gc.getNumGeometries();
        for (int i = 0; i < numGeometries; ++i) {
            inspectGeometry(gc.getGeometryN(i));
        }
    }

    private void inspectSequence(CoordinateSequence cs) {
        int size = cs.size();
        if (size == 0) {
            if (!cs.hasZ()) {
                allHaveZ = false;
            }
            if (cs.hasM()) {
                someHaveM = true;
            }
        }
        for (int i = 0; i < size; ++i) {
            if (!Double.isFinite(cs.getZ(i))) {
                allHaveZ = false;
            }
            if (Double.isFinite(cs.getM(i))) {
                someHaveM = true;
            }
        }
    }
}
