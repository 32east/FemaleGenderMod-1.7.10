/*
Female Gender Mod, 1.7.10 port.
Copyright (C) 2022 WildfireRomeo

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.wildfire.api;

/**
 * Describes how a chest-slot item interacts with breast rendering and physics.
 *
 * <p>Other mods may register their own implementations through
 * {@link com.wildfire.main.WildfireHelper#addGenderArmor}.</p>
 */
public interface IGenderArmor {

    /**
     * Whether this armor "covers" the breasts, or has an open front like the elytra.
     */
    boolean coversBreasts();

    /**
     * Hide the breasts entirely while worn, even when the player asked for them to show in armor.
     * Useful for armors with custom rendering that would only clip.
     */
    boolean alwaysHidesBreasts();

    /**
     * How much physical resistance this armor gives, between {@code 0} (full physics) and {@code 1} (none).
     */
    float physicsResistance();

    /**
     * How "tight" this armor is, between {@code 0} (no compression) and {@code 1} (up to 15% smaller).
     */
    float tightness();
}
