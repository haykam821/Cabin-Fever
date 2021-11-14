package io.github.haykam821.cabinfever.game.map;

import java.util.Random;

import io.github.haykam821.cabinfever.game.CabinFeverConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;
import xyz.nucleoid.map_templates.MapTemplate;

public class CabinFeverMapBuilder {
	private static final BlockState FLOOR = Blocks.SNOW_BLOCK.getDefaultState();
	private static final BlockState CAMPFIRE = Blocks.CAMPFIRE.getDefaultState();
	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final BlockState ROOF = Blocks.SPRUCE_SLAB.getDefaultState();
	private static final BlockState PILLAR = Blocks.COBBLESTONE_WALL.getDefaultState();
	private static final BlockState COAL_ORE = Blocks.COAL_ORE.getDefaultState();
	private static final BlockState COAL_BLOCK = Blocks.COAL_BLOCK.getDefaultState();

	private final CabinFeverConfig config;

	public CabinFeverMapBuilder(CabinFeverConfig config) {
		this.config = config;
	}

	public CabinFeverMap create() {
		MapTemplate template = MapTemplate.createEmpty();
		CabinFeverMapConfig mapConfig = this.config.getMapConfig();

		this.buildLumps(template, mapConfig);
		this.buildFloor(template, mapConfig);

		BlockPos center = new BlockPos(mapConfig.getX() / 2, 1, mapConfig.getZ() / 2);
		this.buildCenter(center, template, mapConfig);

		template.setBiome(BiomeKeys.SNOWY_TAIGA);
		return new CabinFeverMap(template, center);
	}

	private void buildLumps(MapTemplate template, CabinFeverMapConfig mapConfig) {
		Random random = new Random();
		BlockPos.Mutable pos = new BlockPos.Mutable();

		for (int index = 0; index < mapConfig.getLumps(); index++) {
			int lumpX = random.nextInt(mapConfig.getX() - 6) + 3;
			int lumpZ = random.nextInt(mapConfig.getZ() - 6) + 3;

			BlockState state = random.nextInt(5) == 0 ? COAL_BLOCK : COAL_ORE;
			for (int y = 1; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					for (int z = 0; z < 3; z++) {
						if (random.nextInt(y) == 0) {
							pos.set(lumpX + x, y, lumpZ + z);
							template.setBlockState(pos, state);
						}
					}
				}
			}
		}
	}

	private void buildFloor(MapTemplate template, CabinFeverMapConfig mapConfig) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int x = 0; x < mapConfig.getX(); x++) {
			pos.setX(x);
			for (int z = 0; z < mapConfig.getZ(); z++) {
				pos.setZ(z);
				template.setBlockState(pos, FLOOR);
			}
		}
	}

	private void buildCenter(BlockPos center, MapTemplate template, CabinFeverMapConfig mapConfig) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int x = -2; x < 3; x++) {
			for (int z = -2; z < 3; z++) {
				for (int y = 1; y < 6; y++) {
					pos.set(center.getX() + x, y, center.getZ() + z);
					if (y == 5) {
						template.setBlockState(pos, ROOF);
					} else if ((x == -2 || x == 2) && (z == -2 || z == 2)) {
						template.setBlockState(pos, PILLAR);
					} else {
						template.setBlockState(pos, AIR);
					}
				}
			}
		}

		template.setBlockState(center, CAMPFIRE);
	}
}