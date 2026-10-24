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
package org.jkiss.dbeaver.model.sql.semantics.completion;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.completion.CompletionProposalBase;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class SQLCompletionProposalComparatorTest extends DBeaverUnitTest {

    @NotNull
    private static CompletionProposalBase proposal(@NotNull String name, int score, int kind, double usage) {
        CompletionProposalBase proposal = Mockito.mock(CompletionProposalBase.class);
        Mockito.when(proposal.getDisplayString()).thenReturn(name);
        Mockito.when(proposal.getProposalScore()).thenReturn(score);
        Mockito.when(proposal.getProposalTypeSorterScore()).thenReturn(kind);
        Mockito.when(proposal.getProposalUsageScore()).thenReturn(usage);
        return proposal;
    }

    @NotNull
    private static List<String> sort(@NotNull CompletionProposalBase... proposals) {
        List<CompletionProposalBase> list = new ArrayList<>(List.of(proposals));
        list.sort(new SQLCompletionProposalComparator(true, true));
        return list.stream().map(CompletionProposalBase::getDisplayString).toList();
    }

    @Test
    public void usedProposalsComeFirstWhenNothingTyped() {
        // A used schema (kind sorted after tables) comes before used tables, unused ones keep kind and name order
        Assertions.assertEquals(
            List.of("SCH", "SCH.T2", "SCH.T1", "A_TABLE", "OTHER"),
            sort(
                proposal("OTHER", Integer.MAX_VALUE, 250, 0),
                proposal("SCH.T1", Integer.MAX_VALUE, 200, 1),
                proposal("A_TABLE", Integer.MAX_VALUE, 200, 0),
                proposal("SCH", Integer.MAX_VALUE, 250, 5),
                proposal("SCH.T2", Integer.MAX_VALUE, 200, 2)
            )
        );
    }

    @Test
    public void matchScoreComesBeforeUsage() {
        Assertions.assertEquals(
            List.of("T_BEST", "T_USED", "T_OTHER"),
            sort(
                proposal("T_USED", 5, 200, 3),
                proposal("T_OTHER", 5, 200, 0),
                proposal("T_BEST", 9, 200, 0)
            )
        );
    }
}
