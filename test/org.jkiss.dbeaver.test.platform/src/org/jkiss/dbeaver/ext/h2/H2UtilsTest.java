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
package org.jkiss.dbeaver.ext.h2;

import org.jkiss.dbeaver.ext.h2.util.H2Utils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class H2UtilsTest extends DBeaverUnitTest {
    private final String originalAllowedClasses = System.getProperty("h2.allowedClasses");

    @AfterEach
    public void restoreAllowedClasses() {
        if (originalAllowedClasses != null) {
            System.setProperty("h2.allowedClasses", originalAllowedClasses);
        } else {
            System.clearProperty("h2.allowedClasses");
        }
    }

    @Test
    public void detectsRestrictedClassLoading() {
        System.setProperty("h2.allowedClasses", "java.lang.Math,org.h2.mvstore.db.MVTableEngine");
        Assertions.assertTrue(H2Utils.isClassLoadingRestricted());

        System.setProperty("h2.allowedClasses", "*");
        Assertions.assertFalse(H2Utils.isClassLoadingRestricted());
    }
}
