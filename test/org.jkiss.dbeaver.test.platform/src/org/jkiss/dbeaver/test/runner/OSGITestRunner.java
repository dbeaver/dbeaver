/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.test.runner;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.util.ManifestElement;
import org.jkiss.dbeaver.test.annotation.RunnerProxy;
import org.jkiss.dbeaver.test.launcher.TestLauncher;
import org.jkiss.utils.Pair;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OSGITestRunner extends Runner {

    private static final String INITIAL_LOCATION = "initial@";
    public static final Pattern startLevel = Pattern.compile("@(\\d+):start");

    private final Class<?> testClass;
    private final Framework framework;

    private Bundle testBundle;

    public OSGITestRunner(Class<?> testClass) throws InitializationError {
        this.testClass = testClass;
        this.framework = initializeFramework();
    }

    @Override
    public Description getDescription() {
        return Description.createTestDescription(testClass, "OSGi Bundle Runner Description");
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            framework.init();
            // Start the OSGi framework
            BundleContext context = framework.getBundleContext();
            // Load and start all bundles
            loadAndStartBundles(context);
            framework.start();
            TestLauncher launcher = new TestLauncher(context);
            context.registerService(ApplicationLauncher.class.getName(), launcher,
                null);


            launcher.start();
            for (Field field : testClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(RunnerProxy.class)) {
                    Constructor<?> proxy = testBundle.loadClass(field.getType().getName()).getConstructor(Class.class);
                    Object o = (Object) proxy.newInstance(testBundle.loadClass(testClass.getName()));
                    o.getClass().getDeclaredMethods()[0].invoke(o, testBundle.loadClass(RunNotifier.class.getName()).newInstance());
                }
            }
            // Run the tests
//            Statement statement = new Statement() {
//                @Override
//                public void evaluate() throws Throwable {
//                    notifier.fireTestStarted(getDescription());
//                    // Running test methods
//                    for (var method : testClass.getDeclaredMethods()) {
//                        if (method.isAnnotationPresent(org.junit.Test.class)) {
//                            method.invoke(testClass.newInstance());
//                        }
//                    }
//                    notifier.fireTestFinished(getDescription());
//                }
//            };
//            statement.evaluate();

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                framework.stop();
                framework.waitForStop(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Framework initializeFramework() {
        FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        Map<String, String> config = new HashMap<>();
        config.put("org.osgi.framework.storage", "osgi-cache");
        config.put("org.osgi.framework.storage.clean", "onFirstInit");
        return frameworkFactory.newFramework(config);
    }

    private void loadAndStartBundles(BundleContext context) throws Exception {
        // Specify the directory where the bundles are located
        File bundleDir = new File("F:\\dbeaver-workspace\\products\\DBeaver.product\\config.ini");
        Properties props = new Properties();
        Set<String> installed = Arrays.stream(framework.getBundleContext().getBundles()).map(Bundle::getLocation).collect(Collectors.toSet());
        props.load(new FileInputStream(bundleDir));
        PriorityQueue<Pair<Bundle, Integer>> bundlesByStartLevel = new PriorityQueue<>((v1, v2) -> {
            Integer firstStart = v1.getSecond();
            Integer secondStart = v2.getSecond();
            return Integer.compare(firstStart, secondStart);
        }) ;
        // Install all bundles from the directory
        for (String bundleFile : ManifestElement.getArrayFromList(props.getProperty("osgi.bundles"))) {
            if (bundleFile.contains("org.jkiss.dbeaver.ui.app.standalone")) {
                continue;
            }
            Matcher matcher = startLevel.matcher(bundleFile);
            int startLevel = 0;
            if (matcher.find()) {
                startLevel = Integer.parseInt(matcher.group(1));
            }
            if (bundleFile.lastIndexOf('@') >= 0) {
                bundleFile = bundleFile.substring(0, bundleFile.lastIndexOf('@'));
            }
            Path path = Path.of(bundleFile.replace("reference:file:", ""));
            if (path.resolve("target").toFile().exists() && path.resolve("target").toFile().isDirectory()) {
                String newFilename = searchDirectoryForJars(path.resolve("target"));
                if (newFilename != null) {
                    bundleFile = "reference:file:" + newFilename;
                }
            }
            if (installed.contains(bundleFile)) {
                continue;
            }
            try {

                Bundle bundle = context.installBundle(bundleFile);
                bundlesByStartLevel.add(new Pair<>(bundle, startLevel));
                System.out.println("Installed bundle: " + bundle.getSymbolicName());
            } catch (BundleException e) {
                System.out.println(e.getMessage());
                continue;
            }
        }
        testBundle = context.installBundle("reference:file:F:\\dbeaver\\test\\org.jkiss.dbeaver.test.platform/target/org.jkiss.dbeaver.test.platform-1.0.102-SNAPSHOT.jar");
        Bundle headlessAppBundle = context.installBundle(
            "reference:file:F:/idea-workspace-pro/../dbeaver/plugins/org.jkiss.dbeaver.headless/target/org.jkiss.dbeaver.headless-1.0.78-SNAPSHOT.jar");
        // Start all installed bundles
        for (Pair<Bundle, Integer> bundleWithStartLevel : bundlesByStartLevel) {
            Bundle bundle = bundleWithStartLevel.getFirst();
            if (bundle.getState() != Bundle.ACTIVE) {
                try {
                    bundle.start(2);
                    System.out.println("Started bundle: " + bundle.getSymbolicName());
                } catch (BundleException e) {
                    System.out.println("error " + e);
                    continue;
                }
            }
        }
        try {
            if ((headlessAppBundle.getState() & Bundle.UNINSTALLED) == 0) {
                headlessAppBundle.adapt(BundleStartLevel.class).setStartLevel(1);
                testBundle.adapt(BundleStartLevel.class).setStartLevel(1);
            }
            headlessAppBundle.start(2);
            testBundle.start(2);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    private static String searchDirectoryForJars(Path targetDir) {
        for (File file : targetDir.toFile().listFiles()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                // If the file is a JAR file, print its path
                return file.getAbsolutePath();
            }
        }

        return null;
    }

    private Bundle[] getCurrentBundles(boolean includeInitial) {
        Bundle[] installed = framework.getBundleContext().getBundles();
        List<Bundle> initial = new ArrayList<>();
        for (Bundle bundle : installed) {
            if (bundle.getLocation().startsWith(INITIAL_LOCATION)) {
                if (includeInitial)
                    initial.add(bundle);
            } else if (!includeInitial && bundle.getBundleId() != 0)
                initial.add(bundle);
        }
        return initial.toArray(new Bundle[initial.size()]);
    }
}

