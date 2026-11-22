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

import java.util.List;

import dev.abstraction4j.router.core.ImplementationFinder;
import dev.abstraction4j.router.core.RoutingImplementationFinder;
import dev.abstraction4j.router.jdk.JdkAbstractionRouters;

/**
 * Plain Java sample: wires the carriers, a routing finder and the router by hand, then
 * places orders for customers who use different carriers.
 *
 * @author Joe Chambers
 */
public final class JdkSampleApplication {

	private JdkSampleApplication() {
	}

	/**
	 * Run the sample.
	 * @param args ignored
	 */
	public static void main(String[] args) {
		OrderService orderService = new OrderService(shipperService());
		List<Customer> customers = List.of(new Customer("Alice", Carrier.UPS), new Customer("Bob", Carrier.FEDEX),
				new Customer("Carol", Carrier.USPS));
		for (Customer customer : customers) {
			System.out.println(orderService.placeOrder(customer, "2 boxes"));
		}
	}

	/**
	 * Create the {@link ShipperService} router.
	 * @return the router
	 */
	static ShipperService shipperService() {
		// Route on the Customer argument's carrier; anything unmatched goes to USPS.
		ImplementationFinder<ShipperService> finder = RoutingImplementationFinder.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier)
			.route(Carrier.UPS, new UpsShipperService())
			.route(Carrier.FEDEX, new FedexShipperService())
			.otherwise(new UspsShipperService())
			.build();
		return JdkAbstractionRouters.routerFor(ShipperService.class, finder);
	}

}
