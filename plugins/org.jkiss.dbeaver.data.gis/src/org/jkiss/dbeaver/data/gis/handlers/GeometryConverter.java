package org.jkiss.dbeaver.data.gis.handlers;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;
import org.locationtech.jts.io.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Converter JTS Geometry to/from byte array.
 * Inspired by
 * <a href="http://www.dev-garden.org/2011/11/27/loading-mysql-spatial-data-with-jdbc-and-jts-wkbreader/">Loading
 * MySQL Spatial Data with JDBC and JTS WKBReader</a>. Using Object instead
 * of byte[] because of codegen.
 */
public class GeometryConverter {
    public static final InvertCoordinateFilter INVERT_COORDINATE_FILTER = new InvertCoordinateFilter();
    /**
     * Little endian or Big endian
     */
    private int byteOrder = ByteOrderValues.LITTLE_ENDIAN;
    /**
     * Precision model
     */
    private static PrecisionModel precisionModel = new PrecisionModel();
    /**
     * Coordinate sequence factory
     */
    private static CoordinateSequenceFactory coordinateSequenceFactory = CoordinateArraySequenceFactory.instance();
    /**
     * Output dimension
     */
    private static int outputDimension = 2;

    private static final GeometryConverter INSTANCE = new GeometryConverter();

    public static GeometryConverter getInstance() {
        return INSTANCE;
    }

    public Geometry fromWKT(String str) {
        try {
            return new WKTReader().read(str);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Convert byte array containing SRID + WKB Geometry into Geometry object
     */
    public Geometry fromWKB(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            // Read SRID
            byte[] sridBytes = new byte[4];
            inputStream.read(sridBytes);
            int srid = ByteOrderValues.getInt(sridBytes, byteOrder);

            // Prepare Geometry factory
            GeometryFactory geometryFactory = new GeometryFactory(precisionModel, srid, coordinateSequenceFactory);

            // Read Geometry
            WKBReader wkbReader = new WKBReader(geometryFactory);
            Geometry geometry = wkbReader.read(new InputStreamInStream(inputStream));

            return geometry;
        } catch (IOException | ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Convert Geometry object into byte array containing SRID + WKB Geometry
     */
    public byte[] toWKB(Geometry userObject) {
        if (userObject == null) {
            return null;
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Write SRID
            byte[] sridBytes = new byte[4];
            ByteOrderValues.putInt(userObject.getSRID(), sridBytes, byteOrder);
            outputStream.write(sridBytes);
            // Write Geometry
            WKBWriter wkbWriter = new WKBWriter(outputDimension, byteOrder);
            wkbWriter.write(userObject, new OutputStreamOutStream(outputStream));
            return outputStream.toByteArray();
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    private static class InvertCoordinateFilter implements CoordinateFilter {
        public void filter(Coordinate coord) {
            double oldX = coord.x;
            coord.x = coord.y;
            coord.y = oldX;
        }
    }

}