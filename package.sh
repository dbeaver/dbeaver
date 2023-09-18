#!/bin/bash
set -xe

package_version=$1
long_commit_id=$(git rev-parse --short HEAD)
zip_package_name="dbeaver-${package_version}-${long_commit_id}.zip"
package_dir="product/community/target/products/org.jkiss.dbeaver.core.product"

cd ${package_dir}
zip -r ${zip_package_name} linux win32 macosx
