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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the parameter that {@link RoutingImplementationFinder} routes by when the key
 * source is declared with {@code byRoutingKey}. Use it when a parameter's type alone is
 * ambiguous, for example a customer ID passed as a {@link String} next to other strings.
 *
 * <pre class="code">
 * interface OrderService {
 *
 * 	List&lt;Order&gt; getOrders(&#64;RoutingKey String customerId);
 *
 * 	Order getOrder(String orderId, &#64;RoutingKey("customer") UUID customerId);
 *
 * }
 * </pre>
 *
 * <p>
 * Annotate the parameter on the routed interface: the router sees the interface's method,
 * not the implementation's.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RoutingKey {

	/**
	 * Optional name, for methods with several routing keys.
	 * {@code byRoutingKey(type, ...)} matches only unnamed routing keys, and
	 * {@code byRoutingKey(name, type, ...)} only routing keys with that name.
	 * @return the name, or an empty string for an unnamed routing key
	 */
	String value() default "";

}
