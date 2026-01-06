#!/usr/bin/env sh

script_dir="$(realpath "$(dirname "$0")")"
repositories_root_dir="$(realpath "$script_dir/..")"

"$repositories_root_dir"/idea-rcp-launch-config-generator/runGenerator.sh "$script_dir"
