package io.github.haykam821.cabinfever.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class CabinFeverMapConfig {
	public static final Codec<CabinFeverMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(CabinFeverMapConfig::getX),
			Codec.INT.fieldOf("z").forGetter(CabinFeverMapConfig::getZ),
			Codec.INT.optionalFieldOf("lumps", 12).forGetter(CabinFeverMapConfig::getLumps)
		).apply(instance, CabinFeverMapConfig::new);
	});

	private final int x;
	private final int z;
	private final int lumps;

	public CabinFeverMapConfig(int x, int z, int lumps) {
		this.x = x;
		this.z = z;
		this.lumps = lumps;
	}

	public int getX() {
		return this.x;
	}

	public int getZ() {
		return this.z;
	}

	public int getLumps() {
		return this.lumps;
	}
}