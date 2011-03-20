Summary: Universal Database Manager and SQL Client
Name: DBeaver
Version: @productVersion@
Release: 1
Copyright: Freeware
Group: Applications/Databases
Source: @archivePrefix@-@productVersion@-linux.gtk.x86.zip
BuildRoot: /var/tmp/%{name}-buildroot

%description
Free Universal Database Manager and SQL Client. 
Java-based application, supports MySQL, PostgreSQL, Oracle, DB2, 
MSSQL, Sybase and any database which has JDBC driver.

%prep
%setup -q
%patch -p1 -b .buildroot

%build
make RPM_OPT_FLAGS="$RPM_OPT_FLAGS"

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/usr/bin
mkdir -p $RPM_BUILD_ROOT/usr/man/man1

install -s -m 755 eject $RPM_BUILD_ROOT/usr/bin/eject
install -m 644 eject.1 $RPM_BUILD_ROOT/usr/man/man1/eject.1

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
%doc README TODO COPYING ChangeLog

/usr/bin/eject
/usr/man/man1/eject.1

%changelog
* Sun Mar 21 1999 Cristian Gafton <gafton@redhat.com> 
- auto rebuild in the new build environment (release 3)

* Wed Feb 24 1999 Preston Brown <pbrown@redhat.com> 
- Injected new description and group.

[ Some changelog entries trimmed for brevity.  -Editor. ]
