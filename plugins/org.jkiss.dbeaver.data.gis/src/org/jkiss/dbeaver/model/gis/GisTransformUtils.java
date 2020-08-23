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
 */
package org.jkiss.dbeaver.model.gis;

import org.cts.CRSFactory;
import org.cts.IllegalCoordinateException;
import org.cts.crs.CRSException;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.crs.GeodeticCRS;
import org.cts.crs.ProjectedCRS;
import org.cts.op.CoordinateOperation;
import org.cts.op.CoordinateOperationException;
import org.cts.op.CoordinateOperationFactory;
import org.cts.registry.EPSGRegistry;
import org.cts.registry.RegistryException;
import org.cts.registry.RegistryManager;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.List;
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
        //registryManager.addRegistry(new IGNFRegistry());
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

    private static List<Integer> crsCodes;

    public static CRSFactory getCRSFactory() {
        return crsFactory;
    }

    public static synchronized List<Integer> getSortedEPSGCodes() {
        if (crsCodes == null) {
            crsCodes = new ArrayList<>();

            try {
                for (String code : crsFactory.getSupportedCodes(GisConstants.GIS_REG_EPSG)) {
                    crsCodes.add(CommonUtils.toInt(code));
                }
                crsCodes.sort(Integer::compareTo);
            } catch (RegistryException e) {
                log.debug(e);
            }
        }
        return crsCodes;
    }

    public static void transformGisData(GisTransformRequest request) throws DBException {
        try {
            //srcSRID = 3857;
            CoordinateReferenceSystem crs1 = crsFactory.getCRS("EPSG:" + request.getSourceSRID());
            CoordinateReferenceSystem crs2 = crsFactory.getCRS("EPSG:" + request.getTargetSRID());

            try {
                Geometry targetValue = transformGisData(request.getSourceValue(), crs1, crs2);
                targetValue.setSRID(request.getTargetSRID());
                request.setTargetValue(targetValue);
                request.setShowOnMap(true);
            } catch (IllegalArgumentException e) {
                if (CommonUtils.equalObjects(crs1.getCoordinateSystem(), crs3857.getCoordinateSystem())) {
                    Geometry geometry = transformGisData(request.getSourceValue(), crs3857, crs2);
                    geometry.setSRID(request.getTargetSRID());
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
            	CoordinateOperation op = CoordinateOperationFactory.getMostPrecise(coordOps);
                // Transform coord using the op CoordinateOperation from crs1 to crs2
                jtsValue = transformGeometry(jtsValue, op);
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
            coord.x = targetCoord[0];
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

    public static DBGeometry getGeometryValueFromObject(DBSDataContainer dataContainer, DBDValueHandler valueHandler, DBSTypedObject valueType, Object cellValue) {
        if (cellValue instanceof DBGeometry) {
            return (DBGeometry) cellValue;
        }

        // Convert value from string, binary or some other format.
        // This may be needed if use some attribute transformer or some datasource
        // uses plain string data type with GIS value manager.
        // Use void monitor because this transformation shouldn't interact with
        // any external systems or make db queries.
        try (DBCSession utilSession = DBUtils.openUtilSession(new VoidProgressMonitor(), dataContainer, "Convert GIS value"))  {
            Object convertedValue = valueHandler.getValueFromObject(
                utilSession,
                valueType,
                cellValue,
                false, false);
            if (convertedValue instanceof DBGeometry) {
                return (DBGeometry) convertedValue;
            }
        } catch (DBCException e) {
            log.debug("Error transforming geometry value", e);
        }

        return null;
    }

    public static SpatialDataProvider getSpatialDataProvider(DBPDataSource dataSource) {
        if (dataSource instanceof IAdaptable) {
            return ((IAdaptable) dataSource).getAdapter(SpatialDataProvider.class);
        }
        return null;
    }
}
