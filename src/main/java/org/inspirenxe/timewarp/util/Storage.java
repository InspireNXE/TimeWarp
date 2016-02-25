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
package org.inspirenxe.timewarp.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Storage {

    private ConfigurationLoader<CommentedConfigurationNode> loader;
    private CommentedConfigurationNode rootNode;
    private final File configuration;
    private final Map<String, Object> defaultNodes = Maps.newTreeMap();
    private final Logger logger;

    public Storage(PluginContainer container, File configuration, ConfigurationLoader<CommentedConfigurationNode> loader) {
        logger = LoggerFactory.getLogger(container.getName() + " - Storage");
        this.loader = loader;
        this.configuration = configuration;
        this.init();
    }

    /**
     * Initializes the configuration file. File is created if non-existant.
     * @return {@link Storage} for chaining.
     */
    public Storage init() {
        if (!configuration.exists()) {
            try {
                configuration.createNewFile();
            } catch (IOException e) {
                logger.error("Unable to create new configuration file!", e);
            }
        }
        try {
            rootNode = this.loader.load();
        } catch (IOException e) {
            logger.error("Unable to load configuration file!", e);
        }
        return this;
    }

    /**
     * Loads the configuration file.
     * @return {@link Storage} for chaining.
     */
    public Storage load() {
        defaultNodes.entrySet().stream().filter(entry -> entry.getValue() != null).forEach(entry -> {
            final CommentedConfigurationNode node = getChildNode(entry.getKey());
            if (node.getValue() == null) {
                getChildNode(entry.getKey()).setValue(entry.getValue());
            }
        });
        final Queue<CommentedConfigurationNode> queue = Queues.newConcurrentLinkedQueue();
        queue.add(rootNode);
        while (!queue.isEmpty()) {
            final CommentedConfigurationNode node = queue.remove();
            if (node.hasMapChildren()) {
                for (Map.Entry<Object, ? extends CommentedConfigurationNode> entry : node.getChildrenMap().entrySet()) {
                    queue.add(entry.getValue());
                }
            }
        }
        save();
        try {
            loader.load();
        } catch (IOException e) {
            logger.error("Unable to load configuration!", e);
        }
        return this;
    }

    /**
     * Saves the configuration file.
     * @return {@link Storage} for chaining.
     */
    public Storage save() {
        try {
            loader.save(rootNode);
        } catch (IOException e) {
            logger.error("Unable to save configuration!", e);
        }
        return this;
    }

    /**
     * Registers a default node. Calls {@link Storage#save()} and {@link Storage#load()}.
     * @param path The path to register.
     * <p>The path is split by a period for example "path.to.node" is the equivalent of...
     * path {
     *     to {
     *         node=""
     *     }
     * }</p>
     * @param value The value to register.
     */
    public void registerDefaultNode(String path, Object value) {
        final String[] nodes = path.split("\\.");
        final List<String> currentPath = Lists.newArrayList();
        for (int i = 0; i < nodes.length; i++) {
            if (i < nodes.length - 1) {
                currentPath.add(i, nodes[i]);
                final String joinedPath = Joiner.on(",").skipNulls().join(currentPath).replace(",", ".");
                defaultNodes.put(joinedPath, null);
            } else {
                defaultNodes.put(path, value);
            }
        }
        this.save();
        this.load();
    }

    /**
     * Gets the node from the root node.
     * @param path The path to the node split by periods.
     * @return The {@link CommentedConfigurationNode}.
     */
    public CommentedConfigurationNode getChildNode(String path) {
        return rootNode.getNode((Object[]) path.split("\\."));
    }
}
