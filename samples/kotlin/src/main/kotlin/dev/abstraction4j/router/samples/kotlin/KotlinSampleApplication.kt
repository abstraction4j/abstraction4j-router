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

package dev.abstraction4j.router.samples.kotlin

import dev.abstraction4j.router.core.RoutingImplementationFinder
import dev.abstraction4j.router.jdk.JdkAbstractionRouters

/**
 * Plain Kotlin sample: wires the carriers, a routing finder and the router by hand, then
 * places orders for customers who use different carriers.
 */
fun main() {
    val orderService = OrderService(shipperService())
    val customers = listOf(Customer("Alice", Carrier.UPS), Customer("Bob", Carrier.FEDEX), Customer("Carol", Carrier.USPS))
    for (customer in customers) {
        println(orderService.placeOrder(customer, "2 boxes"))
    }
}

/**
 * Create the [ShipperService] router: route on the customer's carrier, and send anything
 * unmatched to USPS.
 */
fun shipperService(): ShipperService {
    val finder = RoutingImplementationFinder.builder(ShipperService::class.java)
        .byArgument(Customer::class.java, Customer::carrier)
        .route(Carrier.UPS, UpsShipperService())
        .route(Carrier.FEDEX, FedexShipperService())
        .otherwise(UspsShipperService())
        .build()
    return JdkAbstractionRouters.routerFor(ShipperService::class.java, finder)
}
