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
<<<<<<<< HEAD:plugins/org.jkiss.dbeaver.model.ai/src/org/jkiss/dbeaver/model/ai/AIConfirmation.java
package org.jkiss.dbeaver.model.ai;

import java.util.UUID;

public abstract class AIConfirmation {

    private final UUID id;

    public AIConfirmation() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    abstract String getMessage();
========

package org.jkiss.dbeaver.model.exec.plan;

/**
 * Execution plan source data format
 */
public enum DBCPlanSourceFormat {

    NONE, // N/A
    TEXT, // string form
    JSON, // string form
    XML, // string form
    RESULT_SET // CachedResultSet

>>>>>>>> 9a0b97bb94 (#40537 web request timeouts configuration):plugins/org.jkiss.dbeaver.model/src/org/jkiss/dbeaver/model/exec/plan/DBCPlanSourceFormat.java
}
