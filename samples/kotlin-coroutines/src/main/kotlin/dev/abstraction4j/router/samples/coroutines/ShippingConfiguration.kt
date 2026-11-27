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

package dev.abstraction4j.router.samples.coroutines

import dev.abstraction4j.router.core.RoutingImplementationFinder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Declares the routing finder. The Spring Boot starter sees this
 * `ImplementationFinder<ShipperService>` bean and registers a `@Primary` [ShipperService]
 * router for it.
 */
@Configuration(proxyBeanMethods = false)
class ShippingConfiguration {

    /**
     * Route by the customer's own carrier, else by the carrier chosen for the current
     * coroutine, else to USPS. The implementations are injected by concrete type; asking
     * for `ShipperService` here would inject the router itself.
     */
    @Bean
    fun shipperServiceFinder(
        ups: UpsShipperService,
        fedex: FedexShipperService,
        usps: UspsShipperService,
    ): RoutingImplementationFinder<ShipperService> = RoutingImplementationFinder.builder(ShipperService::class.java)
        .byArgument(Customer::class.java) { customer -> customer.carrier }
        .or().byContext { CarrierContext.current() }
        .route(Carrier.UPS, ups)
        .route(Carrier.FEDEX, fedex)
        .otherwise(usps)
        .build()

}
