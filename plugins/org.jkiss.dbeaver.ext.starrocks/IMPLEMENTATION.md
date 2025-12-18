# StarRocks DBeaver Plugin - Implementation Documentation

This document describes the implementation details of the StarRocks database plugin for DBeaver, including the architecture, design decisions, and the role of each component within DBeaver's framework.

## Table of Contents

1. [Overview](#overview)
2. [Plugin Architecture](#plugin-architecture)
3. [File Structure](#file-structure)
4. [Configuration Files](#configuration-files)
5. [Core Components](#core-components)
6. [DBeaver Framework Integration](#dbeaver-framework-integration)
7. [Overridden Methods Reference](#overridden-methods-reference)

---

## Overview

The StarRocks plugin extends DBeaver to support StarRocks databases. StarRocks is an OLAP database that uses a MySQL-compatible protocol but has a unique three-level hierarchy: **Catalog → Database → Table**. This differs from MySQL's two-level hierarchy (Database → Table).

### Key Design Decisions

1. **Extends MySQL Provider**: StarRocks uses MySQL's wire protocol and JDBC driver, so the plugin extends `org.jkiss.dbeaver.ext.mysql` to inherit connection handling, authentication, and driver management.

2. **Custom Object Hierarchy**: Implements a complete custom object model (`StarRocksCatalog`, `StarRocksDatabase`, `StarRocksTable`, `StarRocksTableColumn`) to support the three-level hierarchy.

3. **Dual-Context Execution**: The execution context tracks both active catalog and active database independently.

### Why Custom Model Classes Are Required (Not Extending MySQL)

A natural question arises: since StarRocks uses MySQL's protocol, why not simply extend MySQL's model classes (`MySQLCatalog`, `MySQLTable`, etc.) instead of creating entirely new ones?

**The fundamental problem is that MySQL and StarRocks have incompatible object hierarchies:**

```
MySQL (2-level):        StarRocks (3-level):

DataSource              DataSource
    │                       │
    ▼                       ▼
MySQLCatalog ◄──────►  StarRocksCatalog    (NEW LEVEL)
(= Database)                │
    │                       ▼
    ▼                  StarRocksDatabase
MySQLTable             (= MySQL's Catalog)
    │                       │
    ▼                       ▼
MySQLTableColumn       StarRocksTable
                            │
                            ▼
                       StarRocksTableColumn
```

#### Specific Technical Barriers

**1. Parent Type Mismatch**

MySQL's classes have hardcoded parent types that don't match StarRocks' hierarchy:

```java
// MySQLTable expects MySQLCatalog as parent
public class MySQLTable extends JDBCTable<MySQLDataSource, MySQLCatalog> {
    // ...
}

// But StarRocks tables live inside StarRocksDatabase, not StarRocksCatalog
public class StarRocksTable extends JDBCTable<StarRocksDataSource, StarRocksDatabase> {
    // ...
}
```

Java generics enforce these type relationships at compile time. You cannot make `StarRocksTable` extend `MySQLTable` while having a different parent container type.

**2. MySQLCatalog IS the Database**

In MySQL's model, `MySQLCatalog` represents what users call a "database" - it's the container for tables. The class implements `DBSSchema` (schema interface), not `DBSCatalog`:

```java
// From MySQL plugin - MySQLCatalog.java
public class MySQLCatalog implements
    DBSCatalog,           // Implements catalog interface BUT...
    DBSProcedureContainer,
    DBSObjectSelector,
    ... {

    // Contains tables directly
    public Collection<MySQLTable> getTables() { ... }
    public Collection<MySQLView> getViews() { ... }
}
```

StarRocks needs an *additional* layer above this - actual catalogs that contain multiple databases.

**3. Fully Qualified Name Generation**

MySQL generates 2-part names; StarRocks requires 3-part names:

```java
// MySQL: database.table
MySQLTable.getFullyQualifiedName() → "mydb.users"

// StarRocks: catalog.database.table
StarRocksTable.getFullyQualifiedName() → "default_catalog.mydb.users"
```

This affects every SQL statement DBeaver generates - SELECT, INSERT, DDL, etc.

**4. Execution Context Differences**

MySQL tracks one "current database" per connection:
```java
// MySQL context
public class MySQLExecutionContext {
    private MySQLCatalog defaultCatalog;  // Single level
}
```

StarRocks must track both catalog AND database:
```java
// StarRocks context
public class StarRocksExecutionContext {
    private String activeCatalogName;   // Two levels
    private String activeDatabaseName;
}
```

**5. Different SQL Commands for Context Switching**

```sql
-- MySQL: Switch database
USE mydb;

-- StarRocks: Switch catalog AND database (two commands)
SET CATALOG my_catalog;
USE mydb;
```

**6. Object Caching Structure**

MySQL's data source caches catalogs (databases) directly:
```java
// MySQLDataSource
private final CatalogCache catalogCache;  // MySQLCatalog objects
```

StarRocks needs two-level caching:
```java
// StarRocksDataSource
private final CatalogCache catalogCache;  // StarRocksCatalog objects
    // Each StarRocksCatalog has:
    private final DatabaseCache databaseCache;  // StarRocksDatabase objects
```

#### What CAN Be Reused from MySQL

Despite needing custom model classes, the plugin successfully reuses:

| Component | Reused From MySQL | Why It Works |
|-----------|-------------------|--------------|
| `MySQLDataSourceProvider` | Connection UI, driver handling | Protocol-level, not hierarchy-dependent |
| `MySQLDialect` | SQL syntax, keywords, functions | Language features are similar |
| JDBC Driver | `com.mysql.cj.jdbc.Driver` | Wire protocol compatible |
| Authentication | MySQL auth handlers | Same authentication mechanisms |

#### Alternative Approaches Considered

**Approach 1: Treat Catalogs as Virtual**
- Map StarRocks catalogs to some MySQL concept
- **Problem**: No MySQL equivalent; would break queries to non-default catalogs

**Approach 2: Flatten to 2-Level**
- Combine catalog+database into a single "database" name like `catalog__database`
- **Problem**: Breaks SQL generation, confuses users, loses catalog switching

**Approach 3: Use Generic JDBC Plugin**
- DBeaver's generic plugin handles arbitrary hierarchies
- **Problem**: Loses MySQL-specific features (syntax highlighting, connection UI, etc.)

**Chosen Approach: Custom Model + MySQL Provider**
- Extend `MySQLDataSourceProvider` for connection handling
- Extend `MySQLDialect` for SQL syntax
- Create custom model classes for the 3-level hierarchy
- Create custom execution context for dual catalog/database tracking

This provides the best of both worlds: MySQL's mature connection infrastructure with StarRocks' correct object hierarchy.

---

## Plugin Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        DBeaver Core                              │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │ DBPDataSource   │  │ JDBCDataSource   │  │ DBSCatalog    │  │
│  │ Provider        │  │                  │  │ DBSSchema     │  │
│  └────────┬────────┘  └────────┬─────────┘  └───────────────┘  │
└───────────┼────────────────────┼────────────────────────────────┘
            │                    │
┌───────────┼────────────────────┼────────────────────────────────┐
│           │     MySQL Plugin   │                                 │
│  ┌────────▼────────┐  ┌────────▼─────────┐                      │
│  │ MySQLDataSource │  │ MySQLDialect     │                      │
│  │ Provider        │  │                  │                      │
│  └────────┬────────┘  └────────┬─────────┘                      │
└───────────┼────────────────────┼────────────────────────────────┘
            │                    │
┌───────────┼────────────────────┼────────────────────────────────┐
│           │   StarRocks Plugin │                                 │
│  ┌────────▼────────┐  ┌────────▼─────────┐  ┌────────────────┐  │
│  │ StarRocksData   │  │ StarRocksDialect │  │ StarRocks      │  │
│  │ SourceProvider  │  │                  │  │ ExecutionCtx   │  │
│  └────────┬────────┘  └──────────────────┘  └────────────────┘  │
│           │                                                      │
│  ┌────────▼────────┐                                            │
│  │ StarRocksData   │───┐                                        │
│  │ Source          │   │                                        │
│  └─────────────────┘   │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Object Hierarchy                                         │   │
│  │  StarRocksCatalog → StarRocksDatabase → StarRocksTable  │   │
│  │                                          ↓               │   │
│  │                               StarRocksTableColumn       │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## File Structure

```
org.jkiss.dbeaver.ext.starrocks/
├── META-INF/
│   └── MANIFEST.MF              # OSGi bundle manifest
├── src/org/jkiss/dbeaver/ext/starrocks/
│   ├── internal/
│   │   └── StarRocksActivator.java       # Plugin lifecycle
│   ├── model/
│   │   ├── StarRocksCatalog.java         # Catalog container
│   │   ├── StarRocksDatabase.java        # Database/schema container
│   │   ├── StarRocksTable.java           # Table object
│   │   └── StarRocksTableColumn.java     # Column metadata
│   ├── StarRocksDataSource.java          # Main data source
│   ├── StarRocksDataSourceProvider.java  # Factory class
│   ├── StarRocksDialect.java             # SQL dialect
│   └── StarRocksExecutionContext.java    # Execution context
├── icons/                        # Database icons
├── plugin.xml                    # Extension point registration
├── pom.xml                       # Maven build config
└── build.properties              # Eclipse build config
```

---

## Configuration Files

### plugin.xml

The `plugin.xml` file registers the plugin with DBeaver's extension point system:

```xml
<extension point="org.jkiss.dbeaver.dataSourceProvider">
```

**Key Configuration Elements:**

| Element | Purpose |
|---------|---------|
| `class` | Points to `StarRocksDataSourceProvider` |
| `parent="mysql"` | Inherits MySQL provider's UI and driver handling |
| `tree/items` | Defines the navigation tree structure (Catalogs → Databases → Tables) |
| `drivers/driver` | Configures JDBC driver (MySQL 8, port 9030) |

**Feature Support Configuration:**

The plugin disables MySQL features not applicable to StarRocks:

| Feature | Why Disabled |
|---------|--------------|
| `references` | OLAP database - no foreign key constraints |
| `triggers` | Not applicable to analytical databases |
| `events` | MySQL events not supported (StarRocks uses `SUBMIT TASK` instead) |
| `charsets` | Only UTF-8 supported - no charset management needed |
| `collations` | Only UTF-8 collation - limited management UI value |
| `clients` | Not yet investigated |

**Features Not Yet Implemented:**

The following features are supported by StarRocks but not yet implemented in this plugin:

| Feature | StarRocks Support | Implementation Notes |
|---------|-------------------|----------------------|
| Partitions | Full support (expression, range, list) | Would require adapting MySQL's `INFORMATION_SCHEMA.PARTITIONS` queries |
| Users/Roles | Full RBAC (`CREATE USER`, `GRANT`, roles) | MySQL plugin's user UI doesn't work; would need custom implementation |
| Views | Supported | Could add separate "Views" folder in navigation tree |
| Materialized Views | Supported (`CREATE MATERIALIZED VIEW`) | StarRocks-specific feature |
| Routines | Supported (UDFs) | Would need investigation |

### META-INF/MANIFEST.MF

OSGi bundle configuration:

| Header | Value | Purpose |
|--------|-------|---------|
| `Bundle-Activator` | `StarRocksActivator` | Plugin lifecycle management |
| `Require-Bundle` | `org.jkiss.dbeaver.model`, `org.jkiss.dbeaver.ext.mysql` | Dependencies |
| `Export-Package` | `org.jkiss.dbeaver.ext.starrocks*` | Public API packages |

---

## Core Components

### 1. StarRocksDataSourceProvider

**File:** `StarRocksDataSourceProvider.java` (40 lines)

**Role in DBeaver:** The provider acts as a factory for creating data source instances. When a user creates a new StarRocks connection, DBeaver calls this provider to instantiate the appropriate data source class.

**Why Extend MySQLDataSourceProvider:**
- Reuses MySQL's connection dialog UI
- Inherits driver management and JDBC pool handling
- Shares MySQL's authentication mechanisms

```java
public class StarRocksDataSourceProvider extends MySQLDataSourceProvider {
    @Override
    public DBPDataSource openDataSource(...) {
        return new StarRocksDataSource(monitor, container);
    }
}
```

---

### 2. StarRocksDataSource

**File:** `StarRocksDataSource.java` (355 lines)

**Role in DBeaver:** The data source is the root object representing a database connection. It manages the connection lifecycle, provides access to database objects, and handles SQL dialect configuration.

**Key Responsibilities:**
- Manages the catalog cache (first level of object hierarchy)
- Provides data type information to DBeaver's type system
- Handles connection error detection
- Creates execution contexts for SQL operations

**Important Inner Class - `CatalogCache`:**
```java
class CatalogCache extends JDBCObjectCache<StarRocksDataSource, StarRocksCatalog> {
    @Override
    protected JDBCStatement prepareObjectsStatement(...) {
        return session.prepareStatement("SHOW CATALOGS");
    }

    @Override
    protected StarRocksCatalog fetchObject(...) {
        return new StarRocksCatalog(owner, resultSet);
    }
}
```

---

### 3. StarRocksExecutionContext

**File:** `StarRocksExecutionContext.java` (260 lines)

**Role in DBeaver:** Execution contexts represent active SQL sessions. Each editor tab or query window gets its own context. The context tracks which catalog and database are "active" for unqualified table references.

**Why This is Important:**
- When you run `SELECT * FROM my_table`, DBeaver needs to know which catalog and database to use
- The context implements `DBCExecutionContextDefaults` to provide this information
- It also handles switching catalogs/databases when the user selects them in the UI

**Key Interface - `DBCExecutionContextDefaults<C, S>`:**
- `C` = Catalog type (`StarRocksCatalog`)
- `S` = Schema type (`StarRocksDatabase`)

This interface tells DBeaver: "This database supports both catalog and schema selection."

---

### 4. StarRocksDialect

**File:** `StarRocksDialect.java` (223 lines)

**Role in DBeaver:** The SQL dialect defines language-specific features for syntax highlighting, code completion, and SQL generation.

**StarRocks-Specific Additions:**

**Keywords (27):**
```
CATALOG, CATALOGS, OLAP, DUPLICATE, AGGREGATE, UNIQUE, PRIMARY,
DISTRIBUTED, BUCKETS, PROPERTIES, BROKER, ROUTINE, LOAD, LABEL,
SYNC, ASYNC, REFRESH, MATERIALIZED, EXTERNAL, ICEBERG, HIVE,
HUDI, JDBC, ELASTICSEARCH, FILE
```

**Functions (70+):**
- Array: `ARRAY_AGG`, `ARRAY_CONTAINS`, `ARRAY_LENGTH`, etc.
- Bitmap: `BITMAP_UNION`, `BITMAP_INTERSECT`, `TO_BITMAP`, etc.
- JSON: `JSON_QUERY`, `JSON_VALUE`, `PARSE_JSON`, etc.
- Window: `LEAD`, `LAG`, `FIRST_VALUE`, `LAST_VALUE`, etc.
- Aggregate: `APPROX_COUNT_DISTINCT`, `PERCENTILE_APPROX`, etc.

---

### 5. Model Classes

#### StarRocksCatalog

**File:** `model/StarRocksCatalog.java` (161 lines)

**Role:** Represents a StarRocks catalog (e.g., `default_catalog`, `hive_catalog`, `iceberg_catalog`).

**Implements:**
- `DBSCatalog` - Tells DBeaver this is a catalog-level container
- `DBPRefreshableObject` - Supports refresh/reload from the database

**Contains:** `DatabaseCache` inner class for lazy-loading databases.

#### StarRocksDatabase

**File:** `model/StarRocksDatabase.java` (236 lines)

**Role:** Represents a database (schema) within a catalog.

**Implements:**
- `DBSSchema` - Standard schema interface
- `DBSObjectContainer` - Contains child objects (tables)
- `DBPQualifiedObject` - Provides fully qualified naming

**Contains:** `TableCache` inner class extending `JDBCStructCache` for tables + columns.

#### StarRocksTable

**File:** `model/StarRocksTable.java` (180 lines)

**Role:** Represents a table or view.

**Extends:** `JDBCTable<StarRocksDataSource, StarRocksDatabase>`

**Special Handling:**
- Returns empty collections for indexes, constraints, foreign keys (StarRocks doesn't use traditional indexes)
- Implements `DBPQualifiedObject` for three-part naming (`catalog.database.table`)

#### StarRocksTableColumn

**File:** `model/StarRocksTableColumn.java` (239 lines)

**Role:** Represents a column with full type information.

**Type Mapping:** Converts StarRocks type strings to DBeaver's `DBPDataKind`:
```
BOOLEAN         → BOOLEAN
INT/BIGINT      → NUMERIC
VARCHAR/STRING  → STRING
DATETIME        → DATETIME
ARRAY           → ARRAY
JSON            → CONTENT
BITMAP/HLL      → UNKNOWN
```

---

## DBeaver Framework Integration

### Object Caching System

DBeaver uses a lazy-loading cache pattern for database objects:

```
┌─────────────────────────────────────────────────────┐
│ JDBCObjectCache<OWNER, OBJECT>                      │
│                                                     │
│  prepareObjectsStatement()  →  SQL query to run    │
│  fetchObject()              →  Parse one row       │
│  getAllObjects()            →  Returns cached list │
└─────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────┐
│ JDBCStructCache<OWNER, OBJECT, ROW>                 │
│                                                     │
│  Extends JDBCObjectCache with:                      │
│  - prepareChildrenStatement()  →  Query for rows   │
│  - fetchChild()                →  Parse child row  │
│                                                     │
│  Used when parent+child loaded together (Tables +  │
│  Columns)                                           │
└─────────────────────────────────────────────────────┘
```

### Navigation Tree System

The `plugin.xml` `tree` element defines how objects appear in DBeaver's navigator:

```xml
<tree path="starrocks" label="StarRocks">
    <items label="Catalogs" path="catalogs" ...>
        <items label="Databases" path="databases" ...>
            <items label="Tables" path="tables" .../>
        </items>
    </items>
</tree>
```

**Path Resolution:**
- `catalogs` → calls `getCatalogs()` on `StarRocksDataSource`
- `databases` → calls `getDatabases()` on `StarRocksCatalog`
- `tables` → calls `getTables()` on `StarRocksDatabase`

---

## Overridden Methods Reference

### StarRocksDataSourceProvider

| Method | Parent | Purpose in DBeaver |
|--------|--------|-------------------|
| `openDataSource()` | `MySQLDataSourceProvider` | Factory method called when opening a connection. Returns the appropriate data source instance. |

### StarRocksDataSource

| Method | Parent | Purpose in DBeaver |
|--------|--------|-------------------|
| `initialize()` | `JDBCDataSource` | Called after connection is established. Loads server configuration, data types, and caches initial data. |
| `getInternalConnectionProperties()` | `JDBCDataSource` | Returns JDBC driver properties. Sets `useInformationSchema=true` and timezone handling. |
| `createExecutionContext()` | `JDBCDataSource` | Factory for execution contexts. Returns `StarRocksExecutionContext` instances. |
| `getSQLDialect()` | `DBPDataSource` | Returns the SQL dialect instance for syntax analysis. |
| `getLocalDataType()` | `DBPDataTypeProvider` | Resolves a type name/ID to a `DBSDataType`. Used by DBeaver when mapping query results. |
| `getDataTypes()` | `DBPDataTypeProvider` | Returns all supported data types. Displayed in "Data Types" node. |
| `getDefaultDataTypeName()` | `DBPDataTypeProvider` | Returns default type for a `DBPDataKind`. Used when creating columns. |
| `getMaxStringLength()` | `SQLDataSource` | Maximum string column length (65535). Affects DDL generation. |
| `isLimitAppliedAfterOtherClauses()` | `SQLDataSource` | Whether LIMIT affects DML. Returns `true` for StarRocks. |
| `createQueryTransformer()` | `SQLDataSource` | Returns query transformer for adding LIMIT clauses to SELECT statements. |
| `discoverErrorType()` | `JDBCDataSource` | Analyzes SQL exceptions to determine error category (connection lost, etc.). |
| `getErrorPosition()` | `JDBCDataSource` | Extracts error position from exception message for editor highlighting. |
| `getCatalogs()` | Custom | Returns all catalogs. Called by navigation tree. |
| `getCatalog()` | Custom | Finds catalog by name. Used for qualified name resolution. |
| `getDefaultCatalog()` | Custom | Returns "default_catalog". Used as initial catalog. |

### StarRocksExecutionContext

| Method | Parent/Interface | Purpose in DBeaver |
|--------|-----------------|-------------------|
| `getContextDefaults()` | `JDBCExecutionContext` | Returns `this` (implements defaults interface). Tells DBeaver this context tracks catalog+schema. |
| `getDefaultCatalog()` | `DBCExecutionContextDefaults` | Returns current active catalog object. Used for unqualified name resolution. |
| `getDefaultSchema()` | `DBCExecutionContextDefaults` | Returns current active database object. Used for unqualified name resolution. |
| `supportsCatalogChange()` | `DBCExecutionContextDefaults` | Returns `true`. Enables catalog dropdown in UI. |
| `supportsSchemaChange()` | `DBCExecutionContextDefaults` | Returns `true`. Enables database dropdown in UI. |
| `setDefaultCatalog()` | `DBCExecutionContextDefaults` | Called when user selects catalog in UI. Executes `SET CATALOG`. |
| `setDefaultSchema()` | `DBCExecutionContextDefaults` | Called when user selects database in UI. Executes `USE database`. |
| `refreshDefaults()` | `DBCExecutionContextDefaults` | Queries current catalog/database from server. Called on context init. |

### StarRocksDialect

| Method | Parent | Purpose in DBeaver |
|--------|--------|-------------------|
| `getReservedWords()` | `MySQLDialect` | Returns set of reserved keywords. Used for syntax highlighting (blue). |
| `getFunctions()` | `MySQLDialect` | Returns set of function names. Used for syntax highlighting and completion. |
| `getIdentifierQuoteStrings()` | `MySQLDialect` | Returns quote characters (`` ` `` and `"`). Used for identifier escaping. |
| `supportsSubqueries()` | `JDBCSQLDialect` | Returns `true`. Enables subquery support in SQL editor. |
| `supportsAliasInSelect()` | `JDBCSQLDialect` | Returns `true`. Allows `SELECT col AS alias`. |
| `supportsCommentQuery()` | `JDBCSQLDialect` | Returns `true`. Allows `/* comment */` in queries. |
| `supportsNullability()` | `JDBCSQLDialect` | Returns `true`. Columns can have NULL/NOT NULL. |
| `isDelimiterAfterQuery()` | `JDBCSQLDialect` | Returns `false`. Delimiter (`;`) goes at end, not after. |

### StarRocksCatalog

| Method | Interface | Purpose in DBeaver |
|--------|-----------|-------------------|
| `getName()` | `DBSObject` | Returns catalog name. Displayed in navigator tree. |
| `getParentObject()` | `DBSObject` | Returns parent data source. Used for navigation. |
| `getDataSource()` | `DBSObject` | Returns owning data source. |
| `isPersisted()` | `DBSObject` | Returns `true`. Object exists in database (not transient). |
| `getChildren()` | `DBSObjectContainer` | Returns databases. Called by navigator for child nodes. |
| `getChild()` | `DBSObjectContainer` | Finds child database by name. |
| `cacheStructure()` | `DBSObjectContainer` | Pre-loads child objects. Called for eager loading. |
| `refreshObject()` | `DBPRefreshableObject` | Clears caches, reloads from database. |
| `getDatabases()` | Custom | Returns all databases in this catalog. |
| `getDatabase()` | Custom | Finds database by name. |

### StarRocksDatabase

| Method | Interface | Purpose in DBeaver |
|--------|-----------|-------------------|
| `getName()` | `DBSObject` | Returns database name. |
| `getParentObject()` | `DBSObject` | Returns parent catalog. |
| `getFullyQualifiedName()` | `DBPQualifiedObject` | Returns `catalog.database` for SQL or `database` for UI. |
| `getChildren()` | `DBSObjectContainer` | Returns tables. |
| `getChild()` | `DBSObjectContainer` | Finds table by name. |
| `cacheStructure()` | `DBSObjectContainer` | Pre-loads tables and optionally columns. |
| `refreshObject()` | `DBPRefreshableObject` | Clears table cache. |
| `getTables()` | Custom | Returns all tables in database. |
| `getTable()` | Custom | Finds table by name. |

### StarRocksTable

| Method | Parent/Interface | Purpose in DBeaver |
|--------|-----------------|-------------------|
| `getSchema()` | `JDBCTable` | Returns parent database. Required by table interface. |
| `getFullyQualifiedName()` | `DBPQualifiedObject` | Returns `catalog.database.table` for SQL, `database.table` for UI. |
| `getAttributes()` | `DBSEntity` | Returns columns. |
| `getAttribute()` | `DBSEntity` | Finds column by name. |
| `getConstraints()` | `DBSTable` | Returns empty (StarRocks has no constraints). |
| `getAssociations()` | `DBSTable` | Returns empty (no foreign keys). |
| `getReferences()` | `DBSTable` | Returns empty (no references). |
| `getIndexes()` | `DBSTable` | Returns empty (no traditional indexes). |
| `isView()` | `DBSTable` | Returns true if this is a view. |
| `getDescription()` | `DBSObject` | Returns table comment. |

### StarRocksTableColumn

| Method | Parent | Purpose in DBeaver |
|--------|--------|-------------------|
| `getTypeName()` | `JDBCTableColumn` | Returns StarRocks type name (VARCHAR, INT, etc.). |
| `getDataKind()` | `JDBCTableColumn` | Returns `DBPDataKind` enum (STRING, NUMERIC, etc.). Used for value formatting. |
| `getMaxLength()` | `JDBCTableColumn` | Returns maximum length. Used for DDL and validation. |
| `getPrecision()` | `JDBCTableColumn` | Returns numeric precision. Used for DECIMAL types. |
| `getScale()` | `JDBCTableColumn` | Returns numeric scale. Used for DECIMAL types. |
| `getDefaultValue()` | `DBSAttributeBase` | Returns column default value. |
| `isRequired()` | `DBSAttributeBase` | Returns true if NOT NULL. |
| `getDescription()` | `DBSObject` | Returns column comment. |

---

## Summary

The StarRocks plugin follows DBeaver's standard patterns:

1. **Provider Pattern**: `StarRocksDataSourceProvider` creates `StarRocksDataSource` instances
2. **Lazy Loading**: `JDBCObjectCache` and `JDBCStructCache` defer SQL queries until needed
3. **Interface Segregation**: Each model class implements only the interfaces it needs
4. **Dialect Extension**: `StarRocksDialect` extends MySQL while adding StarRocks-specific syntax
5. **Context Management**: `StarRocksExecutionContext` tracks dual catalog+database state

The key innovation is supporting StarRocks' three-level hierarchy (Catalog → Database → Table) while inheriting MySQL's protocol compatibility for connection handling.

---

## References

### StarRocks Documentation

- [Expression Partitioning (Recommended)](https://docs.starrocks.io/docs/table_design/data_distribution/expression_partitioning/) - Partitioning guide
- [SHOW PARTITIONS](https://docs.starrocks.io/docs/sql-reference/sql-statements/table_bucket_part_index/SHOW_PARTITIONS/) - Partition SQL reference
- [information_schema.partitions](https://docs.starrocks.io/docs/sql-reference/information_schema/partitions/) - Partition metadata
- [CREATE USER](https://docs.starrocks.io/docs/sql-reference/sql-statements/account-management/CREATE_USER/) - User management
- [GRANT](https://docs.starrocks.io/docs/sql-reference/sql-statements/account-management/GRANT/) - Privilege management
- [Manage User Privileges](https://docs.starrocks.io/docs/administration/user_privs/authorization/User_privilege/) - RBAC overview
- [SUBMIT TASK](https://docs.starrocks.io/docs/sql-reference/sql-statements/loading_unloading/ETL/SUBMIT_TASK/) - Scheduled tasks
- [System Variables](https://docs.starrocks.io/docs/sql-reference/System_variable/) - Configuration including charset

### DBeaver Documentation

- [DBeaver Wiki - Database Drivers](https://github.com/dbeaver/dbeaver/wiki/Database-drivers)
- [DBeaver Extension Development](https://github.com/dbeaver/dbeaver/wiki/Extension-Development)
