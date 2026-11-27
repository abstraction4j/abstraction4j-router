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

package dev.abstraction4j.router.core;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * Strategy that selects which implementation of {@code T} should handle a call made
 * through an abstraction router.
 *
 * <p>
 * The router calls the finder once per invocation, for every method of {@code T}
 * including default methods, but never for {@code equals}, {@code hashCode} or
 * {@code toString}, which the router handles itself. Finders may be called concurrently
 * and must be thread-safe.
 *
 * <p>
 * For routing on a typed argument, see {@link RoutingImplementationFinder}.
 *
 * @param <T> the interface being routed
 * @author Joe Chambers
 * @since 0.1.0
 */
@FunctionalInterface
public interface ImplementationFinder<T> {

	/**
	 * Find the implementation that should handle the given call. The returned
	 * implementation's method is then invoked with the same arguments. If this method
	 * returns {@code null}, the router throws an {@link IllegalStateException}.
	 * @param method the interface method being invoked
	 * @param args the arguments of the invocation; never {@code null}, and empty for
	 * methods without parameters. Elements are {@code null} where the caller passed
	 * {@code null}. For a Kotlin {@code suspend} function, the last element is the
	 * coroutine's {@code Continuation}.
	 * @return the implementation to invoke, or {@code null} if none applies
	 */
	@Nullable T findImplementation(Method method, @Nullable Object[] args);

}
