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

package dev.abstraction4j.router.samples.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootSampleApplication}.
 *
 * @author Joe Chambers
 */
@SpringBootTest
class SpringBootSampleApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private OrderService orderService;

	@Autowired
	private ShipperService shipperService;

	@Test
	void injectedShipperService_isAutoConfiguredRouter() {
		assertThat(this.shipperService).isSameAs(this.context.getBean("shipperServiceFinderRouter"));
	}

	@Test
	void orderService_routesByCustomerCarrier() {
		assertThat(this.orderService.placeOrder(new Customer("Alice", Carrier.UPS), "1 box"))
			.isEqualTo("UPS is shipping 1 box for Alice");
		assertThat(this.orderService.placeOrder(new Customer("Bob", Carrier.FEDEX), "1 box"))
			.isEqualTo("FedEx is shipping 1 box for Bob");
		assertThat(this.orderService.placeOrder(new Customer("Carol", Carrier.USPS), "1 box"))
			.isEqualTo("USPS is shipping 1 box for Carol");
	}

}
