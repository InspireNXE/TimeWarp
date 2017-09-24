/*
 * This file is part of TimeWarp, licensed under the MIT License (MIT).
 *
 * Copyright (c) InspireNXE <http://github.com/InspireNXE/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.inspirenxe.timewarp.api;

import org.inspirenxe.timewarp.daypart.DayPart;
import org.inspirenxe.timewarp.daypart.DayPartType;
import org.inspirenxe.timewarp.world.WorldDay;

import java.util.Optional;

public interface IMixinWorldServer {

    /**
     * Gets the ticks until the next world time increment
     * @return The ticks until next increment
     */
    long getTicksUntilNextIncrement();

    /**
     * Sets the ticks until the next world time increment
     * @param ticksUntilNextIncrement Ticks until next increment
     */
    void setTicksUntilNextIncrement(long ticksUntilNextIncrement);

    /**
     * Clears TimeWarp cache for this world
     */
    void clearCache();

    /**
     * Clears the cached {@link WorldDay} object from this world
     */
    void clearWorldDayCache();

    /**
     * Clears the cached {@link DayPart} object from this world
     */
    void clearDayPartCache();

    /**
     * Clears the cached {@link DayPartType} object from this world
     */
    void clearDayPartTypeCache();

    /**
     * Gets the current {@link WorldDay} for this world
     * @return The current world day
     */
    Optional<WorldDay> getCachedWorldDay();

    /**
     * Gets the current {@link DayPart} for this world
     * @return The current day part
     */
    Optional<DayPart> getCachedDayPart();


    /**
     * Gets the current {@link DayPartType} for this world
     * @return The current day part type
     */
    Optional<DayPartType> getCachedDayPartType();
}
