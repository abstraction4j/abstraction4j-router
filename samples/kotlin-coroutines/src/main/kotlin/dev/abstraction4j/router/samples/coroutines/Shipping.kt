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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

/**
 * Shipping carriers a customer can use.
 */
enum class Carrier { UPS, FEDEX, USPS }

/**
 * A customer and the carrier they ship with, or `null` if they have no preference.
 */
data class Customer(val name: String, val carrier: Carrier? = null)

/**
 * The abstraction the business code depends on. Shipping suspends while the carrier's API
 * responds, and tracking streams events as they happen.
 */
interface ShipperService {

    suspend fun ship(customer: Customer, parcel: String): String

    fun track(customer: Customer, parcel: String): Flow<String>

}

/**
 * Shared behavior of the carrier implementations. A real implementation would call the
 * carrier's API; `delay` stands in for that call.
 */
abstract class CarrierShipperService(private val label: String) : ShipperService {

    override suspend fun ship(customer: Customer, parcel: String): String {
        delay(100)
        return "$label is shipping $parcel for ${customer.name}"
    }

    override fun track(customer: Customer, parcel: String): Flow<String> = flow {
        for (status in listOf("picked up", "in transit", "delivered")) {
            delay(50)
            emit("$label: ${customer.name}'s $parcel $status")
        }
    }

}

@Component
class UpsShipperService : CarrierShipperService("UPS")

@Component
class FedexShipperService : CarrierShipperService("FedEx")

@Component
class UspsShipperService : CarrierShipperService("USPS")

/**
 * Chooses a carrier for customers without a preference. It suspends, like a call to a
 * remote service would, so it can't run inside the finder; [OrderService] calls it first.
 */
@Component
class CarrierDirectory {

    suspend fun carrierFor(customer: Customer): Carrier {
        delay(100)
        return if (customer.name.startsWith("D")) Carrier.FEDEX else Carrier.USPS
    }

}
