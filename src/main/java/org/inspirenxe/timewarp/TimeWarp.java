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
package org.inspirenxe.timewarp;

import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.world;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.inspirenxe.timewarp.api.IMixinWorldServer;
import org.inspirenxe.timewarp.daypart.DayPartType;
import org.inspirenxe.timewarp.util.Commands;
import org.inspirenxe.timewarp.util.Storage;
import org.inspirenxe.timewarp.world.WorldDay;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.SleepingEvent;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Plugin(id = "timewarp", name = "TimeWarp", description = "Manipulate time itself and extend the time of day!")
public class TimeWarp {

    private static final Set<DimensionType> SUPPORTED_DIMENSION_TYPES = Sets.newHashSet();
    public static TimeWarp instance;
    public Storage storage;
    @Inject public Logger logger;
    @Inject public PluginContainer container;
    @DefaultConfig(sharedRoot = true)
    @Inject private File configuration;
    @DefaultConfig(sharedRoot = true)
    @Inject private ConfigurationLoader<CommentedConfigurationNode> loader;

    @Listener
    public void onGameConstructionEvent(GameConstructionEvent event) {
        instance = this;
        storage = new Storage(container, configuration, loader);
        storage.registerDefaultNode("sync.settings.dimensions", Collections.singletonList("overworld"));
    }

    @Listener
    public void onGameStartedServerEvent(GameStartedServerEvent event) throws ObjectMappingException {
        for (String type : storage.getChildNode("sync.settings.dimensions").getList(TypeToken.of(String.class))) {
            Optional<DimensionType> optType = Sponge.getRegistry().getType(DimensionType.class, type.toLowerCase());
            optType.ifPresent(SUPPORTED_DIMENSION_TYPES::add);
        }

        this.createWorldDays();
    }

    @Listener
    public void onGameInitializationEvent(GameInitializationEvent event) {
        Commands.add(CommandSpec.builder()
                .permission("timewarp.command.daypart")
                .arguments(optional(world(Text.of("world"))))
                .description(Text.of("Gets the current DayPart."))
                .executor((src, args) -> {
                    Optional<WorldProperties> optWorld = args.getOne("world");
                    if (!optWorld.isPresent() && src instanceof Player) {
                        optWorld = Optional.of(((Player) src).getWorld().getProperties());
                    } else if (!optWorld.isPresent()) {
                        throw new CommandException(Text.of("A world must be provided if command sender is not a player."));
                    }

                    final long currentTime = optWorld.get().getWorldTime() % DayPartType.DEFAULT_DAY_LENGTH;

                    Optional<DayPartType> optDayPartType = DayPartType.getTypeFromTime(currentTime);
                    if (!optDayPartType.isPresent()) {
                        throw new CommandException(Text.of("Unable to parse DayPart from time [", TextColors.GRAY, currentTime,TextColors.RED, "]"));
                    }

                    src.sendMessage(Text.of("Current DayPart is [", optDayPartType.get().color, optDayPartType.get().name, TextColors.RESET,
                            "] based on time [", TextColors.GRAY, currentTime, TextColors.RESET, "]"));
                    return CommandResult.success();
                })
                .build(), "daypart");
        Commands.add(CommandSpec.builder()
                .permission("timewarp.command.reload")
                .description(Text.of("Reloads the configuration settings from disk."))
                .executor((src, args) -> {
                    this.createWorldDays();
                    src.sendMessage(Text.of("TimeWarp reloaded."));
                    if (src instanceof Player) {
                        logger.info("TimeWarp reloaded by " + src.getName());
                    }
                    return CommandResult.success();
                })
                .build(), "reload");

        Commands.register(container, container.getId(), "tw");
    }

    @Listener
    public void onGameReloadEvent(GameReloadEvent event) {
        this.createWorldDays();
    }

    @Listener
    public void onSleepingFinishPostEvent(SleepingEvent.Finish.Post event) {
        event.getBed().getLocation().ifPresent(location -> {
            final Optional<WorldDay> optWorldDay = ((IMixinWorldServer) location.getExtent()).getCachedWorldDay();
            optWorldDay.ifPresent(worldDay -> location.getExtent().getProperties().setWorldTime(
                    (worldDay.getDaysPassed() * DayPartType.DEFAULT_DAY_LENGTH) + worldDay.getWakeAtDayPart().defaultStartTime + 1));
        });
    }

    private void createWorldDays() {
        // Initialize the configuration
        storage.init();

        for (World world : Sponge.getServer().getWorlds()) {
            if (!TimeWarp.getSupportedDimensionTypes().contains(world.getDimension().getType())) {
                continue;
            }

            if (!Boolean.valueOf(world.getProperties().getGameRule("doDaylightCycle").orElse("false"))) {
                logger.warn("Unable to warp time for [" + world.getName() + "]. Please enable the daylight cycle (/gamerule doDaylightCycle true) " +
                        "and reload TimeWarp. If this is intentional then please ignore this message.");
            }

            ((IMixinWorldServer) world).setTicksUntilNextIncrement(0L);
            ((IMixinWorldServer) world).clearCache();

            final WorldProperties worldProperties = world.getProperties();
            if (TimeWarp.getSupportedDimensionTypes().contains(worldProperties.getDimensionType())) {
                final String worldRootPath = "sync.worlds." + world.getName().toLowerCase();
                TimeWarp.instance.storage.registerDefaultNode(worldRootPath + ".enabled", false);
                TimeWarp.instance.storage.registerDefaultNode(worldRootPath + ".wake-at-daypart", DayPartType.DAY.name.toUpperCase());

                for (DayPartType type : DayPartType.values()) {
                    TimeWarp.instance.storage.registerDefaultNode(worldRootPath + ".dayparts." + type.name.toLowerCase(), type.defaultLength);
                }
            }
        }

        TimeWarp.instance.storage.save();
    }

    /**
     * Gets the supported {@link DimensionType}s.
     * @return An unmodifiable set of allowed {@link DimensionType}.
     */
    public static Set<DimensionType> getSupportedDimensionTypes() {
        return Collections.unmodifiableSet(SUPPORTED_DIMENSION_TYPES);
    }
}
