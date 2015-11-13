package ru.osslabs.modules.report;

import javax.ejb.Startup;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by ikuchmin on 10.11.15.
 */
@Inherited
@InterceptorBinding
@Retention(RUNTIME)
@Target({TYPE})
public @interface ReportFactoryAnnotation {
    Class<? extends ReportDiscoveryImpl> discovery() default ReportDiscoveryImpl.class;
}