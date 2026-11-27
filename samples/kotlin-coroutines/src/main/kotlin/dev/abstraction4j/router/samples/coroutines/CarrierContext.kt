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

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asContextElement

/**
 * The carrier chosen for the current coroutine. The finder reads it with `byContext`, on
 * whichever thread the coroutine happens to run.
 */
object CarrierContext {

    private val carrier = ThreadLocal<Carrier?>()

    /**
     * The carrier for the current coroutine, or `null` if none was chosen.
     */
    fun current(): Carrier? = carrier.get()

    /**
     * A coroutine context element that makes [current] return `chosen` inside it.
     */
    fun of(chosen: Carrier): ThreadContextElement<Carrier?> = carrier.asContextElement(chosen)

}
