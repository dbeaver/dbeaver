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

import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.utils.CommonUtils;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

/**
 * Geometry value (LOB).
 */
public class DBGeometry implements DBDValue {

    private Object rawValue;
    private int srid;
    private Map<String, Object> properties;

    public DBGeometry() {
        this.rawValue = null;
    }

    public DBGeometry(String rawValue) {
        this.rawValue = rawValue;
    }

    public DBGeometry(Geometry rawValue) {
        this.rawValue = rawValue;
        this.srid = rawValue == null ? 0 : rawValue.getSRID();
    }

    public DBGeometry(Object rawValue, int srid) {
        this.rawValue = rawValue;
        this.srid = srid;
    }

    public Geometry getGeometry() {
        return rawValue instanceof Geometry ? (Geometry) rawValue : null;
    }

    public String getString() {
        return rawValue == null ? null : CommonUtils.toString(rawValue);
    }

    @Override
    public Object getRawValue() {
        return rawValue;
    }

    @Override
    public boolean isNull() {
        return rawValue == null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void release() {

    }

    @Override
    public String toString() {
        return rawValue == null ? null : rawValue.toString();
    }

    public int getSRID() {
        return srid;
    }

    public void setSRID(int srid) {
        this.srid = srid;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
