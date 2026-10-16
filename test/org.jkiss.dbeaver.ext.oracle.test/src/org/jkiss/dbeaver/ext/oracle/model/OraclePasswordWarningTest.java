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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLWarning;

public class OraclePasswordWarningTest extends DBeaverUnitTest {

    @ParameterizedTest
    @ValueSource(ints = {
        OracleConstants.EC_PASSWORD_WILL_EXPIRE,
        OracleConstants.EC_PASSWORD_GRACE_PERIOD
    })
    public void recognizesPasswordGracePeriodWarnings(int errorCode) {
        Assertions.assertTrue(OracleDataSource.isPasswordWillExpireWarning(
            new SQLWarning("Password is in its grace period", "72000", errorCode)
        ));
    }

    @ParameterizedTest
    @ValueSource(ints = {
        OracleConstants.EC_PASSWORD_EXPIRED,
        OracleConstants.EC_INVALID_USERNAME_PASSWORD,
        0
    })
    public void doesNotTreatOtherErrorsAsGracePeriodWarnings(int errorCode) {
        Assertions.assertFalse(OracleDataSource.isPasswordWillExpireWarning(
            new SQLWarning("Not a grace period warning", "72000", errorCode)
        ));
    }
}
