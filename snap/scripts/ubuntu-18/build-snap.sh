#!/bin/bash

cp ./snapcraft.yaml ./dbeaver/snap/
cd ./dbeaver/
snapcraft clean dbeaverapp --step pull
snapcraft cleanbuild
snapcraft login --with ../snapcraft.login
#OUTPUT="$(ls -Art | tail -n 1)"
#snapcraft push "dbeaverapp_latest_OrderedDict([('build-on', 'amd64')]).snap" --release=edge
