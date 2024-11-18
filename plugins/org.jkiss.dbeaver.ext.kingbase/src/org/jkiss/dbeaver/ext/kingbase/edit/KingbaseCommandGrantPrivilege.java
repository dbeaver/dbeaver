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
package org.jkiss.dbeaver.ext.kingbase.edit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDefaultPrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseObjectPrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilegeGrant;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilegeOwner;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilegeType;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseProcedure;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseProcedureKind;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseRole;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseRolePrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableColumn;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
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

/**
 * Grant/Revoke privilege command
 */
public class KingbaseCommandGrantPrivilege extends DBECommandAbstract<KingbasePrivilegeOwner> {
    private final boolean grant;
    private final KingbasePrivilege privilege;
    private final Set<KingbasePrivilegeType> privilegeTypes;
    private final DBSObject privilegeOwner;

    public KingbaseCommandGrantPrivilege(@NotNull KingbasePrivilegeOwner user, boolean grant, @NotNull DBSObject privilegeOwner, @NotNull KingbasePrivilege privilege, @Nullable KingbasePrivilegeType[] privilegeTypes) {
        super(user, grant ? "Grant" : "Revoke");
        this.grant = grant;
        this.privilege = privilege;
        this.privilegeTypes = new HashSet<>();
        this.privilegeOwner = privilegeOwner;

        if (privilegeTypes != null) {
            this.privilegeTypes.addAll(Arrays.asList(privilegeTypes));
        } else {
            // Expand KingbasePrivilegeType.ALL to simplify command merging later
            for (KingbasePrivilegeType type : getObject().getDataSource().getSupportedPrivilegeTypes()) {
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
            privName.add(KingbasePrivilegeType.ALL.name());
        } else {
            for (KingbasePrivilegeType pn : privilegeTypes) {
                privName.add(pn.name());
                withGrantOption |= CommonUtils.isBitSet(privilege.getPermission(pn), KingbasePrivilege.WITH_GRANT_OPTION);
            }
        }

        KingbasePrivilegeOwner object = getObject();
        String objectName = "", roleName;
        String roleType = null;
        if (object instanceof KingbaseRole role) {
            roleName = DBUtils.getQuotedIdentifier(object);
            if (privilegeOwner instanceof KingbaseProcedure) {
                objectName = ((KingbaseProcedure) privilegeOwner).getFullQualifiedSignature();
            } else if (privilege instanceof KingbaseRolePrivilege) {
                objectName = ((KingbaseRolePrivilege) privilege).getFullObjectName();
            }
            roleType = role.getSpecificRoleType();
        } else {
            KingbaseObjectPrivilege permission = (KingbaseObjectPrivilege) this.privilege;
            if (permission.getGrantee() != null) {
                roleName = DBUtils.getQuotedIdentifier(object.getDataSource(), permission.getGrantee().getRoleName());
                roleType = permission.getGrantee().getRoleType();
            } else {
                roleName = "";
            }
            objectName = KingbaseUtils.getObjectUniqueName(object, options);
        }

        String objectType;
        if (privilege instanceof KingbaseRolePrivilege) {
            if (privilegeOwner instanceof KingbaseProcedure) {
                if (((KingbaseProcedure) privilegeOwner).getKind() == KingbaseProcedureKind.p) {
                    ((KingbaseRolePrivilege) privilege).setKind(KingbasePrivilegeGrant.Kind.PROCEDURE);
                }
            }
            objectType = ((KingbaseRolePrivilege) privilege).getKind().name();
        } else {
            objectType = KingbaseUtils.getObjectTypeName(object);
        }

        String grantedCols = "", grantedTypedObject;
        if (object instanceof KingbaseTableColumn) {
            grantedCols = "(" + DBUtils.getQuotedIdentifier(object) + ")";
            grantedTypedObject = ((KingbaseTableColumn) object).getTable().getFullyQualifiedName(DBPEvaluationContext.DDL);
        } else if (privilege instanceof KingbaseDefaultPrivilege) {
            KingbasePrivilegeGrant.Kind underKind = ((KingbaseDefaultPrivilege) privilege).getUnderKind();
            if (underKind == KingbasePrivilegeGrant.Kind.TYPE) {
                grantedTypedObject = "TYPES";
            } else if (underKind == KingbasePrivilegeGrant.Kind.SEQUENCE) {
                grantedTypedObject = "SEQUENCES";
            } else if (underKind == KingbasePrivilegeGrant.Kind.FUNCTION) {
                grantedTypedObject = "FUNCTIONS";
            } else {
                grantedTypedObject = "TABLES";
            }
        } else {
            grantedTypedObject = objectType + " " + objectName;
        }

        String scriptBeginning = "";
        if (privilege instanceof KingbaseDefaultPrivilege) {
            scriptBeginning = "ALTER DEFAULT PRIVILEGES IN SCHEMA " + DBUtils.getQuotedIdentifier(privilege.getOwner()) + " ";
        }

        String grantScript = scriptBeginning + (grant ? "GRANT " : "REVOKE ") + privName + grantedCols +
            " ON " + grantedTypedObject +
            (grant ? " TO " : " FROM ") + (roleType != null ? roleType.toUpperCase() + " " : "") + roleName;
        if (grant && withGrantOption) {
            grantScript += " WITH GRANT OPTION";
        }
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                grant ? "Grant" : "Revoke",
                grantScript
            )
        };
    }

    @NotNull
    @Override
    public DBECommand<?> merge(@NotNull DBECommand<?> prevCommand, @NotNull Map<Object, Object> userParams) {

        final String grantCommandId = makeUniqueName("grant");
        final String revokeCommandId = makeUniqueName("revoke");
        final String mergedCommandId = makeUniqueName("merged") + "#" + hashCode();

        userParams.putIfAbsent(grant ? grantCommandId : revokeCommandId, this);

        final KingbaseCommandGrantPrivilege grantCommand = (KingbaseCommandGrantPrivilege) userParams.get(grantCommandId);
        final KingbaseCommandGrantPrivilege revokeCommand = (KingbaseCommandGrantPrivilege) userParams.get(revokeCommandId);

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

    private void mergePrivilegeTypes(@NotNull Set<KingbasePrivilegeType> granted, @NotNull Set<KingbasePrivilegeType> revoked, @NotNull Collection<KingbasePrivilegeType> modified, boolean grant) {
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
        for (KingbasePrivilegeType type : getObject().getDataSource().getSupportedPrivilegeTypes()) {
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
