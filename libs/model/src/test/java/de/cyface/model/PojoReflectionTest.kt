/*
 * Copyright 2025 Cyface GmbH
 *
 * This file is part of the Serialization.
 *
 * The Serialization is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Serialization is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Serialization. If not, see <http://www.gnu.org/licenses/>.
 */
package de.cyface.model

import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PojoReflectionTest {

    @Test
    fun `Measurement should be POJO-like`() {
        assertPojoCompliant(Measurement::class)
        assertPojoCompliant(MetaData::class)
        assertPojoCompliant(Track::class)
    }

    @Test
    fun `NotAPojo should fail POJO check`() {
        val exception = assertFailsWith<AssertionError> {
            assertPojoCompliant(NotAPojo::class)
        }
        println("Expected failure: ${exception.message}")
    }

    private fun assertPojoCompliant(kClass: KClass<*>) {
        val name = kClass.simpleName ?: kClass.toString()

        // Serializable
        assertTrue(Serializable::class.java.isAssignableFrom(kClass.java), "$name must implement Serializable")

        // No-arg constructor
        val hasNoArgCtor = kClass.constructors.any { ctor ->
            ctor.parameters.isEmpty() || ctor.parameters.all { it.isOptional || it.type.isMarkedNullable }
        }
        assertTrue(hasNoArgCtor, "$name must have a public no-arg constructor")

        // All properties must be mutable (var)
        kClass.memberProperties.forEach { prop ->
            assertTrue(prop is KMutableProperty<*>, "$name.${prop.name} must be mutable (var)")
        }
    }
}

class NotAPojo(@Suppress("unused") val someVal: String) : Serializable {
    companion object {
        @Suppress("unused") // required for `Serializable`
        private const  val serialVersionUID = 1L
    }
}
