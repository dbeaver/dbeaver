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
package org.jkiss.dbeaver.model.lsp.test;

import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentItem;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.lsp.DBLServerSessionProvider;
import org.jkiss.dbeaver.model.lsp.DBLTextDocumentService;
import org.jkiss.dbeaver.model.lsp.context.ContextAwareDocument;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.utils.ResourceUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class DBLTextDocumentServiceWorkspaceTest extends H2DataSourceTest {
    private static final String PROJECT_ID = "DBLTextDocumentServiceProject";
    private static final String DATA_SOURCE_ID = "workspace-test-data-source";

    private DBLTextDocumentService service = new DBLTextDocumentService(new TestSessionProvider());

    protected DBPWorkspace workspace;
    protected DBPProject project;

    @Before
    public void setUpWorkspace() {
        workspace = Mockito.mock(DBPWorkspace.class);
        project = Mockito.mock(DBPProject.class);
        DataSourceRegistry registry = Mockito.mock(DataSourceRegistry.class);
        dataSourceDescriptor.setId(DATA_SOURCE_ID);

        Mockito.when(workspace.getProject(PROJECT_ID)).thenReturn(project);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(registry);
        Mockito.when(registry.getDataSource(dataSourceDescriptor.getId()))
            .thenReturn(dataSourceDescriptor);

        DBLServerSessionProvider sessionProvider = new TestSessionProvider(workspace);
        service = new DBLTextDocumentService(sessionProvider);
    }

    @After
    public void cleanup() {
        DocumentServiceTestUtils.clearDocuments(service);
    }

    @Test
    public void shouldInitContextWithCustomWorkspace() {
        Mockito.when(project.getResourceProperty(
            DocumentServiceTestUtils.BASIC_RESOURCE_PATH,
            ResourceUtils.PROP_CONTEXT_DEFAULT_DATASOURCE
        )).thenReturn(DATA_SOURCE_ID);

        String uri = String.format("lsp://%s/%s", PROJECT_ID, DocumentServiceTestUtils.BASIC_RESOURCE_PATH);
        TextDocumentItem document = new TextDocumentItem(
            uri, DocumentServiceTestUtils.SQL_LANGUAGE_ID, 0, "select * from table"
        );

        service.didOpen(new DidOpenTextDocumentParams(document));

        ContextAwareDocument contextAwareDocument = DocumentServiceTestUtils.getDocument(service, document.getUri());
        Assert.assertNotNull(contextAwareDocument);
        Assert.assertEquals(dataSourceDescriptor.getDataSource(), contextAwareDocument.getDataSource());
    }
}
