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
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * {@link ImplementationFinder} that routes each call by a routing key.
 *
 * <p>
 * The key comes from one or more key sources, chained with {@link RouteBuilder#or() or()}
 * and tried in the order they were declared:
 * <ul>
 * <li>{@code byArgument} takes the first parameter of the invoked method whose declared
 * type is assignable to the given type, and derives the key from that argument;</li>
 * <li>{@code byRoutingKey} does the same, but only considers parameters annotated with
 * {@link RoutingKey @RoutingKey}, for when the type alone is ambiguous;</li>
 * <li>{@code byContext} gets the key from a {@link Supplier}, for example one that reads
 * the SLF4J MDC or another thread-bound context.</li>
 * </ul>
 * A source that yields no key (the method has no such parameter, the argument is
 * {@code null}, or the key is {@code null}) passes on to the next source. The first key
 * found decides the call: the implementation registered for it is returned, or the
 * default implementation if none is registered. Later sources are not consulted. When no
 * source yields a key, the default implementation is returned. Without a default,
 * {@code null} is returned and the router fails the call.
 *
 * <p>
 * A route can also be conditional, for example on a feature flag. Its conditions are
 * checked in order only when the call's key matches the route; the first condition that
 * holds selects its implementation, and the route's {@code orElse} implementation is used
 * when none does:
 *
 * <pre class="code">
 * RoutingImplementationFinder.builder(ShipperService.class)
 * 	.byArgument(Customer.class, Customer::carrier)
 * 	.route(Carrier.UPS)
 * 		.and(Customer.class, (customer) -&gt; flags.isEnabled("ups-v2", customer.tenantId())).to(upsV2)
 * 		.orElse(upsV1)
 * 	.route(Carrier.FEDEX, fedexShipperService)
 * 	.otherwise(uspsShipperService)
 * 	.build();
 * </pre>
 *
 * <pre class="code">
 * ImplementationFinder&lt;ShipperService&gt; finder = RoutingImplementationFinder.builder(ShipperService.class)
 * 	.byArgument(Customer.class, Customer::carrier)
 * 	.or().byArgument(Order.class, Order::carrier)
 * 	.or().byContext(() -&gt; carrierFromMdc())
 * 	.route(Carrier.UPS, upsShipperService)
 * 	.route(Carrier.FEDEX, fedexShipperService)
 * 	.otherwise(uspsShipperService)
 * 	.build();
 * </pre>
 *
 * <p>
 * Instances are immutable and thread-safe, provided the key extractors, suppliers and
 * conditions are.
 *
 * @param <T> the interface being routed
 * @author Joe Chambers
 * @since 0.1.0
 */
public final class RoutingImplementationFinder<T> implements ImplementationFinder<T> {

	private final List<KeySource> keySources;

	private final Map<Object, Route<T>> routes;

	private final @Nullable T defaultImplementation;

	private RoutingImplementationFinder(List<KeySource> keySources, Map<Object, Route<T>> routes,
			@Nullable T defaultImplementation) {
		this.keySources = List.copyOf(keySources);
		this.routes = Map.copyOf(routes);
		this.defaultImplementation = defaultImplementation;
	}

	/**
	 * Start building a finder for the given interface.
	 * @param <T> the interface being routed
	 * @param type the interface being routed
	 * @return a new builder
	 */
	public static <T> Builder<T> builder(Class<T> type) {
		Objects.requireNonNull(type, "type must not be null.");
		return new Builder<>(type);
	}

	@Override
	public @Nullable T findImplementation(Method method, @Nullable Object[] args) {
		for (KeySource keySource : this.keySources) {
			Object key = keySource.key(method, args);
			if (key != null) {
				Route<T> route = this.routes.get(key);
				return (route != null) ? route.implementation(method, args) : this.defaultImplementation;
			}
		}
		return this.defaultImplementation;
	}

	@Override
	public String toString() {
		return "RoutingImplementationFinder[keySources=" + this.keySources + ", routes=" + this.routes.keySet()
				+ ", default=" + (this.defaultImplementation != null) + "]";
	}

	/**
	 * Builder that selects the first key source.
	 *
	 * @param <T> the interface being routed
	 */
	public static final class Builder<T> {

		private final Class<T> type;

		private Builder(Class<T> type) {
			this.type = type;
		}

		/**
		 * Route by a key derived from the first argument whose declared parameter type is
		 * assignable to {@code argumentType}.
		 * @param <A> the routing argument type
		 * @param <K> the routing key type
		 * @param argumentType the type of the argument to route by
		 * @param keyExtractor derives the routing key from the argument; may return
		 * {@code null} to pass on to the next key source
		 * @return a builder for registering routes and further key sources
		 */
		public <A, K> RouteBuilder<T, K> byArgument(Class<A> argumentType,
				Function<? super A, ? extends @Nullable K> keyExtractor) {
			return new RouteBuilder<T, K>(this.type).addArgumentKeySource(ArgumentLocator.anyParameter(argumentType),
					keyExtractor);
		}

		/**
		 * Route by a key derived from the first parameter annotated with an unnamed
		 * {@link RoutingKey @RoutingKey} whose declared type is assignable to
		 * {@code argumentType}. Parameters without the annotation are ignored.
		 * @param <A> the routing argument type
		 * @param <K> the routing key type
		 * @param argumentType the type of the argument to route by
		 * @param keyExtractor derives the routing key from the argument; may return
		 * {@code null} to pass on to the next key source
		 * @return a builder for registering routes and further key sources
		 */
		public <A, K> RouteBuilder<T, K> byRoutingKey(Class<A> argumentType,
				Function<? super A, ? extends @Nullable K> keyExtractor) {
			return new RouteBuilder<T, K>(this.type)
				.addArgumentKeySource(ArgumentLocator.routingKeyParameter("", argumentType), keyExtractor);
		}

		/**
		 * Route by a key derived from the first parameter annotated with
		 * {@code @RoutingKey(name)} whose declared type is assignable to
		 * {@code argumentType}.
		 * @param <A> the routing argument type
		 * @param <K> the routing key type
		 * @param name the {@link RoutingKey#value() name} of the routing key
		 * @param argumentType the type of the argument to route by
		 * @param keyExtractor derives the routing key from the argument; may return
		 * {@code null} to pass on to the next key source
		 * @return a builder for registering routes and further key sources
		 */
		public <A, K> RouteBuilder<T, K> byRoutingKey(String name, Class<A> argumentType,
				Function<? super A, ? extends @Nullable K> keyExtractor) {
			return new RouteBuilder<T, K>(this.type)
				.addArgumentKeySource(ArgumentLocator.routingKeyParameter(name, argumentType), keyExtractor);
		}

		/**
		 * Route by a key from the current context, for example a value in the SLF4J MDC.
		 * The supplier is called on the thread that invokes the router.
		 * @param <K> the routing key type
		 * @param keySupplier supplies the routing key; may return {@code null} to pass on
		 * to the next key source
		 * @return a builder for registering routes and further key sources
		 */
		public <K> RouteBuilder<T, K> byContext(Supplier<? extends @Nullable K> keySupplier) {
			return new RouteBuilder<T, K>(this.type).addContextKeySource(keySupplier);
		}

	}

	/**
	 * Builder that registers the implementation for each routing key, the default
	 * implementation, and further key sources with {@link #or()}.
	 *
	 * @param <T> the interface being routed
	 * @param <K> the routing key type
	 */
	public static final class RouteBuilder<T, K> {

		private final Class<T> type;

		private final List<KeySource> keySources = new ArrayList<>();

		private final Map<Object, Route<T>> routes = new LinkedHashMap<>();

		private @Nullable T defaultImplementation;

		private RouteBuilder(Class<T> type) {
			this.type = type;
		}

		/**
		 * Add a key source that is tried when the previous ones yield no key.
		 * @return a builder for choosing the key source
		 */
		public KeySourceBuilder<T, K> or() {
			return new KeySourceBuilder<>(this);
		}

		/**
		 * Route calls whose key equals {@code key} to {@code implementation}.
		 * @param key the routing key
		 * @param implementation the implementation to use for the key
		 * @return this builder
		 * @throws IllegalArgumentException if a route is already registered for the key,
		 * or the implementation doesn't implement the interface
		 */
		public RouteBuilder<T, K> route(K key, T implementation) {
			return route(key).orElse(implementation);
		}

		/**
		 * Start a conditional route for calls whose key equals {@code key}. Add
		 * conditions with {@link ConditionalRouteBuilder#and(BooleanSupplier) and(...)}
		 * and finish the route with {@link ConditionalRouteBuilder#orElse(Object)
		 * orElse(...)}.
		 * @param key the routing key
		 * @return a builder for the route's conditions
		 * @throws IllegalArgumentException if a route is already registered for the key
		 */
		public ConditionalRouteBuilder<T, K> route(K key) {
			Objects.requireNonNull(key, "key must not be null.");
			checkNoRoute(key);
			return new ConditionalRouteBuilder<>(this, key);
		}

		/**
		 * Use {@code implementation} when no key source yields a key, or the key has no
		 * route.
		 * @param implementation the default implementation
		 * @return this builder
		 * @throws IllegalArgumentException if the implementation doesn't implement the
		 * interface
		 */
		public RouteBuilder<T, K> otherwise(T implementation) {
			checkImplementation(implementation);
			this.defaultImplementation = implementation;
			return this;
		}

		/**
		 * Build the finder.
		 * @return the finder
		 * @throws IllegalStateException if no routes and no default implementation were
		 * registered
		 */
		public RoutingImplementationFinder<T> build() {
			if (this.routes.isEmpty() && this.defaultImplementation == null) {
				throw new IllegalStateException("At least one route or a default implementation is required.");
			}
			return new RoutingImplementationFinder<>(this.keySources, this.routes, this.defaultImplementation);
		}

		private <A> RouteBuilder<T, K> addArgumentKeySource(ArgumentLocator locator,
				Function<? super A, ? extends @Nullable K> keyExtractor) {
			Objects.requireNonNull(keyExtractor, "keyExtractor must not be null.");
			@SuppressWarnings("unchecked")
			Function<Object, ? extends @Nullable K> extractor = (argument) -> keyExtractor.apply((A) argument);
			this.keySources.add(new ArgumentKeySource(locator, extractor));
			return this;
		}

		private RouteBuilder<T, K> addContextKeySource(Supplier<? extends @Nullable K> keySupplier) {
			Objects.requireNonNull(keySupplier, "keySupplier must not be null.");
			this.keySources.add(new ContextKeySource(keySupplier));
			return this;
		}

		private RouteBuilder<T, K> addRoute(K key, Route<T> route) {
			checkNoRoute(key);
			this.routes.put(key, route);
			return this;
		}

		private void checkNoRoute(K key) {
			if (this.routes.containsKey(key)) {
				throw new IllegalArgumentException("A route is already registered for key: " + key);
			}
		}

		private void checkImplementation(T implementation) {
			Objects.requireNonNull(implementation, "implementation must not be null.");
			if (!this.type.isInstance(implementation)) {
				throw new IllegalArgumentException(
						implementation.getClass().getName() + " does not implement " + this.type.getName());
			}
		}

	}

	/**
	 * Builder, returned by {@link RouteBuilder#or()}, that chooses the next key source.
	 * Each source must produce the same key type as the first.
	 *
	 * @param <T> the interface being routed
	 * @param <K> the routing key type
	 */
	public static final class KeySourceBuilder<T, K> {

		private final RouteBuilder<T, K> routeBuilder;

		private KeySourceBuilder(RouteBuilder<T, K> routeBuilder) {
			this.routeBuilder = routeBuilder;
		}

		/**
		 * Route by a key derived from the first argument whose declared parameter type is
		 * assignable to {@code argumentType}.
		 * @param <A> the routing argument type
		 * @param argumentType the type of the argument to route by
		 * @param keyExtractor derives the routing key from the argument; may return
		 * {@code null} to pass on to the next key source
		 * @return the route builder
		 */
		public <A> RouteBuilder<T, K> byArgument(Class<A> argumentType,
				Function<? super A, ? extends @Nullable K> keyExtractor) {
			return this.routeBuilder.addArgumentKeySource(ArgumentLocator.anyParameter(argumentType), keyExtractor);
		}

		/**
		 * Route by a key derived from the first parameter annotated with an unnamed
		 * {@link RoutingKey @RoutingKey} whose declared type is assignable to
		 * {@code argumentType}. Parameters without the annotation are ignored.
		 * @param <A> the routing argument type
		 * @param argumentType the type of the argument to route by
		 * @param keyExtractor derives the routing key from the argument; may return
		 * {@code null} to pass on to the next key source
		 * @return the route builder
		 */
		public <A> RouteBuilder<T, K> byRoutingKey(Class<A> argumentType,
				Function<? super A, ? extends @Nullable K> keyExtractor) {
			return this.routeBuilder.addArgumentKeySource(ArgumentLocator.routingKeyParameter("", argumentType),
					keyExtractor);
		}

		/**
		 * Route by a key derived from the first parameter annotated with
		 * {@code @RoutingKey(name)} whose declared type is assignable to
		 * {@code argumentType}.
		 * @param <A> the routing argument type
		 * @param name the {@link RoutingKey#value() name} of the routing key
		 * @param argumentType the type of the argument to route by
		 * @param keyExtractor derives the routing key from the argument; may return
		 * {@code null} to pass on to the next key source
		 * @return the route builder
		 */
		public <A> RouteBuilder<T, K> byRoutingKey(String name, Class<A> argumentType,
				Function<? super A, ? extends @Nullable K> keyExtractor) {
			return this.routeBuilder.addArgumentKeySource(ArgumentLocator.routingKeyParameter(name, argumentType),
					keyExtractor);
		}

		/**
		 * Route by a key from the current context, for example a value in the SLF4J MDC.
		 * The supplier is called on the thread that invokes the router.
		 * @param keySupplier supplies the routing key; may return {@code null} to pass on
		 * to the next key source
		 * @return the route builder
		 */
		public RouteBuilder<T, K> byContext(Supplier<? extends @Nullable K> keySupplier) {
			return this.routeBuilder.addContextKeySource(keySupplier);
		}

	}

	/**
	 * Builder, returned by {@link RouteBuilder#route(Object)}, for a route whose
	 * implementation depends on conditions such as feature flags. Conditions are checked
	 * in order, only for calls whose key matches the route, and on every such call.
	 *
	 * @param <T> the interface being routed
	 * @param <K> the routing key type
	 */
	public static final class ConditionalRouteBuilder<T, K> {

		private final RouteBuilder<T, K> routeBuilder;

		private final K key;

		private final List<Condition> conditions = new ArrayList<>();

		private final List<T> implementations = new ArrayList<>();

		private ConditionalRouteBuilder(RouteBuilder<T, K> routeBuilder, K key) {
			this.routeBuilder = routeBuilder;
			this.key = key;
		}

		/**
		 * Add a condition that doesn't depend on the call, for example a global feature
		 * flag. It is evaluated on every call, so changes take effect immediately.
		 * @param condition the condition
		 * @return a builder for the implementation to use when the condition holds
		 */
		public ConditionTarget<T, K> and(BooleanSupplier condition) {
			Objects.requireNonNull(condition, "condition must not be null.");
			return new ConditionTarget<>(this, new ContextCondition(condition));
		}

		/**
		 * Add a condition on the first argument whose declared parameter type is
		 * assignable to {@code argumentType}, for example a feature flag for the
		 * customer's tenant. The condition doesn't hold when the call has no such
		 * argument or the argument is {@code null}.
		 * @param <A> the argument type
		 * @param argumentType the type of the argument to test
		 * @param condition the condition
		 * @return a builder for the implementation to use when the condition holds
		 */
		public <A> ConditionTarget<T, K> and(Class<A> argumentType, Predicate<? super A> condition) {
			return addArgumentCondition(ArgumentLocator.anyParameter(argumentType), condition);
		}

		/**
		 * Add a condition on the first parameter annotated with an unnamed
		 * {@link RoutingKey @RoutingKey} whose declared type is assignable to
		 * {@code argumentType}. The condition doesn't hold when the call has no such
		 * argument or the argument is {@code null}.
		 * @param <A> the argument type
		 * @param argumentType the type of the argument to test
		 * @param condition the condition
		 * @return a builder for the implementation to use when the condition holds
		 */
		public <A> ConditionTarget<T, K> andRoutingKey(Class<A> argumentType, Predicate<? super A> condition) {
			return addArgumentCondition(ArgumentLocator.routingKeyParameter("", argumentType), condition);
		}

		/**
		 * Add a condition on the first parameter annotated with {@code @RoutingKey(name)}
		 * whose declared type is assignable to {@code argumentType}, for example
		 * {@code @RoutingKey("tenant") String tenantId}. The condition doesn't hold when
		 * the call has no such argument or the argument is {@code null}.
		 * @param <A> the argument type
		 * @param name the {@link RoutingKey#value() name} of the routing key
		 * @param argumentType the type of the argument to test
		 * @param condition the condition
		 * @return a builder for the implementation to use when the condition holds
		 */
		public <A> ConditionTarget<T, K> andRoutingKey(String name, Class<A> argumentType,
				Predicate<? super A> condition) {
			return addArgumentCondition(ArgumentLocator.routingKeyParameter(name, argumentType), condition);
		}

		/**
		 * Finish the route: use {@code implementation} when none of the conditions hold.
		 * @param implementation the implementation to use when no condition holds
		 * @return the route builder
		 * @throws IllegalArgumentException if the implementation doesn't implement the
		 * interface
		 */
		public RouteBuilder<T, K> orElse(T implementation) {
			this.routeBuilder.checkImplementation(implementation);
			return this.routeBuilder.addRoute(this.key,
					new Route<>(this.conditions, this.implementations, implementation));
		}

		@SuppressWarnings("unchecked")
		private <A> ConditionTarget<T, K> addArgumentCondition(ArgumentLocator locator,
				Predicate<? super A> condition) {
			Objects.requireNonNull(condition, "condition must not be null.");
			return new ConditionTarget<>(this,
					new ArgumentCondition(locator, (argument) -> condition.test((A) argument)));
		}

		private ConditionalRouteBuilder<T, K> addCondition(Condition condition, T implementation) {
			this.routeBuilder.checkImplementation(implementation);
			this.conditions.add(condition);
			this.implementations.add(implementation);
			return this;
		}

	}

	/**
	 * Builder, returned by {@link ConditionalRouteBuilder#and(BooleanSupplier) and(...)},
	 * that sets the implementation used when the condition holds.
	 *
	 * @param <T> the interface being routed
	 * @param <K> the routing key type
	 */
	public static final class ConditionTarget<T, K> {

		private final ConditionalRouteBuilder<T, K> routeBuilder;

		private final Condition condition;

		private ConditionTarget(ConditionalRouteBuilder<T, K> routeBuilder, Condition condition) {
			this.routeBuilder = routeBuilder;
			this.condition = condition;
		}

		/**
		 * Use {@code implementation} when the condition holds.
		 * @param implementation the implementation
		 * @return the route's builder, for further conditions or {@code orElse}
		 * @throws IllegalArgumentException if the implementation doesn't implement the
		 * interface
		 */
		public ConditionalRouteBuilder<T, K> to(T implementation) {
			return this.routeBuilder.addCondition(this.condition, implementation);
		}

	}

	/**
	 * A route: implementations guarded by conditions, checked in order, and the
	 * implementation used when none holds.
	 */
	private static final class Route<T> {

		private final List<Condition> conditions;

		private final List<T> implementations;

		private final T fallback;

		Route(List<Condition> conditions, List<T> implementations, T fallback) {
			this.conditions = List.copyOf(conditions);
			this.implementations = List.copyOf(implementations);
			this.fallback = fallback;
		}

		T implementation(Method method, @Nullable Object[] args) {
			for (int i = 0; i < this.conditions.size(); i++) {
				if (this.conditions.get(i).test(method, args)) {
					return this.implementations.get(i);
				}
			}
			return this.fallback;
		}

	}

	/**
	 * Condition on a call.
	 */
	private interface Condition {

		boolean test(Method method, @Nullable Object[] args);

	}

	/**
	 * Condition that doesn't depend on the call.
	 */
	private static final class ContextCondition implements Condition {

		private final BooleanSupplier condition;

		ContextCondition(BooleanSupplier condition) {
			this.condition = condition;
		}

		@Override
		public boolean test(Method method, @Nullable Object[] args) {
			return this.condition.getAsBoolean();
		}

	}

	/**
	 * Condition on a typed argument; doesn't hold when the call has no such argument.
	 */
	private static final class ArgumentCondition implements Condition {

		private final ArgumentLocator locator;

		private final Predicate<Object> condition;

		ArgumentCondition(ArgumentLocator locator, Predicate<Object> condition) {
			this.locator = locator;
			this.condition = condition;
		}

		@Override
		public boolean test(Method method, @Nullable Object[] args) {
			Object argument = this.locator.find(method, args);
			return argument != null && this.condition.test(argument);
		}

	}

	/**
	 * Yields the routing key for a call, or {@code null} to pass on to the next source.
	 */
	private interface KeySource {

		@Nullable Object key(Method method, @Nullable Object[] args);

	}

	/**
	 * Key source that reads a typed argument.
	 */
	private static final class ArgumentKeySource implements KeySource {

		private final ArgumentLocator locator;

		private final Function<Object, ?> keyExtractor;

		ArgumentKeySource(ArgumentLocator locator, Function<Object, ?> keyExtractor) {
			this.locator = locator;
			this.keyExtractor = keyExtractor;
		}

		@Override
		public @Nullable Object key(Method method, @Nullable Object[] args) {
			Object argument = this.locator.find(method, args);
			return (argument != null) ? this.keyExtractor.apply(argument) : null;
		}

		@Override
		public String toString() {
			return this.locator.toString();
		}

	}

	/**
	 * Key source that asks a supplier, for example one reading the SLF4J MDC.
	 */
	private static final class ContextKeySource implements KeySource {

		private final Supplier<?> keySupplier;

		ContextKeySource(Supplier<?> keySupplier) {
			this.keySupplier = keySupplier;
		}

		@Override
		public @Nullable Object key(Method method, @Nullable Object[] args) {
			return this.keySupplier.get();
		}

		@Override
		public String toString() {
			return "context";
		}

	}

	/**
	 * Finds the first argument whose declared parameter type is assignable to a type,
	 * optionally only among parameters with a given {@link RoutingKey @RoutingKey}. The
	 * argument's position is worked out once per method and cached.
	 */
	private static final class ArgumentLocator {

		private static final int NO_ARGUMENT = -1;

		private final Class<?> argumentType;

		private final @Nullable String routingKeyName;

		private final Map<Method, Integer> argumentIndexCache = new ConcurrentHashMap<>();

		private ArgumentLocator(Class<?> argumentType, @Nullable String routingKeyName) {
			this.argumentType = Objects.requireNonNull(argumentType, "argumentType must not be null.");
			this.routingKeyName = routingKeyName;
		}

		static ArgumentLocator anyParameter(Class<?> argumentType) {
			return new ArgumentLocator(argumentType, null);
		}

		static ArgumentLocator routingKeyParameter(String name, Class<?> argumentType) {
			Objects.requireNonNull(name, "name must not be null.");
			return new ArgumentLocator(argumentType, name);
		}

		@Nullable Object find(Method method, @Nullable Object[] args) {
			int index = this.argumentIndexCache.computeIfAbsent(method, this::findArgumentIndex);
			return (index != NO_ARGUMENT) ? args[index] : null;
		}

		private int findArgumentIndex(Method method) {
			Parameter[] parameters = method.getParameters();
			for (int i = 0; i < parameters.length; i++) {
				if (this.argumentType.isAssignableFrom(parameters[i].getType()) && isRoutingKey(parameters[i])) {
					return i;
				}
			}
			return NO_ARGUMENT;
		}

		private boolean isRoutingKey(Parameter parameter) {
			if (this.routingKeyName == null) {
				return true;
			}
			RoutingKey routingKey = parameter.getAnnotation(RoutingKey.class);
			return routingKey != null && routingKey.value().equals(this.routingKeyName);
		}

		@Override
		public String toString() {
			if (this.routingKeyName == null) {
				return "argument " + this.argumentType.getName();
			}
			String name = this.routingKeyName.isEmpty() ? "" : "(\"" + this.routingKeyName + "\")";
			return "@RoutingKey" + name + " " + this.argumentType.getName();
		}

	}

}
