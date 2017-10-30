/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

/**
 * PostgreGenericTrigger
 */
@Deprecated
public class PostgreGenericTrigger extends GenericTrigger {

    private String manipulation;
    private String orientation;
    private String timing;
    private String source;

    public PostgreGenericTrigger(GenericStructContainer container, GenericTable table, String name, String description, String manipulation, String orientation, String timing, String statement) {
        super(container, table, name, description);
        this.manipulation = manipulation;
        this.orientation = orientation;
        this.timing = timing;
        this.source = statement;
    }

    @Property(viewable = true, editable = true, updatable = false, order = 20)
    public String getTiming() {
        return timing;
    }

    @Property(viewable = true, editable = true, updatable = false, order = 21)
    public String getManipulation() {
        return manipulation;
    }

    @Property(viewable = true, editable = true, updatable = false, order = 22)
    public String getOrientation() {
        return orientation;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return source;
    }

    public void addManipulation(String manipulation) {
        this.manipulation += " OR " + manipulation;
    }
}
