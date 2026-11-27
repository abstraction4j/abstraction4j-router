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

/**
 * Shipping carriers a customer can use.
 */
enum class Carrier { UPS, FEDEX, USPS }

/**
 * A customer and the carrier they ship with.
 */
data class Customer(val name: String, val carrier: Carrier)

/**
 * The abstraction the business code depends on. Each carrier has its own implementation,
 * and the router picks one per call.
 */
interface ShipperService {

    fun ship(customer: Customer, parcel: String): String

}

/**
 * UPS implementation. A real implementation would call the UPS API here.
 */
class UpsShipperService : ShipperService {

    override fun ship(customer: Customer, parcel: String) = "UPS is shipping $parcel for ${customer.name}"

}

/**
 * FedEx implementation.
 */
class FedexShipperService : ShipperService {

    override fun ship(customer: Customer, parcel: String) = "FedEx is shipping $parcel for ${customer.name}"

}

/**
 * USPS implementation, used for every customer no other route matches.
 */
class UspsShipperService : ShipperService {

    override fun ship(customer: Customer, parcel: String) = "USPS is shipping $parcel for ${customer.name}"

}

/**
 * Business code. It depends only on [ShipperService] and contains no carrier selection
 * logic; the router handles that.
 */
class OrderService(private val shipperService: ShipperService) {

    fun placeOrder(customer: Customer, parcel: String) = shipperService.ship(customer, parcel)

}
