-- This script installs the extended stored procedures that implement
-- distributed transaction and XA support for the Microsoft JDBC Driver 4.0 for SQL Server.
-- Works only with SQL 2005 and above

-- Notes for SQL Administrators:

-- #1. Prior to running this script you must copy the extended stored procedure dll SQLJDBC_XA.dll 
--     to the target SQL Server's Binn folder.

-- #2. Permissions to the distributed transaction support procedures for the Microsoft JDBC Driver 4.0 
--     for SQL Server are granted through the SQL Server role [SqlJDBCXAUser].  To maintain a secure default 
--     configuration, no user is granted access to this role by default.

-- Drop and re-create the extended stored procedure definitions in master.

use master
go

-- Drop any existing procedure definitions.
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_init') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_init' 
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_start') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_start'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_end') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_end'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_prepare') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_prepare'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_commit') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_commit'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_rollback') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_rollback'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_forget') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_forget'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_recover') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_recover'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_rollback_ex') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_rollback_ex'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_forget_ex') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_forget_ex'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_prepare_ex') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_prepare_ex'
if exists (select * from sys.objects where object_id = object_id('xp_sqljdbc_xa_init_ex') and OBJECTPROPERTY(object_id, N'IsExtendedProc') = 1) exec sp_dropextendedproc 'xp_sqljdbc_xa_init_ex'
go

-- Install the procedures.
exec sp_addextendedproc 'xp_sqljdbc_xa_init', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_start', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_end', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_prepare', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_commit', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_rollback', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_forget', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_recover', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_rollback_ex', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_forget_ex', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_prepare_ex', 'SQLJDBC_XA.dll'
exec sp_addextendedproc 'xp_sqljdbc_xa_init_ex', 'SQLJDBC_XA.dll'
go

-- Create the [SqlJDBCXAUser] role in master database.
-- The SQL administrator can later add users to this role to allow users to participate 
-- in Microsoft JDBC Driver 4.0 for SQL Server distributed transactions.
if exists (select * from sys.schemas where name = 'SqlJDBCXAUser' ) 
drop schema [SqlJDBCXAUser];

if exists (select * from sys.database_principals where name = 'SqlJDBCXAUser' and type='R') 
drop role [SqlJDBCXAUser];

create role [SqlJDBCXAUser]
go


-- Grant privileges to [SqlJDBCXAUser] role to the extended stored procedures.
grant execute on xp_sqljdbc_xa_init to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_start to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_end to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_prepare to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_commit to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_rollback to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_recover to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_forget to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_rollback_ex to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_forget_ex to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_prepare_ex to [SqlJDBCXAUser]
grant execute on xp_sqljdbc_xa_init_ex to [SqlJDBCXAUser]
go

-- Add users to the [SqlJDBCXAUser] role as needed.

-- Example for adding a SQL authentication user to the SqlJDBCXAUser role.
-- exec sp_addrolemember [SqlJDBCXAUser], 'MySQLUser'

-- Example for adding a windows domain user to the SqlJDBCXAUser role.
-- exec sp_addrolemember [SqlJDBCXAUser], 'MyDomain\MyWindowsUser'

print ''
print 'SQLJDBC XA DLL installation script complete.'
print 'Check for any error messages generated above.'