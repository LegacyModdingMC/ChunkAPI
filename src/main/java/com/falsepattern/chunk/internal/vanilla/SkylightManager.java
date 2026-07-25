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

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.nio.ByteBuffer;

public class SkylightManager extends NibbleManager implements DataManager.SubChunkDataManager {
    @Override
    public String id() {
        return "skylight";
    }

    @Override
    protected NibbleArray getNibbleArray(ExtendedBlockStorage subChunk) {
        return subChunk.getSkylightArray();
    }

    @Override
    public void writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        if (!chunk.worldObj.provider.hasNoSky) {
            super.writeToBuffer(chunk, subChunkMask, forceUpdate, buffer);
        }
    }

    @Override
    public void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        if (!chunk.worldObj.provider.hasNoSky) {
            super.readFromBuffer(chunk, subChunkMask, forceUpdate, buffer);
        }
    }

    @Override
    public void writeToBuffer(Chunk chunk, ExtendedBlockStorage blockStorage, ByteBuffer buffer) {
        if (!chunk.worldObj.provider.hasNoSky) {
            super.writeToBuffer(chunk, blockStorage, buffer);
        }
    }

    @Override
    public void readFromBuffer(Chunk chunk, ExtendedBlockStorage blockStorage, ByteBuffer buffer) {
        if (!chunk.worldObj.provider.hasNoSky) {
            super.readFromBuffer(chunk, blockStorage, buffer);
        }
    }

    @Override
    public boolean subChunkPrivilegedAccess() {
        return true;
    }

    @Override
    public void writeSubChunkToNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        if (!chunk.worldObj.provider.hasNoSky) {
            nbt.setByteArray("SkyLight", subChunk.getSkylightArray().data);
        } else {
            nbt.setByteArray("SkyLight", new byte[subChunk.getBlocklightArray().data.length]);
        }
    }

    @Override
    public void readSubChunkFromNBT(Chunk chunk, ExtendedBlockStorage subChunk, NBTTagCompound nbt) {
        if (!chunk.worldObj.provider.hasNoSky) {
            subChunk.setSkylightArray(new NibbleArray(nbt.getByteArray("SkyLight"), 4));
        }
    }

    @Override
    public void cloneSubChunk(Chunk fromChunk, ExtendedBlockStorage from, ExtendedBlockStorage to) {
        if (!fromChunk.worldObj.provider.hasNoSky) {
            to.setSkylightArray(ArrayUtil.copyArray(from.getSkylightArray(), to.getSkylightArray()));
        } else {
            to.setSkylightArray(null);
        }
    }
}
