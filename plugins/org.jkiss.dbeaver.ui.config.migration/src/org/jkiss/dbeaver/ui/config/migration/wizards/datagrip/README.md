# DataGrip Connection Importer for DBeaver

This feature adds support for importing database connections from DataGrip (JetBrains) into DBeaver.

## How to Use

### Step 1: Export Settings from DataGrip
1. Open DataGrip
2. Go to `File` → `Export Settings...`
3. Select either "Database" settings or "All Settings"
4. Choose a location and export to a ZIP file
5. Extract the ZIP file to a folder

### Step 2: Import into DBeaver
1. Open DBeaver
2. Go to `File` → `Import...`
3. Expand "Third-party" and select "DataGrip"
4. Click "Next"
5. Browse and select either:
   - The extracted settings folder (containing `settings/options/dataSources.xml`)
   - The `dataSources.xml` file directly
6. Click "Next" to see the list of connections
7. Select the connections you want to import
8. Click "Finish"

## Supported Features

- ✅ Multiple database types (PostgreSQL, MySQL, Oracle, SQL Server, SQLite, etc.)
- ✅ Connection names and descriptions
- ✅ Host, port, and database information
- ✅ Read-only connection settings
- ✅ JDBC URL parsing

## Limitations

- ❌ **Passwords are not imported** (DataGrip doesn't include them in exports for security)
- ❌ SSH tunnel configurations (stored separately in DataGrip)
- ❌ SSL/TLS settings (stored separately in DataGrip)
- ❌ Advanced connection properties may need manual configuration

## Supported Database Types

The importer automatically maps DataGrip driver references to DBeaver drivers:

| DataGrip Driver | DBeaver Driver | Notes |
|-----------------|----------------|-------|
| postgresql | PostgreSQL | Full support |
| mysql | MySQL 8+ | Full support |
| mariadb | MariaDB | Full support |
| oracle | Oracle (Thin) | Full support |
| sqlserver* | SQL Server | All versions mapped |
| sqlite | SQLite | Full support |
| h2 | H2 | Full support |
| derby | Derby | Full support |
| hsqldb | HSQLDB | Full support |

For other database types, the importer will create a generic driver definition that you can manually configure.

## File Structure

The expected DataGrip settings structure is:
```
exported-settings/
└── settings/
    └── options/
        └── dataSources.xml
```

Or you can directly select the `dataSources.xml` file.

## Example DataGrip dataSources.xml

```xml
<application>
  <component name="dataSourceStorage" format="xml" multifile-model="true">
    <data-source source="LOCAL" name="my-postgres" uuid="...">
      <driver-ref>postgresql</driver-ref>
      <synchronize>true</synchronize>
      <remarks>My PostgreSQL Database</remarks>
      <jdbc-driver>org.postgresql.Driver</jdbc-driver>
      <jdbc-url>jdbc:postgresql://localhost:5432/mydb</jdbc-url>
      <working-dir>$ProjectFileDir$</working-dir>
    </data-source>
  </component>
</application>
```

## Troubleshooting

### "DataGrip dataSources.xml file not found"
- Make sure you extracted the DataGrip settings ZIP file
- Check that the folder structure contains `settings/options/dataSources.xml`
- Try selecting the `dataSources.xml` file directly

### "No connections found"
- Verify that DataGrip had database connections configured when you exported
- Check that the export included "Database" settings
- Open the `dataSources.xml` file in a text editor to verify it contains `<data-source>` elements

### "Unknown driver type"
- The importer will create a generic driver for unsupported database types
- You can manually configure the driver after import
- Check the DBeaver driver manager for the imported driver

### Missing passwords
- This is expected behavior for security reasons
- You'll need to re-enter passwords after import
- Consider using DBeaver's secure storage for passwords

## Technical Implementation

The DataGrip importer consists of:

- `ConfigImportWizardDataGrip.java` - Main wizard class
- `ConfigImportWizardPageDataGrip.java` - Connection parsing logic
- `ConfigImportWizardPageDataGripSettings.java` - File selection UI
- `DataGripImporterTest.java` - Test class for verification

The importer uses standard XML parsing to read DataGrip's `dataSources.xml` format and maps the connections to DBeaver's import model.

## Testing

You can test the importer using the provided test class:

```bash
# From the DBeaver project root
cd plugins/org.jkiss.dbeaver.ui.config.migration/src
javac -cp "path/to/dbeaver/libs/*" org/jkiss/dbeaver/ui/config/migration/wizards/datagrip/DataGripImporterTest.java
java -cp "path/to/dbeaver/libs/*:." org.jkiss.dbeaver.ui.config.migration.wizards.datagrip.DataGripImporterTest
```

## Contributing

If you encounter issues with specific database types or DataGrip versions, please:

1. Check the DataGrip version and export format
2. Provide a sample (anonymized) `dataSources.xml` file
3. Report the issue with details about the expected vs. actual behavior

The driver mapping can be extended in `ConfigImportWizardPageDataGrip.java` by updating the `DRIVER_MAPPING` constant.
