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

/**
 * GisConstants.
*/
public interface GisConstants {

    // Equirectangular projection.
    // A common CRS among GIS enthusiasts.
    int SRID_4326 = 4326;
    // Spherical Mercator projection
    // The most common CRS for online maps, used by almost all free and commercial tile providers.
    // The default
    int SRID_3857 = 3857;
    // Elliptical Mercator projection. Rarely used by some commercial tile providers.
    int SRID_3395 = 3395;

    // Flat surface
    int SRID_SIMPLE = 0;

    String GIS_REG_EPSG = "EPSG";

    String LL_CRS_SIMPLE = "Simple";
    String LL_CRS_3857 = "EPSG3857";
}
