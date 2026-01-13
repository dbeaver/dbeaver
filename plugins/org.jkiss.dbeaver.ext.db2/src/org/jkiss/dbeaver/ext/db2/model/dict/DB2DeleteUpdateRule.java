/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

/**
 * DB2 Foreign Key Delete/Update Rule
 *
 * @author Denis Forveille
 */
public enum DB2DeleteUpdateRule implements DBPNamedObject {

    A("No Action", DBSForeignKeyModifyRule.NO_ACTION),

    C("Cascade", DBSForeignKeyModifyRule.CASCADE),

    N("Set Null", DBSForeignKeyModifyRule.SET_NULL),

    R("Restrict", DBSForeignKeyModifyRule.RESTRICT);

    private final String title;
    private final DBSForeignKeyModifyRule rule;

    DB2DeleteUpdateRule(String title, DBSForeignKeyModifyRule rule) {
        this.title = title;
        this.rule = rule;
    }

    public static DB2DeleteUpdateRule getDB2RuleFromDBSRule(DBSForeignKeyModifyRule dbsRule) {
        for (DB2DeleteUpdateRule db2Rule : DB2DeleteUpdateRule.values()) {
            if (db2Rule.getRule().equals(dbsRule)) {
                return db2Rule;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return title;
    }

    public DBSForeignKeyModifyRule getRule() {
        return rule;
    }
}