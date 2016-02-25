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
package org.inspirenxe.timewarp.world;

import org.inspirenxe.timewarp.TimeWarp;
import org.inspirenxe.timewarp.daypart.DaypartType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

public class WorldSyncRunnable implements Runnable {

    private final WorldSync worldSync;
    private long customTime;

    public WorldSyncRunnable(WorldSync worldSync) {
        this.worldSync = worldSync;
    }

    @Override
    public void run() {
        final Optional<WorldProperties> optProperties = Sponge.getServer().getWorldProperties(worldSync.worldName);
        if (optProperties.isPresent()) {
            // Disable normal daylight cycle
            optProperties.get().setGameRule("doDaylightCycle", "false");

            // If the world time is a negative value, set it to the same as a positive value. (eg. -20 is set to 20)
            if (optProperties.get().getWorldTime() < 0) {
                optProperties.get().setWorldTime(0);
            }

            try {
                customTime = (customTime % worldSync.worldDay.getDayLength()) + 1;
            } catch (ArithmeticException e) {
                TimeWarp.INSTANCE.logger.warn("The total day length for [" + worldSync.worldName + "] cannot be zero!", e);
            }

            final Optional<DaypartType> optType = DaypartType.getTypeFromTime(optProperties.get().getWorldTime());
            if (optType.isPresent()) {
                long scaledTime = scale(
                        customTime,
                        worldSync.worldDay.getStartTime(optType.get()),
                        worldSync.worldDay.getEndTime(optType.get()),
                        optType.get().defaultStartTime,
                        optType.get().defaultEndTime)
                        % optType.get().defaultDayLength;
                optProperties.get().setWorldTime(scaledTime < 0 ? 0 : scaledTime);
            }
        }
    }

    /**
     * Gets the custom world time of this runnable.
     * @return The custom time.
     */
    public long getCustomTime() {
        return customTime;
    }

    /**
     * Sets the custom world time of this runnable.
     * @param customTime The custom time to set to. If lesser than 1, value will be forced to 1.
     */
    public void setCustomTime(long customTime) {
        this.customTime = customTime < 1 ? 1 : customTime;
    }

    /**
     * Scales time from one range to another.
     * @param time The time to scale.
     * @param originalStart The original start time.
     * @param originalEnd The original end time.
     * @param newStart The new start time.
     * @param newEnd The new end time.
     * @return The scaled time.
     */
    public static long scale(long time, long originalStart, long originalEnd, long newStart, long newEnd) {
        double scale = (double) (newEnd - newStart) / (originalEnd - originalStart);
        return (long) (newStart + ((time - originalStart) * scale));
    }
}
