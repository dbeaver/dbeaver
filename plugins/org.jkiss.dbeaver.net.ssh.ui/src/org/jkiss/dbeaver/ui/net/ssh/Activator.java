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

package org.jkiss.dbeaver.ui.net.ssh;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    private static Activator activator;
    private static BundleContext bundleContext;

    public static Activator getDefault() {
        return activator;
    }

    static BundleContext getContext() {
        return bundleContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        activator = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        activator = null;
        bundleContext = null;
    }

}
