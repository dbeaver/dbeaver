#!/bin/bash

cd ~/mysnaps/dbeaver 
snapcraft clean dbeaver-ce --step pull
snapcraft
snapcraft login
OUTPUT="$(ls -Art | tail -n 1)"
snapcraft push $OUTPUT --release=stable
