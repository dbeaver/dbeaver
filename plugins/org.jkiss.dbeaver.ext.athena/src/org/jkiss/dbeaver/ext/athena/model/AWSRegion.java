/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
 */
package org.jkiss.dbeaver.ext.athena.model;

/**
 * Athena constants
 */
public enum AWSRegion
{
    us_east_1("us-east-1", "US East (N. Virginia)"),
    us_east_2("us-east-2", "US East (Ohio)"),
    us_west_1("us-west-1", "US West (N. California)"),
    us_west_2("us-west-2", "US West (Oregon)"),
    ca_central_1("ca-central-1", "Canada (Central)"),
    eu_central_1("eu-central-1", "EU (Frankfurt)"),
    eu_west_1("eu-west-1", "EU (Ireland)"),
    eu_west_2("eu-west-2", "EU (London)"),
    eu_west_3("eu-west-3", "EU (Paris)"),
    ap_northeast_1("ap-northeast-1", "Asia Pacific (Tokyo)"),
    ap_northeast_2("ap-northeast-2", "Asia Pacific (Seoul)"),
    ap_northeast_3("ap-northeast-3", "Asia Pacific (Osaka-Local)"),
    ap_southeast_1("ap-southeast-1", "Asia Pacific (Singapore)"),
    ap_southeast_2("ap-southeast-2", "Asia Pacific (Sydney)"),
    ap_south_1("ap-south-1", "Asia Pacific (Mumbai)"),
    sa_east_1("sa-east-1", "South America (São Paulo)"),;

    private final String id;
    private final String name;

    AWSRegion(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
