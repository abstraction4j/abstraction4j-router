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

package dev.abstraction4j.router.samples.jdk;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdkSampleApplication}.
 *
 * @author Joe Chambers
 */
class JdkSampleApplicationTests {

	@Test
	void shipperService_routesByCustomerCarrier() {
		OrderService orderService = new OrderService(JdkSampleApplication.shipperService());

		assertThat(orderService.placeOrder(new Customer("Alice", Carrier.UPS), "1 box"))
			.isEqualTo("UPS is shipping 1 box for Alice");
		assertThat(orderService.placeOrder(new Customer("Bob", Carrier.FEDEX), "1 box"))
			.isEqualTo("FedEx is shipping 1 box for Bob");
		assertThat(orderService.placeOrder(new Customer("Carol", Carrier.USPS), "1 box"))
			.isEqualTo("USPS is shipping 1 box for Carol");
	}

	@Test
	void shipperService_isRouter() {
		ShipperService shipperService = JdkSampleApplication.shipperService();

		assertThat(Proxy.isProxyClass(shipperService.getClass())).isTrue();
	}

	@Test
	void main_runs() {
		JdkSampleApplication.main(new String[0]);
	}

}
