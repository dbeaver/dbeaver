/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRInvoker;

import java.util.Collections;
import java.util.Map;

/**
 * External configuration
 */
public class DBPExternalConfiguration {

    private static final Log log = Log.getLog(DBPExternalConfiguration.class);

    private final String id;
    private final DBRInvoker<Map<String, Object>> propertiesGetter;

    public DBPExternalConfiguration(String id, DBRInvoker<Map<String, Object>> propertiesGetter) {
        this.id = id;
        this.propertiesGetter = propertiesGetter;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getProperties() {
        try {
            return propertiesGetter.invoke();
        } catch (DBException e) {
            log.debug(e);
            return Collections.emptyMap();
        }
    }
}
