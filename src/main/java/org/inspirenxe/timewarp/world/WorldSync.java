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
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

public class WorldSync {

    public final String worldName;
    public WorldDay worldDay;
    public WorldSyncRunnable runnable;
    public Task task;

    public WorldSync(String worldName) {
        this.worldName = worldName;
        this.init();
    }

    /**
     * Initializes the {@link WorldSync}.
     * @return {@link WorldSync} for chaining.
     */
    public WorldSync init() {
        final Optional<WorldProperties> optProperties = Sponge.getServer().getWorldProperties(worldName);
        if (optProperties.isPresent() && TimeWarp.getSupportedDimensionTypes().contains(optProperties.get().getDimensionType())) {
            final String rootPath = "sync.worlds." + worldName.toLowerCase();
            TimeWarp.INSTANCE.storage.registerDefaultNode(rootPath + ".enabled", false);
            for (DaypartType type : DaypartType.values()) {
                TimeWarp.INSTANCE.storage.registerDefaultNode(rootPath + ".dayparts." + type.name.toLowerCase(), type.defaultLength);
            }

            if (TimeWarp.INSTANCE.storage.getChildNode(rootPath + ".enabled").getBoolean()) {
                worldDay = new WorldDay(worldName);
                runnable = new WorldSyncRunnable(this);
                task = Task.builder()
                        .name(worldName + " - TimeWarp Sync")
                        .intervalTicks(1)
                        .execute(runnable)
                        .submit(TimeWarp.INSTANCE.container);
            }
        }
        return this;
    }
}
