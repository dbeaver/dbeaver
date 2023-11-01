#!/bin/bash
set -xe

package_version=$1
package_name_prefix=$2
win32_package_name="win32"
linux_x86_package_name="linux-x86_64"
linux_arrch64_package_name="linux-arrch64"
mac_package_name="macosx"
base_pwd=`pwd`

zip_basename=`basename ${package_name_prefix} *.zip`

win32_zip_package_name="${zip_basename}-${package_version}-${win32_package_name}.zip"
linux_x86_zip_package_name="${zip_basename}-${package_version}-${linux_x86_package_name}.zip"
linux_arrch64_zip_package_name="${zip_basename}-${package_version}-${linux_arrch64_package_name}.zip"
mac_zip_package_name="${zip_basename}-${package_version}-${mac_package_name}.zip"

package_dir="product/community/target/products/org.jkiss.dbeaver.core.product"

cd "${base_pwd}/${package_dir}/win32"
zip -r ${win32_zip_package_name} win32
cp ${win32_zip_package_name} ${base_pwd}/${package_dir}
cd ${base_pwd}/${package_dir}
zip -r ${mac_zip_package_name} macosx
zip -r ${linux_x86_zip_package_name} linux/gtk/x86_64
zip -r ${linux_arrch64_zip_package_name} linux/gtk/aarch64

