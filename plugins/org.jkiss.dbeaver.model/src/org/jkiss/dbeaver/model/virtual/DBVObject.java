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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Virtual model object
 */
public abstract class DBVObject implements DBSObject {

    static final Log log = Log.getLog(DBVObject.class);

    private DBVTransformSettings transformSettings;

    @Override
    public abstract DBVContainer getParentObject();

    @Override
    public boolean isPersisted() {
        return true;
    }

    public DBVTransformSettings getTransformSettings() {
        return transformSettings;
    }

    public void setTransformSettings(DBVTransformSettings transformSettings) {
        this.transformSettings = transformSettings;
    }

    abstract public boolean hasValuableData();

}
