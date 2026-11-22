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

/**
 * Business code. It depends only on {@link ShipperService} and contains no carrier
 * selection logic; the router handles that.
 *
 * @author Joe Chambers
 */
public class OrderService {

	private final ShipperService shipperService;

	/**
	 * Create a new order service.
	 * @param shipperService the shipper service, which is the router
	 */
	public OrderService(ShipperService shipperService) {
		this.shipperService = shipperService;
	}

	/**
	 * Place an order and ship it.
	 * @param customer the customer placing the order
	 * @param parcel a description of the parcel
	 * @return the shipping confirmation
	 */
	public String placeOrder(Customer customer, String parcel) {
		return this.shipperService.ship(customer, parcel);
	}

}
