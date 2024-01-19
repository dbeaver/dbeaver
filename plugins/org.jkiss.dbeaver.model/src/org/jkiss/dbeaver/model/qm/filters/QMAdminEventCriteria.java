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
package org.jkiss.dbeaver.model.qm.filters;

import org.jkiss.code.NotNull;

import java.util.Collections;
import java.util.Set;

public class QMAdminEventCriteria {

    private final QMEventCriteria criteria;
    @NotNull
    private Set<String> users = Collections.emptySet();

    public QMAdminEventCriteria(QMEventCriteria criteria, @NotNull Set<String> users) {
        this.criteria = criteria;
        this.users = users;
    }

    public void setUsers(@NotNull Set<String> users) {
        this.users = users;
    }

    @NotNull
    public Set<String> getUsers() {
        return users;
    }

    public boolean hasUsers() {
        return !users.isEmpty();
    }

    public QMEventCriteria getCriteria() {
        return criteria;
    }
}
