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
     * 1 = DAY
     * 2 = EVENING
     * 3 = NIGHT
     */
    private final DayPart[] dayparts = new DayPart[5];
    private DayPartType wakeAtDayPart;
    private long daysPassed = 0;

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
                long daypartValue = TimeWarp.instance.storage.getChildNode(rootPath + ".dayparts." + type.name.toLowerCase()).getLong();
                if (daypartValue != 0 && daypartValue < type.defaultLength) {
                    TimeWarp.instance.logger.warn(String.format("Unable to use value [%1s] in [%2s] for DayPart [%3s] as the value is below "
                            + "vanilla Minecraft length [%4s]. If you are trying to skip this DayPart please use 0 as the value.",
                            daypartValue, rootPath, type.name, type.defaultLength));
                    daypartValue = type.defaultLength;
                }
                this.setDayPart(type, new DayPart(type, daypartValue));
            }
            final String wakeAtPath = rootPath + ".wake-at-daypart";
            final String dayPartCandidate = TimeWarp.instance.storage.getChildNode(wakeAtPath).getString(DayPartType.DAY.name.toUpperCase()).toUpperCase();
            try {
                wakeAtDayPart = DayPartType.valueOf(dayPartCandidate);
            } catch (IllegalArgumentException e) {
                TimeWarp.instance.logger.warn("Unable to parse [" + dayPartCandidate + "] at [" + wakeAtPath + "]. Defaulting to DayPart [" +
                        DayPartType.DAY.name.toUpperCase() + "]");
                wakeAtDayPart = DayPartType.valueOf(DayPartType.DAY.name.toUpperCase());
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
        if (type == null) {
            return Optional.empty();
        }

        for (DayPart daypart : dayparts) {
            if (daypart.getType().equals(type)) {
                return Optional.of(daypart);
            }
        }

        return Optional.empty();
    }

    /**
     * Gets the next available {@link DayPart} with a length that is not equal to 0.
     * @param type The current {@link DayPartType}.
     * @return The next available {@link Optional<DayPart>}, otherwise {@link Optional#empty()}.
     */
    public Optional<DayPart> getNextDayPart(DayPartType type) {
        if (type == null) {
            return Optional.empty();
        }

        // Attempt to find the next daypart excluding the current one
        for (DayPart dayPart : dayparts) {
            if (dayPart.getType() != type && dayPart.getLength() != 0) {
                return Optional.of(dayPart);
            }
        }

        // Attempt to return the current daypart
        final Optional<DayPart> optDayPartCandidate = this.getDayPart(type);
        if (optDayPartCandidate.isPresent() && optDayPartCandidate.get().getLength() != 0) {
            return optDayPartCandidate;
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
            case DAY:
                dayparts[1] = daypart;
                break;
            case EVENING:
                dayparts[2] = daypart;
                break;
            case DUSK:
                dayparts[3] = daypart;
                break;
            case NIGHT:
                dayparts[4] = daypart;
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

    /**
     * Gets the current days passed in the world related to this.
     * @return The current days passed.
     */
    public long getDaysPassed() {
        return this.daysPassed;
    }

    /**
     * Sets the current days passed.
     * @param daysPassed The current days passed.
     */
    public void setDaysPassed(long daysPassed) {
        this.daysPassed = daysPassed;
    }

    /**
     * Gets the {@link DayPartType} to wake up at.
     * @return The {@link DayPartType} to wake up at.
     */
    public DayPartType getWakeAtDayPart() {
        return this.wakeAtDayPart;
    }
}
