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

import net.minecraft.world.chunk.NibbleArray;

import java.util.Arrays;

/**
 * This is an API class covered by the additional permissions in the license.
 * <p>
 * Miscellaneous utilities for in-place array transfer (where possible).
 * @version 0.5.0
 * @since 0.5.0
 */
@ApiStatus.NonExtendable
@SuppressWarnings("DuplicatedCode")
public class ArrayUtil {

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static boolean[] copyArray(boolean[] src, boolean[] dst) {
        if (src == null) {
            return null;
        }
        if (dst == null || src.length != dst.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static byte[] copyArray(byte[] src, byte[] dst) {
        if (src == null) {
            return null;
        }
        if (dst == null || src.length != dst.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static char[] copyArray(char[] src, char[] dst) {
        if (src == null) {
            return null;
        }
        if (dst == null || src.length != dst.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static short[] copyArray(short[] src, short[] dst) {
        if (src == null) {
            return null;
        }
        if (dst == null || src.length != dst.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static int[] copyArray(int[] src, int[] dst) {
        if (src == null) {
            return null;
        }
        if (dst == null || src.length != dst.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static long[] copyArray(long[] src, long[] dst) {
        if (src == null) {
            return null;
        }
        if (dst == null || src.length != dst.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static float[] copyArray(float[] src, float[] dst) {
        if (src == null) {
            return null;
        }
        if (dst == null || src.length != dst.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static double[] copyArray(double[] src, double[] dst) {
        if (src == null) {
            return null;
        }
        if (dst == null || src.length != dst.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static <T> T[] copyArray(T[] src, T[] dst) {
        if (src == null) {
            return null;
        }
        if (dst == null || src.length != dst.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Contract(value = "null, _ -> null;" +
                      "!null, null -> new;",
              mutates = "param2")
    public static NibbleArray copyArray(NibbleArray srcArray, NibbleArray dstArray) {
        if (srcArray == null) {
            return null;
        }
        if (dstArray == null) {
            return new NibbleArray(Arrays.copyOf(srcArray.data, srcArray.data.length), srcArray.depthBits);
        }
        dstArray.depthBits = srcArray.depthBits;
        dstArray.depthBitsPlusFour = srcArray.depthBitsPlusFour;
        dstArray.data = copyArray(srcArray.data, dstArray.data);
        return dstArray;
    }
}
