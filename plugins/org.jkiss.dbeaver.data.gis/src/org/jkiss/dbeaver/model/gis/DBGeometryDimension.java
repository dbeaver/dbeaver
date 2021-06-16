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
package org.jkiss.dbeaver.model.gis;

public enum DBGeometryDimension {
    XY(2, false, false),
    XYZ(3, true, false),
    XYM(3, false, true),
    XYZM(4, true, true);

    private final int coordinates;
    private final boolean z;
    private final boolean m;

    DBGeometryDimension(int coordinates, boolean z, boolean m) {
        this.coordinates = coordinates;
        this.z = z;
        this.m = m;
    }

    public int getCoordinates() {
        return coordinates;
    }

    public boolean hasZ() {
        return z;
    }

    public boolean hasM() {
        return m;
    }
}
