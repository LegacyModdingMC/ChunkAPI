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

import com.falsepattern.chunk.api.DataManager;
import com.falsepattern.chunk.internal.Common;
import lombok.val;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.nio.ByteBuffer;

public abstract class NibbleManager extends VanillaManager implements DataManager.PacketDataManager, DataManager.CubicPacketDataManager {
    public static final int BYTES_PER_SUBCHUNK = Common.BLOCKS_PER_SUBCHUNK / 2;

    protected abstract NibbleArray getNibbleArray(ExtendedBlockStorage subChunk);

    @Override
    public int maxPacketSize() {
        return Common.SUBCHUNKS_PER_CHUNK * BYTES_PER_SUBCHUNK;
    }

    @Override
    public void writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        val subChunks = chunk.getBlockStorageArray();
        for (int i = 0; i < subChunks.length; i++) {
            if ((subChunkMask & (1 << i)) != 0) {
                val subChunk = subChunks[i];
                if (subChunk != null) {
                    buffer.put(getNibbleArray(subChunk).data, 0, BYTES_PER_SUBCHUNK);
                }
            }
        }
    }

    @Override
    public void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        val subChunks = chunk.getBlockStorageArray();
        for (int i = 0; i < subChunks.length; i++) {
            if ((subChunkMask & (1 << i)) != 0) {
                val subChunk = subChunks[i];
                if (subChunk != null) {
                    buffer.get(getNibbleArray(subChunk).data, 0, BYTES_PER_SUBCHUNK);
                }
            }
        }
    }

    @Override
    public int maxPacketSizeCubic() {
        return BYTES_PER_SUBCHUNK;
    }

    @Override
    public void writeToBuffer(Chunk chunk, ExtendedBlockStorage blockStorage, ByteBuffer buffer) {
        buffer.put(getNibbleArray(blockStorage).data, 0, BYTES_PER_SUBCHUNK);
    }

    @Override
    public void readFromBuffer(Chunk chunk, ExtendedBlockStorage blockStorage, ByteBuffer buffer) {
        buffer.get(getNibbleArray(blockStorage).data, 0, BYTES_PER_SUBCHUNK);
    }
}
