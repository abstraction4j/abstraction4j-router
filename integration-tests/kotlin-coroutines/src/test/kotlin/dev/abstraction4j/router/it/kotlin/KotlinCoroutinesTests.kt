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
import dev.abstraction4j.router.jdk.JdkAbstractionRouters
import dev.abstraction4j.router.spring.SpringAbstractionRouters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.MDC
import org.springframework.aop.support.AopUtils
import org.springframework.util.ClassUtils
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * Kotlin coroutines through every kind of router. Runs twice: with
 * kotlinx-coroutines-reactor on the classpath, where Spring AOP adapts suspend functions
 * through Reactor, and without it.
 */
class KotlinCoroutinesTests {

    private val carriers = Carriers()

    @AfterEach
    fun cleanUp() {
        RouterKind.closeContexts()
        MDC.clear()
    }

    @Test
    fun classpath_matchesTheTestTask() {
        val expected = System.getProperty("expectCoroutinesReactor").toBoolean()

        assertThat(ClassUtils.isPresent("kotlinx.coroutines.reactor.MonoKt", null)).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(RouterKind::class)
    fun springRouters_areAopProxies(kind: RouterKind) {
        val router = kind.create(carriers)

        assertThat(AopUtils.isAopProxy(router)).isEqualTo(kind != RouterKind.JDK)
    }

    @ParameterizedTest
    @EnumSource(RouterKind::class)
    fun suspendFunction_isRoutedByRoutingKey(kind: RouterKind) {
        val router = kind.create(carriers)

        runBlocking {
            assertThat(router.ship("ups", "box")).isEqualTo("ups:box")
            assertThat(router.ship("fedex", "box")).isEqualTo("fedex:box")
            assertThat(router.ship("dhl", "box")).isEqualTo("usps:box")
        }
    }

    @ParameterizedTest
    @EnumSource(RouterKind::class)
    fun nullRoutingKey_fallsThroughToPropagatedMdc(kind: RouterKind) {
        val router = kind.create(carriers)

        runBlocking {
            MDC.put("carrier", "fedex")
            // MDCContext carries the MDC to whichever thread the coroutine runs on.
            withContext(Dispatchers.Default + MDCContext()) {
                assertThat(router.ship(null, "box")).isEqualTo("fedex:box")
            }
            MDC.clear()
            withContext(Dispatchers.Default + MDCContext()) {
                assertThat(router.ship(null, "box")).isEqualTo("usps:box")
            }
        }
    }

    @ParameterizedTest
    @EnumSource(RouterKind::class)
    fun unitSuspendFunction_runsOnTheRoutedImplementation(kind: RouterKind) {
        val router = kind.create(carriers)

        runBlocking { router.cancel("fedex", "box") }

        assertThat(carriers.fedex.cancelled).containsExactly("box")
        assertThat(carriers.ups.cancelled).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(RouterKind::class)
    fun nullableResult_isReturnedAsIs(kind: RouterKind) {
        val router = kind.create(carriers)

        runBlocking {
            assertThat(router.estimateDays("ups")).isEqualTo(2)
            assertThat(router.estimateDays("usps")).isNull()
        }
    }

    @ParameterizedTest
    @EnumSource(RouterKind::class)
    fun exceptionAfterSuspending_reachesTheCaller(kind: RouterKind) {
        val router = kind.create(carriers)

        assertThatThrownBy { runBlocking { router.failAfterSuspending("fedex") } }
            .isInstanceOf(ShippingException::class.java)
            .hasMessage("fedex failed")
    }

    @ParameterizedTest
    @EnumSource(RouterKind::class)
    fun flow_isRoutedAndCollected(kind: RouterKind) {
        val router = kind.create(carriers)

        val events = runBlocking { router.track("ups", "box").toList() }

        assertThat(events).containsExactly("ups:box:picked-up", "ups:box:delivered")
    }

    @ParameterizedTest
    @EnumSource(RouterKind::class)
    fun concurrentCoroutines_areEachRoutedByTheirOwnArguments(kind: RouterKind) {
        val router = kind.create(carriers)
        val carrierNames = listOf("ups", "fedex", "usps")

        val results = runBlocking {
            (0 until 300).map { i ->
                async(Dispatchers.Default) { router.ship(carrierNames[i % 3], "box$i") }
            }.awaitAll()
        }

        assertThat(results).isEqualTo((0 until 300).map { i -> "${carrierNames[i % 3]}:box$i" })
    }

    @Test
    fun kotlinFinder_seesNullArgumentsAndTheContinuation() {
        for (create in listOf<(ImplementationFinder<ShipperService>) -> ShipperService>(
            { JdkAbstractionRouters.routerFor(ShipperService::class.java, it) },
            { SpringAbstractionRouters.routerFor(ShipperService::class.java, it) },
        )) {
            var seenMethod: Method? = null
            var seenArgs: Array<Any?> = emptyArray()
            val router = create(ImplementationFinder { method, args ->
                seenMethod = method
                seenArgs = args
                carriers.ups
            })

            runBlocking { router.ship(null, "box") }

            assertThat(seenMethod?.name).isEqualTo("ship")
            assertThat(seenArgs).hasSize(seenMethod!!.parameterCount)
            assertThat(seenArgs[0]).isNull()
            assertThat(seenArgs[1]).isEqualTo("box")
            assertThat(seenArgs.last()).isInstanceOf(Continuation::class.java)
        }
    }

}
