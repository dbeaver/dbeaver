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

/**
 * Cross-classloader bridge for the singleton {@link AbstractApplication} instance: under the OSGi test
 * harness the test and the application run in different classloaders, so the per-classloader INSTANCE
 * static is invisible from the test side. System properties are the only JVM-wide store shared across
 * classloaders; the instance is published there only under the harness ({@code dbeaver.osgi.context}).
 */
public final class TestApplicationHolder {

    private static final String CONTEXT_KEY = "dbeaver.osgi.context"; // set by OSGITestRunner only
    private static final String INSTANCE_KEY = "dbeaver.app.instance";

    private TestApplicationHolder() {
    }

    public static void register(@Nullable DBPApplication application) {
        if (application != null && System.getProperties().get(CONTEXT_KEY) != null) {
            System.getProperties().put(INSTANCE_KEY, application);
        }
    }

    @Nullable
    public static DBPApplication get() {
        return System.getProperties().get(INSTANCE_KEY) instanceof DBPApplication application ? application : null;
    }

}
