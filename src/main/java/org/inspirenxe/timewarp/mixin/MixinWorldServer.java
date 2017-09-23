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
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(WorldServer.class)
public class MixinWorldServer implements IMixinWorldServer {
    private WorldDay cachedWorldDay;
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
        if (this.cachedWorldDay == null) {
            this.updateCache(worldInfo);
        } else {
            final Optional<WorldProperties> optProperties = Sponge.getServer().getWorldProperties(cachedWorldDay.worldName);

            if (optProperties.isPresent()) {

                // Set the days passed to the world day
                this.cachedWorldDay.setDaysPassed(optProperties.get().getWorldTime() / DayPartType.DEFAULT_DAY_LENGTH);

                // Get the current time remaining after we take away all full days
                long currentTime = optProperties.get().getWorldTime() % DayPartType.DEFAULT_DAY_LENGTH;

                final Optional<DayPartType> optDayPartType = DayPartType.getTypeFromTime(currentTime);

                if (optDayPartType.isPresent()) {
                    final Optional<DayPart> optDayPart = this.cachedWorldDay.getDayPart(optDayPartType.get());

                    if (optDayPart.isPresent()) {
                        if (optDayPart.get().getLength() == 0) {
                            // Skip the daypart
                            final Optional<DayPartType> optLastDayPartType = DayPartType.getTypeFromTime(currentTime - 1);
                            optLastDayPartType.ifPresent(dayPartType -> cachedWorldDay.getNextDayPart(dayPartType)
                                    .ifPresent(dayPart -> optProperties.get().setWorldTime(dayPart.getType().defaultStartTime + 1)));
                        } else if (this.ticksUntilIncrement <= 1) {
                            // Tick the world time up by one
                            optProperties.get().setWorldTime((this.cachedWorldDay.getDaysPassed() * DayPartType.DEFAULT_DAY_LENGTH) + ++currentTime);

                            // Get the difference between our current time and our target time
                            final long currentTimeScaled = scale(currentTime,
                                    optDayPartType.get().defaultStartTime,
                                    optDayPartType.get().defaultEndTime,
                                    this.cachedWorldDay.getStartTime(optDayPartType.get()),
                                    this.cachedWorldDay.getEndTime(optDayPartType.get()));
                            final long targetTimeScaled = scale(++currentTime,
                                    optDayPartType.get().defaultStartTime,
                                    optDayPartType.get().defaultEndTime,
                                    this.cachedWorldDay.getStartTime(optDayPartType.get()),
                                    this.cachedWorldDay.getEndTime(optDayPartType.get()));

                            this.ticksUntilIncrement = Math.abs(targetTimeScaled - currentTimeScaled);

                            this.updateCache(worldInfo);
                        } else {
                            // Tick down the time until we next increment the world time
                            this.ticksUntilIncrement--;
                        }

                        // Send time update packets to all players in this world
                        final long totalTime = optProperties.get().getTotalTime();
                        final long worldTime = optProperties.get().getWorldTime();
                        final boolean doDaylightCycle = Boolean.valueOf(optProperties.get().getGameRule("doDaylightCycle").orElse("false"));
                        Sponge.getServer().getWorld(worldInfo.getWorldName()).ifPresent(world -> world.getPlayers().forEach(player ->
                                ((EntityPlayerMP) player).connection.sendPacket(new SPacketTimeUpdate(totalTime, worldTime, doDaylightCycle))));

                        // We do not need to continue
                        return;
                    }
                }
            }
        }

        // Tick the world time as normal
        worldInfo.setWorldTime(worldInfo.getWorldTime() + originalValue);
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
        this.cachedWorldDay = null;
    }

    @Override
    public void updateCache(WorldInfo worldInfo) {
        this.cachedWorldDay = TimeWarp.getWorldDays().stream()
                .filter(worldDay -> worldDay.worldName.equalsIgnoreCase(worldInfo.getWorldName()))
                .findFirst().orElse(null);
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
        double scale = (double) (newEnd - newStart) / (originalEnd - originalStart);
        return (long) (newStart + ((time - originalStart) * scale));
    }
}
