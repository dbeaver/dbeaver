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
package org.jkiss.dbeaver.model.datadam.rpc;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class DBDamNameServiceTest {
    @Test
    void registrationNameIncludesTheLocalComputer() {
        Assertions.assertEquals("DBeaver Test: workstation", DBDamNameService.formatName("DBeaver Test", "workstation"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void registrationNameFallsBackToProductWithoutAComputer(@Nullable String computer) {
        Assertions.assertEquals("DBeaver Test", DBDamNameService.formatName("DBeaver Test", computer));
    }

    @SuppressWarnings("deprecation")
    @Test
    void compatibilityLookupDoesNotInitializeOrRenameAfterRegistration() throws Exception {
        var account = Mockito.mock(DDAccountClient.class);
        Assertions.assertNull(DBDamNameService.getOrInitializeName(account, "client"));
        Mockito.verify(account).getDam("client");
        Mockito.verifyNoMoreInteractions(account);
    }
}
