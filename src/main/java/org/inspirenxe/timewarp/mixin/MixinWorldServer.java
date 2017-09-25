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
package org.inspirenxe.timewarp.mixin;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import org.inspirenxe.timewarp.TimeWarp;
import org.inspirenxe.timewarp.api.IMixinWorldServer;
import org.inspirenxe.timewarp.daypart.DayPart;
import org.inspirenxe.timewarp.daypart.DayPartType;
import org.inspirenxe.timewarp.world.WorldDay;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(WorldServer.class)
public class MixinWorldServer implements IMixinWorldServer {
    private WorldDay cachedWorldDay;
    private DayPartType cachedDayPartType;
    private DayPart cachedDayPart;
    private long ticksUntilIncrement = 0L;

    /**
     * Targets 'this.worldInfo.setWorldTime' in WorldServer#tick. Required for certain builds of Forge.
     * @author Steven Downer (Grinch)
     * @reason Intercept and handle logic for time incrementing
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/WorldInfo;setWorldTime(J)V"), require = 0, expect = 0)
    public void onIncrementTime(WorldInfo worldInfo, long value) {
        incrementTime(worldInfo, value);
    }

    /**
     * Targets 'this.setWorldTime' in WorldServer#tick. Required for certain builds of Forge.
     * @author Steven Downer (Grinch)
     * @reason Intercept and handle logic for time incrementing
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;setWorldTime(J)V"), require = 0, expect = 0)
    public void onIncrementTime(net.minecraft.world.WorldServer world, long value) {
        incrementTime(world.getWorldInfo(), value);
    }

    private void incrementTime(WorldInfo worldInfo, long originalValue) {
        final WorldProperties worldProperties = (WorldProperties) worldInfo;

        // Update our cache if needed
        if (this.cachedWorldDay == null
                && TimeWarp.getSupportedDimensionTypes().contains(worldProperties.getDimensionType())
                && TimeWarp.instance.storage.getChildNode("sync.worlds." + worldInfo.getWorldName().toLowerCase() + ".enabled").getBoolean()) {
            this.cachedWorldDay = new WorldDay(worldProperties.getWorldName()).init();
        }

        // Attempt to continue with our logic
        if (this.cachedWorldDay != null) {
            // Set the days passed to the world day
            this.cachedWorldDay.setDaysPassed(worldProperties.getWorldTime() / DayPartType.DEFAULT_DAY_LENGTH);

            // Get the current time remaining after we take away all full days
            long currentTime = worldProperties.getWorldTime() % DayPartType.DEFAULT_DAY_LENGTH;

            // Update our cache if needed
            if (this.cachedDayPartType == null) {
                this.cachedDayPartType = DayPartType.getTypeFromTime(currentTime).orElse(null);
            }

            // Attempt to continue with our logic
            if (this.cachedDayPartType != null) {

                // Update our cache if needed
                if (cachedDayPart == null) {
                    this.cachedDayPart = this.cachedWorldDay.getDayPart(this.cachedDayPartType).orElse(null);
                }

                // Attempt to continue with our logic
                if (this.cachedDayPart != null) {
                    if (this.cachedDayPart.getLength() == 0) {
                        // Skip the daypart
                        DayPartType.getTypeFromTime(currentTime - 1)
                                .ifPresent(dayPartType -> cachedWorldDay.getNextDayPart(dayPartType)
                                        .ifPresent(dayPart -> worldProperties.setWorldTime(dayPart.getType().defaultStartTime + 1)));

                        // Clear cache
                        this.clearDayPartCache();
                        this.clearDayPartTypeCache();

                        // We do not need to continue
                        return;
                    } else if (this.ticksUntilIncrement <= 1) {
                        // Tick the world time up by one
                        worldProperties.setWorldTime((this.cachedWorldDay.getDaysPassed() * DayPartType.DEFAULT_DAY_LENGTH) + ++currentTime);

                        // Get the difference between our current time and our target time
                        final long currentTimeScaled = scale(currentTime,
                                this.cachedDayPartType.defaultStartTime,
                                this.cachedDayPartType.defaultEndTime,
                                this.cachedWorldDay.getStartTime(this.cachedDayPartType),
                                this.cachedWorldDay.getEndTime(this.cachedDayPartType));
                        final long targetTimeScaled = scale(++currentTime,
                                this.cachedDayPartType.defaultStartTime,
                                this.cachedDayPartType.defaultEndTime,
                                this.cachedWorldDay.getStartTime(this.cachedDayPartType),
                                this.cachedWorldDay.getEndTime(this.cachedDayPartType));

                        this.ticksUntilIncrement = Math.abs(targetTimeScaled - currentTimeScaled);

                        if (!DayPartType.isWithinTimeRange(this.cachedDayPartType, currentTime)) {
                            this.clearDayPartCache();
                            this.clearDayPartTypeCache();
                        }
                    } else {
                        // Tick down the time until we next increment the world time
                        this.ticksUntilIncrement--;
                    }

                    // Send time update packets to all players in this world
                    final long totalTime = worldProperties.getTotalTime();
                    final long worldTime = worldProperties.getWorldTime();
                    final boolean doDaylightCycle = Boolean.valueOf(worldProperties.getGameRule("doDaylightCycle").orElse("false"));
                    ((World) this).getPlayers().forEach(player ->
                            ((EntityPlayerMP) player).connection.sendPacket(new SPacketTimeUpdate(totalTime, worldTime, doDaylightCycle)));

                    // We do not need to continue
                    return;
                }
            }
        }

        // Tick the world time as normal
        worldProperties.setWorldTime(worldProperties.getWorldTime() + originalValue);
    }

    @Override
    public long getTicksUntilNextIncrement() {
        return this.ticksUntilIncrement;
    }

    @Override
    public void setTicksUntilNextIncrement(long ticksUntilIncrement) {
        this.ticksUntilIncrement = ticksUntilIncrement;
    }

    @Override
    public void clearCache() {
        TimeWarp.instance.logger.debug("Clearing all cache types for world [" + ((World) this).getName() + "]");
        this.cachedWorldDay = null;
        this.cachedDayPart = null;
        this.cachedDayPartType = null;
    }

    @Override
    public void clearWorldDayCache() {
        TimeWarp.instance.logger.debug("Clearing cache type [WorldDay] for world [" + ((World) this).getName() + "]");
        this.cachedWorldDay = null;
    }

    @Override
    public void clearDayPartCache() {
        TimeWarp.instance.logger.debug("Clearing cache type [DayPart] for world [" + ((World) this).getName() + "]");
        this.cachedDayPart = null;
    }

    @Override
    public void clearDayPartTypeCache() {
        TimeWarp.instance.logger.debug("Clearing cache type [DayPartType] for world [" + ((World) this).getName() + "]");
        this.cachedDayPartType = null;
    }

    @Override
    public Optional<WorldDay> getCachedWorldDay() {
        return Optional.ofNullable(this.cachedWorldDay);
    }

    @Override
    public Optional<DayPart> getCachedDayPart() {
        return Optional.ofNullable(this.cachedDayPart);
    }

    @Override
    public Optional<DayPartType> getCachedDayPartType() {
        return Optional.ofNullable(this.cachedDayPartType);
    }

    /**
     * Scales time from one range to another.
     *
     * @param time The time to scale.
     * @param originalStart The original start time.
     * @param originalEnd The original end time.
     * @param newStart The new start time.
     * @param newEnd The new end time.
     * @return The scaled time.
     */
    private static long scale(long time, long originalStart, long originalEnd, long newStart, long newEnd) {
        double scale = (newEnd - newStart) / (originalEnd - originalStart);
        return (long) (newStart + ((time - originalStart) * scale));
    }
}
