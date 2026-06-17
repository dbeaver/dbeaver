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
package org.jkiss.junit.osgi;

import org.eclipse.equinox.internal.app.CommandLineArgs;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.launch.EquinoxFactory;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.util.ManifestElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.junit.osgi.annotation.RunWithApplication;
import org.jkiss.junit.osgi.annotation.RunWithProduct;
import org.jkiss.junit.osgi.behaviors.IAsyncApplication;
import org.jkiss.junit.osgi.launcher.TestLauncher;
import org.jkiss.utils.Pair;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleWiring;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h2>OSGIFrameworkHandler</h2>
 * <p>
 *     The class is responsible for OSGI inside IDEA.
 *     It does by starting the OSGi framework and loading all the required bundles.
 *     If OSGI environment is already running, it will not start a new one.
 *     <li>{@link RunWithProduct} annotation to specify the product to run the test in.</li>
 *     <li>{@link RunWithApplication} to specify the application to run the test in.</li>
 *     <br>
 *     Should allow debugging of the tests in the IDEA.
 * </p>
 */
public class OSGIFrameworkHandler {

    public static final Pattern startLevel = Pattern.compile("@(\\d+):start");
    private static final Log log = Log.getLog(OSGIFrameworkHandler.class);
    private static final boolean DEBUG_BUNDLE_LAUNCH = false;
    private final Class<?> testClass;
    private Framework framework;
    private Path productPath;

    private String testBundleName;
    private Bundle testBundle;
    private String appRegistryName;
    private String appBundleName;
    private Set<String> forceDependencies;
    private String[] args;
    private String[] vmArgs;
    private RunWithApplication.Property[] frameworkProperties;
    private boolean waitForWorkbench;
    private TestLauncher launcher;

    public OSGIFrameworkHandler(
        @NotNull Class<?> testClass
    ) throws Exception {
        this.testClass = testClass;
        if (isRunFromIDEA()) {
            //use UTF-8 for run
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

            getAppBundleFromAnnotation();
            this.framework = initializeFramework();
            startFramework();
        }
    }

    private void getAppBundleFromAnnotation() {
        if (testClass.getAnnotation(RunWithApplication.class) != null) {
            RunWithApplication annotation = testClass.getAnnotation(RunWithApplication.class);
            this.appRegistryName = annotation.registryName();
            this.appBundleName = annotation.bundleName();
            this.args = annotation.args();
            this.vmArgs = annotation.vmArgs();
            this.frameworkProperties = annotation.properties();
            this.forceDependencies = Arrays.stream(annotation.forceDependencies()).collect(Collectors.toSet());
            this.waitForWorkbench = annotation.waitForWorkbench();

        } else {
            throw new IllegalArgumentException("Application not found");
        }
    }


    private boolean isRunFromIDEA() {
        return FrameworkUtil.getBundle(this.getClass()) == null;
    }

    @NotNull
    private Path findProduct() {
        if (testClass.getAnnotation(RunWithProduct.class) != null) {
            RunWithProduct annotation = testClass.getAnnotation(RunWithProduct.class);
            String product = annotation.value();
            Path workspace = Path.of(findWorkspaceDir().toString());
            return workspace.resolve(product);
        } else {
            throw new IllegalArgumentException("Product not found");
        }
    }

    @NotNull
    private static Path findWorkspaceDir() {
        Path workPath = Paths.get("").toAbsolutePath();
        Path currentPath = workPath.toAbsolutePath();
        while (currentPath != null) {
            Path potentialWorkspaceDir = currentPath.resolve("dbeaver-workspace/products");
            if (Files.exists(potentialWorkspaceDir)) {
                return workPath.relativize(potentialWorkspaceDir);
            }
            currentPath = currentPath.getParent();
        }
        throw new IllegalStateException("dbeaver-workspace/products directory not found");
    }

    public void waitUntilReady() {
        if (testBundle == null) {
            return;
        }
        // wait for the BundleContext to appear in system properties
        long startTime = System.currentTimeMillis();
        while (System.getProperties().get(TestHarnessConstants.PROP_OSGI_CONTEXT) == null
            && System.currentTimeMillis() - startTime < 300000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                // ignore
            }
        }
        BundleContext context = (BundleContext) System.getProperties().get(TestHarnessConstants.PROP_OSGI_CONTEXT);
        if (context == null) {
            log.error("OSGi context not found in system properties");
            return;
        }

        if (!IAsyncApplication.class.isAssignableFrom(testClass)) {
            return;
        }

        // headless apps need DBPApplicationWorkbench before startup; CLI apps never register it,
        // so skipping the wait saves about 10s for em
        if (waitForWorkbench) {
            long workbenchWaitDeadline = System.currentTimeMillis() + 10000;
            while (context.getServiceReference("org.jkiss.dbeaver.model.app.DBPApplicationWorkbench") == null
                    && System.currentTimeMillis() < workbenchWaitDeadline) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            if (context.getServiceReference("org.jkiss.dbeaver.model.app.DBPApplicationWorkbench") != null) {
                log.info("DBPApplicationWorkbench service is registered.");
            } else {
                log.debug("DBPApplicationWorkbench service not registered within 10s.");
            }
        }

        // start the app only after the workbench wait - else it races Felix SCR services not yet created
        if (launcher != null) {
            log.info("Starting headless application...");
            Thread appThread = new Thread(() -> launcher.start(appRegistryName, args));
            appThread.setDaemon(true);
            appThread.setName("headless-app-launcher");
            appThread.start();
        }

        log.info("Waiting for OSGi application to be ready...");
        try {
            Class<?> runningClass = testBundle.loadClass(testClass.getName());
            Object appInstance = null;
            try {
                appInstance = runningClass.getConstructor().newInstance();
            } catch (Exception e) {
                log.error("Error creating test instance for verifyLaunched", e);
            }
            if (appInstance != null) {
                long appStartTime = System.currentTimeMillis();
                long endTime = 0;
                boolean setUpIsDone = false;
                while (!setUpIsDone && endTime < 300000) {
                    try {
                        Method verifyLaunched = runningClass.getMethod("verifyLaunched");
                        setUpIsDone = (boolean) verifyLaunched.invoke(appInstance);
                    } catch (Exception expected) {
                        // not launched yet
                    }
                    endTime = System.currentTimeMillis() - appStartTime;
                    if (!setUpIsDone) {
                        if (endTime > 0 && endTime % 5000 < 100) {
                            log.info("Still waiting for application... (" + (endTime / 1000) + "s)");
                        }
                        Thread.sleep(100);
                    }
                }
                if (setUpIsDone) {
                    log.info("Application is ready.");
                } else {
                    log.error("Application was not ready after 5 minutes.");
                }
            }
        } catch (Exception e) {
            log.error("Error waiting for application ready", e);
        }
    }

    @Nullable
    public ClassLoader getTestBundleClassLoader() {
        if (testBundle == null) {
            return null;
        }
        return testBundle.adapt(BundleWiring.class).getClassLoader();
    }

    private void startFramework() throws Exception {
        if (vmArgs != null && vmArgs.length > 1 && vmArgs.length % 2 == 0) {
            for (int i = 0; i < vmArgs.length; i += 2) {
                String key = vmArgs[i];
                String value = vmArgs[i + 1];
                System.setProperty(key, value);
            }
        }
        framework.init();
        // Start the OSGi framework
        BundleContext context = framework.getBundleContext();
        System.getProperties().put(TestHarnessConstants.PROP_OSGI_CONTEXT, context);
        // Load and start all bundles
        loadAndStartBundles(context);
        EquinoxConfiguration equinoxConfig = null;
        if (args != null) {
            ServiceReference<EnvironmentInfo> configRef = context.getServiceReference(EnvironmentInfo.class);
            equinoxConfig = (EquinoxConfiguration) context.getService(configRef);
            equinoxConfig.setAllArgs(args);
            equinoxConfig.setAppArgs(args);
        }
        framework.start();
        if (equinoxConfig != null) {
            Method processCommandLine = CommandLineArgs.class.getDeclaredMethod(
                "processCommandLine",
                EnvironmentInfo.class
            );
            processCommandLine.setAccessible(true);
            processCommandLine.invoke(null, equinoxConfig);
        }
        launcher = new TestLauncher(context);
        context.registerService(ApplicationLauncher.class.getName(), launcher,
            null
        );
        if (!IAsyncApplication.class.isAssignableFrom(testClass)) {
            // non-async tests: start the app synchronously now
            launcher.start(appRegistryName, args);
        }
        // async tests start the app in waitUntilReady(), once DBPApplicationWorkbench is registered
    }

    @NotNull
    private Framework initializeFramework() {
        Map<String, String> config = new HashMap<>();
        config.put("org.osgi.framework.storage", "osgi-cache");
        config.put("org.osgi.framework.storage.clean", "onFirstInit");
        // Specify the directory where the dev.properties file is located
        config.put("osgi.dev", "file:" + productPath.toAbsolutePath().resolve("dev.properties").normalize());
        if (DEBUG_BUNDLE_LAUNCH) {
            config.put("osgi.debug", "file:" + productPath.toAbsolutePath().resolve("debug_config").normalize());
            config.put("org.osgi.framework.debug", "true");
            config.put("org.osgi.framework.debug.loader", "true");
            config.put("org.osgi.framework.debug.resolver", "true");
        }
        for (RunWithApplication.Property frameworkProperty : frameworkProperties) {
            config.put(frameworkProperty.name(), frameworkProperty.value());
        }

        // Enable boot delegation, to avoid class loading issues for some classes
        config.put("osgi.compatibility.bootdelegation", "true");
        FrameworkFactory frameworkFactory = new EquinoxFactory();
        return frameworkFactory.newFramework(config);
    }

    @Nullable
    private Bundle loadAndStartBundles(@NotNull BundleContext context) throws Exception {
        // Specify the directory where the bundles are located
        File bundleDir = productPath.resolve("config.ini").toFile();
        Properties props = new Properties();
        Set<String> installed = Arrays.stream(framework.getBundleContext().getBundles())
            .map(Bundle::getLocation)
            .collect(Collectors.toSet());
        try (FileInputStream bundleStream = new FileInputStream(bundleDir)) {
            props.load(bundleStream);
        }
        PriorityQueue<Pair<Bundle, Integer>> bundlesByStartLevel = new PriorityQueue<>((v1, v2) -> {
            Integer firstStart = v1.getSecond();
            Integer secondStart = v2.getSecond();
            return Integer.compare(firstStart, secondStart);
        });
        // Install all bundles from the directory
        for (String bundleFile : ManifestElement.getArrayFromList(props.getProperty("osgi.bundles"))) {
            if (
                // Avoid adding app bundles to bundle start list,
                // We already have a specificied app to run
                bundleFile.contains(".app.")
                    && !bundleFile.contains(appBundleName)
                    && !bundleFile.contains("org.eclipse")
                    && forceDependencies.stream().noneMatch(bundleFile::contains)

            ) {
                continue;
            }
            Matcher matcher = startLevel.matcher(bundleFile);
            int bundleStartLevel = 0;
            if (matcher.find()) {
                bundleStartLevel = Integer.parseInt(matcher.group(1));
            }
            if (bundleFile.lastIndexOf('@') >= 0) {
                bundleFile = bundleFile.substring(0, bundleFile.lastIndexOf('@'));
            }
            if (installed.contains(bundleFile) || bundleFile.contains("org.eclipse.osgi_")) {
                continue;
            }
            try {
                Bundle bundle = context.installBundle(bundleFile);
                if (bundleStartLevel != 0 || bundle.getSymbolicName().equals(testBundleName)) {
                    bundlesByStartLevel.add(new Pair<>(bundle, bundleStartLevel));
                }
            } catch (BundleException e) {
                log.error("Error initializing bundle message", e);
            }
        }

        Bundle appBundle = null;
        // find appBundleContainingClassname app bundle
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().contains(this.appBundleName)) {
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
                    } catch (ClassNotFoundException expected) {
                        // bundle doesn't contain the test class
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

