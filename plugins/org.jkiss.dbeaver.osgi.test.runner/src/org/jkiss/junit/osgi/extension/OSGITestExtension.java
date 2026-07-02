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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.junit.osgi.OSGIFrameworkHandler;
import org.jkiss.junit.osgi.annotation.RunWithApplication;
import org.jkiss.junit.osgi.annotation.RunWithProduct;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class OSGITestExtension implements BeforeAllCallback, AfterAllCallback, InvocationInterceptor {

    private static final Log log = Log.getLog(OSGITestExtension.class);

    // classloader-name markers that identify a non-OSGi (IDE) launch
    private static final String MARKER_APP_CLASSLOADER = "AppClassLoader";
    private static final String MARKER_IDEA = "Idea";
    private static final String MARKER_APP_LOADER_NAME = "app";

    // one OSGi container per product+application, so different products get isolated runtimes
    private static final ConcurrentHashMap<String, OSGIFrameworkHandler> runners = new ConcurrentHashMap<>();

    // per-key locks for double-checked locking on runner creation
    private static final ConcurrentHashMap<String, Object> runnerLocks = new ConcurrentHashMap<>();

    // runner active for the test class on this thread; set in beforeAll, cleared in afterAll
    private static final ThreadLocal<OSGIFrameworkHandler> currentRunner = new ThreadLocal<>();

    // context classloader active before the switch to the OSGi bundle classloader, restored in afterAll
    private static final ThreadLocal<ClassLoader> savedClassLoader = new ThreadLocal<>();

    // IDEA-classloader test instance -> its OSGi-classloader counterpart; weak so entries are GC'd per test
    private static final Map<Object, Object> osgiInstanceMap = Collections.synchronizedMap(new WeakHashMap<>());

    // stable key per product+application; null when the class has no OSGi annotations
    @Nullable
    private static String getRunnerKey(@NotNull Class<?> testClass) {
        RunWithProduct product = testClass.getAnnotation(RunWithProduct.class);
        RunWithApplication app = testClass.getAnnotation(RunWithApplication.class);
        if (product == null || app == null) {
            return null;
        }
        return product.value() + ":" + app.bundleName() + ":" + app.registryName();
    }

    @Override
    public void beforeAll(@NotNull ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        if (testClass.getAnnotation(RunWithProduct.class) == null
            && testClass.getAnnotation(RunWithApplication.class) == null) {
            return;
        }
        String key = getRunnerKey(testClass);
        if (key == null) {
            return;
        }
        OSGIFrameworkHandler runner = getOrCreateRunner(key, testClass);
        if (runner != null) {
            currentRunner.set(runner);
            ClassLoader testBundleClassLoader = runner.getTestBundleClassLoader();
            if (testBundleClassLoader != null) {
                savedClassLoader.set(Thread.currentThread().getContextClassLoader());
                Thread.currentThread().setContextClassLoader(testBundleClassLoader);
            }
        }
    }

    @Nullable
    private OSGIFrameworkHandler getOrCreateRunner(@NotNull String key, @NotNull Class<?> testClass) {
        OSGIFrameworkHandler runner = runners.get(key);
        if (runner != null) {
            return runner;
        }
        Object lock = runnerLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            runner = runners.get(key);
            if (runner == null && isLauncherClassLoader()) {
                try {
                    runner = new OSGIFrameworkHandler(testClass);
                    runner.waitUntilReady();
                    runners.put(key, runner);
                } catch (Exception e) {
                    log.error("Failed to create OSGi test runner for " + key, e);
                    runner = null;
                }
            }
        }
        return runner;
    }

    // bootstrap OSGi only when loaded from the IDE/app classloader (inside Tycho the test already runs in OSGi)
    private boolean isLauncherClassLoader() {
        ClassLoader myLoader = this.getClass().getClassLoader();
        return myLoader != null
            && (myLoader.toString().contains(MARKER_APP_CLASSLOADER)
            || myLoader.toString().contains(MARKER_IDEA)
            || (myLoader.getName() != null && myLoader.getName().equals(MARKER_APP_LOADER_NAME)));
    }

    @Override
    public void afterAll(@NotNull ExtensionContext context) {
        ClassLoader previous = savedClassLoader.get();
        if (previous != null) {
            Thread.currentThread().setContextClassLoader(previous);
            savedClassLoader.remove();
        }
        currentRunner.remove();
    }

    private boolean isRunningFromIdea() {
        OSGIFrameworkHandler runner = currentRunner.get();
        return runner != null && runner.getTestBundleClassLoader() != null;
    }

    private void interceptWithOsgi(
        @NotNull Invocation<Void> invocation,
        @NotNull ReflectiveInvocationContext<Method> invocationContext
    ) throws Throwable {
        if (isRunningFromIdea()) {
            invokeInOsgi(invocation, invocationContext);
        } else {
            invocation.proceed();
        }
    }

    @Nullable
    private Object resolveOsgiInstance(@Nullable Object ideaTarget, @NotNull ClassLoader osgiLoader) {
        if (ideaTarget == null) {
            return null;
        }
        return osgiInstanceMap.computeIfAbsent(
            ideaTarget, k -> {
                try {
                    Class<?> osgiClass = osgiLoader.loadClass(k.getClass().getName());
                    return osgiClass.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create OSGi test instance for " + k.getClass(), e);
                }
            }
        );
    }

    @NotNull
    private Class<?>[] resolveParamTypes(@NotNull Class<?>[] types, @NotNull ClassLoader osgiLoader) throws ClassNotFoundException {
        if (types.length == 0) {
            return types;
        }
        Class<?>[] resolved = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = types[i].isPrimitive() ? types[i] : osgiLoader.loadClass(types[i].getName());
        }
        return resolved;
    }

    private void invokeInOsgi(
        @NotNull Invocation<Void> invocation,
        @NotNull ReflectiveInvocationContext<Method> invocationContext
    ) throws Throwable {
        invocation.skip();

        OSGIFrameworkHandler runner = currentRunner.get();
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
    public void interceptTestMethod(
        @NotNull Invocation<Void> inv,
        @NotNull ReflectiveInvocationContext<Method> ctx,
        @NotNull ExtensionContext ext
    ) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }

    @Override
    public void interceptBeforeEachMethod(
        @NotNull Invocation<Void> inv,
        @NotNull ReflectiveInvocationContext<Method> ctx,
        @NotNull ExtensionContext ext
    ) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }

    @Override
    public void interceptAfterEachMethod(
        @NotNull Invocation<Void> inv,
        @NotNull ReflectiveInvocationContext<Method> ctx,
        @NotNull ExtensionContext ext
    ) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }

    @Override
    public void interceptBeforeAllMethod(
        @NotNull Invocation<Void> inv,
        @NotNull ReflectiveInvocationContext<Method> ctx,
        @NotNull ExtensionContext ext
    ) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }

    @Override
    public void interceptAfterAllMethod(
        @NotNull Invocation<Void> inv,
        @NotNull ReflectiveInvocationContext<Method> ctx,
        @NotNull ExtensionContext ext
    ) throws Throwable {
        interceptWithOsgi(inv, ctx);
    }
}
