package io.github.haykam821.cabinfever.game.phase;

import eu.pb4.holograms.api.Holograms;
import eu.pb4.holograms.api.holograms.AbstractHologram;
import eu.pb4.holograms.api.holograms.AbstractHologram.VerticalAlign;
import io.github.haykam821.cabinfever.game.CabinFeverConfig;
import io.github.haykam821.cabinfever.game.map.CabinFeverMap;
import io.github.haykam821.cabinfever.game.map.CabinFeverMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class CabinFeverWaitingPhase {
	private static final Formatting GUIDE_FORMATTING = Formatting.GOLD;
	private static final Text[] GUIDE_LINES = {
		Text.translatable("gameType.cabinfever.cabin_fever").formatted(GUIDE_FORMATTING).formatted(Formatting.BOLD),
		Text.translatable("text.cabinfever.guide.mine_coal").formatted(GUIDE_FORMATTING),
		Text.translatable("text.cabinfever.guide.place_coal_in_campfire").formatted(GUIDE_FORMATTING),
		Text.translatable("text.cabinfever.guide.dont_run_out_of_coal").formatted(GUIDE_FORMATTING),
		Text.translatable("text.cabinfever.guide.death_expends_coal_faster").formatted(GUIDE_FORMATTING),
	};

	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final CabinFeverMap map;
	private final CabinFeverConfig config;
	private AbstractHologram guideText;

	public CabinFeverWaitingPhase(GameSpace gameSpace, ServerWorld world, CabinFeverMap map, CabinFeverConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<CabinFeverConfig> context) {
		CabinFeverMapBuilder mapBuilder = new CabinFeverMapBuilder(context.config());
		CabinFeverMap map = mapBuilder.create();

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()))
			.setRaining(true)
			.setTimeOfDay(18000);

		return context.openWithWorld(worldConfig, (activity, world) -> {
			CabinFeverWaitingPhase phase = new CabinFeverWaitingPhase(activity.getGameSpace(), world, map, context.config());
			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());

			CabinFeverActivePhase.setRules(activity);
			activity.deny(GameRuleType.PVP);

			// Listeners
			activity.listen(PlayerDamageEvent.EVENT, phase::onPlayerDamage);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getSpawn()).and(() -> {
			offer.player().changeGameMode(GameMode.ADVENTURE);
		});
	}

	private GameResult requestStart() {
		CabinFeverActivePhase.open(this.gameSpace, this.world, this.map, this.config, this.guideText);
		return GameResult.ok();
	}

	private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return ActionResult.FAIL;
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		CabinFeverActivePhase.spawn(this.world, this.map, player);
		return ActionResult.FAIL;
	}

	private void enable() {
		// Spawn guide text
		Vec3d center = Vec3d.of(this.map.getCenter()).add(0.5, 1.8, 0.5);

		this.guideText = Holograms.create(this.world, center, GUIDE_LINES);
		this.guideText.setAlignment(VerticalAlign.CENTER);

		this.guideText.show();
	}
}