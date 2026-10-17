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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MySQLUser#getFullName()}: the account is rendered as back-tick quoted
 * identifiers, which mean the same regardless of NO_BACKSLASH_ESCAPES in sql_mode.
 */
public class MySQLUserAccountTest extends DBeaverUnitTest {

    @NotNull
    private static MySQLUser user(@NotNull String userName, @NotNull String host) {
        MySQLUser user = new MySQLUser(null, null);
        user.setUserName(userName);
        user.setHost(host);
        return user;
    }

    @Test
    public void plainAccountIsQuoted() {
        Assertions.assertEquals("`app`@`%`", user("app", "%").getFullName());
    }

    @Test
    public void singleQuoteNeedsNoEscaping() {
        Assertions.assertEquals("`o'brien`@`ho'st`", user("o'brien", "ho'st").getFullName());
    }

    @Test
    public void backtickIsDoubledInBothParts() {
        Assertions.assertEquals("`a``b`@`c``d`", user("a`b", "c`d").getFullName());
    }

    @Test
    public void backslashIsKeptLiterally() {
        // No backslash escaping inside back-ticks, so the result doesn't depend on NO_BACKSLASH_ESCAPES
        Assertions.assertEquals("`a\\b`@`%`", user("a\\b", "%").getFullName());
        Assertions.assertEquals("`x\\`@`%`", user("x\\", "%").getFullName());
    }

    @Test
    public void anonymousAccountIsQuoted() {
        Assertions.assertEquals("``@`localhost`", user("", "localhost").getFullName());
    }
}
