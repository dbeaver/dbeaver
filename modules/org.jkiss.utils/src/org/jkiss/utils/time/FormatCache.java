/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.utils.time;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>FormatCache is a cache and factory for {@link Format}s.</p>
 * 
 * @since 3.0
 * @version $Id: FormatCache 892161 2009-12-18 07:21:10Z  $
 */
// TODO: Before making public move from getDateTimeInstance(Integer,...) to int; or some other approach.
abstract class FormatCache<F extends Format> {
    /**
     * No date or no time.  Used in same parameters as DateFormat.SHORT or DateFormat.LONG
     */
    static final int NONE= -1;
    
    private final ConcurrentMap<MultipartKey, F> cInstanceCache 
        = new ConcurrentHashMap<MultipartKey, F>(7);
    
    private final ConcurrentMap<MultipartKey, String> cDateTimeInstanceCache 
        = new ConcurrentHashMap<MultipartKey, String>(7);

    /**
     * <p>Gets a formatter instance using the default pattern in the
     * default timezone and locale.</p>
     * 
     * @return a date/time formatter
     */
    public F getInstance() {
        return getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, TimeZone.getDefault(), Locale.getDefault());
    }

    /**
     * <p>Gets a formatter instance using the specified pattern, time zone
     * and locale.</p>
     * 
     * @param pattern  {@link java.text.SimpleDateFormat} compatible
     *  pattern
     * @param timeZone  the non-null time zone
     * @param locale  the non-null locale
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid
     *  or <code>null</code>
     */
    public F getInstance(String pattern, TimeZone timeZone, Locale locale) {
        if (pattern == null) {
            throw new NullPointerException("pattern must not be null");
        }
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        MultipartKey key = new MultipartKey(pattern, timeZone, locale);
        F format = cInstanceCache.get(key);
        if (format == null) {           
            format = createInstance(pattern, timeZone, locale);
            F previousValue= cInstanceCache.putIfAbsent(key, format);
            if (previousValue != null) {
                // another thread snuck in and did the same work
                // we should return the instance that is in ConcurrentMap
                format= previousValue;              
            }
        }
        return format;
    }
    
    /**
     * <p>Create a format instance using the specified pattern, time zone
     * and locale.</p>
     * 
     * @param pattern  {@link java.text.SimpleDateFormat} compatible pattern, this will not be null.
     * @param timeZone  time zone, this will not be null.
     * @param locale  locale, this will not be null.
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid
     *  or <code>null</code>
     */
    abstract protected F createInstance(String pattern, TimeZone timeZone, Locale locale);
        
    /**
     * <p>Gets a date/time formatter instance using the specified style,
     * time zone and locale.</p>
     * 
     * @param dateStyle  date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeStyle  time style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone  optional time zone, overrides time zone of
     *  formatted date
     * @param locale  optional locale, overrides system locale
     * @return a localized standard date/time formatter
     * @throws IllegalArgumentException if the Locale has no date/time
     *  pattern defined
     */
    public F getDateTimeInstance(Integer dateStyle, Integer timeStyle, TimeZone timeZone, Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        MultipartKey key = new MultipartKey(dateStyle, timeStyle, locale);

        String pattern = cDateTimeInstanceCache.get(key);
        if (pattern == null) {
            try {
                DateFormat formatter;
                if (dateStyle == null) {
                    formatter = DateFormat.getTimeInstance(timeStyle, locale);                    
                }
                else if (timeStyle == null) {
                    formatter = DateFormat.getDateInstance(dateStyle, locale);                    
                }
                else {
                    formatter = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
                }
                pattern = ((SimpleDateFormat)formatter).toPattern();
                String previous = cDateTimeInstanceCache.putIfAbsent(key, pattern);
                if (previous != null) {
                    // even though it doesn't matter if another thread put the pattern
                    // it's still good practice to return the String instance that is
                    // actually in the ConcurrentMap
                    pattern= previous;
                }
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("No date time pattern for locale: " + locale);
            }
        }
        
        return getInstance(pattern, timeZone, locale);
    }

    // ----------------------------------------------------------------------
    /**
     * <p>Helper class to hold multi-part Map keys</p>
     */
    private static class MultipartKey {
        private final Object[] keys;
        private int hashCode;

        /**
         * Constructs an instance of <code>MultipartKey</code> to hold the specified objects.
         * @param keys the set of objects that make up the key.  Each key may be null.
         */
        public MultipartKey(Object... keys) {
            this.keys = keys;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ( obj instanceof MultipartKey == false ) {
                return false;
            }
            return Arrays.equals(keys, ((MultipartKey)obj).keys);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            if(hashCode==0) {
                int rc= 0;
                for(Object key : keys) {
                    if(key!=null) {
                        rc= rc*7 + key.hashCode();
                    }
                }
                hashCode= rc;
            }
            return hashCode;
        }
    }

}
