/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.starrocks.internal;

import org.eclipse.core.runtime.Plugin;
import org.jkiss.code.NotNull;
import org.osgi.framework.BundleContext;

import java.util.Objects;

public class StarRocksActivator extends Plugin {

    private static StarRocksActivator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    @NotNull
    public static StarRocksActivator getDefault() {
        return Objects.requireNonNull(plugin, "StarRocks plugin has not been started"); //$NON-NLS-1$
    }
}