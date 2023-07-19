/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableConstraintColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.util.List;

/*
 * AltibaseConstraint types except "0: FOREIGN KEY".
 * 1: NOT NULL, 2: UNIQUE, 3: PRIMARY KEY, 5: TIMESTAMP, 6: LOCAL UNIQUE, 7: CHECK
 * 
 * Refer to SQL: AltibaseMetaModel.prepareUniqueConstraintsLoadStatement
 */
public class AltibaseConstraint extends GenericUniqueKey {

    // public DBSEntityConstraintType(String id, String name, String localizedName, boolean association, boolean unique, boolean custom, boolean logical)
    public static final DBSEntityConstraintType LOCAL_UNIQUE_KEY = new DBSEntityConstraintType(
            "localunique", "LOCAL UNIQUE", "LOCAL UNIQUE", false, true, true, false);
    public static final DBSEntityConstraintType TIMESTAMP = new DBSEntityConstraintType(
            "timestamp", "TIMESTAMP", "TIMESTAMP", false, false, true, false);

    protected List<GenericTableConstraintColumn> columns;
    protected String condition;
    protected boolean validated;

    public AltibaseConstraint(GenericTableBase table, String name, String remarks,
            DBSEntityConstraintType constraintType, boolean persisted, String condition, boolean validated) {
        super(table, name, remarks, constraintType, persisted);
        this.condition = condition;
        this.validated = validated;
    }

    @Property(viewable = true, order = 10)
    public String getCondition() {
        return condition;
    }

    @Property(viewable = true, order = 10)
    public boolean isValidated() {
        return validated;
    }
    
    public boolean isSystemGenerated() {
        return this.getName().startsWith(AltibaseConstants.SYSTEM_GENERATED_PREFIX);
    }
}
