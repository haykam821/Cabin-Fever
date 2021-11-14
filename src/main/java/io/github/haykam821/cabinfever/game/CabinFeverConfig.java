package io.github.haykam821.cabinfever.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.cabinfever.game.map.CabinFeverMapConfig;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class CabinFeverConfig {
	public static final Codec<CabinFeverConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			CabinFeverMapConfig.CODEC.fieldOf("map").forGetter(CabinFeverConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(CabinFeverConfig::getPlayerConfig),
			Codec.INT.optionalFieldOf("guide_ticks", 20 * 30).forGetter(CabinFeverConfig::getGuideTicks),
			Codec.INT.optionalFieldOf("max_coal", 32).forGetter(CabinFeverConfig::getMaxCoal),
			Codec.INT.optionalFieldOf("max_held_coal", 8).forGetter(CabinFeverConfig::getMaxHeldCoal),
			Codec.INT.optionalFieldOf("death_price", 12).forGetter(CabinFeverConfig::getDeathPrice)
		).apply(instance, CabinFeverConfig::new);
	});

	private final CabinFeverMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final int guideTicks;
	private final int maxCoal;
	private final int maxHeldCoal;
	private final int deathPrice;

	public CabinFeverConfig(CabinFeverMapConfig mapConfig, PlayerConfig playerConfig, int guideTicks, int maxCoal, int maxHeldCoal, int deathPrice) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.guideTicks = guideTicks;
		this.maxCoal = maxCoal;
		this.maxHeldCoal = maxHeldCoal;
		this.deathPrice = deathPrice;
	}

	public CabinFeverMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public int getGuideTicks() {
		return this.guideTicks;
	}

	public int getMaxCoal() {
		return this.maxCoal;
	}

	public int getMaxHeldCoal() {
		return this.maxHeldCoal;
	}

	public int getDeathPrice() {
		return this.deathPrice;
	}
}