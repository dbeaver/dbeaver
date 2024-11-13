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

import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.util.ManifestElement;
import org.jkiss.dbeaver.headless.DBeaverHeadlessApplication;
import org.jkiss.dbeaver.headless.DBeaverTestActivator;
import org.jkiss.dbeaver.headless.DBeaverTestPlatform;
import org.jkiss.dbeaver.test.launcher.TestLauncher;
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

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class OSGITestRunner extends Runner {

    private static final String INITIAL_LOCATION = "initial@";

    private final Class<?> testClass;
    private final Framework framework;

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

            // Run the tests
            Statement statement = new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    notifier.fireTestStarted(getDescription());
                    // Running test methods
                    for (var method : testClass.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(org.junit.Test.class)) {
                            method.invoke(testClass.newInstance());
                        }
                    }
                    notifier.fireTestFinished(getDescription());
                }
            };
            statement.evaluate();

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
        // Install all bundles from the directory
        for (String bundleFile : ManifestElement.getArrayFromList(props.getProperty("osgi.bundles"))) {
            if (bundleFile.lastIndexOf('@') >= 0) {
                bundleFile = bundleFile.substring(0, bundleFile.lastIndexOf('@'));
            }
            if (installed.contains(bundleFile)) {
                continue;
            }
            try {
                Bundle bundle = context.installBundle(bundleFile);
                System.out.println("Installed bundle: " + bundle.getSymbolicName());
            } catch (BundleException e) {
                continue;
            }
        }
        Bundle bundle1 = context.installBundle(
            "reference:file:F:/idea-workspace-pro/../dbeaver/plugins/org.jkiss.dbeaver.headless");
        bundle1.start();

        // Start all installed bundles
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getState() != Bundle.ACTIVE) {
                try {
                    bundle.start();
                    System.out.println("Started bundle: " + bundle.getSymbolicName());
                } catch (BundleException e) {
                    continue;
                }
            }
        }
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

