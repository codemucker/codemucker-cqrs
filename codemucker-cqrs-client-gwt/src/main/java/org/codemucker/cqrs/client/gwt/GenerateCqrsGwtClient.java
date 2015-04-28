/*
 * Copyright 2011 Bert van Brakel
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
package org.codemucker.cqrs.client.gwt;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Validator;

import org.codemucker.cqrs.MessageException;
import org.codemucker.cqrs.client.CqrsRequestHandle;
import org.codemucker.jpattern.generate.GeneratorOptions;
import org.codemucker.jpattern.generate.Dependency;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@GeneratorOptions("org.codemucker.cqrs.generator.GwtClientGenerator")
public @interface GenerateCqrsGwtClient {

    /**
     * If enabled keep this pattern in sync with changes after the initial
     * generation. Defaults to true.
     */
    boolean keepInSync() default true;

    /**
     * Generate the asynchronous request methods. Defaults to true.
     */
    boolean generateAsync() default true;

    /**
     * Generate the synchronous request methods. Defaults to true;
     */
    boolean generateSync() default true;

    /**
     * Whether to generate a matching interface for this service.. Defaults to
     * true.
     */
    boolean generateInterface() default true;

    /**
     * Whether to also add the builder methods to the interface. Defaults to false.
     */
    boolean generateInterfaceBuildMethods() default false;

    /**
     * If true, use a {@link Validator} to validate requests before sending. Default true.
     */
    boolean validateRequests() default true;

        
    /**
     * The class to extend the generated service from. Default is empty to use the default.
     */
    Class<?> serviceBaseClass() default AbstractCqrsGwtClient.class;

    Class<?> serviceException() default MessageException.class;

    Class<?> asyncRequestHandle() default CqrsRequestHandle.class;
    
    /**
     * The name of the service to generate, else updates the node this annotation is attached to. Default is empty.
     */
    String serviceName() default "";

    /**
     * If interface generation is enabled, the name of the interface. If not set
     * calculates one. Default is empty.
     */
    String serviceInterfaceName() default "";

    /**
     * The visibility of the the request conversion methods. . Defaults to
     * 'public'.
     */
    String serviceMethodVisibility() default "public";

    /**
     * If set filter the dependencies scanned for request beans
     */
    Dependency[] requestBeanDependencies() default {};

    /**
     * The packages to search in for the request beans. Defaults to search all.
     */
    String[] requestBeanPackages() default {};

    /**
     * The pattern for finding the request command/query beans
     */
    String[] requestBeanNames() default {};
    
    boolean scanSources() default true;
    boolean scanDependencies() default true;
    
}
