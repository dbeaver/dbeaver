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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Grant/Revoke privilege command
 */
public class PostgreCommandGrantPrivilege extends DBECommandAbstract<PostgrePrivilegeOwner> {
    private final boolean grant;
    private final PostgrePrivilege privilege;
    private final Set<PostgrePrivilegeType> privilegeTypes;
    private final DBSObject privilegeOwner;

    public PostgreCommandGrantPrivilege(@NotNull PostgrePrivilegeOwner user, boolean grant, @NotNull DBSObject privilegeOwner, @NotNull PostgrePrivilege privilege, @Nullable PostgrePrivilegeType[] privilegeTypes) {
        super(user, grant ? "Grant" : "Revoke");
        this.grant = grant;
        this.privilege = privilege;
        this.privilegeTypes = new HashSet<>();
        this.privilegeOwner = privilegeOwner;

        if (privilegeTypes != null) {
            this.privilegeTypes.addAll(Arrays.asList(privilegeTypes));
        } else {
            // Expand PostgrePrivilegeType.ALL to simplify command merging later
            for (PostgrePrivilegeType type : getObject().getDataSource().getSupportedPrivilegeTypes()) {
                if (type.supportsType(privilegeOwner.getClass())) {
                    this.privilegeTypes.add(type);
                }
            }
        }
    }

    @NotNull
    @Override
    public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull Map<String, Object> options) {
        if (privilegeTypes.isEmpty()) {
            return new DBEPersistAction[0];
        }

        boolean withGrantOption = false;
        final StringJoiner privName = new StringJoiner(", ");

        if (hasAllPrivilegeTypes()) {
            privName.add(PostgrePrivilegeType.ALL.name());
        } else {
            for (PostgrePrivilegeType pn : privilegeTypes) {
                privName.add(pn.name());
                withGrantOption |= CommonUtils.isBitSet(privilege.getPermission(pn), PostgrePrivilege.WITH_GRANT_OPTION);
            }
        }

        PostgrePrivilegeOwner object = getObject();
        String objectName = "", roleName;
        String roleType = null;
        if (object instanceof PostgreRole role) {
            roleName = DBUtils.getQuotedIdentifier(object);
            if (privilegeOwner instanceof PostgreProcedure) {
                objectName = ((PostgreProcedure) privilegeOwner).getFullQualifiedSignature();
            } else if (privilege instanceof PostgreRolePrivilege) {
                objectName = ((PostgreRolePrivilege) privilege).getFullObjectName();
            }
            roleType = role.getSpecificRoleType();
        } else {
            PostgreObjectPrivilege permission = (PostgreObjectPrivilege) this.privilege;
            if (permission.getGrantee() != null) {
                roleName = DBUtils.getQuotedIdentifier(object.getDataSource(), permission.getGrantee().getRoleName());
                roleType = permission.getGrantee().getRoleType();
            } else {
                roleName = "";
            }
            objectName = PostgreUtils.getObjectUniqueName(object, options);
        }

        String objectType;
        if (privilege instanceof PostgreRolePrivilege) {
            if (privilegeOwner instanceof PostgreProcedure) {
                if (((PostgreProcedure) privilegeOwner).getKind() == PostgreProcedureKind.p) {
                    ((PostgreRolePrivilege) privilege).setKind(PostgrePrivilegeGrant.Kind.PROCEDURE);
                }
            }
            objectType = ((PostgreRolePrivilege) privilege).getKind().name();
        } else {
            objectType = PostgreUtils.getObjectTypeName(object);
        }

        String grantedCols = "", grantedTypedObject;
        if (object instanceof PostgreTableColumn) {
            grantedCols = "(" + DBUtils.getQuotedIdentifier(object) + ")";
            grantedTypedObject = ((PostgreTableColumn) object).getTable().getFullyQualifiedName(DBPEvaluationContext.DDL);
        } else if (privilege instanceof PostgreDefaultPrivilege) {
            PostgrePrivilegeGrant.Kind underKind = ((PostgreDefaultPrivilege) privilege).getUnderKind();
            if (underKind == PostgrePrivilegeGrant.Kind.TYPE) {
                grantedTypedObject = "TYPES";
            } else if (underKind == PostgrePrivilegeGrant.Kind.SEQUENCE) {
                grantedTypedObject = "SEQUENCES";
            } else if (underKind == PostgrePrivilegeGrant.Kind.FUNCTION) {
                grantedTypedObject = "FUNCTIONS";
            } else {
                grantedTypedObject = "TABLES";
            }
        } else {
            grantedTypedObject = objectType + " " + objectName;
        }

        StringBuilder ddl = new StringBuilder();
        if (privilege instanceof PostgreDefaultPrivilege dp) {
            ddl.append("ALTER DEFAULT PRIVILEGES");
            if (dp.getGrantor() != null) {
                ddl.append(" FOR ROLE ").append(DBUtils.getQuotedIdentifier(dp.getDataSource(), dp.getGrantor().getRoleName()));
            }
            ddl.append(" IN SCHEMA ").append(DBUtils.getQuotedIdentifier(privilege.getOwner())).append(" ");
        }
        ddl.append(grant ? "GRANT " : "REVOKE ").append(privName).append(grantedCols).append(" ON ").append(grantedTypedObject);
        ddl.append(grant ? " TO" : " FROM");
        if (roleType != null) {
            ddl.append(" ").append(roleType.toUpperCase());
        }
        ddl.append(" ").append(roleName);
        if (grant && withGrantOption) {
            ddl.append(" WITH GRANT OPTION");
        }
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                grant ? "Grant" : "Revoke",
                ddl.toString()
            )
        };
    }

    @NotNull
    @Override
    public DBECommand<?> merge(@NotNull DBECommand<?> prevCommand, @NotNull Map<Object, Object> userParams) {
        // In order to properly merge grant/revoke commands, we need to capture
        // the first one which grants and one which revokes and merge privileges
        // from other commands into them. Other commands are consumed later in process.

        final String grantCommandId = makeUniqueName("grant");
        final String revokeCommandId = makeUniqueName("revoke");
        final String mergedCommandId = makeUniqueName("merged") + "#" + hashCode();

        userParams.putIfAbsent(grant ? grantCommandId : revokeCommandId, this);

        final PostgreCommandGrantPrivilege grantCommand = (PostgreCommandGrantPrivilege) userParams.get(grantCommandId);
        final PostgreCommandGrantPrivilege revokeCommand = (PostgreCommandGrantPrivilege) userParams.get(revokeCommandId);

        if (!userParams.containsKey(mergedCommandId)) {
            userParams.put(mergedCommandId, true);

            mergePrivilegeTypes(
                grantCommand != null ? grantCommand.privilegeTypes : Collections.emptySet(),
                revokeCommand != null ? revokeCommand.privilegeTypes : Collections.emptySet(),
                new ArrayList<>(privilegeTypes),
                grant
            );
        }

        return grant ? grantCommand : revokeCommand;
    }

    private void mergePrivilegeTypes(@NotNull Set<PostgrePrivilegeType> granted, @NotNull Set<PostgrePrivilegeType> revoked, @NotNull Collection<PostgrePrivilegeType> modified, boolean grant) {
        if (grant) {
            granted.removeAll(modified);
            modified.removeIf(revoked::remove);
            granted.addAll(modified);
        } else {
            revoked.removeAll(modified);
            modified.removeIf(granted::remove);
            revoked.addAll(modified);
        }
    }

    private boolean hasAllPrivilegeTypes() {
        Class<? extends DBSObject> ownerClass = null;
        if (privilegeOwner instanceof DBNDatabaseFolder) {
            ownerClass = ((DBNDatabaseFolder) privilegeOwner).getChildrenClass();
        }
        if (ownerClass == null) {
            ownerClass = privilegeOwner.getClass();
        }
        for (PostgrePrivilegeType type : getObject().getDataSource().getSupportedPrivilegeTypes()) {
            if (type.supportsType(ownerClass) && !privilegeTypes.contains(type)) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    private String makeUniqueName(@NotNull String name) {
        return name + "#" + privilege.hashCode() + "#" + privilegeOwner.hashCode();
    }
}
