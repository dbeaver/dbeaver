#!/usr/bin/env bash
set -euo pipefail
lab_dir="$(cd "$(dirname "$0")" && pwd)"
compose() {
    docker compose --project-name dbeaver7065-ad --project-directory "$lab_dir" --file "$lab_dir/compose.yaml" "$@"
}
case "${1:-help}" in
    up)
        if [[ ! -f "$lab_dir/.env" ]]; then
            python3 - "$lab_dir/.env" <<'PYTHON'
import os
import secrets
import sys
names = ['LAB_ADMIN_PASSWORD', 'LAB_SERVICE_PASSWORD', 'LAB_USER_PASSWORD', 'MSSQL_SA_PASSWORD']
with open(os.open(sys.argv[1], os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as output:
    for name in names:
        output.write(f'{name}=Db7065!{secrets.token_hex(16)}\n')
PYTHON
        fi
        compose up --build --detach --wait dc
        compose exec -T dc bash /lab/bootstrap-domain.sh
        compose up --build --detach --wait sql
        compose exec -T sql bash -c 'SQLCMDPASSWORD="$MSSQL_SA_PASSWORD" /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -i /lab/bootstrap.sql'
        ;;
    check)
        compose exec -T sql bash /lab/check-access.sh
        compose exec -T dc samba-tool group removemembers LabReaders reader
        trap 'compose exec -T dc samba-tool group addmembers LabReaders reader >/dev/null' EXIT
        compose exec -T sql bash /lab/check-access.sh without-group
        compose exec -T dc samba-tool group addmembers LabReaders reader
        trap - EXIT
        ;;
    down)
        compose down
        ;;
    reset)
        compose down --volumes
        ;;
    status)
        compose ps
        ;;
    *)
        echo 'Usage: ./lab.sh {up|check|status|down|reset}'
        ;;
esac
