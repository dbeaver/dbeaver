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

package org.jkiss.dbeaver.erd.model;

import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * ERD constants
 */
public class ERDConstants {


    public static final String PREF_ATTR_VISIBILITY = "erd.attr.visibility";
    public static final String PREF_ATTR_STYLES = "erd.attr.styles";

    public static DBSEntityConstraintType CONSTRAINT_LOGICAL_FK = new DBSEntityConstraintType("erdkey", "Logical Key", null, true, false, false, true);
}
