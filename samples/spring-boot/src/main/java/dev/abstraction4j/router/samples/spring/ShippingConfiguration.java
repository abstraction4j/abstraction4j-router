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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.abstraction4j.router.core.RoutingImplementationFinder;

/**
 * Declares the routing finder. The auto-configuration sees this
 * {@code ImplementationFinder<ShipperService>} bean and registers a {@code @Primary}
 * {@link ShipperService} router named {@code shipperServiceFinderRouter}.
 *
 * @author Joe Chambers
 */
@Configuration(proxyBeanMethods = false)
public class ShippingConfiguration {

	/**
	 * Route {@link ShipperService} calls by the customer's carrier. The implementations
	 * are injected by concrete type; asking for {@code ShipperService} here would inject
	 * the router itself.
	 * @param ups the UPS implementation
	 * @param fedex the FedEx implementation
	 * @param usps the USPS implementation, used when no route matches
	 * @return the finder
	 */
	@Bean
	public RoutingImplementationFinder<ShipperService> shipperServiceFinder(UpsShipperService ups,
			FedexShipperService fedex, UspsShipperService usps) {
		return RoutingImplementationFinder.builder(ShipperService.class)
			.byArgument(Customer.class, Customer::carrier)
			.route(Carrier.UPS, ups)
			.route(Carrier.FEDEX, fedex)
			.otherwise(usps)
			.build();
	}

}
