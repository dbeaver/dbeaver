# DBeaver architecture

### Model / UI separation

- Plugins are split into model (e.g. `o.j.d.ext.mysql`, `o.j.d.model.ai`) and UI (`o.j.d.ext.mysql.ui`, `o.j.d.ui.charts`) bundles.
- Model plugins must not depend on SWT, JFace or any other UI-related bundles.
- Do not execute SQL queries or any other model-level things directly from UI modules, use abstracts in model layer.

### Extension-point driven design

- Features are contributed via Eclipse extension points declared in `plugin.xml`.
- New extensible features should be formed as extension points/extensions.

### Package and class naming

- `DBP*` - Platform-level capability (`DBPDataSource`, `DBPObject`)
- `DBS*` - Database structure/metadata (`DBSObject`, `DBSTable`, `DBSSchema`)
- `DBC*` - Connectivity (execution context) (`DBCSession`, `DBCException`)
- `DBD*` - Data values/formatting (`DBDValueHandler`, `DBDDataFilter`)
- `DBR*` - Runtime (progress, jobs) (`DBRProgressMonitor`, `DBRRunnableWithProgress`)
