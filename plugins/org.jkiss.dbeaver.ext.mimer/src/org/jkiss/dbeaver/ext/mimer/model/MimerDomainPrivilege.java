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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

/**
 * One {@code USAGE} privilege grant on a domain, read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerDomain.PrivilegeCache}) - see {@link
 * AbstractMimerObjectPrivilege} for the shape this and every other single-privilege-type grant
 * class share.
 *
 * @author Mimer Information Technology
 */
public class MimerDomainPrivilege extends AbstractMimerObjectPrivilege<MimerDomain> {

    public MimerDomainPrivilege(@NotNull MimerDomain domain, @NotNull JDBCResultSet dbResult) {
        super(domain, dbResult);
    }

    public MimerDomainPrivilege(@NotNull MimerDomain domain, @NotNull String grantee) {
        super(domain, grantee);
    }

    @NotNull
    public MimerDomain getDomain() {
        return owner;
    }

    @NotNull
    @Override
    protected String getPrivilegeType() {
        return "USAGE";
    }

    @NotNull
    @Override
    protected String getObjectTypeKeyword() {
        return "DOMAIN";
    }

    @NotNull
    @Override
    protected String buildQualifiedOwnerName() {
        return "\"" + owner.getSchema().getName().replace("\"", "\"\"")
            + "\".\"" + owner.getName().replace("\"", "\"\"") + "\"";
    }
}
