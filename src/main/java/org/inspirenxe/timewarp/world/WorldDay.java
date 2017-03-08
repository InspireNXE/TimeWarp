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
import org.inspirenxe.timewarp.daypart.DayPart;
import org.inspirenxe.timewarp.daypart.DayPartType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

public class WorldDay {

    /**
     * The name of the world linked to this group.
     */
    public final String worldName;
    /**
     * 0 = MORNING
     * 1 = NOON
     * 2 = EVENING
     * 3 = NIGHT
     */
    private final DayPart[] dayparts = new DayPart[4];

    public WorldDay(String worldName) {
        this.worldName = worldName;
    }

    /**
     * Initializes all dayparts.
     * @return {@link WorldDay} for chaining.
     */
    public WorldDay init() {
        final Optional<WorldProperties> optProperties = Sponge.getServer().getWorldProperties(worldName);
        if (optProperties.isPresent()) {
            final String rootPath = "sync.worlds." + worldName.toLowerCase();
            for (DayPartType type : DayPartType.values()) {
                long daypartValue = TimeWarp.INSTANCE.storage.getChildNode(rootPath + ".dayparts." + type.name.toLowerCase()).getLong();
                if (daypartValue != 0 && daypartValue < type.defaultLength) {
                    TimeWarp.INSTANCE.logger.warn(String.format("Unable to use value [%1s] in [%2s] for DayPart [%3s] as the value is below "
                            + "vanilla Minecraft length [%4s]. If you are trying to skip this DayPart please use 0 as the value.",
                            daypartValue, rootPath, type.name, type.defaultLength));
                    daypartValue = type.defaultLength;
                }
                this.setDayPart(type, new DayPart(type, daypartValue));
            }
        }
        return this;
    }

    /**
     * Gets the {@link DayPart} from the {@link DayPartType}.
     * @param type The {@link DayPartType} to get the {@link DayPart} from.
     * @return The {@link Optional<DayPart>}.
     */
    public Optional<DayPart> getDayPart(DayPartType type) {
        for (DayPart daypart : dayparts) {
            if (daypart.getType().equals(type)) {
                return Optional.of(daypart);
            }
        }
        return Optional.empty();
    }

    /**
     * Sets the daypart stored to the {@link DayPart} passed in.
     * @param type The {@link DayPartType} to set.
     * @param daypart The {@link DayPart} to set to.
     */
    public void setDayPart(DayPartType type, DayPart daypart) {
        switch (type) {
            case NOON:
                dayparts[1] = daypart;
                break;
            case EVENING:
                dayparts[2] = daypart;
                break;
            case NIGHT:
                dayparts[3] = daypart;
                break;
            default:
                dayparts[0] = daypart;
        }
    }

    /**
     * Gets the total day length using the length of all dayparts added together.
     * @return The total day length.
     */
    public long getDayLength() {
        long length = 0;
        for (DayPart daypart : dayparts) {
            length += daypart.getLength();
        }
        return length;
    }

    /**
     * Gets the start time of the {@link DayPart}.
     * @param type The {@link DayPartType} to get the start time from.
     * @return The start time.
     */
    public long getStartTime(DayPartType type) {
        long start = 0;
        for (DayPart daypart : dayparts) {
            if (type.equals(daypart.getType())) {
                return start;
            }
            start += daypart.getLength();
        }
        return start;
    }

    /**
     * Gets the end time of the {@link DayPart}.
     * @param type The {@link DayPartType} to get the end time from.
     * @return The end time.
     */
    public long getEndTime(DayPartType type) {
        long end = getDayLength();
        for (int i = dayparts.length - 1; i > 0; i--) {
            if (type.equals(dayparts[i].getType())) {
                return end;
            }
            end -= dayparts[i].getLength();
        }
        return end;
    }
}
