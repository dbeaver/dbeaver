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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * {@link MimerLibrary#buildCreateDDL()} - the {@code CREATE LIBRARY} statement for a Mimer SQL
 * 11.1+ external (CLR) library.
 *
 * @author Mimer Information Technology
 */
public class MimerLibraryTest extends DBeaverUnitTest {

    @Mock
    private MimerDataSource dataSource;

    @Test
    public void createDDLQuotesNameAndFilePath() {
        MimerLibrary library = new MimerLibrary(dataSource, "NETLIB");
        library.setFileName("C:\\dev\\MyLib.dll");

        Assertions.assertEquals(
            "CREATE LIBRARY \"NETLIB\" FILE 'C:\\dev\\MyLib.dll' LANGUAGE CLR",
            library.buildCreateDDL());
    }

    @Test
    public void createDDLEscapesSingleQuoteInFilePath() {
        MimerLibrary library = new MimerLibrary(dataSource, "NETLIB");
        library.setFileName("C:\\dev\\Fredrik's Lib.dll");

        Assertions.assertEquals(
            "CREATE LIBRARY \"NETLIB\" FILE 'C:\\dev\\Fredrik''s Lib.dll' LANGUAGE CLR",
            library.buildCreateDDL());
    }

    @Test
    public void createDDLReflectsAnExplicitlySetLanguage() {
        // "CLR" is only today's one real value (see the class Javadoc) - a future server could
        // report something else, so the language must not be hardcoded into the DDL builder.
        MimerLibrary library = new MimerLibrary(dataSource, "PYLIB");
        library.setFileName("mylib.py");
        library.setLanguage("PYTHON");

        Assertions.assertEquals(
            "CREATE LIBRARY \"PYLIB\" FILE 'mylib.py' LANGUAGE PYTHON",
            library.buildCreateDDL());
    }

    @Test
    public void newLibraryDefaultsToClrLanguageAndNotPersisted() {
        MimerLibrary library = new MimerLibrary(dataSource, "NETLIB");

        Assertions.assertEquals("CLR", library.getLanguage());
        Assertions.assertFalse(library.isPersisted());
    }
}
