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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RoutingImplementationFinder}.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
class RoutingImplementationFinderTests {

	private final ShipperService ups = (customer, address) -> "ups";

	private final ShipperService fedex = (customer, address) -> "fedex";

	private final ShipperService usps = (customer, address) -> "usps";

	private final ShipperService upsV2 = (customer, address) -> "ups-v2";

	private final OrderService peopleSoft = mock(OrderService.class);

	private final OrderService oracle = mock(OrderService.class);

	@Test
	void findImplementation_routesByKeyFromTypedArgument() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().otherwise(this.usps).build();

		assertThat(finder.findImplementation(shipMethod(), args(new Customer("ups"), "1 Main St"))).isSameAs(this.ups);
		assertThat(finder.findImplementation(shipMethod(), args(new Customer("fedex"), "2 Main St")))
			.isSameAs(this.fedex);
	}

	@Test
	void findImplementation_unknownKey_returnsDefault() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().otherwise(this.usps).build();

		ShipperService result = finder.findImplementation(shipMethod(), args(new Customer("dhl"), "1 Main St"));

		assertThat(result).isSameAs(this.usps);
	}

	@Test
	void findImplementation_unknownKeyWithoutDefault_returnsNull() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().build();

		ShipperService result = finder.findImplementation(shipMethod(), args(new Customer("dhl"), "1 Main St"));

		assertThat(result).isNull();
	}

	@Test
	void findImplementation_nullArgumentOrKey_returnsDefault() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().otherwise(this.usps).build();

		assertThat(finder.findImplementation(shipMethod(), args(null, "1 Main St"))).isSameAs(this.usps);
		assertThat(finder.findImplementation(shipMethod(), args(new Customer(null), "1 Main St"))).isSameAs(this.usps);
	}

	@Test
	void findImplementation_methodWithoutRoutingParameter_returnsDefault() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().otherwise(this.usps).build();
		Method method = ShipperService.class.getMethod("carriers");

		ShipperService result = finder.findImplementation(method, new Object[0]);

		assertThat(result).isSameAs(this.usps);
	}

	@Test
	void findImplementation_parameterOfSubtype_isUsedForRouting() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().build();
		Method method = ShipperService.class.getMethod("shipPriority", String.class, PriorityCustomer.class);

		ShipperService result = finder.findImplementation(method, args("1 Main St", new PriorityCustomer("fedex")));

		assertThat(result).isSameAs(this.fedex);
	}

	@Test
	void route_duplicateKey_throwsIllegalArgumentException() {
		RoutingImplementationFinder.RouteBuilder<ShipperService, String> builder = routeByCarrier();

		assertThatIllegalArgumentException().isThrownBy(() -> builder.route("ups", this.usps))
			.withMessage("A route is already registered for key: ups");
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void route_implementationOfWrongType_throwsIllegalArgumentException() {
		RoutingImplementationFinder.RouteBuilder builder = routeByCarrier();

		assertThatIllegalArgumentException().isThrownBy(() -> builder.route("dhl", "not a shipper"))
			.withMessageContaining("does not implement " + ShipperService.class.getName());
	}

	@Test
	void build_withoutRoutesOrDefault_throwsIllegalStateException() {
		RoutingImplementationFinder.RouteBuilder<ShipperService, String> builder = RoutingImplementationFinder
			.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier);

		assertThatIllegalStateException().isThrownBy(builder::build);
	}

	@Test
	void build_withOnlyDefault_routesEveryCallToDefault() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = RoutingImplementationFinder.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier)
			.otherwise(this.usps)
			.build();

		ShipperService result = finder.findImplementation(shipMethod(), args(new Customer("ups"), "1 Main St"));

		assertThat(result).isSameAs(this.usps);
	}

	@Test
	void toString_describesKeySourcesRoutesAndDefault() {
		RoutingImplementationFinder<ShipperService> withDefault = routeByCarrier().or()
			.byContext(() -> "ups")
			.otherwise(this.usps)
			.build();
		RoutingImplementationFinder<ShipperService> withoutDefault = routeByCarrier().build();

		assertThat(withDefault.toString())
			.startsWith("RoutingImplementationFinder[keySources=[argument " + Customer.class.getName() + ", context]")
			.contains("ups", "fedex")
			.endsWith("default=true]");
		assertThat(withoutDefault.toString()).endsWith("default=false]");
	}

	@Test
	void laterArgumentSource_isUsedWhenEarlierArgumentIsMissing() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().or()
			.byArgument(Order.class, Order::carrier)
			.otherwise(this.usps)
			.build();

		ShipperService result = finder.findImplementation(shipOrderMethod(), args(new Order("fedex")));

		assertThat(result).isSameAs(this.fedex);
	}

	@Test
	void contextSource_isUsedWhenNoArgumentYieldsKey() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().or()
			.byArgument(Order.class, Order::carrier)
			.or()
			.byContext(() -> "fedex")
			.otherwise(this.usps)
			.build();
		Method method = ShipperService.class.getMethod("carriers");

		assertThat(finder.findImplementation(method, new Object[0])).isSameAs(this.fedex);
		assertThat(finder.findImplementation(shipMethod(), args(null, "1 Main St"))).isSameAs(this.fedex);
		assertThat(finder.findImplementation(shipOrderMethod(), args(new Order(null)))).isSameAs(this.fedex);
	}

	@Test
	void earlierKeySource_takesPrecedenceOverLaterOnes() throws Exception {
		AtomicInteger contextCalls = new AtomicInteger();
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().or().byContext(() -> {
			contextCalls.incrementAndGet();
			return "fedex";
		}).build();

		ShipperService result = finder.findImplementation(shipMethod(), args(new Customer("ups"), "1 Main St"));

		assertThat(result).isSameAs(this.ups);
		assertThat(contextCalls).hasValue(0);
	}

	@Test
	void keyWithoutRoute_usesDefaultWithoutTryingLaterSources() throws Exception {
		AtomicInteger contextCalls = new AtomicInteger();
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().or().byContext(() -> {
			contextCalls.incrementAndGet();
			return "fedex";
		}).otherwise(this.usps).build();

		ShipperService result = finder.findImplementation(shipMethod(), args(new Customer("dhl"), "1 Main St"));

		assertThat(result).isSameAs(this.usps);
		assertThat(contextCalls).hasValue(0);
	}

	@Test
	void noKeySourceYieldsKey_usesDefault() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = routeByCarrier().or()
			.byContext(() -> null)
			.otherwise(this.usps)
			.build();

		ShipperService result = finder.findImplementation(shipOrderMethod(), args(new Order("fedex")));

		assertThat(result).isSameAs(this.usps);
	}

	@Test
	void byContext_routesWithoutArguments() throws Exception {
		AtomicReference<String> context = new AtomicReference<>("fedex");
		RoutingImplementationFinder<ShipperService> finder = RoutingImplementationFinder.builder(ShipperService.class)
			.byContext(context::get)
			.route("ups", this.ups)
			.route("fedex", this.fedex)
			.build();
		Method method = ShipperService.class.getMethod("carriers");

		ShipperService first = finder.findImplementation(method, new Object[0]);
		context.set("ups");
		ShipperService second = finder.findImplementation(method, new Object[0]);

		assertThat(first).isSameAs(this.fedex);
		assertThat(second).isSameAs(this.ups);
	}

	@Test
	void conditionalRoute_followsFlagAtCallTime() throws Exception {
		AtomicBoolean upsV2Enabled = new AtomicBoolean(false);
		RoutingImplementationFinder<ShipperService> finder = RoutingImplementationFinder.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier)
			.route("ups")
			.and(upsV2Enabled::get)
			.to(this.upsV2)
			.orElse(this.ups)
			.otherwise(this.usps)
			.build();
		Object[] args = args(new Customer("ups"), "1 Main St");

		ShipperService beforeFlip = finder.findImplementation(shipMethod(), args);
		upsV2Enabled.set(true);
		ShipperService afterFlip = finder.findImplementation(shipMethod(), args);

		assertThat(beforeFlip).isSameAs(this.ups);
		assertThat(afterFlip).isSameAs(this.upsV2);
	}

	@Test
	void argumentCondition_testsTheTypedArgument() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = RoutingImplementationFinder.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier)
			.route("ups")
			.and(Customer.class, (customer) -> "beta-tenant".equals(customer.tenant()))
			.to(this.upsV2)
			.orElse(this.ups)
			.build();

		assertThat(finder.findImplementation(shipMethod(), args(new Customer("ups", "beta-tenant"), "1 Main St")))
			.isSameAs(this.upsV2);
		assertThat(finder.findImplementation(shipMethod(), args(new Customer("ups", "other-tenant"), "1 Main St")))
			.isSameAs(this.ups);
	}

	@Test
	void argumentCondition_withoutThatArgument_doesNotHold() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = RoutingImplementationFinder.builder(ShipperService.class)
			.byContext(() -> "ups")
			.route("ups")
			.and(Customer.class, (customer) -> true)
			.to(this.upsV2)
			.orElse(this.ups)
			.build();
		Method carriers = ShipperService.class.getMethod("carriers");

		assertThat(finder.findImplementation(carriers, new Object[0])).isSameAs(this.ups);
		assertThat(finder.findImplementation(shipMethod(), args(null, "1 Main St"))).isSameAs(this.ups);
	}

	@Test
	void firstConditionThatHolds_wins() throws Exception {
		RoutingImplementationFinder<ShipperService> finder = RoutingImplementationFinder.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier)
			.route("ups")
			.and(() -> false)
			.to(this.fedex)
			.and(() -> true)
			.to(this.upsV2)
			.and(() -> true)
			.to(this.usps)
			.orElse(this.ups)
			.build();

		ShipperService result = finder.findImplementation(shipMethod(), args(new Customer("ups"), "1 Main St"));

		assertThat(result).isSameAs(this.upsV2);
	}

	@Test
	void conditions_areOnlyCheckedForTheirRoute() throws Exception {
		AtomicInteger conditionChecks = new AtomicInteger();
		RoutingImplementationFinder<ShipperService> finder = RoutingImplementationFinder.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier)
			.route("ups")
			.and(() -> conditionChecks.incrementAndGet() > 0)
			.to(this.upsV2)
			.orElse(this.ups)
			.route("fedex", this.fedex)
			.otherwise(this.usps)
			.build();

		assertThat(finder.findImplementation(shipMethod(), args(new Customer("fedex"), "1 Main St")))
			.isSameAs(this.fedex);
		assertThat(finder.findImplementation(shipMethod(), args(new Customer("dhl"), "1 Main St"))).isSameAs(this.usps);
		assertThat(conditionChecks).hasValue(0);
	}

	@Test
	void conditionalRoute_forRegisteredKey_throwsIllegalArgumentException() {
		RoutingImplementationFinder.RouteBuilder<ShipperService, String> builder = routeByCarrier();

		assertThatIllegalArgumentException().isThrownBy(() -> builder.route("ups"))
			.withMessage("A route is already registered for key: ups");
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void conditionTarget_ofWrongType_throwsIllegalArgumentException() {
		RoutingImplementationFinder.ConditionTarget target = RoutingImplementationFinder.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier)
			.route("ups")
			.and(() -> true);

		assertThatIllegalArgumentException().isThrownBy(() -> target.to("not a shipper"))
			.withMessageContaining("does not implement " + ShipperService.class.getName());
	}

	@Test
	void byRoutingKey_usesAnnotatedParameterRatherThanFirstOfType() throws Exception {
		RoutingImplementationFinder<OrderService> finder = routeOrdersByCustomer().otherwise(this.peopleSoft).build();
		Method getOrder = OrderService.class.getMethod("getOrder", String.class, String.class);

		OrderService result = finder.findImplementation(getOrder, args("order-1", "beta-customer"));

		assertThat(result).isSameAs(this.oracle);
	}

	@Test
	void byRoutingKey_ignoresParametersWithoutTheAnnotation() throws Exception {
		RoutingImplementationFinder<OrderService> finder = routeOrdersByCustomer().otherwise(this.peopleSoft).build();
		Method findOrder = OrderService.class.getMethod("findOrder", String.class);

		OrderService result = finder.findImplementation(findOrder, args("beta-customer"));

		assertThat(result).isSameAs(this.peopleSoft);
	}

	@Test
	void namedRoutingKey_onlyMatchesThatName() throws Exception {
		UUID betaCustomer = UUID.fromString("00000000-0000-0000-0000-000000000001");
		Method getOrderByUuid = OrderService.class.getMethod("getOrderByUuid", String.class, UUID.class);
		RoutingImplementationFinder<OrderService> named = RoutingImplementationFinder.builder(OrderService.class)
			.byRoutingKey("customer", UUID.class, (customerId) -> betaCustomer.equals(customerId) ? "oracle" : null)
			.route("oracle", this.oracle)
			.otherwise(this.peopleSoft)
			.build();
		RoutingImplementationFinder<OrderService> unnamed = RoutingImplementationFinder.builder(OrderService.class)
			.byRoutingKey(UUID.class, (customerId) -> "oracle")
			.route("oracle", this.oracle)
			.otherwise(this.peopleSoft)
			.build();

		assertThat(named.findImplementation(getOrderByUuid, args("order-1", betaCustomer))).isSameAs(this.oracle);
		assertThat(unnamed.findImplementation(getOrderByUuid, args("order-1", betaCustomer))).isSameAs(this.peopleSoft);
	}

	@Test
	void routingKeySources_chainWithOtherSources() throws Exception {
		UUID betaCustomer = UUID.fromString("00000000-0000-0000-0000-000000000001");
		RoutingImplementationFinder<OrderService> finder = RoutingImplementationFinder.builder(OrderService.class)
			.byRoutingKey(String.class, this::omsFor)
			.or()
			.byRoutingKey("customer", UUID.class, (customerId) -> omsFor(customerId.toString()))
			.route("oracle", this.oracle)
			.route("peoplesoft", this.peopleSoft)
			.otherwise(this.peopleSoft)
			.build();
		Method getOrders = OrderService.class.getMethod("getOrders", String.class);
		Method getOrderByUuid = OrderService.class.getMethod("getOrderByUuid", String.class, UUID.class);

		assertThat(finder.findImplementation(getOrders, args("beta-customer"))).isSameAs(this.oracle);
		assertThat(finder.findImplementation(getOrders, args("other-customer"))).isSameAs(this.peopleSoft);
		assertThat(finder.findImplementation(getOrderByUuid, args("order-1", betaCustomer))).isSameAs(this.oracle);
		assertThat(finder.toString()).contains("@RoutingKey java.lang.String",
				"@RoutingKey(\"customer\") java.util.UUID");
	}

	@Test
	void routingKeyCondition_testsAnnotatedParameterRatherThanFirstOfType() throws Exception {
		RoutingImplementationFinder<OrderService> finder = RoutingImplementationFinder.builder(OrderService.class)
			.byContext(() -> "orders")
			.route("orders")
			.andRoutingKey(String.class, "beta-customer"::equals)
			.to(this.oracle)
			.orElse(this.peopleSoft)
			.build();
		Method getOrder = OrderService.class.getMethod("getOrder", String.class, String.class);
		Method findOrder = OrderService.class.getMethod("findOrder", String.class);

		assertThat(finder.findImplementation(getOrder, args("beta-customer", "other-customer")))
			.isSameAs(this.peopleSoft);
		assertThat(finder.findImplementation(getOrder, args("order-1", "beta-customer"))).isSameAs(this.oracle);
		assertThat(finder.findImplementation(findOrder, args("beta-customer"))).isSameAs(this.peopleSoft);
	}

	@Test
	void namedRoutingKeyCondition_onlyTestsThatName() throws Exception {
		RoutingImplementationFinder<OrderService> finder = RoutingImplementationFinder.builder(OrderService.class)
			.byContext(() -> "orders")
			.route("orders")
			.andRoutingKey("tenant", String.class, "beta-tenant"::equals)
			.to(this.oracle)
			.orElse(this.peopleSoft)
			.build();
		Method getTenantOrders = OrderService.class.getMethod("getTenantOrders", String.class, String.class);

		assertThat(finder.findImplementation(getTenantOrders, args("beta-tenant", "customer-1")))
			.isSameAs(this.peopleSoft);
		assertThat(finder.findImplementation(getTenantOrders, args("customer-1", "beta-tenant"))).isSameAs(this.oracle);
		assertThat(finder.findImplementation(getTenantOrders, args("customer-1", null))).isSameAs(this.peopleSoft);
	}

	private RoutingImplementationFinder.RouteBuilder<OrderService, String> routeOrdersByCustomer() {
		return RoutingImplementationFinder.builder(OrderService.class)
			.byRoutingKey(String.class, this::omsFor)
			.route("oracle", this.oracle)
			.route("peoplesoft", this.peopleSoft);
	}

	private String omsFor(String customerId) {
		boolean beta = customerId.equals("beta-customer") || customerId.equals("00000000-0000-0000-0000-000000000001");
		return beta ? "oracle" : "peoplesoft";
	}

	private RoutingImplementationFinder.RouteBuilder<ShipperService, String> routeByCarrier() {
		return RoutingImplementationFinder.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier)
			.route("ups", this.ups)
			.route("fedex", this.fedex);
	}

	private static Method shipMethod() throws NoSuchMethodException {
		return ShipperService.class.getMethod("ship", Customer.class, String.class);
	}

	private static Method shipOrderMethod() throws NoSuchMethodException {
		return ShipperService.class.getMethod("shipOrder", Order.class);
	}

	private static Object[] args(Object... args) {
		return args;
	}

	interface OrderService {

		String getOrders(@RoutingKey String customerId);

		String getOrder(String orderId, @RoutingKey String customerId);

		String getOrderByUuid(String orderId, @RoutingKey("customer") UUID customerId);

		String findOrder(String orderId);

		String getTenantOrders(@RoutingKey String customerId, @RoutingKey("tenant") String tenantId);

	}

	interface ShipperService {

		String ship(Customer customer, String address);

		default String shipPriority(String address, PriorityCustomer customer) {
			return ship(customer, address);
		}

		default String carriers() {
			return "all";
		}

		default String shipOrder(Order order) {
			return "order";
		}

	}

	static class Customer {

		private final String carrier;

		private final String tenant;

		Customer(String carrier) {
			this(carrier, "default");
		}

		Customer(String carrier, String tenant) {
			this.carrier = carrier;
			this.tenant = tenant;
		}

		String carrier() {
			return this.carrier;
		}

		String tenant() {
			return this.tenant;
		}

	}

	record Order(String carrier) {
	}

	static class PriorityCustomer extends Customer {

		PriorityCustomer(String carrier) {
			super(carrier);
		}

	}

}
