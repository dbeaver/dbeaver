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
package org.jkiss.dbeaver.ext.greenplum;

import org.jkiss.dbeaver.ext.greenplum.edit.GreenplumExternalTableManager2Test;
import org.jkiss.dbeaver.ext.greenplum.edit.GreenplumTableManagerTest;
import org.jkiss.dbeaver.ext.greenplum.model.GreenplumExternalTableTest;
import org.jkiss.dbeaver.ext.greenplum.model.GreenplumExternalTableUriLocationsHandlerTest;
import org.jkiss.dbeaver.ext.greenplum.model.GreenplumFunctionTest;
import org.jkiss.dbeaver.ext.greenplum.model.GreenplumTableTest;
import org.jkiss.dbeaver.ext.greenplum.model.GreenplumWithClauseBuilderTest;
import org.jkiss.dbeaver.ext.greenplum.model.PostgreServerGreenplumTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
    GreenplumTableManagerTest.class,
    GreenplumExternalTableManager2Test.class,
    GreenplumWithClauseBuilderTest.class,
    GreenplumTableTest.class,
    GreenplumExternalTableTest.class,
    GreenplumFunctionTest.class,
    PostgreServerGreenplumTest.class,
    GreenplumExternalTableUriLocationsHandlerTest.class
})
public class GreenplumTestSuite {
}
