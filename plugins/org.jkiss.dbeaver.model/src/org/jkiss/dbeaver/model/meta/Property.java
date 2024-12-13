/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.Format;

/**
 * Property
 */
@Target(value = {ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Property
{

    String DEFAULT_LOCAL_STRING = "#"; //NON-NLS-1
    String RESOURCE_TYPE_NAME = "name"; //NON-NLS-1
    String RESOURCE_TYPE_DESCRIPTION = "description"; //NON-NLS-1
    String RESOURCE_TYPE_HINT = "hint"; //NON-NLS-1

    /**
     * Property unique ID (unique within class)
     * @return id
     */
    String id() default ""; //NON-NLS-1

    /**
     * Property human readable name
     *
     * @return name
     */
    String name() default DEFAULT_LOCAL_STRING;

    /**
     * Property name which used on serialization
     *
     * @return name
     */
    String keyName() default DEFAULT_LOCAL_STRING;

    /**
     * Property category (optional). A human readable string
     *
     * @return category
     */
    String category() default ""; //NON-NLS-1

    /**
     * Property description (optional)
     * @return description
     */
    String description() default DEFAULT_LOCAL_STRING;

    /**
     * Property hint (optional)
     * @return hint
     */
    String hint() default DEFAULT_LOCAL_STRING;

    /**
     * Editable flag. If set to true then property may be edited for new objects.
     * @return editable flag
     */
    boolean editable() default false;

    String editableExpr() default "";

    /**
     * Updatable flag. If set to true then property can be changed on any object
     * @return updatable flag
     */
    boolean updatable() default false;

    String updatableExpr() default "";

    /**
     * Viewable flag. Viewable properties are displayed in lists.
     * Note that property editor contains all properties (except hidden).
     * @return viewable flag
     */
    boolean viewable() default false;

    boolean hidden() default false;

    boolean expensive() default false;

    /**
     * Multiline properties usually contain some big texts
     */
    PropertyLength length() default PropertyLength.LONG;

    /**
     * Specific properties ae rendered separate from others
     */
    boolean specific() default false;

    /**
     * Optional property. Won't be rendered at all if value is null.
     */
    boolean optional() default false;

    /**
     * It is possible that value of this property will be an object which can be linked.
     * Used for cacheable properties which may return java.lang.Object ot DBSObject.
     */
    boolean linkPossible() default false;

    /**
     * Hyperlink property
     */
    boolean href() default false;

    /**
     * Makes sense only for lazy properties. If set to true then this property value can be read
     * in non-lazy way with null progress monitor. In this case it will return "preview" value.
     * @return preview support flag
     */
    boolean supportsPreview() default false;

    /**
     * Property holds password. Must be secured in UI.
     */
    boolean password() default false;

    /**
     * Does not show the field in the connection window of CB when this property is true.
     */
    boolean nonSecuredProperty() default false;

    boolean required() default false;

    int order() default Integer.MAX_VALUE;

    String helpContextId() default ""; //NON-NLS-1

    /**
     * Features list as a string with comma delimiter .
     *
     * @return the string
     */
    String[] features() default {};

    /**
     * Can be used to format numbers and date/time property values
     */
    String format() default ""; //NON-NLS-1

    Class<? extends Format> formatter() default Format.class; //NON-NLS-1

    Class<? extends IPropertyValueTransformer> labelProvider() default IPropertyValueTransformer.class;

    Class<? extends IPropertyValueTransformer> valueTransformer() default IPropertyValueTransformer.class;

    Class<? extends IPropertyValueListProvider> listProvider() default IPropertyValueListProvider.class;

    Class<? extends IPropertyValueTransformer> valueRenderer() default IPropertyValueTransformer.class;

    Class<? extends IPropertyValueValidator> valueValidator() default IPropertyValueValidator.class;

    Class<? extends IPropertyValueValidator> visibleIf() default IPropertyValueValidator.class;

}
