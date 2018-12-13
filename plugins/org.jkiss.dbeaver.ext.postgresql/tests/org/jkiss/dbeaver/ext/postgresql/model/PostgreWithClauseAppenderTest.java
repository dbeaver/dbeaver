package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.ext.postgresql.model.impls.greenplum.GreenplumTable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.jkiss.dbeaver.ext.postgresql.model.PostgreWithClauseAppender.generateWithClause;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostgreWithClauseAppenderTest {

    @Mock
    private PostgreTableRegular table;

    @Mock
    private PostgreTableBase tableBase;

    @Mock
    private PostgreDataSource dataSource;

    @Mock
    private PostgreServerExtension serverExtension;

    @Before
    public void setup() {
    }

    @Test
    public void generateWithClause_whenOidsAreSupportedAndNoRelOptions_shouldDisplayWithClauseWithOidsAsTrue() {
        setupGeneralWhenMocks(true, true);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableSupportsOidsButDoesNotHaveOidsAndNoOptions_shouldNotDisplayWithClause() {
        setupGeneralWhenMocks(true, false);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsAGreenPlumTableWithOptions_shouldDisplayWithClauseWithRelOptions() {
        table = mock(GreenplumTable.class);

        setupGeneralWhenMocks(false, false);

        when(tableBase.getRelOptions()).thenReturn(new String[]{"appendonly=true"});

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tappendonly=true\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsGreenPlumTableWithOidsAndOptions_shouldDisplayWithClauseWithOidsAndOptions() {
        table = mock(GreenplumTable.class);

        setupGeneralWhenMocks(true, true);

        when(tableBase.getRelOptions()).thenReturn(new String[]{"appendonly=true"});

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE, appendonly=true\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsGreenPlumTableWithoutOidsAndWithoutOptions_shouldNotDisplayWithClause() {
        table = mock(GreenplumTable.class);

        setupGeneralWhenMocks(false, false);

        when(tableBase.getRelOptions()).thenReturn(null);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsGreenPlumTableWithOidsWithoutOptions_shouldDisplayWithClauseWithOids() {
        table = mock(GreenplumTable.class);

        setupGeneralWhenMocks(true, true);

        when(tableBase.getRelOptions()).thenReturn(null);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsGreenPlumTableWithOidsWithMultipleOptions_shouldDisplayWithClauseWithOidsAndAllTheOptions() {
        table = mock(GreenplumTable.class);

        setupGeneralWhenMocks(true, true);

        when(tableBase.getRelOptions()).thenReturn(new String[]{"appendonly=true", "orientation=column"});

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE, appendonly=true, orientation=column\n)", withClause);
    }

    private void setupGeneralWhenMocks(boolean supportOids, boolean hasOids) {
        when(serverExtension.supportsOids()).thenReturn(supportOids);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(hasOids);
    }
}
