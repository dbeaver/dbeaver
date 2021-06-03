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
package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

public class TestCommandContext extends AbstractCommandContext {
    /**
     * Creates new context
     *
     * @param executionContext Execution context
     * @param atomic           atomic context reflect commands in UI only after all commands were executed. Non-atomic
     */
    public TestCommandContext(DBCExecutionContext executionContext, boolean atomic) {
        super(executionContext, atomic);
    }
}
