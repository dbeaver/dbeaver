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
package org.jkiss.junit.osgi.annotation;

import org.eclipse.core.runtime.Platform;
import org.jkiss.junit.osgi.OSGITestRunner;

import java.lang.annotation.*;
import java.util.concurrent.Callable;

/**
 * Run with product used for @{@link OSGITestRunner}
 * Annotation to provide an application parameters for OSGI tests
 * See {@link org.jkiss.junit.osgi.OSGITestRunner}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RunWithApplication {
    /**
     * Bundle name with application
     */
    String bundleName();

    /**
     Application classname
     */
    String registryName();

    /**
     * Application parameters
     */
    String[] args() default {};

}
