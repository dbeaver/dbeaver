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
package org.jkiss.dbeaver.osgi.test.runner.delegate;

import org.jkiss.code.Nullable;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import java.lang.reflect.Method;

public class RunNotifierDelegate extends RunNotifier {
    private final Object delegate;

    public RunNotifierDelegate(Object delegate) {
        this.delegate = delegate;
    }

    @Override public void addListener(RunListener listener) {
        invokeMethod("addListener", ClassTransferHandler.transfer(listener, delegate.getClass().getClassLoader()));
    }

    @Override public void removeListener(RunListener listener) {
        invokeMethod("removeListener", ClassTransferHandler.transfer(listener, delegate.getClass().getClassLoader()));
    }

    @Override public void fireTestSuiteStarted(Description description) {
        invokeMethod("fireTestSuiteStarted", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override public void fireTestSuiteFinished(Description description) {
        invokeMethod("fireTestSuiteFinished", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override public void pleaseStop() {
        invokeMethod("pleaseStop");
    }

    @Override public void addFirstListener(RunListener listener) {
        invokeMethod("addFirstListener", ClassTransferHandler.transfer(listener, delegate.getClass().getClassLoader()));
    }

    @Override
    public void fireTestRunStarted(Description description) {
        invokeMethod("fireTestRunStarted", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override
    public void fireTestRunFinished(Result result) {
        invokeMethod("fireTestRunFinished", ClassTransferHandler.transfer(result, delegate.getClass().getClassLoader()));
    }

    @Override
    public void fireTestStarted(Description description) throws org.junit.runner.notification.StoppedByUserException {
        invokeMethod("fireTestStarted", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override
    public void fireTestFailure(Failure failure) {
        invokeMethod("fireTestFailure", ClassTransferHandler.transfer(failure, delegate.getClass().getClassLoader()));
    }

    @Override
    public void fireTestAssumptionFailed(Failure failure) {
        invokeMethod("fireTestAssumptionFailed", ClassTransferHandler.transfer(failure, delegate.getClass().getClassLoader()));
    }

    @Override
    public void fireTestIgnored(Description description) {
        invokeMethod("fireTestIgnored", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override
    public void fireTestFinished(Description description) {
        invokeMethod("fireTestFinished", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    private void invokeMethod(String methodName, @Nullable Object argument) {
        try {
            Method method;
            if (argument != null) {
                Class<?> clazz = argument.getClass();
                if (clazz.getName().contains("RunListenerDelegate")) {
                    clazz = clazz.getSuperclass();
                }
                method = delegate.getClass().getMethod(methodName, clazz);
                method.invoke(delegate, argument);
            } else {
                invokeMethod(methodName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error invoking method: " + methodName, e);
        }
    }

    private void invokeMethod(String methodName) {
        try {
            Method method;
            method = delegate.getClass().getMethod(methodName);
            method.invoke(delegate);
        } catch (Exception e) {
            throw new RuntimeException("Error invoking method: " + methodName, e);
        }
    }


}