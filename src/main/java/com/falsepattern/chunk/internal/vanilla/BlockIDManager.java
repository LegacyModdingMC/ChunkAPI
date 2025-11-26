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
import lombok.val;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.falsepattern.chunk.internal.Common.BLOCKS_PER_SUBCHUNK;
import static com.falsepattern.chunk.internal.Common.SUBCHUNKS_PER_CHUNK;

public class BlockIDManager extends VanillaManager implements DataManager.PacketDataManager, DataManager.CubicPacketDataManager, DataManager.BlockPacketDataManager, DataManager.SubChunkDataManager {
    private static final int LSB_BYTES_PER_SUBCHUNK = BLOCKS_PER_SUBCHUNK;
    private static final int MSB_BYTES_PER_SUBCHUNK = BLOCKS_PER_SUBCHUNK / 2;
    private static final int HEADER_SIZE = 2;

    @Override
    public String id() {
        return "blockid";
    }

    @Override
    public int maxPacketSize() {
        return HEADER_SIZE + SUBCHUNKS_PER_CHUNK * (LSB_BYTES_PER_SUBCHUNK + MSB_BYTES_PER_SUBCHUNK);
    }

    @Override
    public void writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        val subChunks = chunk.getBlockStorageArray();
        int currentPos = buffer.position();
        buffer.putShort((short) 0);
        int msbMask = 0;
        for (int i = 0; i < subChunks.length; i++) {
            if ((subChunkMask & (1 << i)) != 0) {
                val subChunk = subChunks[i];
                val lsb = subChunk.getBlockLSBArray();
                val msb = subChunk.getBlockMSBArray();
                buffer.put(lsb);
                if (msb != null) {
                    msbMask |= 1 << i;
                    buffer.put(msb.data);
                }
            }
        }
        int endPos = buffer.position();
        buffer.position(currentPos);
        buffer.putShort((short) msbMask);
        buffer.position(endPos);
    }

    @Override
    public void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        val subChunks = chunk.getBlockStorageArray();
        val msbMask = buffer.getShort() & 0xFFFF;
        for (int i = 0; i < subChunks.length; i++) {
            val subChunk = subChunks[i];
            if ((subChunkMask & (1 << i)) != 0 && subChunk != null) {
                buffer.get(subChunk.getBlockLSBArray());
                if ((msbMask & (1 << i)) != 0) {
                    val msb = subChunk.getBlockMSBArray();
                    if (msb == null) {
                        subChunk.createBlockMSBArray();
                    }
                    buffer.get(subChunk.getBlockMSBArray().data);
                } else {
                    subChunk.setBlockMSBArray(null);
                }
            }
        }
    }

    @Override
    public int maxPacketSizeCubic() {
        return HEADER_SIZE + LSB_BYTES_PER_SUBCHUNK + MSB_BYTES_PER_SUBCHUNK;
    }

    @Override
    public void writeToBuffer(Chunk chunk, ExtendedBlockStorage blockStorage, ByteBuffer buffer) {
        buffer.put(blockStorage.getBlockLSBArray());

        if (blockStorage.getBlockMSBArray() != null) {
            buffer.put((byte) 1);
            buffer.put(blockStorage.getBlockMSBArray().data);
        } else {
            buffer.put((byte) 0);
        }
    }

    @Override
    public void readFromBuffer(Chunk chunk, ExtendedBlockStorage blockStorage, ByteBuffer buffer) {
        buffer.get(blockStorage.getBlockLSBArray());

        if (buffer.get() != 0) {
            if (blockStorage.getBlockMSBArray() == null) {
                blockStorage.createBlockMSBArray();
            }

            buffer.get(blockStorage.getBlockMSBArray().data);
        }
    }

    @Override
    public boolean subChunkPrivilegedAccess() {
        return true;
    }

    @Override
    public void writeSubChunkToNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        nbt.setByteArray("Blocks", subChunk.getBlockLSBArray());

        if (subChunk.getBlockMSBArray() != null) {
            nbt.setByteArray("Add", subChunk.getBlockMSBArray().data);
        }
    }

    @Override
    public void readSubChunkFromNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        subChunk.setBlockLSBArray(nbt.getByteArray("Blocks"));

        if (nbt.hasKey("Add", 7)) {
            subChunk.setBlockMSBArray(new NibbleArray(nbt.getByteArray("Add"), 4));
        }
    }

    @Override
    public void cloneSubChunk(Chunk fromChunk, ExtendedBlockStorage from, ExtendedBlockStorage to) {
        to.setBlockLSBArray(ArrayUtil.copyArray(from.getBlockLSBArray(), to.getBlockLSBArray()));
        to.setBlockMSBArray(ArrayUtil.copyArray(from.getBlockMSBArray(), to.getBlockMSBArray()));
    }

    @Override
    public void writeBlockToPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
    }

    @Override
    public void readBlockFromPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {

    }

    @Override
    public void writeBlockPacketToBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException {
        buffer.writeShort(Block.getIdFromBlock(packet.field_148883_d) & 0xFFF);
    }

    @Override
    public void readBlockPacketFromBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException {
        packet.field_148883_d = Block.getBlockById(buffer.readUnsignedShort() & 0xFFF);
    }
}
