/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.utils;

import java.util.Locale;

/**
 * MIME type parser
 */
public class MimeType {

    private String primaryType;
    private String subType;

    public MimeType() {
        primaryType = "application";
        subType = "*";
    }

    public MimeType(String rawdata) throws IllegalArgumentException {
        parse(rawdata);
    }

    public MimeType(String primary, String sub) {
        primaryType = primary.toLowerCase(Locale.ENGLISH);
        subType = sub.toLowerCase(Locale.ENGLISH);
    }

    private void parse(String rawdata) throws IllegalArgumentException {
        int slashIndex = rawdata.indexOf('/');
        int semIndex = rawdata.indexOf(';');
        if ((slashIndex < 0) && (semIndex < 0)) {
            primaryType = rawdata;
            subType = "*";
        } else if ((slashIndex < 0) && (semIndex >= 0)) {
            primaryType = rawdata.substring(0, semIndex);
            subType = "*";
        } else if ((slashIndex >= 0) && (semIndex < 0)) {
            primaryType = rawdata.substring(0, slashIndex).trim().toLowerCase(Locale.ENGLISH);
            subType = rawdata.substring(slashIndex + 1).trim().toLowerCase(Locale.ENGLISH);
        } else if (slashIndex < semIndex) {
            primaryType = rawdata.substring(0, slashIndex).trim().toLowerCase(Locale.ENGLISH);
            subType = rawdata.substring(slashIndex + 1, semIndex).trim().toLowerCase(Locale.ENGLISH);
        } else {
            // we have a ';' lexically before a '/' which means we
            // have a primary type and a parameter list but no sub type
            throw new IllegalArgumentException("Unable to find a sub type.");
        }
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public String getSubType() {
        return subType;
    }

    public String toString() {
        return getBaseType();
    }

    public String getBaseType() {
        return primaryType + "/" + subType;
    }

    public boolean match(MimeType type) {
        return primaryType.equals(type.getPrimaryType()) &&
            (subType.equals("*") || type.getSubType().equals("*") || (subType.equals(type.getSubType())));
    }

    public boolean match(String rawdata) throws IllegalArgumentException {
        return match(new MimeType(rawdata));
    }

}