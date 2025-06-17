/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.junit.osgi.delegate;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.lang.reflect.Method;

/**
 * Proxy filter for transferring {@link Filter} to another classloader
 * to load it in the same classloader as the test class.
 */
public class ProxyFilter extends Filter {
    Object filter;

    public ProxyFilter(@NotNull Object filter) {
        this.filter = filter;
    }

    @Override
    public boolean shouldRun(Description description) {
        return (boolean) invokeMethod("shouldRun", ClassTransferHandler.transfer(description, filter.getClass().getClassLoader()));
    }

    @Override
    public String describe() {
        return (String) invokeMethod("describe", null);
    }

    private Object invokeMethod(@NotNull String methodName, @Nullable Object argument) {
        try {
            Method method;
            if (argument != null) {
                method = filter.getClass().getDeclaredMethod(methodName, argument.getClass());
                method.setAccessible(true);
                return method.invoke(filter, argument);
            } else {
                return invokeMethod(methodName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error invoking method: " + methodName, e);
        }
    }

    private Object invokeMethod(@NotNull String methodName) {
        try {
            Method method;
            method = filter.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(filter);
        } catch (Exception e) {
            throw new RuntimeException("Error invoking method: " + methodName, e);
        }
    }
}
