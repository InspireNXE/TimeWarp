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
package org.inspirenxe.timewarp.daypart;

import java.util.Optional;

public enum DayPartType {
    MORNING("Morning", 1800, 0, 1800),
    NOON("Noon", 12000, 1800, 13800),
    EVENING("Evening", 1800, 13800, 15600),
    NIGHT("Night", 8400, 15600, 24000);

    public static final long DEFAULT_DAY_LENGTH = 24000L;

    public final String name;
    public final long defaultLength;
    public final long defaultStartTime;
    public final long defaultEndTime;

    DayPartType(String name, long defaultLength, long defaultStartTime, long defaultEndTime) {
        this.name = name;
        this.defaultLength = defaultLength;
        this.defaultStartTime = defaultStartTime;
        this.defaultEndTime = defaultEndTime;
    }

    /**
     * Gets the daypart type based on the time.
     * @param time The time to use for matching the {@link DayPartType}.
     * @return The {@link DayPartType} that matches the time of day.
     */
    public static Optional<DayPartType> getTypeFromTime(long time) {
        for (DayPartType type : DayPartType.values()) {
            if (time >= type.defaultStartTime && time <= type.defaultEndTime) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
