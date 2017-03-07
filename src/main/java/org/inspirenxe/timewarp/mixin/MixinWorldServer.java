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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(WorldServer.class)
public class MixinWorldServer implements IMixinWorldServer {
    private long ticksUntilIncrement = 0L;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/WorldInfo;setWorldTime(J)V"))
    public void onIncrementTime(WorldInfo worldInfo, long value) {
        TimeWarp.getWorldDays().forEach(worldDay -> {
            if (worldDay.worldName.equals(worldInfo.getWorldName())) {
                Optional<WorldProperties> optProperties = Sponge.getServer().getWorldProperties(worldInfo.getWorldName());

                if (optProperties.isPresent()) {
                    final long currentTime = optProperties.get().getWorldTime() % DayPartType.DEFAULT_DAY_LENGTH;
                    final Optional<DayPartType> optDayPartType = DayPartType.getTypeFromTime(currentTime);

                    if (optDayPartType.isPresent()) {
                        Optional<DayPart> optDayPart = worldDay.getDayPart(optDayPartType.get());
                        if (optDayPart.isPresent()) {
                            if (optDayPart.get().getLength() == 0) {
                                worldInfo.setWorldTime(optDayPartType.get().defaultEndTime + 1);
                            } else if (ticksUntilIncrement <= 1) {
                                worldInfo.setWorldTime(currentTime + 1L);

                                final long targetTimeScaled = scale(currentTime + 1L,
                                        optDayPartType.get().defaultStartTime,
                                        optDayPartType.get().defaultEndTime,
                                        worldDay.getStartTime(optDayPartType.get()),
                                        worldDay.getEndTime(optDayPartType.get()));
                                final long nextTargetTimeScaled = scale(currentTime + 2L,
                                        optDayPartType.get().defaultStartTime,
                                        optDayPartType.get().defaultEndTime,
                                        worldDay.getStartTime(optDayPartType.get()),
                                        worldDay.getEndTime(optDayPartType.get()));

                                ticksUntilIncrement = Math.abs(nextTargetTimeScaled - targetTimeScaled);
                            } else {
                                ticksUntilIncrement--;
                            }
                        }
                    }
                }
            }
            final Optional<World> optWorld = Sponge.getServer().getWorld(worldInfo.getWorldName());
            if (optWorld.isPresent()) {
                final long totalTime = optWorld.get().getProperties().getTotalTime();
                final long worldTime = optWorld.get().getProperties().getWorldTime();
                final boolean doDaylightCycle = Boolean.valueOf(optWorld.get().getGameRule("doDaylightCycle").get());

                optWorld.get().getPlayers().forEach(player -> {
                    ((EntityPlayerMP) player).connection.sendPacket(new SPacketTimeUpdate(totalTime, worldTime, doDaylightCycle));
                });
            }
        });
    }

    public long getTicksUntilNextIncrement() {
        return this.ticksUntilIncrement;
    }

    public void setTicksUntilNextIncrement(long ticksUntilIncrement) {
        this.ticksUntilIncrement = ticksUntilIncrement;
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
    private long scale(long time, long originalStart, long originalEnd, long newStart, long newEnd) {
        double scale = (double) (newEnd - newStart) / (originalEnd - originalStart);
        return (long) (newStart + ((time - originalStart) * scale));
    }
}
