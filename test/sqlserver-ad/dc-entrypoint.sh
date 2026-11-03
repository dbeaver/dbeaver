#!/usr/bin/env bash
set -euo pipefail
if [[ ! -f /var/lib/samba/private/sam.ldb ]]; then
    rm -f /etc/samba/smb.conf
    samba-tool domain provision \
        --realm=AD7065.TEST --domain=AD7065 --server-role=dc \
        --dns-backend=SAMBA_INTERNAL --use-rfc2307 \
        --host-name=dc --host-ip=172.30.70.10 \
        --option='acl_xattr:security_acl_name=user.NTACL' \
        --option='acl_xattr:ignore system acls=yes' \
        --adminpass="$LAB_ADMIN_PASSWORD"
fi
cp /var/lib/samba/private/krb5.conf /etc/krb5.conf
exec samba -i --no-process-group
