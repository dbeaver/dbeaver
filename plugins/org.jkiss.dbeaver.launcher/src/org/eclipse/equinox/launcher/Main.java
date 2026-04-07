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
package org.eclipse.equinox.launcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    private static final String DELEGATE_MAIN_CLASS_ARG = "--delegateMainClass";
    private static final String DELEGATE_CLASSPATH_ARG = "--delegateClasspath";

    public static void main(String argString) {
        main(parseCommandLine(argString));
    }

    public static void main(String[] args) {
        try {
            invokeDelegateMain(args);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(13);
        }
    }

    private static void invokeDelegateMain(String[] args)
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, MalformedURLException {
        String delegateMainClass = null;
        String delegateClasspath = null;
        List<String> delegateArgs = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            if (DELEGATE_MAIN_CLASS_ARG.equals(args[i])) {
                if (i + 1 >= args.length || args[i + 1].isBlank()) {
                    throw new IllegalStateException("Missing value for required argument: " + DELEGATE_MAIN_CLASS_ARG);
                }
                delegateMainClass = args[++i];
                continue;
            }
            if (DELEGATE_CLASSPATH_ARG.equals(args[i])) {
                if (i + 1 >= args.length || args[i + 1].isBlank()) {
                    throw new IllegalStateException("Missing value for required argument: " + DELEGATE_CLASSPATH_ARG);
                }
                delegateClasspath = args[++i];
                continue;
            }
            delegateArgs.add(args[i]);
        }
        if (delegateMainClass == null) {
            throw new IllegalStateException("Missing required argument: " + DELEGATE_MAIN_CLASS_ARG);
        }
        ClassLoader delegateClassLoader = Main.class.getClassLoader();
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
        Class<?> delegateClass = Class.forName(delegateMainClass, true, delegateClassLoader);
        Method mainMethod = delegateClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) delegateArgs.toArray(String[]::new));
    }

    private static String[] parseCommandLine(String argString) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < argString.length(); i++) {
            char ch = argString.charAt(i);
            if (quote != 0) {
                if (ch == quote) {
                    quote = 0;
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (Character.isWhitespace(ch)) {
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            if (ch == '"' || ch == '\'') {
                quote = ch;
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result.toArray(String[]::new);
    }
}
