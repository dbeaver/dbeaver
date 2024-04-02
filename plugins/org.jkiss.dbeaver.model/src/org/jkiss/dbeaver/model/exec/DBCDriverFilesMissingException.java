/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.messages.ModelMessages;

@SuppressWarnings("serial")
public class DBCDriverFilesMissingException extends DBCException {

    private DBPDriver driver;

    public DBCDriverFilesMissingException(@NotNull DBPDriver driver) {
        super(ModelMessages.initialization_driver_error_details);
        this.driver = driver;
    }

    public DBPDriver getDriver() {
        return driver;
    }

    public String getErrorMessage() {
        return NLS.bind(ModelMessages.initialization_driver_error_msg,
                driver.getFullName(),
                driver.getDriverClassName());
    }
}
