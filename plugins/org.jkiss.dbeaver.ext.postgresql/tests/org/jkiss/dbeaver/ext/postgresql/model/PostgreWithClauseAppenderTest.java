package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.ext.postgresql.model.impls.greenplum.GreenplumTable;
import org.junit.Before;
import org.junit.Test;

import static org.jkiss.dbeaver.ext.postgresql.model.PostgreWithClauseAppender.generateWithClause;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostgreWithClauseAppenderTest {

    private PostgreTableRegular table;
    private PostgreTableBase tableBase;

    private PostgreDataSource dataSource;
    private PostgreServerExtension serverExtension;

    @Before
    public void setup() {
        dataSource = mock(PostgreDataSource.class);
        serverExtension = mock(PostgreServerExtension.class);
        tableBase = mock(PostgreTableBase.class);
    }

    @Test
    public void generateWithClause_whenOidsAreSupportedAndNoRelOptions_shouldDisplayWithClauseWithOidsAsTrue() {
        table = mock(PostgreTableRegular.class);

        when(serverExtension.supportsOids()).thenReturn(true);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(true);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableSupportsOidsButDoesNotHaveOidsAndNoOptions_shouldNotDisplayWithClause() {
        table = mock(PostgreTableRegular.class);

        when(serverExtension.supportsOids()).thenReturn(true);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(false);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsAGreenPlumTableWithOptions_shouldDisplayWithClauseWithRelOptions() {
        table = mock(GreenplumTable.class);

        when(serverExtension.supportsOids()).thenReturn(false);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(false);

        when(tableBase.getRelOptions()).thenReturn(new String[]{"appendonly=true"});

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tappendonly=true\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsGreenPlumTableWithOidsAndOptions_shouldDisplayWithClauseWithOidsAndOptions() {
        table = mock(GreenplumTable.class);

        when(serverExtension.supportsOids()).thenReturn(true);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(true);

        when(tableBase.getRelOptions()).thenReturn(new String[]{"appendonly=true"});

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE, appendonly=true\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsGreenPlumTableWithoutOidsAndWithoutOptions_shouldNotDisplayWithClause() {
        table = mock(GreenplumTable.class);

        when(serverExtension.supportsOids()).thenReturn(false);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(false);

        when(tableBase.getRelOptions()).thenReturn(null);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsGreenPlumTableWithOidsWithoutOptions_shouldDisplayWithClauseWithOids() {
        table = mock(GreenplumTable.class);

        when(serverExtension.supportsOids()).thenReturn(true);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(true);

        when(tableBase.getRelOptions()).thenReturn(null);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableIsGreenPlumTableWithOidsWithMultipleOptions_shouldDisplayWithClauseWithOidsAndAllTheOptions() {
        table = mock(GreenplumTable.class);

        when(serverExtension.supportsOids()).thenReturn(true);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(true);

        when(tableBase.getRelOptions()).thenReturn(new String[]{"appendonly=true", "orientation=column"});

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE, appendonly=true, orientation=column\n)", withClause);
    }
}
