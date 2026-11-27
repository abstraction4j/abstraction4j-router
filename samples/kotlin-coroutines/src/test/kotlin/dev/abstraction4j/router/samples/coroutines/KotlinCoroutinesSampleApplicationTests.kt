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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@SpringBootTest
class KotlinCoroutinesSampleApplicationTests(
    @Autowired private val context: ApplicationContext,
    @Autowired private val orderService: OrderService,
    @Autowired private val shipperService: ShipperService,
) {

    @Test
    fun injectedShipperService_isAutoConfiguredRouter() {
        assertThat(shipperService).isSameAs(context.getBean("shipperServiceFinderRouter"))
    }

    @Test
    fun placeOrder_routesByCustomerCarrier() {
        runBlocking {
            assertThat(orderService.placeOrder(Customer("Alice", Carrier.UPS), "1 box"))
                .isEqualTo("UPS is shipping 1 box for Alice")
            assertThat(orderService.placeOrder(Customer("Bob", Carrier.FEDEX), "1 box"))
                .isEqualTo("FedEx is shipping 1 box for Bob")
            assertThat(orderService.placeOrder(Customer("Carol", Carrier.USPS), "1 box"))
                .isEqualTo("USPS is shipping 1 box for Carol")
        }
    }

    @Test
    fun placeOrder_withoutPreference_routesByLookedUpCarrier() {
        runBlocking {
            assertThat(orderService.placeOrder(Customer("Dave"), "1 box")).isEqualTo("FedEx is shipping 1 box for Dave")
            assertThat(orderService.placeOrder(Customer("Erin"), "1 box")).isEqualTo("USPS is shipping 1 box for Erin")
        }
    }

    @Test
    fun concurrentOrders_areEachRoutedByTheirOwnCustomer() {
        val customers = (0 until 100).map { i ->
            if (i % 2 == 0) Customer("Customer$i", Carrier.UPS) else Customer("Dave$i")
        }

        val confirmations = runBlocking {
            customers.map { customer ->
                async(Dispatchers.Default) { orderService.placeOrder(customer, "1 box") }
            }.awaitAll()
        }

        assertThat(confirmations).isEqualTo(customers.map { customer ->
            val carrier = if (customer.carrier == Carrier.UPS) "UPS" else "FedEx"
            "$carrier is shipping 1 box for ${customer.name}"
        })
    }

    @Test
    fun track_streamsEventsFromTheRoutedCarrier() {
        val events = runBlocking { orderService.track(Customer("Bob", Carrier.FEDEX), "1 box").toList() }

        assertThat(events).containsExactly(
            "FedEx: Bob's 1 box picked up",
            "FedEx: Bob's 1 box in transit",
            "FedEx: Bob's 1 box delivered",
        )
    }

}
