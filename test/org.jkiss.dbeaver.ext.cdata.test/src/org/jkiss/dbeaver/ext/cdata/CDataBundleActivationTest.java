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
package org.jkiss.dbeaver.ext.cdata;

import org.eclipse.osgi.launch.EquinoxFactory;
import org.jkiss.dbeaver.runtime.ExperimentalBundles;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.FrameworkWiring;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class CDataBundleActivationTest extends DBeaverUnitTest {
    @TempDir
    Path tempDirectory;

    @Test
    public void requireExperimentalFlagInProduct() throws Exception {
        Assertions.assertFalse(resolveBundle("disabled", "false", false));
        Assertions.assertTrue(resolveBundle("enabled", "true", false));
    }

    @Test
    public void disablePreviouslyResolvedBundles() throws Exception {
        Assertions.assertFalse(resolveBundle("previously-enabled", "false", true));
    }

    @Test
    public void refreshDuringLazyActivationDoesNotDeadlock() throws Exception {
        var configuration = new HashMap<String, String>();
        configuration.put(Constants.FRAMEWORK_STORAGE, tempDirectory.resolve("lazy-start").toString());
        configuration.put(ExperimentalBundles.ENABLE_PROPERTY, "false");
        var framework = new EquinoxFactory().newFramework(configuration);
        framework.start();
        try {
            var context = framework.getBundleContext();
            Manifest experimentalManifest = createManifest("org.jkiss.dbeaver.test.experimental");
            experimentalManifest.getMainAttributes().putValue(ExperimentalBundles.BUNDLE_HEADER, "true");
            var experimental = context.installBundle("test:experimental", createBundle(experimentalManifest));
            var wiring = framework.adapt(FrameworkWiring.class);
            Assertions.assertTrue(wiring.resolveBundles(List.of(experimental)));

            Manifest modelManifest = createManifest("org.jkiss.dbeaver.test.lazy-model");
            var attributes = modelManifest.getMainAttributes();
            attributes.putValue(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
            attributes.putValue(Constants.BUNDLE_ACTIVATOR, CDataLazyBundleActivator.class.getName());
            attributes.putValue(Constants.IMPORT_PACKAGE,
                "org.osgi.framework,org.osgi.framework.hooks.resolver,org.osgi.framework.wiring");
            var model = context.installBundle("test:lazy-model", createBundle(
                modelManifest, CDataLazyBundleActivator.class, ExperimentalBundles.class));
            CompletableFuture<Void> refreshed = new CompletableFuture<>();
            context.addFrameworkListener(event -> {
                if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                    refreshed.complete(null);
                }
            });

            model.start(Bundle.START_ACTIVATION_POLICY);
            Assertions.assertEquals(Bundle.STARTING, model.getState());
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> model.loadClass(ExperimentalBundles.class.getName()));
            Assertions.assertEquals(Bundle.ACTIVE, model.getState());
            refreshed.get(5, TimeUnit.SECONDS);
            Assertions.assertFalse(wiring.resolveBundles(List.of(experimental)));
        } finally {
            framework.stop();
            framework.waitForStop(10_000);
        }
    }

    private boolean resolveBundle(String storageName, String experimental, boolean previouslyResolved) throws Exception {
        String header = FrameworkUtil.getBundle(CDataAuthModel.class).getHeaders().get(ExperimentalBundles.BUNDLE_HEADER);
        Assertions.assertEquals("true", header);
        Manifest manifest = new Manifest();
        var attributes = manifest.getMainAttributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, "org.jkiss.dbeaver.test.experimental");
        attributes.putValue(ExperimentalBundles.BUNDLE_HEADER, header);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var jar = new JarOutputStream(bytes, manifest)) {
            jar.finish();
        }

        var configuration = new HashMap<String, String>();
        configuration.put(Constants.FRAMEWORK_STORAGE, tempDirectory.resolve(storageName).toString());
        configuration.put(ExperimentalBundles.ENABLE_PROPERTY, experimental);
        var framework = new EquinoxFactory().newFramework(configuration);
        framework.init();
        try {
            var bundle = framework.getBundleContext().installBundle("test:experimental", new ByteArrayInputStream(bytes.toByteArray()));
            if (previouslyResolved) {
                Assertions.assertTrue(framework.adapt(FrameworkWiring.class).resolveBundles(List.of(bundle)));
            }
            CompletableFuture<Void> refreshed = new CompletableFuture<>();
            framework.getBundleContext().addFrameworkListener(event -> {
                if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                    refreshed.complete(null);
                }
            });
            ExperimentalBundles.initialize(framework.getBundleContext());
            if (previouslyResolved) {
                refreshed.get(5, TimeUnit.SECONDS);
            }
            return framework.adapt(FrameworkWiring.class).resolveBundles(List.of(bundle));
        } finally {
            framework.stop();
            framework.waitForStop(10_000);
        }
    }

    private static Manifest createManifest(String name) {
        Manifest manifest = new Manifest();
        var attributes = manifest.getMainAttributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, name);
        return manifest;
    }

    private static ByteArrayInputStream createBundle(Manifest manifest, Class<?>... classes) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var jar = new JarOutputStream(bytes, manifest)) {
            for (Class<?> type : classes) {
                addClass(jar, type);
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static void addClass(JarOutputStream jar, Class<?> type) throws IOException {
        String path = type.getName().replace('.', '/') + ".class";
        jar.putNextEntry(new JarEntry(path));
        try (var input = type.getResourceAsStream("/" + path)) {
            Assertions.assertNotNull(input);
            input.transferTo(jar);
        }
        jar.closeEntry();
        for (Class<?> nested : type.getDeclaredClasses()) {
            addClass(jar, nested);
        }
    }
}
