#!/bin/bash
set -xe

package_version=$1
package_name_prefix=$2
zip_basename=`basename ${package_name_prefix} *.zip`
short_commit_id=$(git rev-parse --short HEAD)
zip_package_name="${zip_basename}-${package_version}-${short_commit_id}.zip"
package_dir="product/community/target/products/org.jkiss.dbeaver.core.product"

cd ${package_dir}
rm -rf macosx/cocoa/aarch64
zip -r ${zip_package_name} linux win32 macosx
