#!/bin/bash

sudo apt update
sudo apt install build-essential
sudo snap install --classic snapcraft
mkdir -p ~/mysnaps/dbeaver cd ~/mysnaps/dbeaver snapcraft init
cp snapcraft.yaml ~/mysnaps/dbeaver/snap/
sudo addgroup --system lxd
sudo adduser $USER lxd
sudo snap install lxd
lxd init
