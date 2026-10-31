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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PostgreProcedureSourceTest extends DBeaverUnitTest {
    @Test
    public void preservesDefinitionAndRoutineAttributes() {
        String prefix = "CREATE OR REPLACE FUNCTION public.f() RETURNS integer\n"
            + "LANGUAGE plpgsql IMMUTABLE STRICT SECURITY DEFINER PARALLEL SAFE\n"
            + "SET search_path TO public, pg_temp COST 20 AS ";
        String body = "\nBEGIN\n    RETURN 1;\nEND;\n";
        String suffix = ";\nCOMMENT ON FUNCTION public.f() IS 'example';\n"
            + "GRANT EXECUTE ON FUNCTION public.f() TO test_role;\n";
        String ddl = prefix + "$function$" + body + "$function$" + suffix;
        PostgreProcedureSource source = PostgreProcedureSource.parse(ddl);
        assertNotNull(source);
        assertEquals(body, source.getBody());
        assertEquals(ddl, source.getDefinition());
        assertEquals(ddl, source.withBody(body));
        assertEquals(prefix + "$function$\nBEGIN RETURN 2; END;\n$function$" + suffix,
            source.withBody("\nBEGIN RETURN 2; END;\n"));
    }

    @Test
    public void supportsProceduresAndUnnamedQuotes() {
        String ddl = "CREATE PROCEDURE p() LANGUAGE plpgsql AS $$BEGIN NULL; END;$$;";
        PostgreProcedureSource source = PostgreProcedureSource.parse(ddl);
        assertNotNull(source);
        assertEquals("BEGIN NULL; END;", source.getBody());
        assertEquals("CREATE PROCEDURE p() LANGUAGE plpgsql AS $$$$;", source.withBody(""));
    }

    @Test
    public void avoidsDelimiterCollisions() {
        PostgreProcedureSource source = PostgreProcedureSource.parse("CREATE FUNCTION f() RETURNS text AS $fn$old$fn$ LANGUAGE sql;");
        assertNotNull(source);
        String body = "SELECT $fn$inner$fn$, $fn_$also inner$fn_$";
        String edited = source.withBody(body);
        assertEquals("CREATE FUNCTION f() RETURNS text AS $fn__$" + body + "$fn__$ LANGUAGE sql;", edited);
        assertEquals(body, PostgreProcedureSource.parse(edited).getBody());
    }

    @Test
    public void avoidsDelimiterCollisionsAtTheEndOfTheBody() {
        PostgreProcedureSource source = PostgreProcedureSource.parse("CREATE FUNCTION f() RETURNS text AS $$old$$ LANGUAGE sql;");
        assertNotNull(source);
        String body = "SELECT '$' -- trailing $";
        String edited = source.withBody(body);
        assertEquals("CREATE FUNCTION f() RETURNS text AS $_$" + body + "$_$ LANGUAGE sql;", edited);
        assertEquals(body, PostgreProcedureSource.parse(edited).getBody());
    }

    @Test
    public void ignoresQuotedNamesAndDefaultValues() {
        String ddl = "CREATE FUNCTION \"AS $fake$wrong$fake$\"(p text DEFAULT $default$AS $$wrong$$$default$, "
            + "q text DEFAULT 'AS $$also wrong$$') RETURNS text AS $body$right$body$ LANGUAGE sql;";
        assertEquals("right", PostgreProcedureSource.parse(ddl).getBody());
    }

    @Test
    public void ignoresEscapedStringDefaults() {
        String ddl = "CREATE FUNCTION f(p text DEFAULT E'escaped\\' AS $$wrong$$') RETURNS text AS $$right$$ LANGUAGE sql;";
        assertEquals("right", PostgreProcedureSource.parse(ddl).getBody());
    }

    @Test
    public void ignoresCommentsIncludingNestedComments() {
        String ddl = "-- AS $$wrong$$\r\nCREATE FUNCTION f() RETURNS text\n"
            + "/* outer /* nested */ AS $$wrong$$ */ AS /* body follows */ $body$right$body$ LANGUAGE sql;";
        assertEquals("right", PostgreProcedureSource.parse(ddl).getBody());
    }

    @Test
    public void supportsCaseWhitespaceAndWindowsLineEndings() {
        String ddl = "create function f() returns text as\r\n$body$\r\nselect 'value';\r\n$body$ language sql;";
        PostgreProcedureSource source = PostgreProcedureSource.parse(ddl);
        assertNotNull(source);
        assertEquals("\r\nselect 'value';\r\n", source.getBody());
        assertEquals(ddl, source.withBody(source.getBody()));
    }

    @Test
    public void retainsEditsAcrossRepeatedConversions() {
        PostgreProcedureSource first = PostgreProcedureSource.parse("CREATE FUNCTION f() RETURNS integer AS $$SELECT 1$$ LANGUAGE sql;");
        PostgreProcedureSource second = PostgreProcedureSource.parse(first.withBody("SELECT 2"));
        assertEquals("SELECT 2", second.getBody());
        PostgreProcedureSource third = PostgreProcedureSource.parse(second.getDefinition().replace("f()", "renamed()"));
        assertEquals("CREATE FUNCTION renamed() RETURNS integer AS $$SELECT 3$$ LANGUAGE sql;", third.withBody("SELECT 3"));
    }

    @Test
    public void leavesUnsupportedOrIncompleteDefinitionsIntact() {
        assertNull(PostgreProcedureSource.parse("CREATE AGGREGATE f(integer) (SFUNC = sum, STYPE = integer);"));
        assertNull(PostgreProcedureSource.parse("CREATE FUNCTION f() RETURNS int LANGUAGE sql RETURN 1;"));
        assertNull(PostgreProcedureSource.parse("CREATE FUNCTION f() RETURNS int AS 'symbol' LANGUAGE internal;"));
        assertNull(PostgreProcedureSource.parse("CREATE FUNCTION f() RETURNS int AS $body$unfinished"));
        assertNull(PostgreProcedureSource.parse("CREATE FUNCTION f() RETURNS int /* unfinished AS $$wrong$$"));
    }
}
