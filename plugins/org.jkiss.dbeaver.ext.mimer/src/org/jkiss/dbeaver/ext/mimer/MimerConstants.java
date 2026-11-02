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
package org.jkiss.dbeaver.ext.mimer;

import org.jkiss.code.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Mimer SQL constants.
 *
 * @author Mimer Information Technology
 */
public class MimerConstants {

    public static final String DRIVER_CLASS = "com.mimer.jdbc.Driver";

    // System databanks (EXT_DATABANKS) - always present, not user storage targets.
    // Excluded from any "choose a databank" picker (e.g. sequence/table creation).
    public static final Set<String> SYSTEM_DATABANKS = Set.of("LOGDB", "TRANSDB", "SQLDB", "SYSDB");

    // Mimer SQL's own built-in schemas. The driver's getTables() reports every object in these as
    // TABLE_TYPE "SYSTEM TABLE", losing the real VIEW/TABLE distinction, so
    // MimerMetaModel.prepareTableLoadStatement reads the TABLES catalog view directly for these.
    public static final Set<String> SYSTEM_SCHEMAS = Set.of("INFORMATION_SCHEMA", "SYSTEM", "MIMER", "ODBC", "BUILTIN");

    // The built-in group every ident already belongs to - Mimer SQL rejects an explicit
    // GRANT/REVOKE MEMBER ON GROUP PUBLIC, so it's excluded from any "grant membership" picker.
    public static final String GROUP_PUBLIC = "PUBLIC";

    // INFORMATION_SCHEMA.EXT_SOURCE_DEFINITION object type values - domains are not among
    // these, see MimerUtils#buildDomainSource.
    public static final String SOURCE_TYPE_VIEW = "VIEW";
    public static final String SOURCE_TYPE_TRIGGER = "TRIGGER";
    public static final String SOURCE_TYPE_MODULE = "MODULE";
    public static final String SOURCE_TYPE_PROCEDURE = "PROCEDURE";
    public static final String SOURCE_TYPE_FUNCTION = "FUNCTION";

    public static final String SOURCE_NOT_AVAILABLE = "-- Source code not available";

    // The four BUILTIN.GIS_* types: their catalog DATA_TYPE code has no java.sql.Types match,
    // so it resolves to DBPDataKind.UNKNOWN unless MimerDataSource#resolveDataKind forces it
    // to BINARY instead - their result-set typeName is plain "BINARY(n)", which the Data grid
    // already renders correctly once the catalog-side kind is fixed.
    public static final Set<String> TYPE_NAMES_UNKNOWN_CATALOG_KIND = Set.of(
        "BUILTIN.GIS_LOCATION",
        "BUILTIN.GIS_LATITUDE",
        "BUILTIN.GIS_LONGITUDE",
        "BUILTIN.GIS_COORDINATE"
    );

    // Mimer SQL's three LOB types. CREATE TABLE accepts CHARACTER LARGE OBJECT, CHAR LARGE
    // OBJECT, or CLOB (and the NATIONAL equivalents) interchangeably as input - see the Syntax
    // Rules data-type reference in the Mimer SQL Manual, https://docs.mimer.com/MimerSqlManual/
    // latest - but what the catalog actually stores and echoes back for the NATIONAL one is the
    // abbreviated "NATIONAL CHAR LARGE OBJECT", not the spelled-out "NATIONAL CHARACTER LARGE
    // OBJECT" listed first there. The
    // plain (non-national) form isn't known to have the same abbreviation quirk, so it's kept as
    // the spelled-out "CHARACTER LARGE OBJECT" - unconfirmed either way. This set is used for
    // name matching against whatever the catalog actually reports, so it needs to hold the real
    // stored form, not necessarily the form the docs list as canonical.
    // NATIONAL CHARACTER LARGE OBJECT (NCLOB) is also missing entirely from the driver's own
    // getTypeInfo() (so absent from every Data Type picker - see
    // MimerDataTypeCache#addCustomObjects, same "driver doesn't enumerate it" gap as the
    // INTERVAL family) and gets a "?" tree icon for an already-existing column of that type (see
    // MimerDataSource#resolveDataKind - same DBPDataKind.UNKNOWN gap as the BUILTIN.GIS_* types
    // above, just for a real java.sql.Types code the driver apparently doesn't map correctly
    // rather than one with no java.sql.Types match at all). The other two (BLOB, CHARACTER LARGE
    // OBJECT/CLOB) aren't independently confirmed broken, but are included here too since the
    // same driver very plausibly mishandles all three identically - harmless even if one of them
    // turns out to already work fine (MimerDataTypeCache's own duplicate-name handling tolerates
    // a redundant entry).
    public static final Set<String> TYPE_NAMES_LARGE_OBJECT = Set.of(
        "BINARY LARGE OBJECT",
        "CHARACTER LARGE OBJECT",
        "NATIONAL CHAR LARGE OBJECT"
    );

    // The short abbreviations for the three types above (BLOB/CLOB/NCLOB, plus the
    // in-between "CHAR LARGE OBJECT"/"NCHAR LARGE OBJECT" forms the manual also lists) -
    // added as their own picker entries alongside the spelled-out names, same reasoning as
    // TYPE_NAMES_LARGE_OBJECT above: the driver's own getTypeInfo() doesn't report these as
    // distinct catalog types either (only the spelled-out forms are ever actually stored), so
    // without this they're only usable by hand-typing them in, and the framework's generic
    // column-modifier logic doesn't resolve the name correctly on its own:
    // AbstractSQLDialect#getColumnTypeModifiers deliberately skips auto-appending a Length
    // suffix for any CONTENT/BINARY-kind type whose name contains "LOB" (reasonable in general -
    // most databases' CLOB/BLOB don't take one - but wrong for Mimer SQL, where
    // "CHARACTER LARGE OBJECT(n[K|M|G])" genuinely accepts an explicit size). Having a real,
    // exact-cased cache entry for each abbreviation isn't enough on its own to fix that (the
    // suppression is by name-contains-"LOB", not by whether the type is "known") -
    // MimerSQLDialect#getColumnTypeModifiers overrides the behavior for every name in this set
    // (and TYPE_NAMES_LARGE_OBJECT) so a user-set Length is never silently dropped.
    public static final Set<String> TYPE_NAMES_LARGE_OBJECT_ABBREVIATED = Set.of(
        "BLOB",
        "CLOB",
        "NCLOB",
        "CHAR LARGE OBJECT",
        "NCHAR LARGE OBJECT"
    );

    // CHARACTER VARYING/NATIONAL CHARACTER VARYING require an explicit length - unlike CHAR,
    // which defaults to length 1 when omitted, there's no sensible single-character default for
    // a "varying" type. "NATIONAL CHAR VARYING" is included defensively, not confirmed - the
    // abbreviated-catalog-spelling quirk already confirmed for NATIONAL CHAR LARGE OBJECT (see
    // TYPE_NAMES_LARGE_OBJECT's own comment) may or may not apply here too.
    public static final Set<String> TYPE_NAMES_VARYING_CHARACTER_REQUIRES_LENGTH = Set.of(
        "CHARACTER VARYING",
        "NATIONAL CHARACTER VARYING",
        "NATIONAL CHAR VARYING"
    );

    // The length CREATE_TABLE.htm/the create-column dialog used to pre-fill for a new
    // CHARACTER VARYING column - now supplied by MimerSQLDialect#getColumnTypeModifiers itself
    // when the Length field is left blank, rather than pre-filled in the UI (which risked
    // carrying a stale value over if the type was then changed to something else, e.g.
    // INTEGER(100) - past Mimer's real max of 45).
    public static final int DEFAULT_VARYING_CHARACTER_LENGTH = 100;

    // CURRENT_COLLATION_1/2/3 - server-session-level dynamic collation bindings ("SET CURRENT
    // COLLATION n TO ..."), not real catalog rows, so a column/index-column COLLATE clause can
    // reference one without ever being returned by INFORMATION_SCHEMA.COLLATIONS/getCollations().
    // A column referencing e.g. CURRENT_COLLATION_3 changes sort/match behavior whenever that
    // session-level binding changes, with no ALTER on the column itself - the same effect a real
    // collation's own name has, just resolved dynamically instead of fixed at create time. Added
    // to the Collation dropdowns alongside the real, catalog-listed collations - plain literal
    // keywords, not schema-qualified/quoted identifiers like a real collation's name.
    public static final List<String> CURRENT_COLLATION_NAMES = List.of(
        "CURRENT_COLLATION_1",
        "CURRENT_COLLATION_2",
        "CURRENT_COLLATION_3"
    );

    /**
     * True for a character type - plain or national, ordinary or large-object - the only kinds
     * Mimer SQL allows a {@code COLLATE} clause on (a table column's own collation, or an index
     * column's - see {@code MimerTableColumn}/{@code MimerTableIndexColumn}). {@code CHARACTER
     * LARGE OBJECT}/{@code NATIONAL CHAR LARGE OBJECT} both start with a matched prefix already,
     * so no separate LOB exclusion is needed here the way {@code MimerCreateIndexPage}'s own
     * character-type check (used for the unrelated Algorithm column, which LOB columns can't
     * carry at all) needs one - {@code BINARY LARGE OBJECT} is correctly left out, since it
     * matches none of the prefixes below. "NATIONAL CHAR" (not "NATIONAL CHARACTER") is
     * deliberate - see {@link #TYPE_NAMES_LARGE_OBJECT}'s own comment on the catalog's
     * abbreviated spelling.
     */
    public static boolean isCharacterType(@Nullable String typeName) {
        if (typeName == null) {
            return false;
        }
        String upperTypeName = typeName.toUpperCase(Locale.ENGLISH);
        return upperTypeName.startsWith("CHARACTER") || upperTypeName.startsWith("CHAR") || upperTypeName.startsWith("VARCHAR")
            || upperTypeName.startsWith("NATIONAL CHAR") || upperTypeName.startsWith("NCHAR") || upperTypeName.startsWith("NVARCHAR");
    }

    // Connection provider property (see MimerDataSourceProvider#getConnectionURL and
    // .ui's MimerConnectionSettingsPage) - which JDBC connection protocol to use.
    // "tcp" is the default (same as omitting the protocol entirely); "local" uses
    // shared-memory IPC to a Mimer SQL server on the same machine and drops the host from
    // the URL, per the Mimer SQL JDBC Driver Guide's "jdbc:mimer:local://user:pass@/db" form.
    public static final String PROP_PROTOCOL = "protocol";
    public static final String PROTOCOL_TCP = "tcp";
    public static final String PROTOCOL_LOCAL = "local";

    // Real JDBC connection properties (passed straight through to the driver, unlike
    // PROP_PROTOCOL above) backing Mimer SQL's PROGRAM security layer - equivalent to
    // "ENTER <program> USING <password>" in SQL. Set via .ui's MimerConnectionSettingsPage.
    public static final String PROP_PROGRAM = "program";
    public static final String PROP_PROGRAM_PASSWORD = "programPwd";

    // Mimer SQL's INTERVAL types - the driver's own getTypeInfo() doesn't report any of these
    // (there's no single fixed "INTERVAL" type, just a family of qualifiers), so they never show
    // up in the Data Type picker on their own; MimerDataTypeCache adds them as synthetic entries.
    // "(p)"/"(p,s)" are literal placeholder text - picking one fills the type field with the
    // placeholder still in it. Left unreplaced, MimerTableColumn#getTypeName strips it back out
    // at DDL-generation time (a not-yet-persisted column only) rather than sending the literal
    // placeholder text to the server - Mimer SQL applies its own default precision when none is
    // given, same idea as leaving a CHAR column's Length blank.
    public static final List<String> INTERVAL_TYPE_NAMES = List.of(
        "INTERVAL YEAR(p)",
        "INTERVAL MONTH(p)",
        "INTERVAL DAY(p)",
        "INTERVAL HOUR(p)",
        "INTERVAL MINUTE(p)",
        "INTERVAL SECOND(p,s)",
        "INTERVAL YEAR(p) TO MONTH",
        "INTERVAL DAY(p) TO HOUR",
        "INTERVAL DAY(p) TO MINUTE",
        "INTERVAL DAY(p) TO SECOND(s)",
        "INTERVAL HOUR(p) TO MINUTE",
        "INTERVAL HOUR(p) TO SECOND(s)",
        "INTERVAL MINUTE(p) TO SECOND(s)"
    );

    // Mimer SQL's per-index-column CREATE INDEX ... "col" [FOR <algorithm>] [ASC|DESC] clause -
    // an alternate access-path algorithm for full-text/PinYin lookups instead of the ordinary
    // ("SIMPLE") ordered-key structure. A CLUSTERED index can only use SIMPLE columns - see
    // MimerIndexManager/MimerCreateIndexPage's clustered<->algorithm mutual-exclusion handling.
    // PINYIN_WORD_START exists in Mimer SQL's own grammar but the user confirmed it doesn't
    // currently work correctly, so it's deliberately left out of the choices offered here.
    // "SIMPLE" itself is never written to DDL (the FOR clause is simply omitted for it) - it only
    // ever appears as a read-back value from EXT_ACCESS_PATHS.INDEX_ALGORITHM /
    // EXT_INDEX_COLUMN_USAGE.INDEX_ALGORITHM (see MimerAccessPathColumn).
    public static final String INDEX_ALGORITHM_SIMPLE = "SIMPLE";
    public static final String INDEX_ALGORITHM_WORD_SEARCH = "WORD_SEARCH";
    public static final String INDEX_ALGORITHM_PINYIN_START = "PINYIN_START";
    public static final String INDEX_ALGORITHM_PINYIN_START_T9 = "PINYIN_START_T9";

    // {algorithm keyword -> display label}, in display order, for the create-index dialog's
    // per-column "Algorithm" combo (MimerCreateIndexPage). "Simple" (no FOR clause at all) isn't
    // itself a DDL keyword, so it's handled separately by the dialog rather than included here.
    // An index may have AT MOST ONE column with a non-SIMPLE algorithm at all -
    // a second one (even the same algorithm on a different column) is rejected server-side
    // ("... contains multiple type clauses which is not allowed") - MimerCreateIndexPage enforces
    // this by resetting every other column the moment one is picked, MimerIndexConfigurator keeps
    // only the first regardless as a backstop.
    public static final Map<String, String> INDEX_ALGORITHMS_FOR_CLAUSE;

    static {
        Map<String, String> algorithms = new LinkedHashMap<>();
        algorithms.put(INDEX_ALGORITHM_WORD_SEARCH, "Word Search");
        algorithms.put(INDEX_ALGORITHM_PINYIN_START, "PinYin Start");
        algorithms.put(INDEX_ALGORITHM_PINYIN_START_T9, "PinYin Start (T9)");
        INDEX_ALGORITHMS_FOR_CLAUSE = Collections.unmodifiableMap(algorithms);
    }

    // The six values Mimer SQL system privileges can cover (System Privileges create dialog,
    // see MimerSystemPrivilegeManager) - DATABANK is the default selection. Declared here rather than on
    // MimerSystemPrivilegeManager itself (org.jkiss.dbeaver.ext.mimer.edit) so .ui's create
    // dialog can reference it directly - that package is never exported (see the cross-plugin
    // class= reference gotcha elsewhere in this codebase), unlike this one.
    public static final String[] SYSTEM_PRIVILEGE_TYPES = {"BACKUP", "DATABANK", "IDENT", "SCHEMA", "SHADOW", "STATISTICS"};

    // --- "Don't ask again" suppression for the extra CASCADE-drop confirmation ---
    // A global preference "<prefix><typeKey> = true" hides MimerCascadeDropUtil's extra
    // "are you sure - CASCADE is recursive" prompt for that object type. Production connections
    // (DBPConnectionType.isConfirmExecute()) ignore these keys and always prompt. The keys are
    // written by the dialog's "Don't ask again" checkbox and managed on the Mimer SQL preference
    // page (org.jkiss.dbeaver.ext.mimer.ui.views.PrefPageMimer). Declared here (an exported
    // package) rather than on the .edit util so the .ui page can reference it.
    public static final String PREF_DROP_CASCADE_CONFIRM_PREFIX = "mimer.confirm.drop_cascade.";

    // Ordered {typeKey -> display label} for every object type whose DROP can carry CASCADE.
    // typeKey matches MimerCascadeDropUtil.typeKey() (lower-case label, spaces -> '_').
    public static final Map<String, String> DROP_CASCADE_CONFIRM_TYPES;

    static {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("schema", "Schema");
        types.put("table", "Table");
        types.put("view", "View");
        types.put("sequence", "Sequence");
        types.put("procedure", "Procedure");
        types.put("function", "Function");
        types.put("module", "Module");
        types.put("trigger", "Trigger");
        types.put("collation", "Collation");
        types.put("index", "Index");
        types.put("domain", "Domain");
        types.put("databank", "Databank");
        types.put("library", "Library");
        types.put("type", "User-defined type");
        types.put("method", "UDT method");
        types.put("method_specification", "UDT method specification");
        types.put("user", "User");
        types.put("group", "Group");
        types.put("program", "Program");
        DROP_CASCADE_CONFIRM_TYPES = Collections.unmodifiableMap(types);
    }

    // Global preferences for the GIS "View as" support (see the model.gis package) - one switch
    // per behavior, no per-connection/per-column override. All default to true except
    // PREF_GIS_COORDINATE_AUTO_MAP (see MimerGisUtils#registerGisDefaults for why). Every
    // reader/writer of these keys MUST register the real default first -
    // store.setDefault(key, ...) - before calling getBoolean()/setValue() on it (see
    // registerGisDefaults and PrefPageMimer's own copy of it): a plain
    // DBPPreferenceStore#setValue(key, false) compares the new value against Eclipse's own
    // unregistered-key fallback (always false), so unticking a checkbox and saving would silently
    // never persist. Managed on the Mimer SQL preference page
    // (org.jkiss.dbeaver.ext.mimer.ui.views.PrefPageMimer).
    public static final String PREF_GIS_LOCATION_AUTO_MAP = "mimer.gis.location.auto_map";
    public static final String PREF_GIS_COORDINATE_AUTO_MAP = "mimer.gis.coordinate.auto_map";
    public static final String PREF_GIS_LATLONG_AUTO_DECIMAL = "mimer.gis.latlong.auto_decimal";
    public static final String PREF_GIS_GRID_POINT_TEXT = "mimer.gis.grid_point_text";

    private MimerConstants() {
        // constants only
    }
}
