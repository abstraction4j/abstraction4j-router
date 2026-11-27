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

package dev.abstraction4j.router.it.kotlin

import dev.abstraction4j.router.core.ImplementationFinder
import dev.abstraction4j.router.core.RoutingImplementationFinder
import dev.abstraction4j.router.jdk.JdkAbstractionRouters
import dev.abstraction4j.router.spring.SpringAbstractionRouters
import org.slf4j.MDC
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean

class Carriers {

    val ups = CarrierShipperService("ups", 2)

    val fedex = CarrierShipperService("fedex", 1)

    val usps = CarrierShipperService("usps", null)

    /**
     * Routes by the `@RoutingKey` carrier, then by the carrier in the MDC, then to USPS.
     */
    fun finder(): ImplementationFinder<ShipperService> = RoutingImplementationFinder.builder(ShipperService::class.java)
        .byRoutingKey(String::class.java) { carrier -> carrier }
        .or().byContext { MDC.get("carrier") }
        .route("ups", ups)
        .route("fedex", fedex)
        .otherwise(usps)
        .build()

}

/**
 * The three ways an application gets a router.
 */
enum class RouterKind {

    JDK {
        override fun create(carriers: Carriers): ShipperService =
            JdkAbstractionRouters.routerFor(ShipperService::class.java, carriers.finder())
    },

    SPRING {
        override fun create(carriers: Carriers): ShipperService =
            SpringAbstractionRouters.routerFor(ShipperService::class.java, carriers.finder())
    },

    SPRING_BOOT {
        override fun create(carriers: Carriers): ShipperService {
            val context = SpringApplicationBuilder(ShippingApplication::class.java)
                .web(WebApplicationType.NONE)
                .initializers({ it.beanFactory.registerSingleton("carriers", carriers) })
                .run()
            contexts.add(context)
            check(context.containsBean("shipperServiceFinderRouter")) { "The starter registered no router." }
            return context.getBean(ShipperService::class.java)
        }
    };

    abstract fun create(carriers: Carriers): ShipperService

    companion object {

        val contexts = mutableListOf<ConfigurableApplicationContext>()

        fun closeContexts() {
            contexts.forEach { it.close() }
            contexts.clear()
        }

    }

}

/**
 * Spring Boot application that only declares the finder; the starter registers the router.
 */
@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
class ShippingApplication {

    @Bean
    fun shipperServiceFinder(carriers: Carriers): ImplementationFinder<ShipperService> = carriers.finder()

}
