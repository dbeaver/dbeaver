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
package org.jkiss.dbeaver.model.app;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * Runtime type of the current application.
 */
public enum DBPApplicationType {
    DESKTOP,
    DISTRIBUTED_DESKTOP,
    WEB,
    DISTRIBUTED_WEB,
    CLI;

    @NotNull
    public static DBPApplicationType getCurrentApplicationType() {
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        if (application.isMultiuser()) {
            return application.isDistributed() ? DISTRIBUTED_WEB : WEB;
        }
        if (application.isHeadlessMode()) {
            return CLI;
        }
        return application.isDistributed() ? DISTRIBUTED_DESKTOP : DESKTOP;
    }
}
