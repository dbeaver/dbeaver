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
package org.jkiss.junit.osgi.delegate;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.lang.reflect.Method;


/**
 * Serializes and transfers object to another classloaders
 */
public class RunListenerDelegate extends RunListener {
    private final Object delegate;

    public RunListenerDelegate(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        invokeMethod("fireTestRunStarted", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        invokeMethod("fireTestRunFinished", ClassTransferHandler.transfer(result, delegate.getClass().getClassLoader()));
    }

    @Override
    public void testSuiteStarted(Description description) throws Exception {
        invokeMethod("fireTestSuiteStarted", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override
    public void testSuiteFinished(Description description) throws Exception {
        invokeMethod("fireTestSuiteFinished", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override
    public void testStarted(Description description) throws Exception {
        invokeMethod("fireTestStarted", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override
    public void testFinished(Description description) throws Exception {
        invokeMethod("fireTestFinished", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        invokeMethod("fireTestFailure", ClassTransferHandler.transfer(failure, delegate.getClass().getClassLoader()));
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        invokeMethod("fireTestAssumptionFailure", ClassTransferHandler.transfer(failure, delegate.getClass().getClassLoader()));
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        invokeMethod("fireTestIgnored", ClassTransferHandler.transfer(description, delegate.getClass().getClassLoader()));
    }

    private void invokeMethod(@NotNull String methodName, @Nullable Object argument) {
        try {
            Method method;
            if (argument != null) {
                method = delegate.getClass().getDeclaredMethod(methodName, argument.getClass());
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
            method = delegate.getClass().getDeclaredMethod(methodName);
            method.invoke(delegate);
        } catch (Exception e) {
            throw new RuntimeException("Error invoking method: " + methodName, e);
        }
    }
}
