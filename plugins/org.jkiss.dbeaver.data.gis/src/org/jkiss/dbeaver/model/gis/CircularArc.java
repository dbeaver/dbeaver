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
package org.jkiss.dbeaver.model.gis;

import org.cugos.wkg.Coordinate;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.decomposition.lu.LUDecompositionAlt_DDRM;
import org.ejml.dense.row.linsol.lu.LinearSolverLu_DDRM;
import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Taken from the GeoTools library, with small adjustments
 *
 * @see <a href="https://github.com/geotools/geotools/blob/main/modules/library/main/src/main/java/org/geotools/geometry/jts/CircularArc.java">CircularArc.java</a>
 */
public class CircularArc {
    private static final int DEFAULT_SEGMENTS_QUADRANT = 12;
    private static final int MAXIMUM_SEGMENTS_QUADRANT = 10000;
    private static final double HALF_PI = Math.PI / 2;
    private static final double DOUBLE_PI = Math.PI * 2;

    private final Coordinate[] controlPoints;
    private double radius;
    private double centerX;
    private double centerY;

    public CircularArc(@NotNull Coordinate[] controlPoints) {
        this.radius = Double.NaN;

        if (controlPoints.length == 3) {
            this.controlPoints = controlPoints;
        } else {
            throw new IllegalArgumentException("Invalid control point array, it must be made of of 3 control points, start, mid and end");
        }
    }

    public CircularArc(@NotNull Coordinate start, @NotNull Coordinate mid, @NotNull Coordinate end) {
        this(new Coordinate[]{start, mid, end});
    }

    @NotNull
    public List<Coordinate> linearize(double tolerance) {
        initializeCenterRadius();

        if (radius != Double.POSITIVE_INFINITY && radius != 0.0D) {
            return linearize(tolerance, new ArrayList<>());
        } else {
            return List.of(controlPoints);
        }
    }

    @NotNull
    private List<Coordinate> linearize(double tolerance, @NotNull List<Coordinate> array) {
        initializeCenterRadius();

        double sx = controlPoints[0].getX();
        double sy = controlPoints[0].getY();
        double mx = controlPoints[1].getX();
        double my = controlPoints[1].getY();
        double ex = controlPoints[2].getX();
        double ey = controlPoints[2].getY();
        double sa = Math.atan2(sy - centerY, sx - centerX);
        double ma = Math.atan2(my - centerY, mx - centerX);
        double ea = Math.atan2(ey - centerY, ex - centerX);
        boolean clockwise = sa > ma && ma > ea || sa > ma && sa < ea || ma > ea && sa < ea;

        if (clockwise) {
            double tx = sx;
            double ty = sy;
            double ta = sa;

            sx = ex;
            ex = tx;
            sy = ey;
            ey = ty;
            sa = ea;
            ea = ta;
        }

        if (ma < sa) {
            ma += DOUBLE_PI;
            ea += DOUBLE_PI;
        } else if (ea < sa) {
            ea += DOUBLE_PI;
        }

        double step = HALF_PI / computeSegmentsPerQuadrant(tolerance);
        double angle = (Math.floor(sa / step) + 1.0D) * step;

        if (angle <= ea) {
            int start = array.size();

            array.add(Coordinate.create2D(sx, sy));

            if (angle > ma) {
                array.add(Coordinate.create2D(mx, my));

                if (equals(angle, ma)) {
                    angle += step;
                }
            }

            for (double next, end = ea - 1.0E-12D; angle < end; angle = next) {
                final double cx = centerX + radius * Math.cos(angle);
                final double cy = centerY + radius * Math.sin(angle);

                array.add(Coordinate.create2D(cx, cy));
                next = angle + step;

                if (angle < ma && next > ma && !equals(angle, ma) && !equals(next, ma)) {
                    array.add(Coordinate.create2D(mx, my));
                }
            }

            array.add(Coordinate.create2D(ex, ey));

            if (clockwise) {
                Collections.reverse(array.subList(start, array.size()));
            }
        } else {
            array.addAll(List.of(controlPoints));
        }

        return array;
    }

    private int computeSegmentsPerQuadrant(double tolerance) {
        if (tolerance < 0.0D) {
            throw new IllegalArgumentException("The tolerance must be a positive number, " +
                "zero to use the default number of segments per quadrant (" + DEFAULT_SEGMENTS_QUADRANT + "), " +
                "or Double.MAX_VALUE to use the max number of segments per quadrant (" + MAXIMUM_SEGMENTS_QUADRANT + ")");
        }

        int segmentsPerQuadrant;
        double chordDistance;

        if (tolerance == 0.0D) {
            segmentsPerQuadrant = DEFAULT_SEGMENTS_QUADRANT;
        } else if (tolerance == Double.MAX_VALUE) {
            segmentsPerQuadrant = MAXIMUM_SEGMENTS_QUADRANT;
        } else {
            segmentsPerQuadrant = 2;
            chordDistance = computeChordCircleDistance(segmentsPerQuadrant);

            if (chordDistance >= tolerance) {
                while (chordDistance > tolerance && segmentsPerQuadrant < MAXIMUM_SEGMENTS_QUADRANT) {
                    segmentsPerQuadrant *= 2;
                    chordDistance = computeChordCircleDistance(segmentsPerQuadrant);
                }
            } else {
                while (chordDistance < tolerance && segmentsPerQuadrant > 1) {
                    segmentsPerQuadrant /= 2;
                    chordDistance = computeChordCircleDistance(segmentsPerQuadrant);
                }

                if (chordDistance > tolerance) {
                    segmentsPerQuadrant *= 2;
                }
            }
        }

        return segmentsPerQuadrant;
    }

    private double computeChordCircleDistance(int segmentsPerQuadrant) {
        double halfChordLength = radius * Math.sin(HALF_PI / segmentsPerQuadrant);
        double apothem = Math.sqrt(radius * radius - halfChordLength * halfChordLength);
        return radius - apothem;
    }

    private void initializeCenterRadius() {
        if (!Double.isNaN(radius)) {
            return;
        }

        double sx = controlPoints[0].getX();
        double sy = controlPoints[0].getY();
        double mx = controlPoints[1].getX();
        double my = controlPoints[1].getY();
        double ex = controlPoints[2].getX();
        double ey = controlPoints[2].getY();
        double dx12;
        double dy12;
        double rs;
        double dy13;
        double dx23;
        double rm;
        double sqs1;
        double sqs2;
        double re;

        if (equals(sx, ex) && equals(sy, ey)) {
            centerX = sx + (mx - sx) / 2.0D;
            centerY = sy + (my - sy) / 2.0D;
        } else {
            dx12 = sx - mx;
            dy12 = sy - my;
            rs = sx - ex;
            dy13 = sy - ey;
            dx23 = mx - ex;
            rm = my - ey;
            sqs1 = dx12 * dx12 + dy12 * dy12;
            sqs2 = rs * rs + dy13 * dy13;
            re = dx23 * dx23 + rm * rm;
            DMatrixRMaj b;
            double sqs;
            DMatrixRMaj A;
            if (sqs1 <= re && sqs2 <= re) {
                A = new DMatrixRMaj(2, 2, true, dx12, dy12, rs, dy13);
                b = new DMatrixRMaj(2, 1, true, 0.5D * (dx12 * (sx + mx) + dy12 * (sy + my)), 0.5D * (rs * (sx + ex) + dy13 * (sy + ey)));
                sqs = sqs1 + sqs2;
            } else if (sqs1 <= sqs2 && re <= sqs2) {
                A = new DMatrixRMaj(2, 2, true, dx12, dy12, dx23, rm);
                b = new DMatrixRMaj(2, 1, true, 0.5D * (dx12 * (sx + mx) + dy12 * (sy + my)), 0.5D * (dx23 * (mx + ex) + rm * (my + ey)));
                sqs = sqs1 + re;
            } else {
                A = new DMatrixRMaj(2, 2, true, rs, dy13, dx23, rm);
                b = new DMatrixRMaj(2, 1, true, 0.5D * (rs * (sx + ex) + dy13 * (sy + ey)), 0.5D * (dx23 * (mx + ex) + rm * (my + ey)));
                sqs = sqs2 + re;
            }

            LUDecompositionAlt_DDRM lu = new LUDecompositionAlt_DDRM();
            LinearSolverLu_DDRM solver = new LinearSolverLu_DDRM(lu);
            if (!solver.setA(A)) {
                radius = Double.POSITIVE_INFINITY;
                return;
            }

            double R = 2.0D * Math.abs(lu.computeDeterminant().getReal()) / sqs;
            double k = (1.0D + Math.sqrt(1.0D - R * R)) / R;
            if (k > 20000.0D) {
                radius = Double.POSITIVE_INFINITY;
                return;
            }

            DMatrixRMaj x = new DMatrixRMaj(2, 1);
            solver.solve(b, x);
            centerX = x.get(0);
            centerY = x.get(1);
        }

        rs = Math.sqrt(Math.pow(centerX - sx, 2) + Math.pow(centerY - sy, 2));
        rm = Math.sqrt(Math.pow(centerX - mx, 2) + Math.pow(centerY - my, 2));
        re = Math.sqrt(Math.pow(centerX - ex, 2) + Math.pow(centerY - ey, 2));
        radius = Math.min(Math.max(rs, rm), re);
    }

    private static boolean equals(double a, double b) {
        return Math.abs(a - b) < 1.0E-12D;
    }
}
