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

import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

/**
 * GisExpressionFunctions
 */
public class GisExpressionFunctions {

    private static final Log log = Log.getLog(GisExpressionFunctions.class);

    public static Object wktPoint(Object longitude, Object latitude) {
        return wktPoint(longitude, latitude, GisConstants.SRID_4326);
    }

    public static Object wktPoint(Object longitude, Object latitude, Object srid) {
        if (longitude == null || latitude == null) {
            return null;
        }
        if (longitude instanceof Number && ((Number) longitude).doubleValue() == 0.0 &&
            latitude instanceof Number && ((Number) latitude).doubleValue() == 0.0)
        {
            // Zeroes
            return null;
        }
        String strValue = "POINT(" + longitude + " " + latitude + ")";
        return new DBGeometry(strValue, CommonUtils.toInt(srid, GisConstants.SRID_4326));
    }

}
