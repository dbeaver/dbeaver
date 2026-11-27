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
package org.jkiss.dbeaver.model.exec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DBCDDLTransactionBehaviorTest {
    @Test
    void resolvesUnsupportedTransactionsAsImmediate() {
        Assertions.assertEquals(
            DBCDDLTransactionBehavior.IMMEDIATE,
            DBCDDLTransactionBehavior.resolve(false, false, false, false, false)
        );
    }

    @Test
    void resolvesTransactionalDDL() {
        Assertions.assertEquals(
            DBCDDLTransactionBehavior.TRANSACTIONAL,
            DBCDDLTransactionBehavior.resolve(true, true, false, false, false)
        );
    }

    @Test
    void resolvesImplicitCommitDDL() {
        Assertions.assertEquals(
            DBCDDLTransactionBehavior.IMMEDIATE,
            DBCDDLTransactionBehavior.resolve(true, false, true, true, false)
        );
    }

    @Test
    void resolvesIgnoredDDL() {
        Assertions.assertEquals(
            DBCDDLTransactionBehavior.IGNORED,
            DBCDDLTransactionBehavior.resolve(true, false, true, false, true)
        );
    }

    @Test
    void resolvesUnknownAndContradictoryMetadataConservatively() {
        Assertions.assertEquals(
            DBCDDLTransactionBehavior.TRANSACTIONAL,
            DBCDDLTransactionBehavior.resolve(null, null, null, null, null)
        );
        Assertions.assertEquals(
            DBCDDLTransactionBehavior.TRANSACTIONAL,
            DBCDDLTransactionBehavior.resolve(true, true, false, true, false)
        );
        Assertions.assertEquals(
            DBCDDLTransactionBehavior.TRANSACTIONAL,
            DBCDDLTransactionBehavior.resolve(true, false, true, null, null)
        );
    }
}
