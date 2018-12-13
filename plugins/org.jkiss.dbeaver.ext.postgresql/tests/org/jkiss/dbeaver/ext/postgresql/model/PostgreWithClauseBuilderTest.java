package org.jkiss.dbeaver.ext.postgresql.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.jkiss.dbeaver.ext.postgresql.model.PostgreWithClauseBuilder.generateWithClause;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostgreWithClauseBuilderTest {

    @Mock
    private PostgreTableRegular table;

    @Mock
    private PostgreDataSource dataSource;

    @Mock
    private PostgreServerExtension serverExtension;

    @Test
    public void generateWithClause_whenOidsAreSupported_shouldDisplayWithClauseWithOidsAsTrue() {
        setupGeneralWhenMocks(true, true);

        String withClause = generateWithClause(table);
        assertEquals("\nWITH (\n\tOIDS=TRUE\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableSupportsOidsButDoesNotHaveOids_shouldNotDisplayWithClause() {
        setupGeneralWhenMocks(true, false);

        String withClause = generateWithClause(table);
        assertEquals("", withClause);
    }

    @Test
    public void generateWithClause_whenTableDoesNotSupportsOids_shouldNotDisplayWithClause() {
        setupGeneralWhenMocks(false, false);

        String withClause = generateWithClause(table);
        assertEquals("", withClause);
    }

    private void setupGeneralWhenMocks(boolean supportOids, boolean hasOids) {
        when(serverExtension.supportsOids()).thenReturn(supportOids);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(hasOids);
    }
}
