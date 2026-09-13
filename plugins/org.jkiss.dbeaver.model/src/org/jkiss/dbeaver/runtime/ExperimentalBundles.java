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
package org.jkiss.dbeaver.runtime;

import org.jkiss.code.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

import java.util.Arrays;
import java.util.Collection;

public final class ExperimentalBundles {
    public static final String ENABLE_PROPERTY = "dbeaver.experimental";
    public static final String BUNDLE_HEADER = "DBeaver-Experimental";

    private ExperimentalBundles() {
    }

    public static void initialize(@NotNull BundleContext context) {
        if (Boolean.parseBoolean(context.getProperty(ENABLE_PROPERTY))) {
            return;
        }
        ResolverHook hook = new ExperimentalResolverHook();
        context.registerService(ResolverHookFactory.class, triggers -> hook, null);
        var resolvedBundles = Arrays.stream(context.getBundles())
            .filter(ExperimentalBundles::isExperimental)
            .filter(bundle -> bundle.getState() != Bundle.INSTALLED && bundle.getState() != Bundle.UNINSTALLED)
            .toList();
        if (!resolvedBundles.isEmpty()) {
            // discard wiring restored from a previous run with the experimental flag enabled
            // never wait in an activator: refresh callbacks may need this bundle to finish starting
            context.getBundle(0).adapt(FrameworkWiring.class).refreshBundles(resolvedBundles);
        }
    }

    private static boolean isExperimental(@NotNull Bundle bundle) {
        return Boolean.parseBoolean(bundle.getHeaders("").get(BUNDLE_HEADER));
    }

    private static final class ExperimentalResolverHook implements ResolverHook {
        @Override
        public void filterResolvable(@NotNull Collection<BundleRevision> candidates) {
            candidates.removeIf(revision -> isExperimental(revision.getBundle()));
        }

        @Override
        public void filterSingletonCollisions(@NotNull BundleCapability singleton, @NotNull Collection<BundleCapability> candidates) {
        }

        @Override
        public void filterMatches(@NotNull BundleRequirement requirement, @NotNull Collection<BundleCapability> candidates) {
        }

        @Override
        public void end() {
        }
    }
}
