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

import com.google.common.collect.Maps;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Commands {

    private static final Map<List<String>, CommandSpec> children = Maps.newHashMap();

    /**
     * Add a child command with aliases.
     * @param child The child command.
     * @param aliases The child aliases to register with.
     */
    public static void add(CommandSpec child, String... aliases) {
        children.put(Arrays.asList(aliases), child);
    }

    /**
     * Register commands using only the {@link PluginContainer}'s name as an alias.
     * @param container The {@link PluginContainer}.
     */
    public static void register(PluginContainer container) {
        register(container, container.getId().toLowerCase());
    }

    /**
     * Register commands with aliases.
     * @param container The {@link PluginContainer}.
     * @param aliases The master aliases to register with.
     */
    public static void register(PluginContainer container, String... aliases) {
        Sponge.getCommandManager().register(container, CommandSpec.builder().children(children).build(), aliases);
    }
}
