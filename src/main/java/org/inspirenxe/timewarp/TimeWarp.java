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
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.SleepingEvent;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameLoadCompleteEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Plugin(id = "timewarp", name = "TimeWarp")
public class TimeWarp {

    private static final Set<DimensionType> SUPPORTED_DIMENSION_TYPES = Sets.newHashSet();
    private static final Set<WorldDay> WORLD_DAYS = Sets.newHashSet();
    public static TimeWarp INSTANCE;
    public Storage storage;
    @Inject public Logger logger;
    @Inject public PluginContainer container;
    @DefaultConfig(sharedRoot = true)
    @Inject private File configuration;
    @DefaultConfig(sharedRoot = true)
    @Inject private ConfigurationLoader<CommentedConfigurationNode> loader;

    @Listener
    public void onGameConstructionEvent(GameConstructionEvent event) {
        INSTANCE = this;
        storage = new Storage(container, configuration, loader);
        storage.registerDefaultNode("sync.settings.dimensions", Collections.singletonList("overworld"));
    }

    @Listener
    public void onGameLoadCompleteEvent(GameLoadCompleteEvent event) throws ObjectMappingException {
        for (String type : storage.getChildNode("sync.settings.dimensions").getList(TypeToken.of(String.class))) {
            Optional<DimensionType> optType = Sponge.getRegistry().getType(DimensionType.class, type.toLowerCase());
            optType.ifPresent(SUPPORTED_DIMENSION_TYPES::add);
        }
    }

    @Listener
    public void onGameStartedServerEvent(GameStartedServerEvent event) {
        this.createWorldDays();
    }

    @Listener
    public void onGameInitializationEvent(GameInitializationEvent event) {
        Commands.add(CommandSpec.builder()
                .permission("timewarp.command.reload")
                .description(Text.of("Reloads the configuration settings from disk."))
                .executor((src, args) -> {
                    this.createWorldDays();
                    src.sendMessage(Text.of("TimeWarp reloaded."));
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
        Sponge.getServer().getWorldProperties(event.getBed().getWorldUniqueId()).ifPresent(worldProperties -> worldProperties.setWorldTime(DayPartType.MORNING.defaultStartTime));
    }

    /**
     * Creates a {@link WorldDay} for every applicable world.
     */
    private void createWorldDays() {
        // Initialize the configuration
        storage.init();

        // Clear the set to ensure a fresh start.
        WORLD_DAYS.clear();

        for (World world : Sponge.getServer().getWorlds()) {
            if (!TimeWarp.getSupportedDimensionTypes().contains(world.getDimension().getType())) {
                continue;
            }

            if (!storage.getChildNode("sync.worlds." + world.getName().toLowerCase() + ".enabled").getBoolean()) {
                continue;
            }

            if (!Boolean.valueOf(world.getProperties().getGameRule("doDaylightCycle").get())) {
                logger.warn("Unable to warp time for [" + world.getName() + "]. Please enable the daylight cycle (/gamerule doDaylightCycle true) " +
                        "and reload TimeWarp. If this is intentional then please ignore this message.");
            }

            ((IMixinWorldServer) world).setTicksUntilNextIncrement(0L);

            final Optional<WorldProperties> optProperties = Sponge.getServer().getWorldProperties(world.getName());
            if (optProperties.isPresent() && TimeWarp.getSupportedDimensionTypes().contains(optProperties.get().getDimensionType())) {
                final String rootPath = "sync.worlds." + world.getName().toLowerCase();
                TimeWarp.INSTANCE.storage.registerDefaultNode(rootPath + ".enabled", false);

                for (DayPartType type : DayPartType.values()) {
                    TimeWarp.INSTANCE.storage.registerDefaultNode(rootPath + ".dayparts." + type.name.toLowerCase(), type.defaultLength);
                }

                if (TimeWarp.INSTANCE.storage.getChildNode(rootPath + ".enabled").getBoolean()) {
                    WORLD_DAYS.add(new WorldDay(world.getName()).init());
                }
            }
        }
    }

    /**
     * Gets the supported {@link DimensionType}s.
     * @return An unmodifiable set of allowed {@link DimensionType}.
     */
    public static Set<DimensionType> getSupportedDimensionTypes() {
        return Collections.unmodifiableSet(SUPPORTED_DIMENSION_TYPES);
    }

    /**
     * Gets all available {@link WorldDay}s
     * @return An unmodifiable set of all {@link WorldDay}.
     */
    public static Set<WorldDay> getWorldDays() {
        return Collections.unmodifiableSet(WORLD_DAYS);
    }
}
