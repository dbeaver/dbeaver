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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.h2.util.H2InitScriptValidator;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class H2InitScriptValidatorTest {
    @Test
    public void rechecksChangedBootstrapBoundariesAndOrder() throws DBCException {
        H2InitScriptValidator validator = new H2InitScriptValidator();
        String first = "SELECT 1 /*";
        String second = "CALL FILE_WRITE(STRINGTOUTF8('marker'), '/tmp/marker'); -- */";
        List<String> single = List.of(first + ";\n" + second);
        validator.validateScripts("jdbc:h2:mem:test", single, scripts -> true);
        validator.validateScripts("jdbc:h2:mem:test", single, scripts -> Assertions.fail("Already approved"));
        Assertions.assertThrows(DBCException.class, () ->
            validator.validateScripts("jdbc:h2:mem:test", List.of(first, second), scripts -> false));
        validator.validateScripts("jdbc:h2:mem:test", List.of(first, second), scripts -> true);
        Assertions.assertThrows(DBCException.class, () ->
            validator.validateScripts("jdbc:h2:mem:test", List.of(second, first), scripts -> false));
    }

    @Test
    public void snapshotsBootstrapApproval() throws DBCException {
        H2InitScriptValidator validator = new H2InitScriptValidator();
        List<String> scripts = new ArrayList<>(List.of("CALL 1"));
        validator.validateScripts(null, scripts, snapshot -> {
            Assertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.add("CALL 2"));
            return true;
        });
        scripts.add("CALL 2");
        Assertions.assertThrows(DBCException.class, () -> validator.validateScripts(null, scripts, snapshot -> false));
    }

    @Test
    public void skipsConnectionsWithoutInit() throws DBCException {
        for (String url : new String[] {null, "jdbc:h2:mem:test", "jdbc:h2:mem:INIT=CALL 1;MODE=PostgreSQL"}) {
            new H2InitScriptValidator().validate(url, new Properties(), script -> {
                Assertions.fail("Unexpected confirmation: " + script);
                return false;
            });
        }
    }

    @Test
    public void detectsInitRegardlessOfCaseAndEscapedKey() throws DBCException {
        for (String key : new String[] {"INIT", "init", "InIt", "I\\NIT", "ınıt"}) {
            Assertions.assertEquals(List.of("CALL 1"), collect("jdbc:h2:mem:test;" + key + "=CALL 1", new Properties()));
        }
    }

    @Test
    public void detectsInitInPropertiesWithoutUrl() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("init", "CALL 1");
        Assertions.assertEquals(List.of("CALL 1"), collect(null, properties));
    }

    @Test
    public void preservesEscapedStatementsAndEqualsInSql() throws DBCException {
        Assertions.assertEquals(
            List.of("CREATE SCHEMA TEST;SET SCHEMA TEST;CALL 'a=b'"),
            collect("jdbc:h2:mem:test;INIT=CREATE SCHEMA TEST\\;SET SCHEMA TEST\\;CALL 'a=b';MODE=PostgreSQL", new Properties())
        );
    }

    @Test
    public void handlesEscapedBackslashesAndTrailingBackslash() throws DBCException {
        Assertions.assertEquals(
            List.of("CALL 'C:\\test'", "CALL 2\\"),
            collect("jdbc:h2:mem:test;INIT=CALL 'C:\\\\test';INIT=CALL 2\\", new Properties())
        );
    }

    @Test
    public void treatsQuotesLikeH2UrlTokenizer() throws DBCException {
        Assertions.assertEquals(
            List.of("CALL 'first", "CALL 2'"),
            collect("jdbc:h2:mem:test;INIT=CALL 'first;INIT=CALL 2'", new Properties())
        );
    }

    @Test
    public void doesNotConfuseInitWithOtherSettings() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("OTHER", "INIT=CALL 1");
        Assertions.assertEquals(
            List.of(),
            collect("jdbc:h2:mem:test;OTHER=INIT=CALL 1;INIT_OTHER=CALL 2; INIT=CALL 3", properties)
        );
    }

    @Test
    public void skipsEmptyInit() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("INIT", " ");
        Assertions.assertEquals(List.of(), collect("jdbc:h2:mem:test;INIT=", properties));
    }

    @Test
    public void checksBothUrlAndProperties() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("INIT", "CALL 1");
        Assertions.assertEquals(List.of("CALL 1", "CALL 2"), collect("jdbc:h2:mem:test;INIT=CALL 2", properties));
        Assertions.assertEquals(List.of("CALL 1"), collect("jdbc:h2:mem:test;INIT=CALL 1", properties));
    }

    @Test
    public void rejectsUnapprovedInitAndDoesNotRememberDenial() {
        H2InitScriptValidator validator = new H2InitScriptValidator();
        AtomicInteger confirmations = new AtomicInteger();
        for (int attempt = 0; attempt < 2; attempt++) {
            Assertions.assertThrows(DBCException.class, () -> validator.validate(
                "jdbc:h2:mem:test;INIT=CALL 1", new Properties(), script -> {
                    confirmations.incrementAndGet();
                    return false;
                }
            ));
        }
        Assertions.assertEquals(2, confirmations.get());
    }

    @Test
    public void remembersOnlyApprovedUrlAndScriptInCurrentValidator() throws DBCException {
        H2InitScriptValidator validator = new H2InitScriptValidator();
        AtomicInteger confirmations = new AtomicInteger();
        Properties properties = new Properties();
        properties.setProperty("INIT", "CALL 1");
        for (String url : new String[] {"jdbc:h2:mem:first", "jdbc:h2:mem:first", "jdbc:h2:mem:second"}) {
            validator.validate(url, properties, script -> {
                confirmations.incrementAndGet();
                return true;
            });
        }
        properties.setProperty("INIT", "CALL 2");
        validator.validate("jdbc:h2:mem:second", properties, script -> {
            confirmations.incrementAndGet();
            return true;
        });
        Assertions.assertEquals(3, confirmations.get());
        Assertions.assertThrows(DBCException.class, () -> new H2InitScriptValidator().validate(
            "jdbc:h2:mem:second", properties, script -> false
        ));
    }

    @NotNull
    private List<String> collect(@Nullable String url, @NotNull Properties properties) throws DBCException {
        List<String> scripts = new ArrayList<>();
        new H2InitScriptValidator().validate(url, properties, script -> {
            scripts.add(script);
            return true;
        });
        return scripts;
    }
}
