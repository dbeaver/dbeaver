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
package org.jkiss.dbeaver.model.navigator.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTermProvider;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DBXTreeFolderTest extends DBeaverUnitTest {
    private final AbstractDescriptor source = Mockito.mock(AbstractDescriptor.class);
    private final IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
    private final DBPDataSource dataSource = Mockito.mock(DBPDataSource.class, Mockito.withSettings().extraInterfaces(DBPTermProvider.class));
    private final DBPTermProvider termProvider = (DBPTermProvider) dataSource;

    @BeforeEach
    public void setUp() {
        Mockito.when(config.getAttribute("label")).thenReturn("Tables");
        Mockito.when(config.getAttribute("label", "de")).thenReturn("Tabellen");
        Mockito.when(config.getAttribute("labelTerm")).thenReturn("table");
    }

    @Test
    public void usesPluralTermFromFirstItemChild() {
        DBXTreeFolder folder = createFolder(null);
        createFolder(folder);
        addItem(folder, "table");
        addItem(folder, "view");
        Mockito.when(termProvider.getObjectTypeTerm("table", "table", true)).thenReturn("Entities");

        Assertions.assertEquals("Entities", folder.getNodeTypeLabel(dataSource, null));
        Assertions.assertEquals("Entities", folder.getNodeTypeLabel(dataSource, "de"));
        Assertions.assertEquals("Entities", folder.getChildrenTypeLabel(dataSource, null));
        Mockito.verify(termProvider, Mockito.times(3)).getObjectTypeTerm("table", "table", true);
        Mockito.verifyNoMoreInteractions(termProvider);
    }

    @Test
    public void preservesLabelsWithoutTermProvider() {
        DBXTreeFolder folder = createFolder(null);
        addItem(folder, "table");

        assertLabels(folder, null);
        assertLabels(folder, Mockito.mock(DBPDataSource.class));
    }

    @Test
    public void preservesLabelsWithoutLabelTerm() {
        Mockito.when(config.getAttribute("labelTerm")).thenReturn(null);
        DBXTreeFolder folder = createFolder(null);
        addItem(folder, "table");
        assertLabels(folder, dataSource);

        Mockito.when(config.getAttribute("labelTerm")).thenReturn("");
        folder = createFolder(null);
        addItem(folder, "table");
        assertLabels(folder, dataSource);
        Mockito.verifyNoInteractions(termProvider);
    }

    @Test
    public void preservesLabelsForNullOrEmptyTerm() {
        DBXTreeFolder folder = createFolder(null);
        addItem(folder, "table");
        addItem(folder, "view");

        assertLabels(folder, dataSource);
        Mockito.when(termProvider.getObjectTypeTerm("table", "table", true)).thenReturn("");
        assertLabels(folder, dataSource);
        Mockito.verify(termProvider, Mockito.times(4)).getObjectTypeTerm("table", "table", true);
        Mockito.verifyNoMoreInteractions(termProvider);
    }

    @Test
    public void preservesLabelsWithoutItemChildren() {
        DBXTreeFolder folder = createFolder(null);
        assertLabels(folder, dataSource);
        addItem(createFolder(folder), "table");
        assertLabels(folder, dataSource);
        Mockito.verifyNoInteractions(termProvider);
    }

    @Test
    public void copiesLabelTermAndItemPath() {
        DBXTreeFolder folder = createFolder(null);
        addItem(folder, "table");
        Mockito.when(termProvider.getObjectTypeTerm("table", "table", true)).thenReturn("Entities");

        DBXTreeFolder copy = new DBXTreeFolder(source, null, folder);
        Assertions.assertEquals("Entities", copy.getNodeTypeLabel(dataSource, null));
        Assertions.assertEquals("Entities", copy.getNodeTypeLabel(dataSource, "de"));
        assertLabels(copy, null);
    }

    @Test
    public void explicitInjectedLabelClearsCopiedTerm() {
        DBXTreeFolder folder = createFolder(null);
        addItem(folder, "table");
        DBXTreeFolder copy = new DBXTreeFolder(source, null, folder);
        IConfigurationElement injection = Mockito.mock(IConfigurationElement.class);
        Mockito.when(injection.getAttribute("changeFolderLabel", "de")).thenReturn("Datensätze");
        copy.setInjectedConfig(injection);
        copy.setLabel("Datasets");

        Assertions.assertEquals("Datasets", copy.getNodeTypeLabel(dataSource, null));
        Assertions.assertEquals("Datensätze", copy.getNodeTypeLabel(dataSource, "de"));
        Assertions.assertEquals("Datasets", new DBXTreeFolder(source, null, copy).getNodeTypeLabel(dataSource, null));
        Mockito.verifyNoInteractions(termProvider);

        Mockito.when(injection.getAttribute("changeFolderLabel", "de")).thenReturn("");
        Assertions.assertEquals("Tabellen", copy.getNodeTypeLabel(dataSource, "de"));
        Mockito.when(injection.getAttribute("changeFolderLabel", "de")).thenReturn(null);
        Assertions.assertEquals("Tabellen", copy.getNodeTypeLabel(dataSource, "de"));

        Mockito.when(termProvider.getObjectTypeTerm("table", "table", true)).thenReturn("Entities");
        Assertions.assertEquals("Entities", folder.getNodeTypeLabel(dataSource, null));
    }

    @NotNull
    private DBXTreeFolder createFolder(@Nullable DBXTreeNode parent) {
        return new DBXTreeFolder(source, parent, config, "table", true, false, null, false);
    }

    private void addItem(@NotNull DBXTreeFolder folder, @NotNull String path) {
        new DBXTreeItem(source, folder, config, path, path, false, true, false, false, false, null, null);
    }

    private void assertLabels(@NotNull DBXTreeFolder folder, @Nullable DBPDataSource dataSource) {
        Assertions.assertEquals("Tables", folder.getNodeTypeLabel(dataSource, null));
        Assertions.assertEquals("Tabellen", folder.getNodeTypeLabel(dataSource, "de"));
    }
}
