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

import dev.abstraction4j.router.core.RoutingKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Abstraction with the kinds of methods Kotlin code routes: suspend functions returning a
 * value, `Unit` or `null`, one that fails after suspending, and one returning a [Flow].
 */
interface ShipperService {

    suspend fun ship(@RoutingKey carrier: String?, parcel: String): String

    suspend fun cancel(@RoutingKey carrier: String, parcel: String)

    suspend fun failAfterSuspending(@RoutingKey carrier: String): String

    suspend fun estimateDays(@RoutingKey carrier: String): Int?

    fun track(@RoutingKey carrier: String, parcel: String): Flow<String>

}

class ShippingException(message: String) : RuntimeException(message)

/**
 * Implementation that really suspends, and resumes on another thread, in every suspend
 * function.
 */
open class CarrierShipperService(private val name: String, private val days: Int?) : ShipperService {

    val cancelled = ConcurrentLinkedQueue<String>()

    override suspend fun ship(carrier: String?, parcel: String): String {
        delay(1)
        return withContext(Dispatchers.Default) { "$name:$parcel" }
    }

    override suspend fun cancel(carrier: String, parcel: String) {
        delay(1)
        cancelled.add(parcel)
    }

    override suspend fun failAfterSuspending(carrier: String): String {
        delay(1)
        throw ShippingException("$name failed")
    }

    override suspend fun estimateDays(carrier: String): Int? {
        delay(1)
        return days
    }

    override fun track(carrier: String, parcel: String): Flow<String> = flow {
        emit("$name:$parcel:picked-up")
        delay(1)
        emit("$name:$parcel:delivered")
    }

}
