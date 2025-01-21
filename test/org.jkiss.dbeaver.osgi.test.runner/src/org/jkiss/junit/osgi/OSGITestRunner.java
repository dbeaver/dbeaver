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
package org.jkiss.junit.osgi;

import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.util.ManifestElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.junit.osgi.annotation.RunWithProduct;
import org.jkiss.junit.osgi.annotation.RunnerProxy;
import org.jkiss.junit.osgi.launcher.TestLauncher;
import org.jkiss.utils.Pair;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleWiring;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h2>OSGITestRunner</h2>
 * <p>
 *     The class is responsible for running the OSGi tests inside IDEA.
 *     Does it by bundles and starting the OSGi framework.
 *     If OSGI environment is already running, it will not start a new one.
 *     Uses {@link RunWithProduct} annotation to specify the product to run the test in.
 *     and {@link RunnerProxy} to specify the runner which should be execute in OSGI environment
 *     Should allow debugging of the tests in the IDEA.
 * </p>
 * <h3>Temporary Limitations</h3>
 * <p>
 *     No UI results are shown in the IDE for the tests if OSGI environment was created.
 * </p>
 */
public class OSGITestRunner extends Runner {
    public static final Pattern startLevel = Pattern.compile("@(\\d+):start");
    private static final Log log = Log.getLog(OSGITestRunner.class);
    private static final String WORKSPACE_DIR = "../../../dbeaver-workspace/products";
    private final Class<?> testClass;
    private Framework framework;
    private Path productPath;

    private String testBundleName;
    private Bundle testBundle;

    public OSGITestRunner(Class<?> testClass) {
        this.testClass = testClass;
        if (isRunFromIDEA()) {
            try {
                // Determine name of test bundle
                // Analyze classpath, we don't have other way because we are not in OSGI container yet
                // All test bundles are compiled and classes are in <bundle-path>/target
                URL resource = testClass.getClassLoader().getResource(testClass.getName().replace('.', '/') + ".class");
                if (resource != null) {
                    String testClassPath = resource.toString();
                    Pattern pluginNamePattern = Pattern.compile(".+/([\\w.]+)/target/");
                    Matcher matcher = pluginNamePattern.matcher(testClassPath);
                    if (matcher.find()) {
                        testBundleName = matcher.group(1);
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }

            this.productPath = findProduct();
            this.framework = initializeFramework();
        }
    }

    @Override
    public Description getDescription() {
        return Description.createTestDescription(testClass, testClass.getName());
    }

    @Override
    public void run(RunNotifier notifier) {
        if (isRunFromIDEA()) {
            runInsideOSGI(notifier);
        } else {
            launchInExistingOSGI(notifier);
        }
    }

    private boolean isRunFromIDEA() {
        return "app".equals(this.getClass().getClassLoader().getName());
    }

    private Path findProduct() {
        if (testClass.getAnnotation(RunWithProduct.class) != null) {
            RunWithProduct annotation = testClass.getAnnotation(RunWithProduct.class);
            String product = annotation.value();
            Path workspace = Path.of(WORKSPACE_DIR);
            return workspace.resolve(product);
        } else {
            throw new IllegalArgumentException("Product not found");
        }
    }

    private void launchInExistingOSGI(RunNotifier notifier) {
        try {
            if (testClass.getAnnotation(RunnerProxy.class) != null) {
                Constructor<?> constructor = testClass
                    .getClassLoader()
                    .loadClass(testClass.getAnnotation(RunnerProxy.class).value().getName())
                    .getConstructor(Class.class);
                Object o = constructor.newInstance(testClass);
                Arrays.stream(o.getClass().getMethods()).filter(it -> it.getName().equals("run")).findFirst().orElseThrow().invoke(
                    o,
                    notifier
                );
            }
        } catch (Throwable throwable) {
            log.error("An error occurred while running the test", throwable);
        }
    }

    private void runInsideOSGI(RunNotifier notifier) {
        try {
            framework.init();
            // Start the OSGi framework
            BundleContext context = framework.getBundleContext();
            // Load and start all bundles
            Bundle bundle = loadAndStartBundles(context);
            framework.start();
            TestLauncher launcher = new TestLauncher(context);
            context.registerService(ApplicationLauncher.class.getName(), launcher,
                null
            );
            launcher.start(bundle.getSymbolicName());

            if (testClass.getAnnotation(RunnerProxy.class) != null) {
                Constructor<?> proxy = testBundle.loadClass(testClass.getAnnotation(RunnerProxy.class).value().getName()).getConstructor(Class.class);
                Object o = proxy.newInstance(testBundle.loadClass(testClass.getName()));
                Method runMethod = Arrays.stream(o.getClass().getMethods()).filter(it -> it.getName().equals("run"))
                    .findFirst().orElseThrow();
                Object proxyNotifier = createProxyNotifier(notifier);
                runMethod.invoke(o, proxyNotifier);

            }
        } catch (Throwable throwable) {
            log.error("An error occurred while running the test", throwable);
        } finally {
            try {
                framework.stop();
                framework.waitForStop(0);
            } catch (Exception e) {
                log.error("Error stopping framework", e);
            }
        }
    }

    @NotNull
    private Object createProxyNotifier(RunNotifier notifier) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        Object newOsgiNotifier = testBundle.loadClass(RunNotifier.class.getName()).getConstructor().newInstance();

        try {
            Class<?> osgiListenerClass = testBundle.loadClass(OSGITestRunListener.class.getName());
            Object osgiListener = osgiListenerClass.getConstructor(Object.class).newInstance(notifier);
            Method addListenerMethod = Arrays.stream(newOsgiNotifier.getClass().getMethods())
                .filter(method -> method.getName().equals("addListener")).findFirst().orElseThrow();
            addListenerMethod.invoke(newOsgiNotifier, osgiListener);
        } catch (Throwable e) {
            log.debug(e);
        }

        return newOsgiNotifier;
    }

    private Framework initializeFramework() {
        Map<String, String> config = new HashMap<>();
        config.put("org.osgi.framework.storage", "osgi-cache");
        config.put("org.osgi.framework.storage.clean", "onFirstInit");
        config.put("osgi.dev", "file:" + productPath.toAbsolutePath().resolve("dev.properties").normalize());
        FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        return frameworkFactory.newFramework(config);
    }

    private Bundle loadAndStartBundles(BundleContext context) throws Exception {
        // Specify the directory where the bundles are located
        File bundleDir = productPath.resolve("config.ini").toFile();
        Properties props = new Properties();
        Set<String> installed = Arrays.stream(framework.getBundleContext().getBundles())
            .map(Bundle::getLocation)
            .collect(Collectors.toSet());
        props.load(new FileInputStream(bundleDir));
        PriorityQueue<Pair<Bundle, Integer>> bundlesByStartLevel = new PriorityQueue<>((v1, v2) -> {
            Integer firstStart = v1.getSecond();
            Integer secondStart = v2.getSecond();
            return Integer.compare(firstStart, secondStart);
        });
        // Install all bundles from the directory
        for (String bundleFile : ManifestElement.getArrayFromList(props.getProperty("osgi.bundles"))) {
//            if (bundleFile.contains("junit")) {
//                continue;
//            }
            if (bundleFile.contains(".app") && !bundleFile.contains("headless") && !bundleFile.contains("org.eclipse")) {
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
            if (installed.contains(bundleFile) || bundleFile.contains("org.eclipse.osgi_")) {
                continue;
            }
            try {
                Bundle bundle = context.installBundle(bundleFile);
                if (startLevel != 0 || bundle.getSymbolicName().equals(testBundleName)) {
                    bundlesByStartLevel.add(new Pair<>(bundle, startLevel));
                }
            } catch (BundleException e) {
                log.error("Error initializing bundle message", e);
            }
        }

        Bundle appBundle = null;
        // find headless app bundle
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().contains("headless")) {
                appBundle = bundle;
                break;
            }
        }
        // Start all installed bundles
        for (Pair<Bundle, Integer> bundleWithStartLevel : bundlesByStartLevel) {
            Bundle bundle = bundleWithStartLevel.getFirst();

            if (bundle instanceof EquinoxBundle eb && eb.isFragment()) {
                // We need to activate main test bundle (it has to be in the list of auto-activation bundles)
                // For that we also check that test bundle is a fragment.
                // In this case we activate fragment host instead of main bundle
                Bundle hostBundle = null;
                if (bundle.getSymbolicName().equals(testBundleName)) {
                    Dictionary<String, String> headers = bundle.getHeaders();
                    String hostBundleHeader = headers.get("Fragment-Host");
                    if (hostBundleHeader != null) {
                        for (Bundle b : context.getBundles()) {
                            if (b.getSymbolicName().equals(hostBundleHeader)) {
                                hostBundle = b;
                                break;
                            }
                        }
                    }
                }
                if (hostBundle != null) {
                    bundle = hostBundle;
                }
            }

            if (bundle.getState() != Bundle.ACTIVE) {
                try {
                    bundle.start();
                    try {
                        bundle.loadClass(testClass.getName());
                        testBundle = bundle;
                    } catch (ClassNotFoundException e) {
                        // ignore, expected
                        //log.error(e);
                    }
                    log.debug("Started bundle: " + bundle.getSymbolicName());
                } catch (BundleException e) {
                    if (!e.getMessage().contains("Invalid operation on a fragment")) {
                        log.error("Error starting bundle message", e);
                    }
                }
            }
        }
        for (Pair<Bundle, Integer> bundleIntegerPair : bundlesByStartLevel) {
            if (bundleIntegerPair.getFirst().adapt(BundleWiring.class) == null) {
                log.error("Bundle not resolved: " + bundleIntegerPair.getFirst().getSymbolicName());
            }
        }
        return appBundle;
    }
}

