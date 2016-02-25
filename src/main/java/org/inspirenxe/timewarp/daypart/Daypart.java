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

public class Daypart {

    private final DaypartType type;
    private long length;

    public Daypart(DaypartType type, long length) {
        this.type = type;
        this.setLength(length);
    }

    /**
     * Gets the {@link DaypartType}.
     * @return The daypart type.
     */
    public DaypartType getType() {
        return type;
    }

    /**
     * Gets the custom length of the daypart.
     * @return The custom daypart length.
     */
    public long getLength() {
        return length;
    }

    /**
     * Sets the custom length of the daypart, cannot go below zero.
     * @param value The value to use for the daypart length.
     */
    public void setLength(long value) {
        if (value < 0) {
            value = 0;
        }
        length = value;
    }

    @Override
    public String toString() {
        return "Daypart{" +
                "name=" + type.name +
                ", length=" + length +
                ", defaultLength=" + type.defaultLength +
                ", defaultStartTime=" + type.defaultStartTime +
                ", defaultEndTime=" + type.defaultEndTime +
                '}';
    }
}
