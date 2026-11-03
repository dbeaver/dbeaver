IF SUSER_ID(N'AD7065\directuser') IS NULL
    CREATE LOGIN [AD7065\directuser] FROM WINDOWS;
IF SUSER_ID(N'AD7065\LabReaders') IS NULL
    CREATE LOGIN [AD7065\LabReaders] FROM WINDOWS;
GO
IF DB_ID(N'access_lab') IS NULL
    CREATE DATABASE [access_lab];
GO
USE [access_lab];
IF OBJECT_ID(N'dbo.sample', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sample (id int NOT NULL PRIMARY KEY, label nvarchar(100) NOT NULL);
    INSERT INTO dbo.sample VALUES (1, N'AD access test');
END;
IF USER_ID(N'AD7065\directuser') IS NULL
    CREATE USER [AD7065\directuser] FOR LOGIN [AD7065\directuser];
IF USER_ID(N'AD7065\LabReaders') IS NULL
    CREATE USER [AD7065\LabReaders] FOR LOGIN [AD7065\LabReaders];
ALTER ROLE db_datareader ADD MEMBER [AD7065\directuser];
GRANT SELECT ON dbo.sample TO [AD7065\LabReaders];
GO
SELECT name, type_desc FROM master.sys.server_principals
WHERE name IN (N'AD7065\directuser', N'AD7065\LabReaders');
