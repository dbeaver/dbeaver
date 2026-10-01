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
package org.jkiss.junit.osgi.launcher;

import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.service.runnable.ParameterizedRunnable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class TestLauncher implements ApplicationLauncher {
    private volatile ParameterizedRunnable runnable = null;
    private final BundleContext context;

    public TestLauncher(BundleContext context) {
        this.context = context;
        findRunnableService();
    }

    @Override
    public void launch(ParameterizedRunnable runnable, Object context) {
        this.runnable = runnable;
    }

    @Override
    public void shutdown() {

    }

    /**
     * Start the application with the given appID and args.
     *
     * @param appID the application ID
     * @param args the arguments
     * @return the result of evaluating the application in the given context
     */
    @Nullable
    public Object start(@NotNull String appID, @Nullable String[] args) {
        try {
            ((BundleContextImpl) context).getContainer().getConfiguration().setConfiguration("eclipse.application", appID);
            if (args != null && args.length != 0) {
                ((BundleContextImpl) context).getContainer().getConfiguration().setAllArgs(args);
            }
            // wait for Equinox to set runnable via the launch() callback
            long startTime = System.currentTimeMillis();
            while (runnable == null && System.currentTimeMillis() - startTime < 30000) {
                Thread.sleep(100);
            }
            if (runnable == null) {
                throw new RuntimeException("Application launcher runnable was not initialized within 30 seconds. " +
                    "Equinox did not call launch() for application: " + appID);
            }
            return runnable.run(context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for application launcher", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void findRunnableService() {
        // look for a ParameterizedRunnable registered as a service by runtimes (3.0,
        // 3.1)
        String appClass = ParameterizedRunnable.class.getName();
        ServiceReference<?>[] runRefs = null;
        try {
            runRefs = context.getServiceReferences(
                ParameterizedRunnable.class.getName(),
                "(&(objectClass=" + appClass + ")(eclipse.application=*))"
            ); //$NON-NLS-1$//$NON-NLS-2$
        } catch (InvalidSyntaxException e) {
            // ignore this. It should never happen as we have tested the above format.
        }
        if (runRefs != null && runRefs.length > 0) {
            runnable = (ParameterizedRunnable) context.getService(runRefs[0]);
        }
    }
}