/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.util.HashMap;
import java.util.Map;

/**
 * PostgreConstants
 */
public class PostgreConstants {

    public static final int DEFAULT_PORT = 5432;
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_DATABASE = "postgres";
    public static final String DEFAULT_DATA_TYPE = "varchar";
    public static final String DEFAULT_USER = "postgres";

    public static final String PROP_SHOW_NON_DEFAULT_DB = DBConstants.INTERNAL_PROP_PREFIX + "show-non-default-db@";
    public static final String PROP_SHOW_TEMPLATES_DB = DBConstants.INTERNAL_PROP_PREFIX + "show-template-db@";

    public static final String PROP_SSL = "ssl";

    public static final String PROP_SSL_CLIENT_CERT = "clientCert";
    public static final String PROP_SSL_CLIENT_KEY = "clientKey";
    public static final String PROP_SSL_ROOT_CERT = "rootCert";
    public static final String PROP_SSL_MODE = "sslMode";
    public static final String PROP_SSL_FACTORY = "sslFactory";
    public static final String PROP_SERVER_TYPE = "serverType";

    public static final String OPTION_DDL_SHOW_PERMISSIONS = "pg.ddl.show.permissions";
    public static final String OPTION_DDL_SHOW_COLUMN_COMMENTS = "pg.ddl.show.column.comments";

    public static final DBSObjectState STATE_UNAVAILABLE = new DBSObjectState("Unavailable", DBIcon.OVER_EXTERNAL);
    public static final DBSEntityConstraintType CONSTRAINT_TRIGGER = new DBSEntityConstraintType("trigger", "TRIGGER", "Trigger constraint", false, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType CONSTRAINT_EXCLUSIVE = new DBSEntityConstraintType("exclusive", "EXCLUSIVE", "Exclusive constraint", false, false, false); //$NON-NLS-1$

    public static final String INFO_SCHEMA_NAME = "information_schema";
    public static final String SYSTEM_SCHEMA_PREFIX = "pg_";
    public static final String CATALOG_SCHEMA_NAME = "pg_catalog";
    public static final String TEMP_SCHEMA_NAME = "pg_temp";
    public static final String TOAST_SCHEMA_PREFIX = "pg_toast";
    public static final String TEMP_SCHEMA_PREFIX = "pg_temp_";
    public static final String PUBLIC_SCHEMA_NAME = "public";

    public static final String PG_OBJECT_CLASS = "org.postgresql.util.PGobject";
    public static final String PG_ARRAY_CLASS = "org.postgresql.jdbc.PgArray";

    public static final DBDPseudoAttribute PSEUDO_ATTR_OID = new DBDPseudoAttribute(DBDPseudoAttributeType.ROWID, "oid",
        "oid", "oid", "Row identifier", false);

    public static final String TYPE_VARCHAR = "varchar";
    public static final String TYPE_HSTORE = "hstore";
    public static final String TYPE_JSON = "json";
    public static final String TYPE_JSONB = "jsonb";
    public static final String TYPE_BIT = "bit";
    public static final String TYPE_REFCURSOR = "refcursor";
    public static final String TYPE_MONEY = "money";
    public static final String TYPE_GEOMETRY = "geometry";

    public static final String HANDLER_SSL = "postgre_ssl";

    /**
     * @see https://www.postgresql.org/docs/9.2/static/errcodes-appendix.html
     */
    public static final String EC_PERMISSION_DENIED = "42501"; //$NON-NLS-1$
    public static final String EC_QUERY_CANCELED = "57014"; //$NON-NLS-1$

    public static final String PG_INSTALL_REG_KEY = "SOFTWARE\\PostgreSQL\\Installations";
    public static final String PG_INSTALL_PROP_BASE_DIRECTORY = "Base Directory";
    public static final String PG_INSTALL_PROP_VERSION = "Version";
    public static final String PG_INSTALL_PROP_BRANDING = "Branding";
    public static final String PG_INSTALL_PROP_DATA_DIRECTORY = "Data Directory";
    public static final String BIN_FOLDER = "bin";

    public static final Map<String, String> SERIAL_TYPES = new HashMap<>();
    public static final Map<String, String> DATA_TYPE_ALIASES = new HashMap<>();
    public static final Map<String, String> DATA_TYPE_CANONICAL_NAMES = new HashMap<>();

    public static final String TYPE_INT2 = "int2";
    public static final String TYPE_INT4 = "int4";
    public static final String TYPE_INT8 = "int8";
    public static final String TYPE_FLOAT4 = "float4";
    public static final String TYPE_FLOAT8 = "float8";

    public static final String ERROR_ADMIN_SHUTDOWN = "57P01";
    public static final String PSQL_EXCEPTION_CLASS_NAME = "org.postgresql.util.PSQLException";

    static {
        DATA_TYPE_ALIASES.put("integer", TYPE_INT4);
        DATA_TYPE_ALIASES.put("int", TYPE_INT4);
        DATA_TYPE_ALIASES.put("bigint", TYPE_INT8);
        DATA_TYPE_ALIASES.put("smallint", TYPE_INT2);

        DATA_TYPE_ALIASES.put("double precision", TYPE_FLOAT8);
        DATA_TYPE_ALIASES.put("real", TYPE_FLOAT4);
        DATA_TYPE_ALIASES.put("void", "void");

        SERIAL_TYPES.put("serial", TYPE_INT4);
        SERIAL_TYPES.put("serial8", TYPE_INT8);
        SERIAL_TYPES.put("serial2", TYPE_INT2);
        SERIAL_TYPES.put("smallserial", TYPE_INT2);
        SERIAL_TYPES.put("bigserial", TYPE_INT8);

        DATA_TYPE_CANONICAL_NAMES.put(TYPE_INT4, "integer");
        DATA_TYPE_CANONICAL_NAMES.put(TYPE_INT8, "bigint");
        DATA_TYPE_CANONICAL_NAMES.put(TYPE_INT2, "smallint");
        DATA_TYPE_CANONICAL_NAMES.put(TYPE_FLOAT4, "real");
        DATA_TYPE_CANONICAL_NAMES.put("character varying", "varchar");
    }

    public static final String[] POSTGIS_FUNCTIONS = {

        // 8.3. Management Functions

        "AddGeometryColumn",
        "DropGeometryColumn",
        "DropGeometryTable",
        "PostGIS_Full_Version",
        "PostGIS_GEOS_Version",
        "PostGIS_Liblwgeom_Version",
        "PostGIS_LibXML_Version",
        "PostGIS_Lib_Build_Date",
        "PostGIS_Lib_Version",
        "PostGIS_PROJ_Version",
        "PostGIS_Scripts_Build_Date",
        "PostGIS_Scripts_Installed",
        "PostGIS_Scripts_Released",
        "PostGIS_Version",
        "Populate_Geometry_Columns",
        "UpdateGeometrySRID",

        // 8.4. Geometry Constructors

        "ST_BdPolyFromText",
        "ST_BdMPolyFromText",
        "ST_Box2dFromGeoHash",
        "ST_GeogFromText",
        "ST_GeographyFromText",
        "ST_GeogFromWKB",
        "ST_GeomFromTWKB",
        "ST_GeomCollFromText",
        "ST_GeomFromEWKB",
        "ST_GeomFromEWKT",
        "ST_GeometryFromText",
        "ST_GeomFromGeoHash",
        "ST_GeomFromGML",
        "ST_GeomFromGeoJSON",
        "ST_GeomFromKML",
        "ST_GMLToSQL",
        "ST_GeomFromText",
        "ST_GeomFromWKB",
        "ST_LineFromEncodedPolyline",
        "ST_LineFromMultiPoint",
        "ST_LineFromText",
        "ST_LineFromWKB",
        "ST_LinestringFromWKB",
        "ST_MakeBox2D",
        "ST_3DMakeBox",
        "ST_MakeLine",
        "ST_MakeEnvelope",
        "ST_MakePolygon",
        "ST_MakePoint",
        "ST_MakePointM",
        "ST_MLineFromText",
        "ST_MPointFromText",
        "ST_MPolyFromText",
        "ST_Point",
        "ST_PointFromGeoHash",
        "ST_PointFromText",
        "ST_PointFromWKB",
        "ST_Polygon",
        "ST_PolygonFromText",
        "ST_WKBToSQL",
        "ST_WKTToSQL",

        //8.5. Geometry Accessors

        "GeometryType",
        "ST_Boundary",
        "ST_CoordDim",
        "ST_Dimension",
        "ST_EndPoint",
        "ST_Envelope",
        "ST_BoundingDiagonal",
        "ST_ExteriorRing",
        "ST_GeometryN",
        "ST_GeometryType",
        "ST_InteriorRingN",
        "ST_IsPolygonCCW",
        "ST_IsPolygonCW",
        "ST_IsClosed",
        "ST_IsCollection",
        "ST_IsEmpty",
        "ST_IsRing",
        "ST_IsSimple",
        "ST_IsValid",
        "ST_IsValidReason",
        "ST_IsValidDetail",
        "ST_M",
        "ST_NDims",
        "ST_NPoints",
        "ST_NRings",
        "ST_NumGeometries",
        "ST_NumInteriorRings",
        "ST_NumInteriorRing",
        "ST_NumPatches",
        "ST_NumPoints",
        "ST_PatchN",
        "ST_PointN",
        "ST_Points",
        "ST_SRID",
        "ST_StartPoint",
        "ST_Summary",
        "ST_X",
        "ST_XMax",
        "ST_XMin",
        "ST_Y",
        "ST_YMax",
        "ST_YMin",
        "ST_Z",
        "ST_ZMax",
        "ST_Zmflag",
        "ST_ZMin",

        //8.6. Geometry Editors

        "ST_AddPoint",
        "ST_Affine",
        "ST_Force2D",
        "ST_Force3D",
        "ST_Force3DZ",
        "ST_Force3DM",
        "ST_Force4D",
        "ST_ForcePolygonCCW",
        "ST_ForceCollection",
        "ST_ForcePolygonCW",
        "ST_ForceSFS",
        "ST_ForceRHR",
        "ST_ForceCurve",
        "ST_LineMerge",
        "ST_CollectionExtract",
        "ST_CollectionHomogenize",
        "ST_Multi",
        "ST_Normalize",
        "ST_RemovePoint",
        "ST_Reverse",
        "ST_Rotate",
        "ST_RotateX",
        "ST_RotateY",
        "ST_RotateZ",
        "ST_Scale",
        "ST_Segmentize",
        "ST_SetPoint",
        "ST_SetSRID",
        "ST_SnapToGrid",
        "ST_Snap",
        "ST_Transform",
        "ST_Translate",
        "ST_TransScale",

        //8.7. Geometry Outputs

        "ST_AsBinary",
        "ST_AsEncodedPolyline",
        "ST_AsEWKB",
        "ST_AsEWKT",
        "ST_AsGeoJSON",
        "ST_AsGML",
        "ST_AsHEXEWKB",
        "ST_AsKML",
        "ST_AsLatLonText",
        "ST_AsSVG",
        "ST_AsText",
        "ST_AsTWKB",
        "ST_AsX3D",
        "ST_GeoHash",
        "ST_AsGeobuf",
        "ST_AsMVTGeom",
        "ST_AsMVT",


    };

}
