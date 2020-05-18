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
package org.jkiss.dbeaver.data.gis.handlers;

import org.cugos.wkg.WKBReader;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.utils.CommonUtils;

/**
 * WKG geometry utils
 */
public class WKGUtils {

    public static DBGeometry parseWKB(String hexString) throws DBCException {
        org.cugos.wkg.Geometry wkgGeometry = new WKBReader().read(hexString);
        if (wkgGeometry != null) {
            return new DBGeometry(wkgGeometry, CommonUtils.toInt(wkgGeometry.getSrid()));
        }
        throw new DBCException("Invalid geometry object");
    }

}
