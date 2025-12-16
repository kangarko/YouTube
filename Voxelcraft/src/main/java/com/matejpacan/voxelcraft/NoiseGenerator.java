package com.matejpacan.voxelcraft;

public class NoiseGenerator {

	private final int[] permutation;

	private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
	private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;

	public NoiseGenerator(final long seed) {
		this.permutation = new int[512];

		final int[] p = new int[256];
		for (int i = 0; i < 256; i++)
			p[i] = i;

		final java.util.Random random = new java.util.Random(seed);
		for (int i = 255; i > 0; i--) {
			final int j = random.nextInt(i + 1);
			final int temp = p[i];
			p[i] = p[j];
			p[j] = temp;
		}

		for (int i = 0; i < 512; i++)
			this.permutation[i] = p[i & 255];
	}

	public double noise2D(final double x, final double y) {
		final double s = (x + y) * NoiseGenerator.F2;
		final int i = NoiseGenerator.fastFloor(x + s);
		final int j = NoiseGenerator.fastFloor(y + s);

		final double t = (i + j) * NoiseGenerator.G2;
		final double X0 = i - t;
		final double Y0 = j - t;
		final double x0 = x - X0;
		final double y0 = y - Y0;

		int i1, j1;
		if (x0 > y0) {
			i1 = 1;
			j1 = 0;
		} else {
			i1 = 0;
			j1 = 1;
		}

		final double x1 = x0 - i1 + NoiseGenerator.G2;
		final double y1 = y0 - j1 + NoiseGenerator.G2;
		final double x2 = x0 - 1.0 + 2.0 * NoiseGenerator.G2;
		final double y2 = y0 - 1.0 + 2.0 * NoiseGenerator.G2;

		final int ii = i & 255;
		final int jj = j & 255;

		double n0, n1, n2;

		double t0 = 0.5 - x0 * x0 - y0 * y0;
		if (t0 < 0)
			n0 = 0.0;
		else {
			t0 *= t0;
			n0 = t0 * t0 * NoiseGenerator.grad(this.permutation[ii + this.permutation[jj]], x0, y0);
		}

		double t1 = 0.5 - x1 * x1 - y1 * y1;
		if (t1 < 0)
			n1 = 0.0;
		else {
			t1 *= t1;
			n1 = t1 * t1 * NoiseGenerator.grad(this.permutation[ii + i1 + this.permutation[jj + j1]], x1, y1);
		}

		double t2 = 0.5 - x2 * x2 - y2 * y2;
		if (t2 < 0)
			n2 = 0.0;
		else {
			t2 *= t2;
			n2 = t2 * t2 * NoiseGenerator.grad(this.permutation[ii + 1 + this.permutation[jj + 1]], x2, y2);
		}

		return 70.0 * (n0 + n1 + n2);
	}

	public double octaveNoise2D(final double x, final double y, final int octaves, final double persistence,
			final double lacunarity) {
		double total = 0;
		double frequency = 1;
		double amplitude = 1;
		double maxValue = 0;

		for (int i = 0; i < octaves; i++) {
			total += this.noise2D(x * frequency, y * frequency) * amplitude;
			maxValue += amplitude;
			amplitude *= persistence;
			frequency *= lacunarity;
		}

		return total / maxValue;
	}

	private static int fastFloor(final double x) {
		final int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}

	private static double grad(final int hash, final double x, final double y) {
		final int h = hash & 7;
		final double u = h < 4 ? x : y;
		final double v = h < 4 ? y : x;
		return ((h & 1) != 0 ? -u : u) + ((h & 2) != 0 ? -2.0 * v : 2.0 * v);
	}

}
