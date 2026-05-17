package br.ufrn.middleware.annotations;

import br.ufrn.middleware.lifecycle.LifecycleType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Lifecycle {
    LifecycleType value() default LifecycleType.STATIC_INSTANCE;
}
