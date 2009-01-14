/*
 * Copyright (c) 2007, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.repository.object.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on methods that intercept another method invocation. Placed on methods
 * that take an {@link InvocationContext} as the parameter. This method will be
 * called around the original method invocation.
 * <ul>
 * <li><b>name</b> regular expression that must match the entire method name.</li>
 * <li><b>parameterType</b> list of parameterTypes were each given type
 * {@link Class#isAssignableFrom(Class)} the method parameter type</li>
 * <li><b>returnType</b> the method return type must be
 * {@link Class#isAssignableFrom(Class)} the given type</li>
 * <li><b>declaringClass</b> the method must be declared in the given class or
 * one of its super classes</li>
 * <li><b>conditionMethod</b> the name of a static method declared in the same
 * class, with a return type of boolean and a parameter of type Method. Methods
 * will only be intercepted if the given method returns true.</li>
 * </ul>
 * 
 * @author James Leigh
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface intercepts {
	String method() default "";

	int argc() default -1;

	Class<?>[] parameters() default { intercepts.class };

	Class<?> returns() default intercepts.class;

	Class<?> declaring() default intercepts.class;

	String conditional() default "";
}
