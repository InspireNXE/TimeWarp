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
package org.inspirenxe.timewarp.tasks;

import org.spongepowered.api.world.World;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class WorldSyncTask implements Runnable {
    private final long dayInMilliseconds;
    private final long worldTicksInMilliseconds;
    private final World world;
    private final ZoneId timezone;

    public WorldSyncTask(World world, String timezone, long length) {
        this.world = world;
        this.timezone = ZoneId.of(timezone);

        // Make sure we are not less than 24000 else it would cause a java.lang.ArithmeticException in the math below
        this.dayInMilliseconds = length < 24000 ? 24000 : length;
        this.worldTicksInMilliseconds = dayInMilliseconds / 24000;

        this.world.getProperties().setGameRule("doDaylightCycle", "false");
    }

    @Override
    public void run() {
        final ZonedDateTime now = ZonedDateTime.now(timezone);
        final LocalDate tomorrow = now.toLocalDate().plusDays(1);
        final ZonedDateTime tomorrowStart = tomorrow.atStartOfDay(timezone);
        final Duration duration = Duration.between(now, tomorrowStart);
        final long millisecondsUntilTomorrow = duration.toMillis();
        final long currentTimeInMillis = dayInMilliseconds - millisecondsUntilTomorrow;
        final long currentTimeInTicks = currentTimeInMillis / worldTicksInMilliseconds;

        world.getProperties().setWorldTime(currentTimeInTicks + 18000);
    }
}
