#!/bin/bash

sudo apt update
sudo apt install snapd
sudo apt install build-essential
sudo snap install --classic snapcraft
mkdir -p ./snap
cp snapcraft.yaml ./snap/

