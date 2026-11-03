#!/usr/bin/env bash
set -euo pipefail
create_user() {
    if ! samba-tool user show "$1" >/dev/null 2>&1; then
        samba-tool user create "$1" "$2"
    fi
    samba-tool user setexpiry "$1" --noexpiry
}
create_user sqlsvc "$LAB_SERVICE_PASSWORD"
python3 - <<'PYTHON'
import ldb
from samba.auth import system_session
from samba.param import LoadParm
from samba.samdb import SamDB
lp = LoadParm()
lp.load_default()
db = SamDB(url='/var/lib/samba/private/sam.ldb', session_info=system_session(), lp=lp)
message = ldb.Message()
message.dn = ldb.Dn(db, 'CN=sqlsvc,CN=Users,DC=ad7065,DC=test')
message['msDS-SupportedEncryptionTypes'] = ldb.MessageElement('24', ldb.FLAG_MOD_REPLACE, 'msDS-SupportedEncryptionTypes')
db.modify(message)
PYTHON
create_user directuser "$LAB_USER_PASSWORD"
create_user reader "$LAB_USER_PASSWORD"
create_user outsider "$LAB_USER_PASSWORD"
create_user guiuser "$LAB_USER_PASSWORD"
samba-tool domain passwordsettings set --max-pwd-age=0
for name in LabReaders GuiReaders; do
    if ! samba-tool group show "$name" >/dev/null 2>&1; then
        samba-tool group add "$name"
    fi
done
if ! samba-tool group listmembers GuiReaders | grep -qx guiuser; then
    samba-tool group addmembers GuiReaders guiuser
fi
if ! samba-tool group listmembers LabReaders | tr -d '\r' | grep -qx reader; then
    samba-tool group addmembers LabReaders reader
fi
for spn in MSSQLSvc/sql MSSQLSvc/sql:1433 MSSQLSvc/sql.ad7065.test MSSQLSvc/sql.ad7065.test:1433 MSSQLSvc/sql.ad7065.test:17065; do
    if ! samba-tool spn list sqlsvc | grep -Fq "$spn"; then
        samba-tool spn add "$spn" sqlsvc
    fi
done
samba-tool dns add 127.0.0.1 ad7065.test sql A 172.30.70.20 -U "Administrator%$LAB_ADMIN_PASSWORD" 2>/dev/null || \
    samba-tool dns query 127.0.0.1 ad7065.test sql A -U "Administrator%$LAB_ADMIN_PASSWORD"
if ! samba-tool dns zonelist 127.0.0.1 -U "Administrator%$LAB_ADMIN_PASSWORD" | grep -q 70.30.172.in-addr.arpa; then
    samba-tool dns zonecreate 127.0.0.1 70.30.172.in-addr.arpa -U "Administrator%$LAB_ADMIN_PASSWORD"
fi
for entry in '10 dc.ad7065.test' '20 sql.ad7065.test'; do
    read -r address name <<< "$entry"
    samba-tool dns add 127.0.0.1 70.30.172.in-addr.arpa "$address" PTR "$name" -U "Administrator%$LAB_ADMIN_PASSWORD" 2>/dev/null || \
        samba-tool dns query 127.0.0.1 70.30.172.in-addr.arpa "$address" PTR -U "Administrator%$LAB_ADMIN_PASSWORD"
done
rm -f /lab-secrets/mssql.keytab
samba-tool domain exportkeytab /lab-secrets/mssql.keytab --principal=sqlsvc
for spn in MSSQLSvc/sql MSSQLSvc/sql:1433 MSSQLSvc/sql.ad7065.test MSSQLSvc/sql.ad7065.test:1433 MSSQLSvc/sql.ad7065.test:17065; do
    samba-tool domain exportkeytab /lab-secrets/mssql.keytab --principal="$spn"
done
chown 10001:10001 /lab-secrets/mssql.keytab
chmod 440 /lab-secrets/mssql.keytab
