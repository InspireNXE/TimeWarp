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
import org.inspirenxe.timewarp.tasks.WorldSyncTask;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameLoadCompleteEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Plugin(id = "timewarp", name = "TimeWarp")
public class TimeWarp {

    private static final Set<Task> SYNC_TASKS = Sets.newHashSet();
    private static final Set<DimensionType> ALLOWED_DIMENSION_TYPES = Sets.newHashSet();
    public static TimeWarp INSTANCE;
    public Storage storage;
    @Inject public Logger logger;
    @Inject public PluginContainer container;
    @DefaultConfig(sharedRoot = true)
    @Inject private File configuration;
    @DefaultConfig(sharedRoot = true)
    @Inject private ConfigurationLoader<CommentedConfigurationNode> loader;

    @Listener
    public void onGameConstructionEvent(GameConstructionEvent event) throws IOException {
        INSTANCE = this;
        storage = new Storage(container, configuration, loader).load();
        storage.registerDefaultNode("sync.settings.dimensions", Arrays.asList("overworld"));
    }


    @Listener
    public void onGameLoadCompleteEvent(GameLoadCompleteEvent event) throws ObjectMappingException {
        for (String dimensionType : storage.getChildNode("sync.settings.dimensions").getList(TypeToken.of(String.class))) {
            Optional<DimensionType> dimensionTypeOpt = Sponge.getRegistry().getType(DimensionType.class, dimensionType.toLowerCase());
            if (dimensionTypeOpt.isPresent()) {
                ALLOWED_DIMENSION_TYPES.add(dimensionTypeOpt.get());
            }
        }
    }

    @Listener
    public void onGameStartedServerEvent(GameStartedServerEvent event) {
        for (World world : Sponge.getServer().getWorlds()) {
            // Only run logic for worlds that match allowed dimensions
            if (!ALLOWED_DIMENSION_TYPES.contains(world.getDimension().getType())) {
                continue;
            }

            final String name = world.getName().toLowerCase();

            // Register default configuration settings
            storage.registerDefaultNode("sync.worlds." + name + ".timezone", ZoneId.systemDefault().toString());
            storage.registerDefaultNode("sync.worlds." + name + ".enabled", true);
            storage.registerDefaultNode("sync.worlds." + name + ".length", 86400000);

            // Create tasks for syncing for each world if enabled
            if (storage.getChildNode("sync.worlds." + name + ".enabled").getBoolean()) {
                final WorldSyncTask worldSyncTask = new WorldSyncTask(
                        world,
                        storage.getChildNode("sync.worlds." + name + ".timezone").getString("America/Chicago"),
                        storage.getChildNode("sync.worlds." + name + ".length").getLong(86400000));
                SYNC_TASKS.add(Task.builder()
                        .name(name + " - TimeWarp Sync")
                        .intervalTicks(1)
                        .execute(worldSyncTask)
                        .submit(container));
            }
        }
    }


    public static Set<Task> getSyncTasks() {
        return Collections.unmodifiableSet(SYNC_TASKS);
    }

    public static Set<DimensionType> getAllowedDimensionTypes() {
        return Collections.unmodifiableSet(ALLOWED_DIMENSION_TYPES);
    }
}
