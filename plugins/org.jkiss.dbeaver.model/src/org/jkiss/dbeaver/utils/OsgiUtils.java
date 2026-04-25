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
package org.jkiss.dbeaver.utils;

import org.eclipse.core.runtime.FileLocator;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * OSGi-related helper utilities.
 *
 */
public final class OsgiUtils {

    private OsgiUtils() {
        // no instance
    }

    /**
     * Returns filesystem paths of OSGi bundles associated with the bundle
     * that loaded the given class.
     *
     * @param clazz a class from the root bundle
     * @param excludeSystemBundle whether to exclude the OSGi system bundle
     * @return bundle paths, or empty if the class is not loaded from OSGi
     * @throws IOException if a bundle path cannot be resolved
     */
    @NotNull
    public static Collection<Path> collectBundlePaths(
        @NotNull Class<?> clazz,
        boolean excludeSystemBundle
    ) throws IOException {

        Bundle bundle = FrameworkUtil.getBundle(clazz);
        if (bundle == null) {
            return List.of();
        }

        return collectBundlePaths(bundle, excludeSystemBundle);
    }

    /**
     * Collects paths of the given bundle and all required bundles.
     * The given bundle itself goes first.
     * For directory bundles, returns {@code target/classes} if it exists.
     *
     * @param bundle root bundle
     * @param excludeSystemBundle whether to skip the OSGi system bundle
     * @return ordered bundle paths, or an empty collection if bundle wiring is unavailable
     * @throws IOException if a bundle path cannot be resolved
     */
    @NotNull
    public static Collection<Path> collectBundlePaths(
        @NotNull Bundle bundle,
        boolean excludeSystemBundle
    ) throws IOException {
        List<BundleWiring> wirings = collectBundleWirings(bundle, excludeSystemBundle);
        List<Path> result = new ArrayList<>(wirings.size());

        for (BundleWiring wiring : wirings) {
            Path p = bundleToPath(wiring.getBundle());

            if (Files.isDirectory(p)) {
                Path classes = p.resolve("target/classes");
                if (Files.isDirectory(classes)) {
                    result.add(classes.normalize());
                    continue;
                }
            }

            result.add(p.normalize());
        }

        return result;
    }

    /**
     * Collects symbolic names of the given bundle and all required bundles.
     * The given bundle itself goes first.
     * Uses the same traversal as {@link #collectBundlePaths(Bundle, boolean)}.
     *
     * @param bundle root bundle
     * @param excludeSystemBundle whether to skip the OSGi system bundle
     * @return ordered bundle names, or an empty list if bundle wiring is unavailable
     */
    @NotNull
    public static List<String> collectBundleNames(
        @NotNull Bundle bundle,
        boolean excludeSystemBundle
    ) {
        List<BundleWiring> wirings = collectBundleWirings(bundle, excludeSystemBundle);
        List<String> result = new ArrayList<>(wirings.size());

        for (BundleWiring wiring : wirings) {
            result.add(wiring.getBundle().getSymbolicName());
        }
        return result;
    }

    @Nullable
    public static String getBundleName(@NotNull Bundle bundle) {
        String bundleName = bundle.getHeaders().get("Bundle-Name");
        if (CommonUtils.isEmpty(bundleName)) {
            bundleName = bundle.getSymbolicName();
        }
        return bundleName;
    }

    private static void collectWirings(
        @NotNull BundleWiring wiring,
        @NotNull Map<Long, BundleWiring> out,
        boolean excludeSystemBundle
    ) {
        Bundle bundle = wiring.getBundle();
        long id = bundle.getBundleId();

        if (excludeSystemBundle && id == 0) {
            return;
        }
        if (out.containsKey(id)) {
            return;
        }

        BundleRevision rev = bundle.adapt(BundleRevision.class);
        if (rev != null && (rev.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
            return;
        }

        out.put(id, wiring);

        List<BundleWire> wires = new ArrayList<>();
        wires.addAll(wiring.getRequiredWires(BundleRevision.BUNDLE_NAMESPACE));
        wires.addAll(wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE));

        for (BundleWire wire : wires) {
            BundleWiring provider = wire.getProviderWiring();
            if (provider != null) {
                collectWirings(provider, out, excludeSystemBundle);
            }
        }
    }

    @NotNull
    private static Path bundleToPath(@NotNull Bundle bundle) throws IOException {
        try {
            return FileLocator
                .getBundleFileLocation(bundle)
                .map(File::toPath)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .orElseThrow(() ->
                    new IOException("Cannot resolve bundle file for " + bundle)
                );

        } catch (Exception e) {
            throw new IOException("Cannot resolve bundle path: " + bundle, e);
        }
    }

    @NotNull
    private static List<BundleWiring> collectBundleWirings(
        @NotNull Bundle bundle,
        boolean excludeSystemBundle
    ) {
        BundleWiring root = bundle.adapt(BundleWiring.class);
        if (root == null) {
            return List.of();
        }

        Map<Long, BundleWiring> wirings = new LinkedHashMap<>();
        collectWirings(root, wirings, excludeSystemBundle);
        return new ArrayList<>(wirings.values());
    }

}
