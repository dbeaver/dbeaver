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
package org.jkiss.dbeaver.ext.hsqldb.model;

import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;

/**
 * HSQLTrigger
 */
public class HSQLTrigger extends GenericTrigger {

    private String manipulation;
    private String orientation;
    private String timing;

    private String statement;

    HSQLTrigger(GenericStructContainer container, GenericTableBase table, String name, JDBCResultSet dbResult) {
        super(container, table, name, null);
        manipulation = JDBCUtils.safeGetString(dbResult, "EVENT_MANIPULATION");
        orientation = JDBCUtils.safeGetString(dbResult, "ACTION_ORIENTATION");
        timing = JDBCUtils.safeGetString(dbResult, "ACTION_TIMING");
        statement = JDBCUtils.safeGetString(dbResult, "ACTION_STATEMENT");
        if (statement != null) {
            statement = SQLFormatUtils.formatSQL(getDataSource(), statement);
        }
    }

    @Property(viewable = true, order = 11)
    public String getManipulation() {
        return manipulation;
    }

    @Property(viewable = true, order = 12)
    public String getOrientation() {
        return orientation;
    }

    @Property(viewable = true, order = 10)
    public String getTiming() {
        return timing;
    }

    public String getStatement() {
        return statement;
    }
}
