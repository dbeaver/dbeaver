/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.database;

/**
* Mapping type
*/
public enum DatabaseMappingType {
    unspecified(false, false),
    existing(true, false),
    create(true, false),
    skip(false, false);

    private final boolean isValid;
    private final boolean isAttrOnly;

    DatabaseMappingType(boolean isValid, boolean isAttrOnly) {
        this.isValid = isValid;
        this.isAttrOnly = isAttrOnly;
    }

    public boolean isValid() {
        return isValid;
    }

    public boolean isAttrOnly() {
        return isAttrOnly;
    }
}
