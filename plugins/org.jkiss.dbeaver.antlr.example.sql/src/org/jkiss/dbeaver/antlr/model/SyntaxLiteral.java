package org.jkiss.dbeaver.antlr.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SyntaxLiteral {
    String name();
    String xstring() default "";
}
