package io.github.haykam821.cabinfever;

import io.github.haykam821.cabinfever.game.CabinFeverConfig;
import io.github.haykam821.cabinfever.game.phase.CabinFeverWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	private static final String MOD_ID = "cabinfever";

	private static final Identifier CABIN_FEVER_ID = new Identifier(MOD_ID, "cabin_fever");
	public static final GameType<CabinFeverConfig> CABIN_FEVER_TYPE = GameType.register(CABIN_FEVER_ID, CabinFeverConfig.CODEC, CabinFeverWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}
}