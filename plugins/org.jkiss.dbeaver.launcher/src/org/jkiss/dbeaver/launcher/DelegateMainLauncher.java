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
package org.jkiss.dbeaver.launcher;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DelegateMainLauncher {

    private static final String DELEGATE_MAIN_CLASS_ARG = "--delegateMainClass";
    private static final String DELEGATE_CLASSPATH_ARG = "--delegateClasspath";
    private static final String DELEGATE_ARGS_ARG = "--delegateArgs";

    private DelegateMainLauncher() {
    }

    public static boolean canHandle(String[] args) {
        return Arrays.stream(args).anyMatch(arg ->
            DELEGATE_MAIN_CLASS_ARG.equals(arg) || DELEGATE_CLASSPATH_ARG.equals(arg)
        );
    }

    public static void run(String[] args) throws ReflectiveOperationException {
        String delegateMainClass = null;
        String delegateClasspath = null;
        List<String> delegateArgs = new ArrayList<>(args.length);
        boolean collectDelegateArgs = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (collectDelegateArgs) {
                delegateArgs.add(arg);
                continue;
            }
            if (DELEGATE_MAIN_CLASS_ARG.equals(arg)) {
                delegateMainClass = requireArgumentValue(args, i, DELEGATE_MAIN_CLASS_ARG);
                i++;
                continue;
            }
            if (DELEGATE_CLASSPATH_ARG.equals(arg)) {
                delegateClasspath = requireArgumentValue(args, i, DELEGATE_CLASSPATH_ARG);
                i++;
                continue;
            }
            if (DELEGATE_ARGS_ARG.equals(arg)) {
                collectDelegateArgs = true;
            }
        }

        if (delegateMainClass == null) {
            throw new IllegalStateException("Missing required argument: " + DELEGATE_MAIN_CLASS_ARG);
        }

        ClassLoader delegateClassLoader = getClassLoader(delegateClasspath);
        Class<?> delegateClass = Class.forName(delegateMainClass, true, delegateClassLoader);
        Method mainMethod = delegateClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) delegateArgs.toArray(String[]::new));
    }

    private static ClassLoader getClassLoader(String delegateClasspath) {
        ClassLoader delegateClassLoader = DelegateMainLauncher.class.getClassLoader();
        if (delegateClasspath != null) {
            URL[] urls = Arrays.stream(delegateClasspath.split(File.pathSeparator))
                .filter(entry -> !entry.isBlank())
                .map(Path::of)
                .map(Path::toUri)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .toArray(URL[]::new);
            delegateClassLoader = new URLClassLoader(urls, delegateClassLoader);
            Thread.currentThread().setContextClassLoader(delegateClassLoader);
        }
        return delegateClassLoader;
    }

    private static String requireArgumentValue(String[] args, int optionIndex, String optionName) {
        int valueIndex = optionIndex + 1;
        if (valueIndex >= args.length || args[valueIndex].isBlank()) {
            throw new IllegalStateException("Missing value for required argument: " + optionName);
        }
        return args[valueIndex];
    }
}
