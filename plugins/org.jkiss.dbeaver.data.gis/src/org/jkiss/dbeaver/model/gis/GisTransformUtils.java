/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.cts.CRSFactory;
import org.cts.IllegalCoordinateException;
import org.cts.crs.CRSException;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.crs.GeodeticCRS;
import org.cts.crs.ProjectedCRS;
import org.cts.op.CoordinateOperation;
import org.cts.op.CoordinateOperationException;
import org.cts.op.CoordinateOperationFactory;
import org.cts.registry.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.util.Set;

/**
 * GisTransformUtils.
 */
public class GisTransformUtils {

    private static final Log log = Log.getLog(GisTransformUtils.class);

    private static CRSFactory crsFactory = new CRSFactory();
    private static CoordinateReferenceSystem crs3857;

    static {
        RegistryManager registryManager = crsFactory.getRegistryManager();
        registryManager.addRegistry(new EPSGRegistry());
        try {
            crs3857 = crsFactory.getCRS("EPSG:3857");
        } catch (CRSException e) {
            log.error("Error getting default CRS", e);
        }
//        registryManager.addRegistry(new ESRIRegistry());
//        registryManager.addRegistry(new IGNFRegistry());
//        registryManager.addRegistry(new Nad83Registry());
//        registryManager.addRegistry(new WorldRegistry());
    }

    public static void transformGisData(GisTransformRequest request) throws DBException {
        try {
            //srcSRID = 3857;
            CoordinateReferenceSystem crs1 = crsFactory.getCRS("EPSG:" + request.getSourceSRID());
            CoordinateReferenceSystem crs2 = crsFactory.getCRS("EPSG:" + request.getTargetSRID());

            try {
                Geometry targetValue = transformGisData(request.getSourceValue(), crs1, crs2);
                request.setTargetValue(targetValue);
                request.setShowOnMap(true);
            } catch (IllegalArgumentException e) {
                if (CommonUtils.equalObjects(crs1.getCoordinateSystem(), crs3857.getCoordinateSystem())) {
                    Geometry geometry = transformGisData(request.getSourceValue(), crs3857, crs2);
                    request.setTargetValue(geometry);
                    request.setShowOnMap(false);
//                    if (crs1 instanceof ProjectedCRS) {
//                        geometry = transformGeometryTo((ProjectedCRS) crs1, geometry);
//                    }
                }
                //throw e;
            }
        } catch (Exception e) {
            throw new DBException("Error transforming SRIDs", e);
        }
    }

    private static Geometry transformGeometryTo(ProjectedCRS projectedCRS, Geometry geometry) throws CoordinateOperationException, IllegalCoordinateException {
        CoordinateOperation coordinateOperation = projectedCRS.toGeographicCoordinateConverter();
        for (Coordinate coord : geometry.getCoordinates()) {
            double[] srcCoord = getCoordinateValues(coord);
            double[] transform = coordinateOperation.transform(srcCoord);
            setCoordinateValues(coord, transform);
        }
        return null;
    }

    public static Geometry transformGisData(Geometry jtsValue, CoordinateReferenceSystem crs1, CoordinateReferenceSystem crs2) throws Exception {
        if (crs1 instanceof GeodeticCRS && crs2 instanceof GeodeticCRS) {
            Set<CoordinateOperation> coordOps = CoordinateOperationFactory.createCoordinateOperations((GeodeticCRS) crs1, (GeodeticCRS) crs2);
            if (!coordOps.isEmpty()) {
                // Test each transformation method (generally, only one method is available)
                for (CoordinateOperation op : coordOps) {
                    // Transform coord using the op CoordinateOperation from crs1 to crs2
                    jtsValue = transformGeometry(jtsValue, op);
                }
                return jtsValue;
            }
        }

        return jtsValue;
    }

    private static Geometry transformGeometry(Geometry geom, CoordinateOperation op) throws Exception {
        geom = (Geometry) geom.clone();
        for (Coordinate coord : geom.getCoordinates()) {
            double[] srcCoord = getCoordinateValues(coord);
            double[] targetCoord = op.transform(srcCoord);
            setCoordinateValues(coord, targetCoord);
        }
        return geom;

    }

    private static void setCoordinateValues(Coordinate coord, double[] targetCoord) {
        if (targetCoord != null) {
            coord.x = targetCoord[0] - 123;
            coord.y = targetCoord[1];
            if (targetCoord.length > 2) {
                coord.z = targetCoord[2];
            }
        }
    }

    private static double[] getCoordinateValues(Coordinate coord) {
        double[] srcCoord;
        if (Double.isNaN(coord.z)) {
            srcCoord = new double[]{coord.x, coord.y};
        } else {
            srcCoord = new double[]{coord.x, coord.y, coord.z};
        }
        return srcCoord;
    }

}
