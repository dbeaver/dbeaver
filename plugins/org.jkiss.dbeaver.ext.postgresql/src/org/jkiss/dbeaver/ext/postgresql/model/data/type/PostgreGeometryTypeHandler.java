/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.data.type;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.model.gis.DBGeometryDimension;
import org.jkiss.utils.CommonUtils;

public class PostgreGeometryTypeHandler extends PostgreTypeHandler {

    public static final PostgreGeometryTypeHandler INSTANCE = new PostgreGeometryTypeHandler();

    private static final int GEOMETRY_TYPE_GEOMETRY                 = 0x0000_0000;
    private static final int GEOMETRY_TYPE_POINT                    = 0x0000_0004;
    private static final int GEOMETRY_TYPE_LINESTRING               = 0x0000_0008;
    private static final int GEOMETRY_TYPE_POLYGON                  = 0x0000_000C;
    private static final int GEOMETRY_TYPE_MULTIPOINT               = 0x0000_0010;
    private static final int GEOMETRY_TYPE_MULTILINESTRING          = 0x0000_0014;
    private static final int GEOMETRY_TYPE_MULTIPOLYGON             = 0x0000_0018;
    private static final int GEOMETRY_TYPE_GEOMETRYCOLLECTION       = 0x0000_001C;
    private static final int GEOMETRY_TYPE_CIRCULARSTRING           = 0x0000_0020;
    private static final int GEOMETRY_TYPE_COMPOUNDCURVE            = 0x0000_0024;
    private static final int GEOMETRY_TYPE_CURVEPOLYGON             = 0x0000_0028;
    private static final int GEOMETRY_TYPE_MULTICURVE               = 0x0000_002C;
    private static final int GEOMETRY_TYPE_MULTISURFACE             = 0x0000_0030;
    private static final int GEOMETRY_TYPE_POLYHEDRALSURFACE        = 0x0000_0034;
    private static final int GEOMETRY_TYPE_TRIANGLE                 = 0x0000_0038;
    private static final int GEOMETRY_TYPE_TIN                      = 0x0000_003C;
    private static final int GEOMETRY_DIMENSION_M                   = 0x0000_0001;
    private static final int GEOMETRY_DIMENSION_Z                   = 0x0000_0002;
    private static final int GEOMETRY_DIMENSION_ZM                  = 0x0000_0003;
    private static final int GEOMETRY_MASK_TYPE                     = 0x0000_00fc;
    private static final int GEOMETRY_MASK_SRID                     = 0x00ff_ff00;
    private static final int GEOMETRY_MASK_DIMENSION                = 0x0000_0003;

    private static final String GEOMETRY_NAME_GEOMETRY              = "geometry";
    private static final String GEOMETRY_NAME_POINT                 = "point";
    private static final String GEOMETRY_NAME_LINESTRING            = "linestring";
    private static final String GEOMETRY_NAME_POLYGON               = "polygon";
    private static final String GEOMETRY_NAME_MULTIPOINT            = "multipoint";
    private static final String GEOMETRY_NAME_MULTILINESTRING       = "multilinestring";
    private static final String GEOMETRY_NAME_MULTIPOLYGON          = "multipolygon";
    private static final String GEOMETRY_NAME_GEOMETRYCOLLECTION    = "geometrycollection";
    private static final String GEOMETRY_NAME_CIRCULARSTRING        = "circularstring";
    private static final String GEOMETRY_NAME_COMPOUNDCURVE         = "compoundcurve";
    private static final String GEOMETRY_NAME_CURVEPOLYGON          = "curvepolygon";
    private static final String GEOMETRY_NAME_MULTICURVE            = "multicurve";
    private static final String GEOMETRY_NAME_MULTISURFACE          = "multisurface";
    private static final String GEOMETRY_NAME_POLYHEDRALSURFACE     = "polyhedralsurface";
    private static final String GEOMETRY_NAME_TRIANGLE              = "triangle";
    private static final String GEOMETRY_NAME_TIN                   = "tin";

    private PostgreGeometryTypeHandler() {
        // disallow constructing singleton class
    }

    @Override
    public int getTypeModifiers(@NotNull PostgreDataType type, @NotNull String typeName, @NotNull String[] typmod) throws DBException {
        switch (typmod.length) {
            case 0:
                return EMPTY_MODIFIERS;
            case 1:
                return getGeometryModifiers(typmod[0].toLowerCase(), 0);
            case 2:
                return getGeometryModifiers(typmod[0].toLowerCase(), CommonUtils.toInt(typmod[1]));
            default:
                return super.getTypeModifiers(type, typeName, typmod);
        }
    }

    @NotNull
    @Override
    public String getTypeModifiersString(@NotNull PostgreDataType type, int typmod) {
        final StringBuilder sb = new StringBuilder();
        if (typmod >= 0) {
            sb.append('(').append(getGeometryType(typmod));
            final DBGeometryDimension dimension = getGeometryDimension(typmod);
            if (dimension.hasZ()) {
                sb.append('z');
            }
            if (dimension.hasM()) {
                sb.append('m');
            }
            final int srid = getGeometrySRID(typmod);
            if (srid > 0) {
                sb.append(", ").append(srid);
            }
            sb.append(')');
        }
        return sb.toString();
    }

    @Nullable
    public static String getGeometryType(int typmod) {
        if (typmod < 0) {
            return null;
        }
        switch ((typmod & GEOMETRY_MASK_TYPE)) {
            case GEOMETRY_TYPE_GEOMETRY:
                return GEOMETRY_NAME_GEOMETRY;
            case GEOMETRY_TYPE_POINT:
                return GEOMETRY_NAME_POINT;
            case GEOMETRY_TYPE_LINESTRING:
                return GEOMETRY_NAME_LINESTRING;
            case GEOMETRY_TYPE_POLYGON:
                return GEOMETRY_NAME_POLYGON;
            case GEOMETRY_TYPE_MULTIPOINT:
                return GEOMETRY_NAME_MULTIPOINT;
            case GEOMETRY_TYPE_MULTILINESTRING:
                return GEOMETRY_NAME_MULTILINESTRING;
            case GEOMETRY_TYPE_MULTIPOLYGON:
                return GEOMETRY_NAME_MULTIPOLYGON;
            case GEOMETRY_TYPE_GEOMETRYCOLLECTION:
                return GEOMETRY_NAME_GEOMETRYCOLLECTION;
            case GEOMETRY_TYPE_CIRCULARSTRING:
                return GEOMETRY_NAME_CIRCULARSTRING;
            case GEOMETRY_TYPE_COMPOUNDCURVE:
                return GEOMETRY_NAME_COMPOUNDCURVE;
            case GEOMETRY_TYPE_CURVEPOLYGON:
                return GEOMETRY_NAME_CURVEPOLYGON;
            case GEOMETRY_TYPE_MULTICURVE:
                return GEOMETRY_NAME_MULTICURVE;
            case GEOMETRY_TYPE_MULTISURFACE:
                return GEOMETRY_NAME_MULTISURFACE;
            case GEOMETRY_TYPE_POLYHEDRALSURFACE:
                return GEOMETRY_NAME_POLYHEDRALSURFACE;
            case GEOMETRY_TYPE_TRIANGLE:
                return GEOMETRY_NAME_TRIANGLE;
            case GEOMETRY_TYPE_TIN:
                return GEOMETRY_NAME_TIN;
            default:
                throw new IllegalArgumentException("Error obtaining geometry type from typmod: " + Integer.toHexString(typmod));
        }
    }

    @NotNull
    public static DBGeometryDimension getGeometryDimension(int typmod) {
        switch (typmod & GEOMETRY_MASK_DIMENSION) {
            case GEOMETRY_DIMENSION_M:
                return DBGeometryDimension.XYM;
            case GEOMETRY_DIMENSION_Z:
                return DBGeometryDimension.XYZ;
            case GEOMETRY_DIMENSION_ZM:
                return DBGeometryDimension.XYZM;
            default:
                return DBGeometryDimension.XY;
        }
    }

    public static int getGeometrySRID(int typmod) {
        return (typmod & GEOMETRY_MASK_SRID) >> 8;
    }

    private static int getGeometryModifiers(@NotNull String name, int srid) throws DBException {
        int typmod = (srid & 0xffff) << 8;
        if (name.endsWith("zm")) {
            typmod |= GEOMETRY_DIMENSION_ZM;
            name = name.substring(0, name.length() - 2);
        } else if (name.endsWith("z")) {
            typmod |= GEOMETRY_DIMENSION_Z;
            name = name.substring(0, name.length() - 1);
        } else if (name.endsWith("m")) {
            typmod |= GEOMETRY_DIMENSION_M;
            name = name.substring(0, name.length() - 1);
        }
        switch (name) {
            case GEOMETRY_NAME_GEOMETRY:
                typmod |= GEOMETRY_TYPE_GEOMETRY;
                break;
            case GEOMETRY_NAME_POINT:
                typmod |= GEOMETRY_TYPE_POINT;
                break;
            case GEOMETRY_NAME_LINESTRING:
                typmod |= GEOMETRY_TYPE_LINESTRING;
                break;
            case GEOMETRY_NAME_POLYGON:
                typmod |= GEOMETRY_TYPE_POLYGON;
                break;
            case GEOMETRY_NAME_MULTIPOINT:
                typmod |= GEOMETRY_TYPE_MULTIPOINT;
                break;
            case GEOMETRY_NAME_MULTILINESTRING:
                typmod |= GEOMETRY_TYPE_MULTILINESTRING;
                break;
            case GEOMETRY_NAME_MULTIPOLYGON:
                typmod |= GEOMETRY_TYPE_MULTIPOLYGON;
                break;
            case GEOMETRY_NAME_GEOMETRYCOLLECTION:
                typmod |= GEOMETRY_TYPE_GEOMETRYCOLLECTION;
                break;
            case GEOMETRY_NAME_CIRCULARSTRING:
                typmod |= GEOMETRY_TYPE_CIRCULARSTRING;
                break;
            case GEOMETRY_NAME_COMPOUNDCURVE:
                typmod |= GEOMETRY_TYPE_COMPOUNDCURVE;
                break;
            case GEOMETRY_NAME_CURVEPOLYGON:
                typmod |= GEOMETRY_TYPE_CURVEPOLYGON;
                break;
            case GEOMETRY_NAME_MULTICURVE:
                typmod |= GEOMETRY_TYPE_MULTICURVE;
                break;
            case GEOMETRY_NAME_MULTISURFACE:
                typmod |= GEOMETRY_TYPE_MULTISURFACE;
                break;
            case GEOMETRY_NAME_POLYHEDRALSURFACE:
                typmod |= GEOMETRY_TYPE_POLYHEDRALSURFACE;
                break;
            case GEOMETRY_NAME_TRIANGLE:
                typmod |= GEOMETRY_TYPE_TRIANGLE;
                break;
            case GEOMETRY_NAME_TIN:
                typmod |= GEOMETRY_TYPE_TIN;
                break;
            default:
                throw new DBException("Unsupported geometry type: '" + name + "'");
        }
        return typmod;
    }
}
