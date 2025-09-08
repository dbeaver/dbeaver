/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.meta.Property;

public final class DBPConnectionInformation {
    @NotNull
    private final String url;
    @NotNull
    private final String driverName;
    @NotNull
    private final String productName;
    @NotNull
    private final String productVersion;

    public DBPConnectionInformation(
        @NotNull String url,
        @NotNull String driverName,
        @NotNull String productName,
        @NotNull String productVersion
    ) {
        this.url = url;
        this.driverName = driverName;
        this.productName = productName;
        this.productVersion = productVersion;
    }

    @NotNull
    @Property(order = 1)
    public String getUrl() {
        return url;
    }

    @NotNull
    @Property(order = 2)
    public String getDriverName() {
        return driverName;
    }

    @NotNull
    @Property(order = 3)
    public String getProductName() {
        return productName;
    }

    @NotNull
    @Property(order = 4)
    public String getProductVersion() {
        return productVersion;
    }

}
