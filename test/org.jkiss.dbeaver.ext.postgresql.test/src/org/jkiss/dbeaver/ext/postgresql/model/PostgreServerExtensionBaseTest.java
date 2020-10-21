package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostgreServerExtensionBaseTest {

    @Mock
    private PostgreTableRegular table;

    @Mock
    private PostgreDataSource dataSource;

    private PostgreServerPostgreSQL serverExtension;

    @Before
    public void setUp() {
        serverExtension = new PostgreServerPostgreSQL(dataSource);
    }

    @Test
    public void createWithClause_whenOidsAreSupported_shouldDisplayWithClauseWithOidsAsTrue() {
        setupGeneralWhenMocks(true);

        String withClause = serverExtension.createWithClause(table, null);
        assertEquals("\nWITH (\n\tOIDS=TRUE\n)", withClause);
    }

    @Test
    public void createWithClause_whenTableSupportsOidsButDoesNotHaveOids_shouldNotDisplayWithClause() {
        setupGeneralWhenMocks(false);

        String withClause = serverExtension.createWithClause(table, null);
        assertEquals("", withClause);
    }

    @Test
    public void createWithClause_whenTableDoesNotSupportsOids_shouldNotDisplayWithClause() {
        setupGeneralWhenMocks(false);

        String withClause = serverExtension.createWithClause(table, null);
        assertEquals("", withClause);
    }

    private void setupGeneralWhenMocks(boolean hasOids) {
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(hasOids);
    }
}
