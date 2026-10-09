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

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * {@link AbstractMimerObjectPrivilege#buildGrantDDL()}/{@link
 * AbstractMimerObjectPrivilege#buildRevokeDDL()} via two representative subclasses - {@link
 * MimerSequencePrivilege} (schema-qualified owner name) and {@link MimerProgramPrivilege} (a
 * program has no schema, so its owner name is bare) - covering the one real difference between
 * subclasses of this shared base. Also covers the {@code WITH GRANT OPTION} branch, since that
 * logic lives entirely in the base class and only needs proving once.
 *
 * @author Mimer Information Technology
 */
public class AbstractMimerObjectPrivilegeTest extends DBeaverUnitTest {

    @Mock
    private MimerSequence sequence;

    @Mock
    private DBSObject sequenceSchema;

    @Mock
    private MimerProgram program;

    @Test
    public void schemaQualifiedOwnerGrantAndRevokeDDL() {
        when(sequence.getName()).thenReturn("my_seq");
        when(sequence.getParentObject()).thenReturn(sequenceSchema);
        when(sequenceSchema.getName()).thenReturn("my_schema");

        MimerSequencePrivilege privilege = new MimerSequencePrivilege(sequence, "my_ident");

        Assertions.assertEquals(
            "GRANT USAGE ON SEQUENCE \"my_schema\".\"my_seq\" TO \"my_ident\"",
            privilege.buildGrantDDL());
        Assertions.assertEquals(
            "REVOKE USAGE ON SEQUENCE \"my_schema\".\"my_seq\" FROM \"my_ident\"",
            privilege.buildRevokeDDL());
    }

    @Test
    public void withGrantOptionAppendsClauseOnlyToGrant() {
        when(sequence.getName()).thenReturn("my_seq");
        when(sequence.getParentObject()).thenReturn(sequenceSchema);
        when(sequenceSchema.getName()).thenReturn("my_schema");

        MimerSequencePrivilege privilege = new MimerSequencePrivilege(sequence, "my_ident");
        privilege.setGrantable(true);

        Assertions.assertTrue(privilege.buildGrantDDL().endsWith("WITH GRANT OPTION"), privilege.buildGrantDDL());
        Assertions.assertFalse(privilege.buildRevokeDDL().contains("WITH GRANT OPTION"), privilege.buildRevokeDDL());
    }

    @Test
    public void bareOwnerNameGrantAndRevokeDDL() {
        when(program.getName()).thenReturn("my_prog");

        MimerProgramPrivilege privilege = new MimerProgramPrivilege(program, "my_ident");

        Assertions.assertEquals(
            "GRANT EXECUTE ON PROGRAM \"my_prog\" TO \"my_ident\"",
            privilege.buildGrantDDL());
        Assertions.assertEquals(
            "REVOKE EXECUTE ON PROGRAM \"my_prog\" FROM \"my_ident\"",
            privilege.buildRevokeDDL());
    }
}
