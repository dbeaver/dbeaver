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

package org.jkiss.dbeaver.model.exec;

import org.jkiss.utils.CommonUtils;

/**
 * Execution purpose.
 *
 * Each query which executed withing application have some purpose.
 * Some of queries are instantiated by user, some are executed internally to obtain metadata, etc.
 * This enum defines different query purposes.
 *
 * Note: for sure, we can't detect ALL executed queries. Some of them are executed by drivers internally,
 * some are executed by plugins and not reported to query manager.
 */
public enum DBCExecutionPurpose {

    USER(0, "User", true),               // User query
    USER_FILTERED(1, "User filtered", true),      // User query with additional filters
    USER_SCRIPT(2, "User script", true),        // User script query
    UTIL(3, "Util", false),              // Utility query (utility method initialized by user)
    META(4, "Meta", false),              // Metadata query, processed by data source providers internally
    META_DDL(5, "Meta DDL", false),
    ;

    private final int id;
    private final String title;
    private final boolean user;

    DBCExecutionPurpose(int id, String title, boolean user) {
        this.id = id;
        this.title = title;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isUser() {
        return user;
    }           // Metadata modifications (DDL)

    public static DBCExecutionPurpose getById(int id) {
        for (DBCExecutionPurpose purpose : values()) {
            if (purpose.getId() == id) {
                return purpose;
            }
        }
        return USER;
    }

}
