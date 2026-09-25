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

import java.util.List;
import java.util.Set;

/**
 * {@link MimerDatabank}'s DDL builders - {@code CREATE}/{@code DROP DATABANK}, the {@code SET
 * DATABANK ONLINE/OFFLINE} statement family, and {@link MimerDatabank#buildAlterDDL} (up to two
 * statements - SET for changed/set properties, DROP for sizes cleared back to blank).
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankTest extends DBeaverUnitTest {

    @Mock
    private MimerDataSource dataSource;

    @Test
    public void createDDLOmitsOptionalClausesWhenUnset() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");

        // New-databank constructor defaults: type=TRANSACTION, no file/sizes/removable.
        Assertions.assertEquals("CREATE DATABANK \"MYDB\" SET OPTION TRANSACTION", databank.buildCreateDDL());
    }

    @Test
    public void createDDLIncludesEveryClauseInOrderWhenAllAreSet() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        databank.setFile("mydb.dbf");
        databank.setFileSize("10M");
        databank.setMinSize("2000K");
        databank.setGoalSize("10M");
        databank.setMaxSize("100M");
        databank.setRemovable(true);
        databank.setType("WORK");

        Assertions.assertEquals(
            "CREATE DATABANK \"MYDB\" SET FILE 'mydb.dbf', FILESIZE 10M, MINSIZE 2000K, GOALSIZE 10M, " +
            "MAXSIZE 100M, REMOVABLE, OPTION WORK",
            databank.buildCreateDDL());
    }

    @Test
    public void dropDDLIsAPlainDropWithNoCascadeOption() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        Assertions.assertEquals("DROP DATABANK \"MYDB\"", databank.buildDropDDL());
    }

    @Test
    public void setOnlineDDLGoingOfflineHasNoLogOption() {
        Assertions.assertEquals(
            "SET DATABANK \"MYDB\" OFFLINE",
            MimerUtils.buildSetOnlineDDL("DATABANK", List.of("MYDB"), MimerUtils.ONLINE_STATE_OFFLINE));
    }

    @Test
    public void setOnlineDDLGoingOnlineCarriesTheChosenLogMode() {
        Assertions.assertEquals(
            "SET DATABANK \"MYDB\" ONLINE PRESERVE LOG",
            MimerUtils.buildSetOnlineDDL("DATABANK", List.of("MYDB"), MimerUtils.ONLINE_STATE_PRESERVE));
        Assertions.assertEquals(
            "SET SHADOW \"MYDB\" ONLINE RESET LOG",
            MimerUtils.buildSetOnlineDDL("SHADOW", List.of("MYDB"), MimerUtils.ONLINE_STATE_RESET));
    }

    @Test
    public void setOnlineDDLQuotesAndCommaSeparatesAMultiObjectList() {
        Assertions.assertEquals(
            "SET SHADOW \"A\", \"B\" OFFLINE",
            MimerUtils.buildSetOnlineDDL("SHADOW", List.of("A", "B"), MimerUtils.ONLINE_STATE_OFFLINE));
    }

    @Test
    public void onlineStateOptionsDependOnCurrentState() {
        Assertions.assertArrayEquals(
            new String[]{MimerUtils.ONLINE_STATE_OFFLINE},
            MimerUtils.onlineStatesFor(true));
        Assertions.assertArrayEquals(
            new String[]{MimerUtils.ONLINE_STATE_PRESERVE, MimerUtils.ONLINE_STATE_RESET},
            MimerUtils.onlineStatesFor(false));
    }

    @Test
    public void stateGoesOnlineIsTrueForEitherOnlineVariant() {
        Assertions.assertFalse(MimerUtils.stateGoesOnline(MimerUtils.ONLINE_NOCHANGE));
        Assertions.assertFalse(MimerUtils.stateGoesOnline(MimerUtils.ONLINE_STATE_OFFLINE));
        Assertions.assertTrue(MimerUtils.stateGoesOnline(MimerUtils.ONLINE_STATE_PRESERVE));
        Assertions.assertTrue(MimerUtils.stateGoesOnline(MimerUtils.ONLINE_STATE_RESET));
    }

    @Test
    public void changeStateDropdownDefaultsToTheNoChangeSentinelAndEmitsNoDDL() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        Assertions.assertEquals(MimerUtils.ONLINE_NOCHANGE, databank.getOnlineTransition());
        Assertions.assertNull(databank.buildSetOnlineDDL());
    }

    @Test
    public void changeStateDropdownBuildsTheSetStatementForItsPick() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        databank.setOnlineTransition(MimerUtils.ONLINE_STATE_OFFLINE);
        Assertions.assertEquals("SET DATABANK \"MYDB\" OFFLINE", databank.buildSetOnlineDDL());
        databank.setOnlineTransition(MimerUtils.ONLINE_STATE_RESET);
        Assertions.assertEquals("SET DATABANK \"MYDB\" ONLINE RESET LOG", databank.buildSetOnlineDDL());
    }

    @Test
    public void changeStateDropdownOptionsLeadWithTheSentinelThenTheValidStates() {
        MimerDatabank online = new MimerDatabank(dataSource, "MYDB");
        online.setOnline(true);
        Assertions.assertArrayEquals(
            new String[]{MimerUtils.ONLINE_NOCHANGE, MimerUtils.ONLINE_STATE_OFFLINE},
            new MimerDatabank.OnlineTransitionListProvider().getPossibleValues(online));

        MimerDatabank offline = new MimerDatabank(dataSource, "MYDB");
        offline.setOnline(false);
        Assertions.assertArrayEquals(
            new String[]{MimerUtils.ONLINE_NOCHANGE, MimerUtils.ONLINE_STATE_PRESERVE, MimerUtils.ONLINE_STATE_RESET},
            new MimerDatabank.OnlineTransitionListProvider().getPossibleValues(offline));
    }

    @Test
    public void alterDDLReturnsNothingWhenNoTrackedPropertyChanged() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        Assertions.assertTrue(databank.buildAlterDDL(Set.of()).isEmpty());
    }

    @Test
    public void alterDDLEmitsOnlyASetStatementWhenEverythingIsBeingSet() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        databank.setType("LOG");
        databank.setMinSize("2000K");
        databank.setRemovable(true);

        // "file" is NOT part of buildAlterDDL - it has its own confirmed statement (buildSetFileDDL).
        List<String> statements = databank.buildAlterDDL(Set.of("type", "minSize", "removable"));

        Assertions.assertEquals(1, statements.size());
        Assertions.assertEquals(
            "ALTER DATABANK \"MYDB\" SET OPTION LOG, MINSIZE 2000K, REMOVABLE",
            statements.get(0));
    }

    @Test
    public void setFileDDLIsItsOwnStatement() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        Assertions.assertNull(databank.buildSetFileDDL());
        databank.setFile("  moved/mydb.dbf  ");
        Assertions.assertEquals("ALTER DATABANK \"MYDB\" SET FILE 'moved/mydb.dbf'", databank.buildSetFileDDL());
    }

    @Test
    public void alterDDLEmitsOnlyADropStatementWhenASizeIsClearedBackToBlank() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        databank.setMinSize(""); // cleared in the properties grid

        List<String> statements = databank.buildAlterDDL(Set.of("minSize"));

        Assertions.assertEquals(1, statements.size());
        Assertions.assertEquals("ALTER DATABANK \"MYDB\" DROP MINSIZE", statements.get(0));
    }

    @Test
    public void alterDDLSetsFileSizeWhenChanged() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        databank.setFileSize("10M"); // K/M/G units, same as CREATE

        List<String> statements = databank.buildAlterDDL(Set.of("fileSize"));

        Assertions.assertEquals(1, statements.size());
        Assertions.assertEquals("ALTER DATABANK \"MYDB\" SET FILESIZE 10M", statements.get(0));
    }

    @Test
    public void alterDDLDropsFileSizeWhenClearedBackToBlank() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        databank.setFileSize("");

        List<String> statements = databank.buildAlterDDL(Set.of("fileSize"));

        Assertions.assertEquals(1, statements.size());
        Assertions.assertEquals("ALTER DATABANK \"MYDB\" DROP FILESIZE", statements.get(0));
    }

    /**
     * A mixed edit - one size cleared, another set, plus a non-size property changed - needs
     * BOTH statements, since Mimer SQL's grammar allows only one action (SET or DROP) per
     * {@code ALTER DATABANK}.
     */
    @Test
    public void alterDDLEmitsBothSetAndDropStatementsForAMixedEdit() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        databank.setGoalSize("10M");
        databank.setMaxSize(""); // cleared
        databank.setRemovable(false); // explicit DROP REMOVABLE

        List<String> statements = databank.buildAlterDDL(Set.of("goalSize", "maxSize", "removable"));

        Assertions.assertEquals(2, statements.size());
        Assertions.assertEquals("ALTER DATABANK \"MYDB\" SET GOALSIZE 10M", statements.get(0));
        Assertions.assertEquals("ALTER DATABANK \"MYDB\" DROP MAXSIZE, REMOVABLE", statements.get(1));
    }

    @Test
    public void alterDDLIgnoresAnUnchangedPropertyEvenIfItHasAValue() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        databank.setFile("mydb.dbf"); // set, but not in changedProperties below - must not appear

        List<String> statements = databank.buildAlterDDL(Set.of("type"));

        Assertions.assertEquals(1, statements.size());
        Assertions.assertFalse(statements.get(0).contains("FILE"),
            () -> "file wasn't marked changed, shouldn't appear: " + statements);
    }

    @Test
    public void isSingleFileReflectsTheLoadedFileCount() {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        // The new-databank constructor never sets fileCount explicitly, so it defaults to 1.
        Assertions.assertTrue(databank.isSingleFile());
    }

    @Test
    public void isSystemDatabankRecognisesTheFourBuiltIns() {
        for (String name : new String[]{"SQLDB", "TRANSDB", "LOGDB", "SYSDB", "sqldb"}) {
            Assertions.assertTrue(new MimerDatabank(dataSource, name).isSystemDatabank(), name);
        }
        Assertions.assertFalse(new MimerDatabank(dataSource, "MYDB").isSystemDatabank());
        Assertions.assertFalse(new MimerDatabank(dataSource, "SQLDB_2").isSystemDatabank());
    }

    @Test
    public void fileNameIsNotEditableForASystemDatabank() {
        Assertions.assertTrue(new MimerDatabank(dataSource, "MYDB").isFileNameEditable());
        Assertions.assertFalse(new MimerDatabank(dataSource, "SYSDB").isFileNameEditable());
    }

    @Test
    public void systemDatabankGetsTheLockedTreeIcon() {
        Assertions.assertEquals(
            org.jkiss.dbeaver.model.DBIcon.TREE_LOCKED,
            new MimerDatabank(dataSource, "SYSDB").getObjectImage());
        // A user databank returns null → the tree's default #tablespace icon.
        Assertions.assertNull(new MimerDatabank(dataSource, "MYDB").getObjectImage());
    }
}
