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


import org.locationtech.jts.geom.Geometry;

/**
 * GisTransformRequest
*/
public class GisTransformRequest {

    private final Geometry sourceValue;
    private Geometry targetValue;
    private final int sourceSRID;
    private int targetSRID;

    private boolean showOnMap;

    public GisTransformRequest(Geometry sourceValue, int sourceSRID, int targetSRID) {
        this.sourceValue = sourceValue;
        this.sourceSRID = sourceSRID;
        this.targetSRID = targetSRID;
    }

    public Geometry getSourceValue() {
        return sourceValue;
    }

    public Geometry getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Geometry targetValue) {
        this.targetValue = targetValue;
        if (this.targetValue != null) {
            this.targetSRID = this.targetValue.getSRID();
        } else {
            this.targetSRID = 0;
        }
    }

    public int getSourceSRID() {
        return sourceSRID;
    }

    public int getTargetSRID() {
        return targetSRID;
    }

    public void setTargetSRID(int targetSRID) {
        this.targetSRID = targetSRID;
    }

    public boolean isShowOnMap() {
        return showOnMap;
    }

    public void setShowOnMap(boolean showOnMap) {
        this.showOnMap = showOnMap;
    }
}
