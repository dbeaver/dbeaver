# SQL Server / Active Directory local lab

This lab exercises Windows users, Windows groups and SQL Server permissions for dbeaver/pro#7065. It runs a Samba AD domain controller and SQL Server 2022 Developer in a dedicated Docker network. It does not change the host DNS, Kerberos configuration or domain membership.

SQL Server uses Kerberos and a service keytab. Samba supplies the AD directory, DNS and KDC. This is a functional development lab; Microsoft's documented deployment uses a Windows domain controller. On Apple Silicon the SQL Server container runs through x86-64 emulation, which Microsoft does not officially support.

## Start and test

From this directory:

```sh
./lab.sh up
./lab.sh check
./lab.sh status
```

The first start builds the images and generates random local passwords in `.env` with mode `0600`. The file is ignored by Git and excluded from Docker build contexts. Domain databases, SQL Server data and the service keytab are stored in this Compose project's named volumes.

The `check` command verifies:

- authentication through a direct Windows login;
- authentication through an AD group login;
- SELECT permission granted to the group;
- rejection of an UPDATE that was not granted;
- rejection of a valid domain user with no SQL login or permitted group;
- loss of SQL access after removing a user from the AD group, followed by restoration of the original membership.

Checks use an isolated Kerberos ticket cache inside the SQL container. They do not change the host ticket cache.

## Connect from DBeaver

Use the SQL Server driver and SQL Server authentication:

| Setting | Value |
| --- | --- |
| Host | `localhost` |
| Port | `17065` |
| Database | `access_lab` |
| User | `sa` |
| Password | `MSSQL_SA_PASSWORD` from `.env` |
| Driver property | `trustServerCertificate=true` |

The AD endpoints are available only inside the Docker network. SQL Server is published only on loopback. The fixed subnet is `172.30.70.0/24` and the test realm is `AD7065.TEST` (`AD7065` in Windows account names).

## Test accounts

All domain test users use `LAB_USER_PASSWORD` from `.env`.

| Account | Initial state |
| --- | --- |
| `AD7065\directuser` | Direct Windows login, mapped database user, member of `db_datareader` |
| `AD7065\reader` | Member of `AD7065\LabReaders`; access comes through the group login |
| `AD7065\LabReaders` | Windows group login with SELECT on `access_lab.dbo.sample` |
| `AD7065\outsider` | Valid domain user without SQL Server access |
| `AD7065\guiuser` | Domain user available for creating a new login through the GUI |
| `AD7065\GuiReaders` | Group containing `guiuser`, available for creating a group login through the GUI |

To test creation in DBeaver, create a login for `AD7065\guiuser` or `AD7065\GuiReaders` using Windows authentication. These principals exist in AD but the startup SQL does not create their SQL Server logins. Then open **Roles**, select the `access_lab` scope, enable the database mapping and assign a database role. Use **Permissions** to manage explicit permissions for the same scope.

The automated checks use `LabReaders` and `reader`. Use the separate GUI accounts for manual changes so the automated baseline remains intact.

## Stop or recreate

```sh
./lab.sh down
```

This stops the lab and preserves its data. Run `./lab.sh up` to start it again.

```sh
./lab.sh reset
./lab.sh up
```

`reset` removes only this lab's containers, network and named volumes, including manual SQL and AD changes. Passwords remain in `.env`; remove that file after resetting if new passwords are needed.

SYSVOL ACLs use `user.NTACL` inside the private Docker volume so the DC does not require a privileged container. This storage setting is intended only for this local lab.

## References

- [SQL Server container AD configuration](https://learn.microsoft.com/en-us/sql/linux/containers/tutorial-adutil?view=sql-server-ver17)
- [SQL Server AD authentication internals](https://learn.microsoft.com/en-us/sql/linux/sql-server-linux-ad-auth-understanding?view=sql-server-ver17)
- [Samba AD capabilities](https://www.samba.org/samba/what_is_samba.html)
- [SQL Server container platform support](https://learn.microsoft.com/en-us/sql/linux/install-upgrade/quickstart-install-docker)
