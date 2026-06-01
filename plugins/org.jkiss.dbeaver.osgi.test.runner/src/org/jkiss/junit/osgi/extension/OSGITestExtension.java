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
package org.jkiss.junit.osgi.extension;

import org.jkiss.junit.osgi.OSGITestRunner;
import org.jkiss.junit.osgi.annotation.RunWithApplication;
import org.jkiss.junit.osgi.annotation.RunWithProduct;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class OSGITestExtension implements BeforeAllCallback, AfterAllCallback, InvocationInterceptor {

    /**
     * One OSGi container per product+application combination, so that test classes
     * using different products (e.g. dbvr-unittest vs DBeaverUltimate) each get
     * their own isolated OSGi runtime instead of sharing one global singleton.
     */
    private static final ConcurrentHashMap<String, OSGITestRunner> runners = new ConcurrentHashMap<>();

    /** per-key locks for double-checked locking during runner creation. */
    private static final ConcurrentHashMap<String, Object> runnerLocks = new ConcurrentHashMap<>();

    /**
     * per-thread: the runner that is active for the test class currently executing
     * on this thread.  Set in beforeAll(), cleared in afterAll().
     */
    private static final ThreadLocal<OSGITestRunner> currentRunner = new ThreadLocal<>();

    /**
     * per-thread storage for the classloader that was active before we switched
     * to the OSGi bundle classloader, so we can restore it in afterAll().
     */
    private static final ThreadLocal<ClassLoader> savedClassLoader = new ThreadLocal<>();

    /**
     * maps IDEA-classloader test instances to their OSGi-classloader counterparts.
     * Keyed weakly so entries are GC'd automatically after each test method.
     */
    private static final java.util.Map<Object, Object> osgiInstanceMap =
        Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Derives a stable key from the product + application annotations so that
     * test classes sharing the same product/app reuse the same OSGi container.
     * Returns {@code null} when the class has no OSGi annotations.
     */
    private static String getRunnerKey(Class<?> testClass) {
        RunWithProduct product = testClass.getAnnotation(RunWithProduct.class);
        RunWithApplication app = testClass.getAnnotation(RunWithApplication.class);
        if (product == null || app == null) {
            return null;
        }
        return product.value() + ":" + app.bundleName() + ":" + app.registryName();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        if (testClass.getAnnotation(RunWithProduct.class) == null
                && testClass.getAnnotation(RunWithApplication.class) == null) {
            return;
        }

        String key = getRunnerKey(testClass);
        if (key == null) {
            return;
        }

        OSGITestRunner runner = runners.get(key);
        if (runner == null) {
            Object lock = runnerLocks.computeIfAbsent(key, k -> new Object());
            synchronized (lock) {
                runner = runners.get(key);
                if (runner == null) {
                    try {
                        ClassLoader myLoader = this.getClass().getClassLoader();
                        if (myLoader != null
                                && (myLoader.toString().contains("AppClassLoader")
                                    || myLoader.toString().contains("Idea")
                                    || (myLoader.getName() != null && myLoader.getName().equals("app")))) {
                            OSGITestRunner newRunner = new OSGITestRunner(testClass);
                            newRunner.waitUntilReady();
                            runners.put(key, newRunner);
                            runner = newRunner;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (runner != null) {
            currentRunner.set(runner);
            ClassLoader testBundleClassLoader = runner.getTestBundleClassLoader();
            if (testBundleClassLoader != null) {
                savedClassLoader.set(Thread.currentThread().getContextClassLoader());
                Thread.currentThread().setContextClassLoader(testBundleClassLoader);
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        ClassLoader previous = savedClassLoader.get();
        if (previous != null) {
            Thread.currentThread().setContextClassLoader(previous);
            savedClassLoader.remove();
        }
        currentRunner.remove();
    }

    // -------------------------------------------------------------------------
    // InvocationInterceptor – run test/lifecycle methods inside OSGi classloader
    // -------------------------------------------------------------------------

    private boolean isRunningFromIdea() {
        OSGITestRunner runner = currentRunner.get();
        return runner != null && runner.getTestBundleClassLoader() != null;
    }

    /**
     * Shared dispatch: if running from IDEA, re-run the method inside the OSGi
     * bundle's classloader; otherwise proceed with the default JUnit invocation.
     */
    private void interceptWithOsgi(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext
    ) throws Throwable {
        if (isRunningFromIdea()) {
            invokeInOsgi(invocation, invocationContext);
        } else {
            invocation.proceed();
        }
    }

    /**
     * Returns the OSGi-classloader instance that corresponds to the given IDEA
     * test instance, creating it on first access.
     */
    private Object resolveOsgiInstance(Object ideaTarget, ClassLoader osgiLoader) {
        if (ideaTarget == null) {
            return null;
        }
        return osgiInstanceMap.computeIfAbsent(ideaTarget, k -> {
            try {
                Class<?> osgiClass = osgiLoader.loadClass(k.getClass().getName());
                return osgiClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create OSGi test instance for " + k.getClass(), e);
            }
        });
    }

    /**
     * Resolves JVM parameter types against the OSGi classloader.
     * Primitive types are returned unchanged.
     */
    private Class<?>[] resolveParamTypes(Class<?>[] types, ClassLoader osgiLoader) throws ClassNotFoundException {
        if (types.length == 0) {
            return types;
        }
        Class<?>[] resolved = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = types[i].isPrimitive() ? types[i] : osgiLoader.loadClass(types[i].getName());
        }
        return resolved;
    }

    /**
     * Skips the default JUnit invocation and re-runs the method inside the OSGi
     * bundle's classloader so that all static singletons (application, platform,
     * etc.) are resolved from the correct classloader.
     */
    private void invokeInOsgi(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext
    ) throws Throwable {
        invocation.skip();

        OSGITestRunner runner = currentRunner.get();
        ClassLoader osgiLoader = runner.getTestBundleClassLoader();
        Method ideaMethod = invocationContext.getExecutable();

        Class<?> osgiDeclaringClass = osgiLoader.loadClass(ideaMethod.getDeclaringClass().getName());
        Method osgiMethod = osgiDeclaringClass.getDeclaredMethod(
            ideaMethod.getName(),
            resolveParamTypes(ideaMethod.getParameterTypes(), osgiLoader)
        );
        osgiMethod.setAccessible(true);

        Object osgiInstance = resolveOsgiInstance(invocationContext.getTarget().orElse(null), osgiLoader);

        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(osgiLoader);
            osgiMethod.invoke(osgiInstance);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> inv, ReflectiveInvocationContext<Method> ctx, ExtensionContext ext) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> inv, ReflectiveInvocationContext<Method> ctx, ExtensionContext ext) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> inv, ReflectiveInvocationContext<Method> ctx, ExtensionContext ext) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }

    @Override
    public void interceptBeforeAllMethod(Invocation<Void> inv, ReflectiveInvocationContext<Method> ctx, ExtensionContext ext) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }

    @Override
    public void interceptAfterAllMethod(Invocation<Void> inv, ReflectiveInvocationContext<Method> ctx, ExtensionContext ext) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }
}
