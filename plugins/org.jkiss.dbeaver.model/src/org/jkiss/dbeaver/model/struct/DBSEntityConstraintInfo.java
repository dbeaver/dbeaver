/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.struct;

/**
 * DBSEntityConstraintInfo
 */
public class DBSEntityConstraintInfo {

    private final DBSEntityConstraintType type;
    private final Class<? extends DBSEntityConstraint> implClass;

    private DBSEntityConstraintInfo(DBSEntityConstraintType type, Class<? extends DBSEntityConstraint> implClass) {
        this.type = type;
        this.implClass = implClass;
    }

    public DBSEntityConstraintType getType() {
        return type;
    }

    public Class<? extends DBSEntityConstraint> getImplClass() {
        return implClass;
    }

    public static DBSEntityConstraintInfo of(DBSEntityConstraintType type, Class<? extends DBSEntityConstraint> implClass) {
        return new DBSEntityConstraintInfo(type, implClass);
    }
}