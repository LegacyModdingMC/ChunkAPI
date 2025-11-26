/*
 * ChunkAPI
 *
 * Copyright (C) 2023-2025 FalsePattern, The MEGA Team, LegacyModdingMC contributors
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.chunk.internal;

import com.falsepattern.chunk.api.DataManager;
import com.falsepattern.chunk.api.OrderedManager;
import lombok.Data;
import lombok.val;
import lombok.var;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.util.Constants;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.StartupQuery;
import cpw.mods.fml.common.ZipperUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DataRegistryImpl {
    private static final Map<String, OrderedManager> managersUnordered = new HashMap<>();
    private static final SortedSet<OrderedManager> managers = new TreeSet<>();
    private static final DualMap<PacketManagerInfo> packetManagers = new DualMap<>();
    private static final DualMap<CubicPacketManagerInfo> cubicPacketManagers = new DualMap<>();
    private static final DualMap<DataManager.BlockPacketDataManager> blockPacketManagers = new DualMap<>();
    private static final DualMap<DataManager.StorageDataManager> NBTManagers = new DualMap<>();
    private static final SortedMap<OrderedManager, DataManager.ChunkDataManager> chunkNBTManagers = new TreeMap<>();
    private static final SortedMap<OrderedManager, DataManager.SubChunkDataManager> subChunkNBTManagers = new TreeMap<>();
    private static final Set<String> disabledManagers = new HashSet<>();
    private static int maxPacketSize = 4;
    private static int maxPacketSizeCubic = 4;

    @Data
    private static class PacketManagerInfo {
        public final int maxPacketSize;
        public final DataManager.PacketDataManager manager;
    }

    @Data
    private static class CubicPacketManagerInfo {
        public final int maxPacketSize;
        public final DataManager.CubicPacketDataManager manager;
    }

    public static void registerDataManager(DataManager manager, int ordering) throws IllegalStateException, IllegalArgumentException {
        if (Loader.instance().getLoaderState() != LoaderState.INITIALIZATION) {
            throw new IllegalStateException("ChunkDataManager registration is not allowed at this time! Please register your ChunkDataManager in the init phase.");
        }
        var id = manager.domain() + ":" + manager.id();
        if (managersUnordered.containsKey(id)) {
            throw new IllegalArgumentException("ChunkDataManager " + manager + " has a duplicate id!");
        }

        // If the manager was disabled, do not add it to the list of managers.
        if (disabledManagers.contains(id)) {
            return;
        }

        val ord = new OrderedManager(ordering, id);
        managersUnordered.put(id, ord);
        managers.add(ord);
        if (manager instanceof DataManager.PacketDataManager) {
            val packetManager = (DataManager.PacketDataManager) manager;
            val maxSize = packetManager.maxPacketSize();
            maxPacketSize += 4 + id.getBytes(StandardCharsets.UTF_8).length + 4 + maxSize;
            val man = new PacketManagerInfo(maxSize, packetManager);
            packetManagers.put(ord, man);
        }
        if (manager instanceof DataManager.BlockPacketDataManager) {
            val blockPacketManager = (DataManager.BlockPacketDataManager) manager;
            blockPacketManagers.put(ord, blockPacketManager);
        }
        if (manager instanceof DataManager.CubicPacketDataManager) {
            val cubicPacketManager = (DataManager.CubicPacketDataManager) manager;
            val maxSize = cubicPacketManager.maxPacketSizeCubic();
            maxPacketSizeCubic += 4 + id.getBytes(StandardCharsets.UTF_8).length + 4 + maxSize;
            cubicPacketManagers.put(ord, new CubicPacketManagerInfo(maxSize, cubicPacketManager));
        }
        if (manager instanceof DataManager.StorageDataManager) {
            NBTManagers.put(ord, (DataManager.StorageDataManager) manager);
            if (manager instanceof DataManager.ChunkDataManager) {
                chunkNBTManagers.put(ord, (DataManager.ChunkDataManager) manager);
            }
            if (manager instanceof DataManager.SubChunkDataManager) {
                subChunkNBTManagers.put(ord, (DataManager.SubChunkDataManager) manager);
            }
        }
    }

    public static void disableDataManager(String domain, String id) {
        if (Loader.instance().getLoaderState() != LoaderState.POSTINITIALIZATION) {
            throw new IllegalStateException("ChunkDataManager disabling is not allowed at this time! Please disable any ChunkDataManagers in the postInit phase.");
        }
        Common.LOG.debug("Disabling ChunkDataManager " + id + " in domain " + domain + ". See the stacktrace for the source of this event.\nThis is NOT an error.",
                         new Throwable());
        val manager = domain + ":" + id;
        val ord = managersUnordered.remove(manager);
        //Remove the manager from the list of managers, if it exists
        if (ord != null) {
            managers.remove(ord);
            //Clear the maps
            if (packetManagers.containsKey(ord)) {
                val removed = packetManagers.remove(ord);
                maxPacketSize -= 4 + id.getBytes(StandardCharsets.UTF_8).length + 4 + removed.maxPacketSize;
            }
            if (cubicPacketManagers.containsKey(ord)) {
                val removed = cubicPacketManagers.remove(ord);
                maxPacketSizeCubic -= 4 + id.getBytes(StandardCharsets.UTF_8).length + 4 + removed.maxPacketSize;
            }
            blockPacketManagers.remove(ord);
            chunkNBTManagers.remove(ord);
            subChunkNBTManagers.remove(ord);
            NBTManagers.remove(ord);
        }

        //Add the manager to the list of disabled managers, in case it gets registered after this disable call.
        disabledManagers.add(manager);
    }

    public static int maxPacketSize() {
        return maxPacketSize;
    }

    public static int maxPacketSizeCubic() {
        return maxPacketSizeCubic;
    }

    private static void writeString(ByteBuffer buffer, String string) {
        val bytes = string.getBytes();
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    private static String readString(ByteBuffer buffer) {
        val length = buffer.getInt();
        val bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes);
    }

    public static void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, byte[] data) {
        val buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int count = buf.getInt();
        for (int i = 0; i < count; i++) {
            val id = readString(buf);
            val length = buf.getInt();
            val managerInfo = packetManagers.get(id);
            if (managerInfo == null) {
                Common.LOG.error("Received data for unknown PacketDataManager " + id + ". Skipping.");
                buf.position(buf.position() + length);
                continue;
            }
            if (length > managerInfo.maxPacketSize) {
                Common.LOG.error("Received packet larger than max size for PacketDataManager " + id + "! Continuing anyways, things might break!");
            }
            int start = buf.position();
            val slice = createSlice(buf, start, length);
            managerInfo.manager.readFromBuffer(chunk, subChunkMask, forceUpdate, slice);
            buf.position(start + length);
        }
    }

    public static void readFromBufferCubic(Chunk chunk, ExtendedBlockStorage blockStorage, byte[] data) {
        val buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int count = buf.getInt();
        for (int i = 0; i < count; i++) {
            val id = readString(buf);
            val length = buf.getInt();
            val managerInfo = cubicPacketManagers.get(id);
            if (managerInfo == null) {
                Common.LOG.error("Received data for unknown CubicPacketDataManager " + id + ". Skipping.");
                buf.position(buf.position() + length);
                continue;
            }
            if (length > managerInfo.maxPacketSize) {
                Common.LOG.error("Received packet larger than max size for CubicPacketDataManager " + id + "! Continuing anyways, things might break!");
            }
            int start = buf.position();
            val slice = createSlice(buf, start, length);
            managerInfo.manager.readFromBuffer(chunk, blockStorage, slice);
            buf.position(start + length);
        }
    }

    public static int writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, byte[] data) {
        val buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(packetManagers.size());
        for (val pair : packetManagers.entrySet()) {
            val ord = pair.getKey();
            val managerInfo = pair.getValue();
            writeString(buf, ord.id);
            int start = buf.position() + 4;
            val slice = createSlice(buf, start, managerInfo.maxPacketSize);
            managerInfo.manager.writeToBuffer(chunk, subChunkMask, forceUpdate, slice);
            int length = slice.position();
            buf.putInt(length);
            buf.position(start + length);
        }
        return buf.position();
    }

    public static int writeToBufferCubic(Chunk chunk, ExtendedBlockStorage blockStorage, byte[] data) {
        val buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(cubicPacketManagers.size());
        for (val pair : cubicPacketManagers.entrySet()) {
            val ord = pair.getKey();
            val managerInfo = pair.getValue();
            writeString(buf, ord.id);
            int start = buf.position() + 4;
            val slice = createSlice(buf, start, managerInfo.maxPacketSize);
            managerInfo.manager.writeToBuffer(chunk, blockStorage, slice);
            int length = slice.position();
            buf.putInt(length);
            buf.position(start + length);
        }
        return buf.position();
    }

    public static void writeBlockToPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        for (val manager : blockPacketManagers.values()) {
            manager.writeBlockToPacket(chunk, x, y, z, packet);
        }
    }

    public static void readBlockFromPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        for (val manager : blockPacketManagers.values()) {
            manager.readBlockFromPacket(chunk, x, y, z, packet);
        }
    }

    public static void writeBlockPacketToBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException {
        buffer.writeInt(blockPacketManagers.size());
        for (val pair : blockPacketManagers.entrySet()) {
            val ord = pair.getKey();
            val manager = pair.getValue();
            buffer.writeStringToBuffer(ord.id);
            manager.writeBlockPacketToBuffer(packet, buffer);
        }
    }

    public static void readBlockPacketFromBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException {
        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            val id = buffer.readStringFromBuffer(1024);
            val manager = blockPacketManagers.get(id);
            manager.readBlockPacketFromBuffer(packet, buffer);
        }
    }

    private static ByteBuffer createSlice(ByteBuffer buffer, int start, int length) {
        int oldLimit = buffer.limit();
        int oldPosition = buffer.position();
        buffer.position(start);
        buffer.limit(start + length);
        ByteBuffer slice = buffer.slice();
        buffer.limit(oldLimit);
        buffer.position(oldPosition);
        return slice;
    }

    private static NBTTagCompound createManagerNBT(boolean privileged, NBTTagCompound root, DataManager manager) {
        if (privileged) {
            return root;
        }
        val domain = manager.domain();
        val id = manager.id();
        NBTTagCompound domainNBT;
        if (root.hasKey(domain)) {
            domainNBT = root.getCompoundTag(domain);
        } else {
            domainNBT = new NBTTagCompound();
            root.setTag(domain, domainNBT);
        }
        if (domainNBT.hasKey(id)) {
            return domainNBT.getCompoundTag(id);
        } else {
            val subNBT = new NBTTagCompound();
            domainNBT.setTag(id, subNBT);
            return subNBT;
        }
    }

    private static NBTTagCompound getManagerNBT(boolean privileged, NBTTagCompound root, DataManager manager) {
        if (privileged) {
            return root;
        }
        val domain = manager.domain();
        if (!root.hasKey(domain)) {
            return new NBTTagCompound();
        }
        val domainNBT = root.getCompoundTag(domain);
        if (!domainNBT.hasKey(manager.id())) {
            return new NBTTagCompound();
        }
        return domainNBT.getCompoundTag(manager.id());
    }

    public static void writeSubChunkToNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        for (val manager : subChunkNBTManagers.values()) {
            manager.writeSubChunkToNBT(chunk, subChunk, createManagerNBT(manager.subChunkPrivilegedAccess(), nbt, manager));
        }
    }

    public static void readSubChunkFromNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        for (val manager : subChunkNBTManagers.values()) {
            manager.readSubChunkFromNBT(chunk, subChunk, getManagerNBT(manager.subChunkPrivilegedAccess(), nbt, manager));
        }
    }

    public static void writeChunkToNBT(Chunk chunk, NBTTagCompound chunkNBT) {
        for (val manager : chunkNBTManagers.values()) {
            manager.writeChunkToNBT(chunk, createManagerNBT(manager.chunkPrivilegedAccess(), chunkNBT, manager));
        }
    }

    public static void readChunkFromNBT(Chunk chunk, NBTTagCompound chunkNBT) {
        for (val manager : chunkNBTManagers.values()) {
            manager.readChunkFromNBT(chunk, getManagerNBT(manager.chunkPrivilegedAccess(), chunkNBT, manager));
        }
    }

    public static void cloneChunk(Chunk from, Chunk to) {
        for (val manager : chunkNBTManagers.values()) {
            manager.cloneChunk(from, to);
        }
    }

    public static void cloneSubChunk(Chunk fromChunk, ExtendedBlockStorage from, ExtendedBlockStorage to) {
        for (val manager : subChunkNBTManagers.values()) {
            manager.cloneSubChunk(fromChunk, from, to);
        }
    }

    public static Set<String> getRegisteredManagers() {
        return Collections.unmodifiableSet(managersUnordered.keySet());
    }

    public static SortedSet<OrderedManager> getRegisteredManagersOrdered() {
        return Collections.unmodifiableSortedSet(managers);
    }

    public static void readLevelDat(NBTTagCompound tag) {
        boolean triggerScreen = false;
        boolean newlyInstalled = false;
        val builder = new StringBuilder();
        if (!tag.hasKey("version")) {
            newlyInstalled = true;
            builder.append("The world you are trying to load is a vanilla world, or a world that was created with a very old version of ChunkAPI.\n");
        } // Compat code will be added here if needed.
        val managerCompat = verifyManagerCompatibility(tag);
        if (managerCompat != null) {
            //Avoid duplicate messages
            if (!newlyInstalled) {
                builder.append("The world you are trying to load was created with a different version of ChunkAPI data managers.\n" +
                               "Data managers are used by other mods to save extra data in chunks, and changes to these data managers can cause worlds to become corrupted.\n");
            }
            triggerScreen = true;
        }
        if (triggerScreen) {
            builder.append("ChunkAPI will attempt to load the world, but it is possible that chunks might get corrupted.\n" +
                           "Read the following text carefully, and make sure you understand the risks before continuing.\n\n");
        }
        if (managerCompat != null) {
            builder.append(managerCompat);
        }

        if (triggerScreen) {
            builder.append("\nA world backup will be automatically created in your saves directory.\n\n");
            boolean confirmed = StartupQuery.confirm(builder.toString());
            if (!confirmed) {
                StartupQuery.abort();
            }

            try {
                ZipperUtil.backupWorld();
            } catch (IOException e) {
                StartupQuery.notify("The world backup couldn't be created.\n\n" + e);
                StartupQuery.abort();
            }
        }
    }

    private static String verifyManagerCompatibility(NBTTagCompound tag) {
        boolean compatWarning = false;
        StringBuilder builder = new StringBuilder();
        val saveManagers = readManagers(tag);
        val removedManagers = new HashSet<>(saveManagers.keySet());
        for (val nbtManager: NBTManagers.keySet()) {
            removedManagers.remove(nbtManager.id);
        }
        if (!removedManagers.isEmpty()) {
            compatWarning = true;
            builder.append("\nThe following data managers are no longer present:\n");
            for (val manager : removedManagers) {
                val saveManager = saveManagers.get(manager);
                builder.append(manager);
                if (saveManager.version != null) {
                    builder.append(' ').append(saveManager.version);
                }
                builder.append("\nUninstall information: ");
                if (saveManager.uninstallMessage != null) {
                    builder.append(saveManager.getUninstallMessage());
                } else {
                    builder.append("No uninstall information available.");
                }
                builder.append('\n');
            }
        }
        val addedManagers = NBTManagers.keySet()
                                       .stream()
                                       .filter(manager -> !manager.id.startsWith("minecraft:"))
                                       .filter(manager -> !saveManagers.containsKey(manager.id))
                                       .collect(Collectors.toSet());
        if (!addedManagers.isEmpty()) {
            compatWarning = true;
            builder.append("\nThe following data managers have been newly added:\n");
            for (val manager : addedManagers) {
                val addedManager = NBTManagers.get(manager);
                if (addedManager == null) {
                    continue;
                }
                builder.append(manager).append(' ').append(addedManager.version()).append('\n');
                val installMessage = addedManager.newInstallDescription();
                if (installMessage != null) {
                    builder.append("Install information: ").append(addedManager.newInstallDescription()).append('\n');
                }
            }
        }
        boolean changePrompt = false;
        for (val savedManager : saveManagers.entrySet()) {
            val managerName = savedManager.getKey();
            if (NBTManagers.containsKey(managerName)) {
                val manager = savedManager.getValue();
                val currentManager = NBTManagers.get(managerName);
                val extraMessage = currentManager.versionChangeMessage(manager.version);
                if (extraMessage != null) {
                    compatWarning = true;
                    if (!changePrompt) {
                        builder.append("\nThe following data managers have changed versions in an incompatible way:\n");
                        changePrompt = true;
                    }
                    builder.append(managerName).append(" has changed versions from ").append(manager.version).append(" to ").append(currentManager.version()).append(".\n");
                    builder.append("Upgrade information: ").append(extraMessage).append('\n');
                }
            }
        }
        if (!compatWarning) {
            return null;
        }
        return "The following managers have changed:\n" + builder;
    }

    public static void writeLevelDat(NBTTagCompound tag) {
        val managers = new NBTTagCompound();
        tag.setTag("managers", managers);
        tag.setString("version", Tags.MOD_VERSION);
        for (val manager : NBTManagers.entrySet()) {
            val ord = manager.getKey();
            if (ord.id.startsWith("minecraft:")) {
                continue;
            }
            val value = manager.getValue();
            managers.setTag(ord.id, SaveManagerInfo.fromManager(value).toNBT());
        }
    }

    private static Map<String, SaveManagerInfo> readManagers(NBTTagCompound tag) {
        val managers = new HashMap<String, SaveManagerInfo>();
        if (!tag.hasKey("managers")) {
            return managers;
        }
        if (tag.func_150299_b("managers") != Constants.NBT.TAG_COMPOUND) {
            return managers;
        }
        val managerTag = tag.getCompoundTag("managers");
        for (val key : managerTag.func_150296_c()) {
            managers.put(key, SaveManagerInfo.fromNBT(managerTag.getCompoundTag(key)));
        }
        return managers;
    }

    @Data
    private static class SaveManagerInfo {
        @Nullable
        public final String version;
        @Nullable
        public final String uninstallMessage;

        public NBTTagCompound toNBT() {
            val tag = new NBTTagCompound();
            if (version != null) {
                tag.setString("version", version);
            }
            if (uninstallMessage != null) {
                tag.setString("uninstallMessage", uninstallMessage);
            }
            return tag;
        }

        public static SaveManagerInfo fromNBT(NBTTagCompound tag) {
            return new SaveManagerInfo(getNBTStringNullable(tag, "version"), getNBTStringNullable(tag, "uninstallMessage"));
        }

        private static String getNBTStringNullable(NBTTagCompound tag, String key) {
            if (tag.hasKey(key) && tag.func_150299_b(key) == Constants.NBT.TAG_STRING) {
                return tag.getString(key);
            } else {
                return null;
            }
        }

        public static SaveManagerInfo fromManager(DataManager.StorageDataManager manager) {
            return new SaveManagerInfo(manager.version(), manager.uninstallMessage());
        }
    }
}
