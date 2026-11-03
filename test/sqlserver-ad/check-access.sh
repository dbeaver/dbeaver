#!/usr/bin/env bash
set -euo pipefail
export KRB5CCNAME=FILE:/tmp/dbeaver7065-check.ccache
trap 'kdestroy 2>/dev/null || true' EXIT
kerberos_query() {
    printf '%s\n' "$LAB_USER_PASSWORD" | kinit "$1@AD7065.TEST" >/dev/null
    /opt/mssql-tools18/bin/sqlcmd -S sql.ad7065.test,1433 -E -C -b -h -1 -W -d access_lab -Q "SET NOCOUNT ON; $2"
    kdestroy
}
if [[ "${1:-}" == without-group ]]; then
    printf '%s\n' "$LAB_USER_PASSWORD" | kinit 'reader@AD7065.TEST' >/dev/null
    if /opt/mssql-tools18/bin/sqlcmd -S sql.ad7065.test,1433 -E -C -b -d access_lab -Q 'SELECT 1' >/tmp/dbeaver7065-removed.log 2>&1; then
        echo 'FAIL: user removed from the AD group unexpectedly authenticated' >&2
        exit 1
    fi
    grep -q 'Login failed' /tmp/dbeaver7065-removed.log
    echo 'PASS: removing the user from the AD group removes SQL Server access'
    exit 0
fi
kerberos_query directuser 'SELECT CAST(ORIGINAL_LOGIN() AS nvarchar(128)) AS authenticated_user; SELECT COUNT(*) AS row_count FROM dbo.sample;'
kerberos_query reader 'SELECT CAST(ORIGINAL_LOGIN() AS nvarchar(128)) AS authenticated_user; SELECT COUNT(*) AS row_count FROM dbo.sample;'
printf '%s\n' "$LAB_USER_PASSWORD" | kinit 'reader@AD7065.TEST' >/dev/null
if /opt/mssql-tools18/bin/sqlcmd -S sql.ad7065.test,1433 -E -C -b -d access_lab -Q 'UPDATE dbo.sample SET label = label WHERE id = 1' >/tmp/dbeaver7065-denied.log 2>&1; then
    echo 'FAIL: group member unexpectedly has UPDATE permission' >&2
    exit 1
fi
grep -Eq 'Msg 229|UPDATE permission was denied' /tmp/dbeaver7065-denied.log
kdestroy
printf '%s\n' "$LAB_USER_PASSWORD" | kinit 'outsider@AD7065.TEST' >/dev/null
if /opt/mssql-tools18/bin/sqlcmd -S sql.ad7065.test,1433 -E -C -b -d access_lab -Q 'SELECT 1' >/tmp/dbeaver7065-outsider.log 2>&1; then
    echo 'FAIL: user without login or permitted group unexpectedly authenticated' >&2
    exit 1
fi
grep -q 'Login failed' /tmp/dbeaver7065-outsider.log
echo 'PASS: direct login, group login, SELECT permission, denied UPDATE and rejected outsider'
