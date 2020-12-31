package io.github.haykam821.cabinfever.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;

public class CabinFeverMap {
	private final MapTemplate template;
	private final BlockPos center;

	public CabinFeverMap(MapTemplate template, BlockPos center) {
		this.template = template;
		this.center = center;
	}

	public BlockPos getCenter() {
		return this.center;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}