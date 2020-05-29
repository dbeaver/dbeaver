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
package org.jkiss.dbeaver.model.sql.task;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLToolExecuteSettings
 */
public class SQLToolExecuteSettings<OBJECT_TYPE extends DBSObject> {

    private static final Log log = Log.getLog(SQLToolExecuteSettings.class);

    private List<OBJECT_TYPE> objectList = new ArrayList<>();

    protected SQLToolExecuteSettings() {
    }

    public List<OBJECT_TYPE> getObjectList() {
        return objectList;
    }

    public void setObjectList(List<OBJECT_TYPE> objectList) {
        this.objectList = objectList;
    }

    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {

    }

    public void saveConfiguration(Map<String, Object> config) {
        List<Map<String, Object>> objectsConfig = new ArrayList<>();
        config.put("objects", objectsConfig);
        for (OBJECT_TYPE obj : objectList) {
            Map<String, Object> objectInfo = new LinkedHashMap<>();
            objectInfo.put("project", obj.getDataSource().getContainer().getProject().getName());
            objectInfo.put("objectId", DBUtils.getObjectFullId(obj));
            objectsConfig.add(objectInfo);
        }
    }

}
