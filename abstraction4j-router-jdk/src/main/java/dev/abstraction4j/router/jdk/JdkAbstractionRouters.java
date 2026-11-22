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

import java.lang.reflect.Proxy;
import java.util.Objects;

import dev.abstraction4j.router.core.ImplementationFinder;

/**
 * Factory for abstraction routers built on {@link Proxy JDK dynamic proxies}.
 *
 * <p>
 * Each call on the router asks the {@link ImplementationFinder} for an implementation and
 * invokes the same method on it. Exceptions thrown by the implementation reach the caller
 * unchanged. If the finder returns {@code null}, the call fails with an
 * {@link IllegalStateException}. {@code equals} and {@code hashCode} use the router's
 * identity and {@code toString} describes the router; none of them reach the finder.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
public final class JdkAbstractionRouters {

	private JdkAbstractionRouters() {
	}

	/**
	 * Create a router for the given interface, defined in the interface's class loader.
	 * @param <T> the interface being routed
	 * @param type the interface to route; may be non-public
	 * @param finder the finder that selects the implementation for each call
	 * @return the router
	 * @throws IllegalArgumentException if {@code type} is not an interface
	 */
	public static <T> T routerFor(Class<T> type, ImplementationFinder<T> finder) {
		Objects.requireNonNull(type, "type must not be null.");
		ClassLoader classLoader = type.getClassLoader();
		return routerFor((classLoader != null) ? classLoader : JdkAbstractionRouters.class.getClassLoader(), type,
				finder);
	}

	/**
	 * Create a router for the given interface, defined in the given class loader.
	 * @param <T> the interface being routed
	 * @param classLoader the class loader to define the proxy class in; it must be able
	 * to see the interface
	 * @param type the interface to route
	 * @param finder the finder that selects the implementation for each call
	 * @return the router
	 * @throws IllegalArgumentException if {@code type} is not an interface
	 */
	public static <T> T routerFor(ClassLoader classLoader, Class<T> type, ImplementationFinder<T> finder) {
		Objects.requireNonNull(classLoader, "classLoader must not be null.");
		Objects.requireNonNull(type, "type must not be null.");
		Objects.requireNonNull(finder, "finder must not be null.");
		if (!type.isInterface()) {
			throw new IllegalArgumentException(type.getName() + " is not an interface.");
		}
		DynamicJdkAbstractionInvocationHandler<T> handler = new DynamicJdkAbstractionInvocationHandler<>(type, finder);
		return type.cast(Proxy.newProxyInstance(classLoader, new Class<?>[] { type }, handler));
	}

}
