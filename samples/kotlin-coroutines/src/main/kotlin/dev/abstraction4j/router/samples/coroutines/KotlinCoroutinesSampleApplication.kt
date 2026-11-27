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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

/**
 * Kotlin coroutines sample: the Spring Boot starter creates the [ShipperService] router,
 * and [OrderService] calls its suspend functions and collects its [kotlinx.coroutines.flow.Flow].
 */
@SpringBootApplication(proxyBeanMethods = false)
class KotlinCoroutinesSampleApplication {

    /**
     * Place orders for customers who use different carriers, all at once, then track one
     * of them.
     */
    @Bean
    fun placeOrders(orderService: OrderService) = ApplicationRunner {
        runBlocking {
            val customers = listOf(
                Customer("Alice", Carrier.UPS),
                Customer("Bob", Carrier.FEDEX),
                Customer("Carol", Carrier.USPS),
                Customer("Dave"),
            )
            // Each order suspends while its carrier responds, so they run concurrently.
            val confirmations = customers.map { customer -> async { orderService.placeOrder(customer, "2 boxes") } }
            confirmations.awaitAll().forEach(::println)
            orderService.track(customers[0], "2 boxes").collect(::println)
        }
    }

}

fun main(args: Array<String>) {
    runApplication<KotlinCoroutinesSampleApplication>(*args)
}
