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
package org.jkiss.dbeaver.model.sql.completion;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SQLCompletionHelperTest extends DBeaverUnitTest {

    @Test
    public void partitionReferenceClassDoesNotRequireResolution() throws DBException {
        DBSObjectReference reference = mock(DBSObjectReference.class);
        when(reference.isTablePartition()).thenReturn(null);
        doReturn(DBSTablePartition.class).when(reference).getObjectClass();

        Assertions.assertEquals(Boolean.TRUE, SQLCompletionHelper.isTablePartitionReference(reference));
        verify(reference, never()).resolveObject(any());
    }

    @Test
    public void partitionFlagTrueIsUsedWithoutResolution() throws DBException {
        DBSObjectReference reference = mock(DBSObjectReference.class);
        when(reference.isTablePartition()).thenReturn(true);

        Assertions.assertEquals(Boolean.TRUE, SQLCompletionHelper.isTablePartitionReference(reference));
        verify(reference, never()).resolveObject(any());
    }

    @Test
    public void partitionFlagFalseIsUsedWithoutResolution() throws DBException {
        DBSObjectReference reference = mock(DBSObjectReference.class);
        when(reference.isTablePartition()).thenReturn(false);

        Assertions.assertEquals(Boolean.FALSE, SQLCompletionHelper.isTablePartitionReference(reference));
        verify(reference, never()).resolveObject(any());
    }

    @Test
    public void tableReferenceWithoutFlagIsUnknownAndNotResolved() throws DBException {
        DBSObjectReference reference = mock(DBSObjectReference.class);
        when(reference.isTablePartition()).thenReturn(null);
        doReturn(DBSTable.class).when(reference).getObjectClass();

        Assertions.assertNull(SQLCompletionHelper.isTablePartitionReference(reference));
        verify(reference, never()).resolveObject(any());
    }

    @Test
    public void viewReferenceWithoutFlagIsUnknownAndNotResolved() throws DBException {
        DBSObjectReference reference = mock(DBSObjectReference.class);
        when(reference.isTablePartition()).thenReturn(null);
        doReturn(DBSView.class).when(reference).getObjectClass();

        Assertions.assertNull(SQLCompletionHelper.isTablePartitionReference(reference));
        verify(reference, never()).resolveObject(any());
    }

    @Test
    public void nonTableReferenceIsUnknownAndNotResolved() throws DBException {
        DBSObjectReference reference = mock(DBSObjectReference.class);
        when(reference.isTablePartition()).thenReturn(null);
        doReturn(String.class).when(reference).getObjectClass();

        Assertions.assertNull(SQLCompletionHelper.isTablePartitionReference(reference));
        verify(reference, never()).resolveObject(any());
    }
}
