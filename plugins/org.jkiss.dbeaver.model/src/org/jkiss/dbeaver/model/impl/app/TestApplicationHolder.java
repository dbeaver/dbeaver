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
package org.jkiss.dbeaver.model.impl.app;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPApplication;

// shares the application instance via JVM system properties so the OSGi test harness can read it
// active only under the harness
public final class TestApplicationHolder {

    private TestApplicationHolder() {
    }

    public static void register(@Nullable DBPApplication application) {
        if (application != null && System.getProperties().get(TestHarnessConstants.PROP_OSGI_CONTEXT) != null) {
            System.getProperties().put(TestHarnessConstants.PROP_APP_INSTANCE, application);
        }
    }

    @Nullable
    public static DBPApplication get() {
        return System.getProperties().get(TestHarnessConstants.PROP_APP_INSTANCE) instanceof DBPApplication application
            ? application : null;
    }
}
