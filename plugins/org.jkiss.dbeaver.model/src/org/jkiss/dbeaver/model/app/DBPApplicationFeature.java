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

package org.jkiss.dbeaver.model.app;

/**
 * Application feature.
 */
public class DBPApplicationFeature {

    private final DBPApplicationFeature parent;
    private final String id;
    private final String label;
    private final String description;

    public DBPApplicationFeature(DBPApplicationFeature parent, String id, String label, String description) {
        this.parent = parent;
        this.id = id;
        this.label = label;
        this.description = description;
    }

    public DBPApplicationFeature getParent() {
        return parent;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

}
