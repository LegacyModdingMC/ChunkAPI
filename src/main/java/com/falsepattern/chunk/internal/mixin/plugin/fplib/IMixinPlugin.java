/*
 * This file is part of FalsePatternLib.
 *
 * Copyright (C) 2022-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * FalsePatternLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * FalsePatternLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FalsePatternLib. If not, see <https://www.gnu.org/licenses/>.
 */
package com.falsepattern.chunk.internal.mixin.plugin.fplib;

import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.minecraft.launchwrapper.Launch;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.nio.file.Files.walk;

public interface IMixinPlugin extends IMixinConfigPlugin {
    Path MODS_DIRECTORY_PATH = FileUtil.getMinecraftHome().toPath().resolve("mods");

    static Logger createLogger(String modName) {
        return LogManager.getLogger(modName + " Mixin Loader");
    }

    /**
     * @deprecated since 1.4.0
     */
    @Deprecated
    static File findJarOf(final ITargetedMod mod) {
        File result = null;
        try (val stream = walk(MODS_DIRECTORY_PATH)) {
            result = stream.filter(mod::isMatchingJar).map(Path::toFile).findFirst().orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result == null) {
            File found = null;
            for (URL url : Launch.classLoader.getURLs()) {
                try {
                    val file = new File(url.toURI());
                    Path path = file.toPath();
                    if (mod.isMatchingJar(path)) {
                        found = file;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
            result = found;
        }
        return result;
    }

    /**
     * @since 1.4.0
     */
    static Set<File> findJarsOf(IMixinPlugin self, final ITargetedMod mod) {
        if (!self.useNewFindJar()) {
            val jar = findJarOf(mod);
            return jar == null ? Collections.emptySet() : Collections.singleton(jar);
        }
        val results = new HashSet<File>();
        try (val stream = walk(MODS_DIRECTORY_PATH)) {
            results.addAll(stream.filter(mod::isMatchingJar).map(Path::toFile).collect(Collectors.toSet()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (URL url : Launch.classLoader.getURLs()) {
            try {
                val file = new File(url.toURI());
                Path path = file.toPath();
                if (mod.isMatchingJar(path)) {
                    results.add(file);
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        return results;
    }

    Logger getLogger();

    IMixin[] getMixinEnumValues();

    ITargetedMod[] getTargetedModEnumValues();

    /**
     * @since 1.4.0
     */
    default boolean useNewFindJar() {
        return false;
    }

    @Override
    @ApiStatus.Internal
    default void onLoad(String mixinPackage) {

    }

    @Override
    @ApiStatus.Internal
    default String getRefMapperConfig() {
        return null;
    }

    @Override
    @ApiStatus.Internal
    default boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    @ApiStatus.Internal
    default void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    @ApiStatus.Internal
    default List<String> getMixins() {
        val isDevelopmentEnvironment = (boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
        val targetedMods = getTargetedModEnumValues();
        val loadedMods = Arrays.stream(targetedMods)
                               //Java 21 compat
                               .filter(new Predicate<ITargetedMod>() {
                                   @Override
                                   public boolean test(ITargetedMod mod) {
                                       boolean loadJar = IMixinPlugin.this.loadJarOf(mod);
                                       return (mod.isLoadInDevelopment() && isDevelopmentEnvironment)
                                              || loadJar;
                                   }
                               })
                               .collect(Collectors.toList());

        for (val mod : targetedMods) {
            if (loadedMods.contains(mod)) {
                getLogger().info("Found " + mod.getModName() + "! Integrating now...");
            } else {
                getLogger().info("Could not find " + mod.getModName() + "! Skipping integration....");
            }
        }

        List<String> mixins = new ArrayList<>();
        for (val mixin : getMixinEnumValues()) {
            if (mixin.shouldLoad(loadedMods)) {
                String mixinClass = mixin.getSide().name().toLowerCase() + "." + mixin.getMixin();
                mixins.add(mixinClass);
                getLogger().info("Loading mixin: " + mixinClass);
            }
        }
        return mixins;
    }

    @ApiStatus.Internal
    default boolean loadJarOf(final ITargetedMod mod) {
        try {
            val jars = findJarsOf(this, mod);
            if (jars.isEmpty()) {
                getLogger().info("Jar not found for {}", mod);
                return false;
            }
            for (val jar: jars) {
                getLogger().info("Attempting to add {} to the URL Class Path", jar);
                try {
                    MinecraftURLClassPath.addJar(jar);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    @ApiStatus.Internal
    default void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    @ApiStatus.Internal
    default void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
