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

package org.jkiss.dbeaver.model.exec.plan;

import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

/**
 * Execution plan node kind
 */
public enum DBCPlanNodeKind {

    DEFAULT("Node", null),
    SELECT("Select", null),
    TABLE_SCAN("Table scan", RelationalObjectType.TYPE_TABLE),
    INDEX_SCAN("Index scan", RelationalObjectType.TYPE_INDEX),
    JOIN("Join", null),
    HASH("Hash", null),
    UNION("Union", null),
    FILTER("Filter", null),
    AGGREGATE("Aggregate", null),
    SORT("Sort", null),
    RESULT("Result", null),
    SET("Set", null),
    MERGE("Merge", null),
    GROUP("Group", null),
    MATERIALIZE("Materialize", null),
    FUNCTION("Function", null),
    MODIFY("Modify", null);

    private final String title;
    private final DBSObjectType objectType;

    DBCPlanNodeKind(String title, DBSObjectType objectType) {
        this.title = title;
        this.objectType = objectType;
    }

    public String getTitle() {
        return title;
    }

    public DBSObjectType getObjectType() {
        return objectType;
    }
}