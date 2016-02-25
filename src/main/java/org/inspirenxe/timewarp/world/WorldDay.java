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
import org.inspirenxe.timewarp.daypart.Daypart;
import org.inspirenxe.timewarp.daypart.DaypartType;
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
    private final Daypart[] dayparts = new Daypart[4];

    public WorldDay(String worldName) {
        this.worldName = worldName;
        this.init();
    }

    /**
     * Initializes all dayparts.
     * @return {@link WorldDay} for chaining.
     */
    public WorldDay init() {
        final Optional<WorldProperties> optProperties = Sponge.getServer().getWorldProperties(worldName);
        if (optProperties.isPresent()) {
            final String rootPath = "sync.worlds." + worldName.toLowerCase();
            for (DaypartType type : DaypartType.values()) {
                this.setDaypart(type, new Daypart(type, TimeWarp.INSTANCE.storage.getChildNode(rootPath + ".dayparts." + type.name.toLowerCase())
                        .getLong()));
            }
        }
        return this;
    }

    /**
     * Gets the {@link Daypart} from the {@link DaypartType}.
     * @param type The {@link DaypartType} to get the {@link Daypart} from.
     * @return The {@link Optional<Daypart>}.
     */
    public Optional<Daypart> getDaypart(DaypartType type) {
        for (Daypart daypart : dayparts) {
            if (daypart.getType().equals(type)) {
                return Optional.of(daypart);
            }
        }
        return Optional.empty();
    }

    /**
     * Sets the daypart stored to the {@link Daypart} passed in.
     * @param type The {@link DaypartType} to set.
     * @param daypart The {@link Daypart} to set to.
     */
    public void setDaypart(DaypartType type, Daypart daypart) {
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
        for (Daypart daypart : dayparts) {
            length += daypart.getLength();
        }
        return length;
    }

    /**
     * Gets the start time of the {@link Daypart}.
     * @param type The {@link DaypartType} to get the start time from.
     * @return The start time.
     */
    public long getStartTime(DaypartType type) {
        long start = 0;
        for (Daypart daypart : dayparts) {
            if (type.equals(daypart.getType())) {
                return start;
            }
            start += daypart.getLength();
        }
        return start;
    }

    /**
     * Gets the end time of the {@link Daypart}.
     * @param type The {@link DaypartType} to get the end time from.
     * @return The end time.
     */
    public long getEndTime(DaypartType type) {
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
