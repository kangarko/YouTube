package com.matejpacan.voxelcraft;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.BufferUtils;

public final class BufferPool {
	private static final ConcurrentLinkedQueue<FloatBuffer> floatPool = new ConcurrentLinkedQueue<>();
	private static final ConcurrentLinkedQueue<IntBuffer> intPool = new ConcurrentLinkedQueue<>();
	private static final int DEFAULT_FLOAT_SIZE = 250000;
	private static final int DEFAULT_INT_SIZE = 400000;

	private BufferPool() {
	}

	public static FloatBuffer acquireFloat(int minCapacity) {
		final FloatBuffer buf = floatPool.poll();
		if (buf == null || buf.capacity() < minCapacity)
			return BufferUtils.createFloatBuffer(Math.max(minCapacity, DEFAULT_FLOAT_SIZE));
		buf.clear();
		return buf;
	}

	public static void releaseFloat(FloatBuffer buf) {
		if (buf == null)
			return;
		if (buf.capacity() >= DEFAULT_FLOAT_SIZE / 2)
			floatPool.offer(buf);
	}

	public static IntBuffer acquireInt(int minCapacity) {
		final IntBuffer buf = intPool.poll();
		if (buf == null || buf.capacity() < minCapacity)
			return BufferUtils.createIntBuffer(Math.max(minCapacity, DEFAULT_INT_SIZE));
		buf.clear();
		return buf;
	}

	public static void releaseInt(IntBuffer buf) {
		if (buf == null)
			return;
		if (buf.capacity() >= DEFAULT_INT_SIZE / 2)
			intPool.offer(buf);
	}
}
