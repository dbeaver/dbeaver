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
package org.jkiss.dbeaver.utils;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link DataSourceUtils#getDataSourceBySpec}, focused on connection spec
 * parsing - in particular splitting on {@code |} and the escaping of {@code \|} / {@code \\}.
 */
public class DataSourceUtilsTest extends DBeaverUnitTest {

    private DBPProject project;
    private DBPDataSourceRegistry registry;

    @BeforeEach
    public void setUp() {
        project = Mockito.mock(DBPProject.class);
        registry = Mockito.mock(DBPDataSourceRegistry.class);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(registry);
        Mockito.when(project.getName()).thenReturn("Test Project");
    }

    /**
     * Registers a datasource (with an empty, mutable connection configuration) that the
     * registry will return when looked up by the given name.
     */
    @NotNull
    private DBPDataSourceContainer registerByName(@NotNull String name) {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getConnectionConfiguration()).thenReturn(new DBPConnectionConfiguration());
        Mockito.when(registry.findDataSourceByName(name)).thenReturn(container);
        return container;
    }

    @Nullable
    private DBPDataSourceContainer resolve(@NotNull String spec) {
        return DataSourceUtils.getDataSourceBySpec(project, spec, null, false, false);
    }

    @Test
    public void resolvesDataSourceByName() {
        String spec = "name=My DB";
        DBPDataSourceContainer expected = registerByName("My DB");

        DBPDataSourceContainer result = resolve(spec);

        Assertions.assertSame(expected, result);
    }

    @Test
    public void resolvesSingleTokenAsIdOrName() {
        String spec = "my-ds-id";
        DBPDataSourceContainer expected = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(expected.getConnectionConfiguration()).thenReturn(new DBPConnectionConfiguration());
        Mockito.when(registry.getDataSource("my-ds-id")).thenReturn(expected);

        DBPDataSourceContainer result = resolve(spec);

        Assertions.assertSame(expected, result);
    }

    @Test
    public void splitsParametersOnPipe() {
        String spec = "name=My DB|user=admin|password=secret";
        DBPDataSourceContainer expected = registerByName("My DB");

        DBPDataSourceContainer result = resolve(spec);

        Assertions.assertSame(expected, result);
        DBPConnectionConfiguration cfg = result.getConnectionConfiguration();
        Assertions.assertEquals("admin", cfg.getUserName());
        Assertions.assertEquals("secret", cfg.getUserPassword());
    }

    @Test
    public void unescapesPipeInsideName() {
        String spec = "name=My\\|DB";
        DBPDataSourceContainer expected = registerByName("My|DB");

        DBPDataSourceContainer result = resolve(spec);

        Assertions.assertSame(expected, result);
    }

    @Test
    public void unescapesPipeInsideValue() {
        String spec = "name=My DB|password=p@ss\\|word";
        DBPDataSourceContainer expected = registerByName("My DB");

        DBPDataSourceContainer result = resolve(spec);

        Assertions.assertSame(expected, result);
        Assertions.assertEquals("p@ss|word", result.getConnectionConfiguration().getUserPassword());
    }

    @Test
    public void unescapesEscapedBackslash() {
        String spec = "name=a\\\\b";
        DBPDataSourceContainer expected = registerByName("a\\b");

        DBPDataSourceContainer result = resolve(spec);

        Assertions.assertSame(expected, result);
    }

    @Test
    public void escapedBackslashBeforePipeStillSeparates() {
        String spec = "name=a\\\\|user=admin";
        DBPDataSourceContainer expected = registerByName("a\\");

        DBPDataSourceContainer result = resolve(spec);

        Assertions.assertSame(expected, result);
        Assertions.assertEquals("admin", result.getConnectionConfiguration().getUserName());
    }

    @Test
    public void returnsNullWhenDataSourceNotFound() {
        String spec = "name=Missing DB";

        DBPDataSourceContainer result = resolve(spec);

        Assertions.assertNull(result);
    }
}
