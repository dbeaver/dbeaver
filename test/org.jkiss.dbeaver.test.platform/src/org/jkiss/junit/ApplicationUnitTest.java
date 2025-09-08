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
package org.jkiss.junit;

import org.jkiss.junit.osgi.OSGITestRunner;
import org.jkiss.junit.osgi.annotation.RunWithApplication;
import org.jkiss.junit.osgi.annotation.RunnerProxy;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;


@RunnerProxy(MockitoJUnitRunner.class)
@RunWith(OSGITestRunner.class)
@RunWithApplication(bundleName = "org.jkiss.dbeaver.headless", registryName = "org.jkiss.dbeaver.headless.application")
public abstract class ApplicationUnitTest {

}
