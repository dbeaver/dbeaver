/*
 * H2GIS ecplise plugin to register a H2GIS spatial database to 
 * DBeaver, the  Universal Database Manager
 *
 * For more information, please consult: <http://www.h2gis.org/>
 * or contact directly: info_at_h2gis.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.jkiss.dbeaver.ext.h2gis;

import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;

/**
 * Used to create an H2GIS datasource that loads the H2GIS driver from an
 * eclipse extension point, see plugin.xml
 *
 * @author Erwan Bocher, CNRS
 * @author Serge Rider (serge@jkiss.org)
 */
public class H2GISDataSourceProvider extends GenericDataSourceProvider {

    public H2GISDataSourceProvider() {
    }

}
