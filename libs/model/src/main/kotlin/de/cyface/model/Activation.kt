/*
 * Copyright 2023 Cyface GmbH
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

/**
 * E-Mail Activation templates which the client can choose from during account registration.
 *
 * This allows to white-label the activation emails and adjust the activation link (native activation).
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 */
@Suppress("unused") // Part of the API
enum class Activation {
    CYFACE_ANDROID,
    CYFACE_IOS,
    CYFACE_WEB,
    R4R_ANDROID,
    R4R_IOS,
    R4R_WEB
}