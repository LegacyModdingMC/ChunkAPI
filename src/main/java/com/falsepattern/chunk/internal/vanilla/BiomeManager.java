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

package com.falsepattern.chunk.internal.vanilla;

import com.falsepattern.chunk.api.ArrayUtil;
import com.falsepattern.chunk.api.DataManager;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;

import java.nio.ByteBuffer;

public class BiomeManager extends VanillaManager implements DataManager.PacketDataManager, DataManager.ChunkDataManager {
    public static final int BYTES_PER_CHUNK = 256;

    @Override
    public String id() {
        return "biome";
    }

    @Override
    public int maxPacketSize() {
        return BYTES_PER_CHUNK;
    }

    @Override
    public void writeToBuffer(@NotNull Chunk chunk, int subChunkMask, boolean forceUpdate, @NotNull ByteBuffer buffer) {
        if (forceUpdate) {
            buffer.put(chunk.getBiomeArray());
        }
    }

    @Override
    public void readFromBuffer(@NotNull Chunk chunk, int subChunkMask, boolean forceUpdate, @NotNull ByteBuffer buffer) {
        if (forceUpdate) {
            buffer.get(chunk.getBiomeArray());
        }
    }

    @Override
    public boolean chunkPrivilegedAccess() {
        return true;
    }


    @Override
    public void writeChunkToNBT(@NotNull Chunk chunk, @NotNull NBTTagCompound nbt) {
        nbt.setByteArray("Biomes", chunk.getBiomeArray());
    }

    @Override
    public void readChunkFromNBT(@NotNull Chunk chunk, @NotNull NBTTagCompound nbt) {
        if (nbt.hasKey("Biomes", 7)) {
            chunk.setBiomeArray(nbt.getByteArray("Biomes"));
        }
    }

    @Override
    public void cloneChunk(Chunk from, Chunk to) {
        to.setBiomeArray(ArrayUtil.copyArray(from.getBiomeArray(), to.getBiomeArray()));
    }
}
