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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

/**
 * Business code. It depends only on [ShipperService] and contains no carrier selection
 * logic; the router handles that.
 */
@Service
class OrderService(private val shipperService: ShipperService, private val carrierDirectory: CarrierDirectory) {

    suspend fun placeOrder(customer: Customer, parcel: String): String {
        if (customer.carrier != null) {
            // Routed by the Customer argument.
            return shipperService.ship(customer, parcel)
        }
        // The finder can't suspend, so look the carrier up first, then route by it
        // through the coroutine context.
        val carrier = carrierDirectory.carrierFor(customer)
        return withContext(CarrierContext.of(carrier)) {
            shipperService.ship(customer, parcel)
        }
    }

    fun track(customer: Customer, parcel: String): Flow<String> = shipperService.track(customer, parcel)

}
