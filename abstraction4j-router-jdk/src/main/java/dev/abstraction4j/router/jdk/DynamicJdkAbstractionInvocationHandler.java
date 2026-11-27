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

package dev.abstraction4j.router.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import dev.abstraction4j.router.core.ImplementationFinder;

/**
 * {@link InvocationHandler} that routes each call on a JDK dynamic proxy to the
 * implementation chosen by an {@link ImplementationFinder}.
 *
 * @param <T> the interface being routed
 * @author Joe Chambers
 */
final class DynamicJdkAbstractionInvocationHandler<T> implements InvocationHandler {

	private static final Object[] NO_ARGUMENTS = {};

	private final ImplementationFinder<T> finder;

	private final String description;

	DynamicJdkAbstractionInvocationHandler(Class<T> type, ImplementationFinder<T> finder) {
		this.finder = finder;
		this.description = "Abstraction router for " + type.getName() + " using " + finder;
	}

	@Override
	public @Nullable Object invoke(Object proxy, Method method, @Nullable Object @Nullable [] args) throws Throwable {
		@Nullable Object[] arguments = (args != null) ? args : NO_ARGUMENTS;
		if (isEqualsMethod(method)) {
			return proxy == arguments[0];
		}
		if (isHashCodeMethod(method)) {
			return System.identityHashCode(proxy);
		}
		if (isToStringMethod(method)) {
			return this.description;
		}
		T target = this.finder.findImplementation(method, arguments);
		if (target == null) {
			throw new IllegalStateException("Unable to find implementation for method: <" + method + ">");
		}
		return invokeTarget(target, method, arguments);
	}

	private static @Nullable Object invokeTarget(Object target, Method method, @Nullable Object[] arguments)
			throws Throwable {
		// Methods of non-public interfaces aren't accessible from this package.
		if (!method.canAccess(target)) {
			method.trySetAccessible();
		}
		try {
			return method.invoke(target, arguments);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}

	private static boolean isEqualsMethod(Method method) {
		return method.getName().equals("equals") && method.getParameterCount() == 1
				&& method.getParameterTypes()[0] == Object.class;
	}

	private static boolean isHashCodeMethod(Method method) {
		return method.getName().equals("hashCode") && method.getParameterCount() == 0;
	}

	private static boolean isToStringMethod(Method method) {
		return method.getName().equals("toString") && method.getParameterCount() == 0;
	}

	@Override
	public String toString() {
		return this.description;
	}

}
