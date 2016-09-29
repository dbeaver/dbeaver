/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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