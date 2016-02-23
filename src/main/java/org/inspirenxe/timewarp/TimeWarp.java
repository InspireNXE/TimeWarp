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
import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.inspirenxe.timewarp.tasks.WorldSyncTask;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@Plugin(id = "timewarp", name = "TimeWarp")
public class TimeWarp {

    private static final Set<Task> SYNC_TASKS = Sets.newHashSet();
    public Storage storage;
    @Inject public Logger logger;
    @Inject public PluginContainer container;
    @DefaultConfig(sharedRoot = true)
    @Inject private File configuration;
    @DefaultConfig(sharedRoot = true)
    @Inject private ConfigurationLoader<CommentedConfigurationNode> loader;


    @Listener
    public void onGameStartedEvent(GameStartedServerEvent event) throws IOException {
        storage = new Storage(configuration, loader).load();
        for (World world : Sponge.getServer().getWorlds()) {
            final String name = world.getName();
            if (storage.getChildNode("sync.worlds." + name.toLowerCase() + ".enabled").getBoolean()) {
                if (world.getDimension().getType().equals(DimensionTypes.OVERWORLD)) {
                    final WorldSyncTask worldSyncTask = new WorldSyncTask(
                            world,
                            storage.getChildNode("sync.worlds." + name.toLowerCase() + ".timezone").getString("America/Chicago"),
                            storage.getChildNode("sync.worlds." + name.toLowerCase() + ".length").getLong(86400000));
                    SYNC_TASKS.add(Task.builder()
                            .name(name + " - TimeWarp Sync")
                            .intervalTicks(1)
                            .execute(worldSyncTask)
                            .submit(container));
                }
            }
        }
    }


    public static Set<Task> getSyncTasks() {
        return Collections.unmodifiableSet(SYNC_TASKS);
    }
}
