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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public class Storage {

    private ConfigurationLoader<CommentedConfigurationNode> loader;
    private CommentedConfigurationNode rootNode;
    private final Map<String, Object> defaultNodes = Maps.newTreeMap();

    public Storage(File configuration, ConfigurationLoader<CommentedConfigurationNode> loader) throws IOException {
        this.loader = loader;
        if (!configuration.exists()) {
            configuration.createNewFile();

            this.rootNode = this.loader.load(ConfigurationOptions.defaults().setHeader(
                    "For details regarding this configuration file please refer " +
                    "to our wiki page <https://github.com/InspireNXE/WorldSync/wiki/>"));
            this.loader.save(rootNode);
        }
        this.rootNode = this.loader.load();

        for (World world : Sponge.getServer().getWorlds()) {
            if (!world.getDimension().getType().equals(DimensionTypes.OVERWORLD)) {
                continue;
            }
            final String name = world.getName().toLowerCase();
            registerDefaultNode("sync.worlds." + name + ".timezone", ZoneId.systemDefault().toString());
            registerDefaultNode("sync.worlds." + name + ".enabled", true);
            registerDefaultNode("sync.worlds." + name + ".length", 86400000);
        }
    }

    public Storage load() throws IOException {
        defaultNodes.entrySet().stream().filter(entry -> entry.getValue() != null).forEach(entry -> {
            final CommentedConfigurationNode node = this.getChildNode(entry.getKey());
            if (node.getValue() == null) {
                this.getChildNode(entry.getKey()).setValue(entry.getValue());
            }
        });
        final Queue<CommentedConfigurationNode> queue = Queues.newConcurrentLinkedQueue();
        queue.add(this.rootNode);
        while (!queue.isEmpty()) {
            final CommentedConfigurationNode node = queue.remove();
            if (node.getParent() != null && node.getPath() != null && node.getValue() != null) {
                final String path = Joiner.on(",").skipNulls().join(node.getPath()).replace(",", ".");
                if (!this.defaultNodes.containsKey(path)) {
                    node.setValue(null);
                }
            }
            if (node.hasMapChildren()) {
                for (Map.Entry<Object, ? extends CommentedConfigurationNode> entry : node.getChildrenMap().entrySet()) {
                    queue.add(entry.getValue());
                }
            }
        }
        this.loader.save(rootNode);
        this.loader.load();
        return this;
    }

    /**
     * Registers a node as a default node to compare against when loading.
     * @param path The path to register
     * @param value The value to register
     */
    public void registerDefaultNode(String path, Object value) {
        final String[] nodes = path.split("\\.");
        final List<String> currentPath = Lists.newArrayList();
        for (int i = 0; i < nodes.length; i++) {
            if (i < nodes.length - 1) {
                currentPath.add(i, nodes[i]);
                final String joinedPath = Joiner.on(",").skipNulls().join(currentPath).replace(",", ".");
                this.defaultNodes.put(joinedPath, null);
            } else {
                this.defaultNodes.put(path, value);
            }
        }
    }

    /**
     * Gets the node from the root node
     * @param path The path to the node split by periods
     * @return The {@link CommentedConfigurationNode}
     */
    public CommentedConfigurationNode getChildNode(String path) {
        return this.rootNode.getNode((Object[]) path.split("\\."));
    }
}
