/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.abstraction4j.router.spring;

import java.lang.reflect.Method;
import java.util.Objects;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;

import dev.abstraction4j.router.core.ImplementationFinder;

/**
 * {@link MethodInterceptor} that routes each call on a Spring AOP proxy to the
 * implementation chosen by an {@link ImplementationFinder}.
 *
 * @param <T> the interface being routed
 * @author Joe Chambers
 */
final class DynamicSpringAbstractionHandler<T> implements MethodInterceptor {

	private static final Object[] NO_ARGUMENTS = {};

	private final ImplementationFinder<T> finder;

	private final String description;

	DynamicSpringAbstractionHandler(Class<T> type, ImplementationFinder<T> finder) {
		this.finder = finder;
		this.description = "Abstraction router for " + type.getName() + " using " + finder;
	}

	@Override
	public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		// Spring answers equals and hashCode itself; toString would otherwise reach the
		// finder.
		if (AopUtils.isToStringMethod(method)) {
			return this.description;
		}
		// Spring passes an empty array for methods without parameters, but another
		// advisor on the router can replace the arguments with null.
		@Nullable Object[] args = Objects.requireNonNullElse(invocation.getArguments(), NO_ARGUMENTS);
		T target = this.finder.findImplementation(method, args);
		Assert.state(target != null, () -> "Unable to find implementation for method: <" + method + ">");
		// Makes non-public interface methods accessible and unwraps
		// InvocationTargetException.
		return AopUtils.invokeJoinpointUsingReflection(target, method, args);
	}

	@Override
	public String toString() {
		return this.description;
	}

}
