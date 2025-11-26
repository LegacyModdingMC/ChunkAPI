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

package com.falsepattern.chunk.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This is an API class covered by the additional permissions in the license.
 * <p>
 * Singleton instances that can manage custom in chunks. This class only manages the registration of
 * managers. For the actual networking and data storage, see the internal interfaces of this interface.
 * Note: This interface does nothing by itself, you also need to implement one or more of the internal interfaces.
 *
 * @author FalsePattern
 * @version 0.5.0
 * @see PacketDataManager
 * @see ChunkDataManager
 * @see SubChunkDataManager
 * @see DataRegistry
 * @since 0.5.0
 */
public interface DataManager {

    /**
     * @return The domain of this manager. Usually the modid of the mod that owns this manager.
     */
    @Contract(pure = true)
    String domain();

    /**
     * @return The id of this manager. Usually the name of the manager. Unique per domain.
     */
    @Contract(pure = true)
    String id();

    /**
     * Implement this interface if you want to synchronize your data with the client.
     *
     * @author FalsePattern
     * @since 0.5.0
     */
    interface PacketDataManager extends DataManager {
        /**
         * @return The maximum amount of bytes your data can take up in a packet.
         *
         * @implSpec This is used to determine the size of the packet compression/decompression buffer.
         * Only called ONCE, during registration!
         */
        @Contract(pure = true)
        int maxPacketSize();

        /**
         * Serializes your data into a packet.
         *
         * @param chunk The chunk to serialize.
         * @param subChunkMask The mask that controls which subchunks need to be serialized. This will be 0 when cubic chunks is
         *                     present - only chunk-specific information (like biomes) should be sent in this case.
         * @param forceUpdate True when the chunk is first synced.
         */
        @Contract(mutates = "param4")
        void writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer);

        /**
         * Deserializes your data from a packet.
         *
         * @param chunk The chunk to deserialize.
         * @param subChunkMask The mask that controls which subchunks need to be deserialized. This will be 0 when cubic chunks is
         *                     present - only chunk-specific information (like biomes) should be sent in this case.
         * @param forceUpdate True when the chunk is first synced.
         */
        @Contract(mutates = "param1,param4")
        void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer);
    }

    /**
     * A cube-specific variant of {@link PacketDataManager}. This is only used by cubic chunks.
     *
     * @author RecursivePineapple
     * @since 0.7.1
     * @apiNote Cubes, subchunks, and ExtendedBlockStorages are equivalent. A cube is a subchunk and a cube contains an EBS.
     * To keep dependencies simple, cube-specific information is not available to this interface, but a cube's location can be
     * retrieved by combining the chunk location and the EBS Y level. Note that the EBS's Y level can be <0 and >= 16.
     */
    @ApiStatus.Experimental
    interface CubicPacketDataManager extends DataManager {

        /**
         * @return The maximum amount of bytes your data can take up in a packet.
         *
         * @implSpec This is used to determine the size of the packet compression/decompression buffer.
         * Only called ONCE, during registration!
         */
        @Contract(pure = true)
        int maxPacketSizeCubic();

        /**
         * Serializes your data into a packet.
         *
         * @param chunk The chunk that contains the subchunk.
         * @param blockStorage The subchunk (cube) that was updated.
         */
        @Contract(mutates = "param3")
        void writeToBuffer(Chunk chunk, ExtendedBlockStorage blockStorage, ByteBuffer buffer);

        /**
         * Deserializes your data from a packet.
         *
         * @param chunk The chunk that contains the subchunk.
         * @param blockStorage The subchunk (cube) that was updated.
         */
        @Contract(mutates = "param1,param2,param3")
        void readFromBuffer(Chunk chunk, ExtendedBlockStorage blockStorage, ByteBuffer buffer);
    }

    /**
     * Implement this interface if you additionally want to synchronize your data on single and multi-block updates,
     * not just chunk updates.
     *
     * @author FalsePattern
     * @version 0.6.0
     * @since 0.6.0
     */
    interface BlockPacketDataManager extends DataManager {
        @Contract(mutates = "param1")
        void writeBlockToPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet);

        @Contract(mutates = "param1,param5")
        void readBlockFromPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet);

        /**
         * Serializes your block data into a buffer.
         */
        @Contract(mutates = "param2")
        void writeBlockPacketToBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException;

        /**
         * Deserializes your block data from a buffer.
         */
        @Contract(mutates = "param1,param2")
        void readBlockPacketFromBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException;
    }

    /**
     * The common superinterface for RootDataManager and SubChunkDataManager.
     * Contains version information and messages for users attempting to upgrade/remove versions.
     *
     * @author FalsePattern
     * @version 0.5.0
     * @since 0.5.0
     */
    interface StorageDataManager extends DataManager {
        /**
         * @return The current version of the data manager
         */
        @Contract(pure = true)
        @NotNull String version();

        /**
         * @return The message to show to users when a world is opened with this mod for the first time.
         * Return null to show no message, and treat the manager as fully compatible with vanilla.
         */
        @Contract(pure = true)
        @Nullable String newInstallDescription();

        /**
         * @return The message to show to users when this manager is removed, AND they try to load the world (stored in the
         * world's NBT during previous saves)
         */
        @Contract(pure = true)
        @NotNull String uninstallMessage();

        /**
         * @param priorVersion The version of the manager this world was saved with.
         *
         * @return A warning message to show to the user when upgrading.\
         * If null, the manager is treated as fully compatible with the old version, and no warning is shown.
         */
        @Contract(pure = true)
        @Nullable String versionChangeMessage(String priorVersion);
    }

    /**
     * Implement this interface if you want to save your data to disk. This is called once per chunk, hence "root"
     *
     * @author FalsePattern
     * @version 0.5.0
     * @since 0.5.0
     */
    interface ChunkDataManager extends StorageDataManager {
        /**
         * If false, the given nbt compound will be a freshly created object that gets inserted into the actual
         * level NBT tag under the `domain:id` name.
         * <p>
         * If true, the nbt tag passed in into the write/read methods will be the raw Level NBT tag without filtering.
         *
         * @implNote This is used internally for reimplementing the vanilla logic. Only change this if you know what you're doing.
         */
        @Contract(pure = true)
        default boolean chunkPrivilegedAccess() {
            return false;
        }

        /**
         * Serializes your data into an NBT tag. This is used when saving the chunk to disk.
         */
        @Contract(mutates = "param2")
        void writeChunkToNBT(Chunk chunk, NBTTagCompound nbt);


        /**
         * Deserializes your data from an NBT tag. This is used when loading the chunk from disk.
         * The NBT *may* be null if the chunk was saved before this manager was registered
         * (e.g., loading save before the mod was added), and the manager is not {@link #chunkPrivilegedAccess() privileged}.
         * In this case, you should initialize the data to a sane default.
         */
        @Contract(mutates = "param1")
        void readChunkFromNBT(Chunk chunk, NBTTagCompound nbt);

        /**
         * Directly copies data from one chunk to another chunk.
         *
         * @param from The chunk to copy data from.
         * @param to   The chunk to copy data to.
         */
        @Contract(mutates = "param2")
        void cloneChunk(Chunk from, Chunk to);
    }

    /**
     * Implement this interface if you want to save your subChunk data to disk. This is called once per
     * ExtendedBlockStorage per chunk. (16 times per chunk)
     *
     * @author FalsePattern
     * @version 0.5.0
     * @since 0.5.0
     */
    interface SubChunkDataManager extends StorageDataManager {
        /**
         * If false, the given nbt compound will be a freshly created object that gets inserted into the actual
         * segment NBT tag under the `domain:id` name.
         * <p>
         * If true, the nbt tag passed in into the write/read methods will be the raw segment NBT tag without filtering.
         *
         * @implNote This is used internally for reimplementing the vanilla logic. Only change this if you know what you're doing.
         */
        @Contract(pure = true)
        default boolean subChunkPrivilegedAccess() {
            return false;
        }

        /**
         * Serializes your data into an NBT tag. This is used when saving the chunk to disk.
         */
        @Contract(mutates = "param3")
        void writeSubChunkToNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt);

        /**
         * Deserializes your data from an NBT tag. This is used when loading the chunk from disk.
         * The NBT *may* be null if the chunk was saved before this manager was registered
         * (e.g., loading save before the mod was added), and the manager is not {@link #subChunkPrivilegedAccess() privileged}.
         * In this case, you should initialize the data to a sane default.
         */
        @Contract(mutates = "param2")
        void readSubChunkFromNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt);

        /**
         * Directly copies data from one subChunk to another.
         *
         * @param fromChunk The owner of the subChunk to copy data from.
         * @param from      The subChunk to copy data from.
         * @param to        The subChunk to copy data to.
         */
        @Contract(mutates = "param3")
        void cloneSubChunk(Chunk fromChunk, ExtendedBlockStorage from, ExtendedBlockStorage to);
    }
}
