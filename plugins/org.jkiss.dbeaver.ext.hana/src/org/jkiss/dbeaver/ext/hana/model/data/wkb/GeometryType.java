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
 *
 * Contributors:
 *    Stefan Uhrig - initial implementation
 */
package org.jkiss.dbeaver.ext.hana.model.data.wkb;

/**
 * The geometry types supported by HANA.
 */
public enum GeometryType {

    POINT(1), LINESTRING(2), POLYGON(3), MULTIPOINT(4), MULTILINESTRING(5), MULTIPOLYGON(6), GEOMETRYCOLLECTION(
            7), CIRCULARSTRING(8);

    private int typeCode;

    GeometryType(int typeCode) {
        this.typeCode = typeCode;
    }

    public int getTypeCode() {
        return typeCode;
    }

    public static GeometryType getFromCode(int code) {
        for (GeometryType type : GeometryType.values()) {
            if (type.typeCode == code) {
                return type;
            }
        }
        return null;
    }
}
