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
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.crs.GeodeticCRS;
import org.cts.op.CoordinateOperation;
import org.cts.op.CoordinateOperationFactory;
import org.cts.registry.*;
import org.jkiss.dbeaver.DBException;

import java.util.Set;

/**
 * GisUtils.
 */
public class GisUtils {

    private static CRSFactory crsFactory = new CRSFactory();

    static {
        RegistryManager registryManager = crsFactory.getRegistryManager();
        registryManager.addRegistry(new EPSGRegistry());
        registryManager.addRegistry(new ESRIRegistry());
        registryManager.addRegistry(new IGNFRegistry());
        registryManager.addRegistry(new Nad83Registry());
        registryManager.addRegistry(new WorldRegistry());
    }

    public static Object transformGisData(Geometry jtsValue, int srcSRID, int targetSRID) throws DBException {
        try {
            //srcSRID = 3857;
            CoordinateReferenceSystem crsDefault = crsFactory.getCRS("EPSG:3857");
            CoordinateReferenceSystem crs1 = crsFactory.getCRS("EPSG:" + srcSRID);
            CoordinateReferenceSystem crs2 = crsFactory.getCRS("EPSG:" + targetSRID);

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
        } catch (Exception e) {
            throw new DBException("Error transforming SRIDs", e);
        }

        return jtsValue;
    }

    private static Geometry transformGeometry(Geometry geom, CoordinateOperation op) throws Exception {
        geom = (Geometry) geom.clone();
        for (Coordinate coord : geom.getCoordinates()) {
            double[] srcCoord;
            if (Double.isNaN(coord.z)) {
                srcCoord = new double[]{coord.x, coord.y};
            } else {
                srcCoord = new double[]{coord.x, coord.y, coord.z};
            }
            double[] targetCoord = op.transform(srcCoord);
            if (targetCoord != null) {
                coord.x = targetCoord[0];
                coord.y = targetCoord[1];
                if (targetCoord.length > 2) {
                    coord.z = targetCoord[2];
                }
            }
        }
        return geom;

    }

}
