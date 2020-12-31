package io.github.haykam821.cabinfever.game.phase;

import io.github.haykam821.cabinfever.game.CabinFeverConfig;
import io.github.haykam821.cabinfever.game.map.CabinFeverMap;
import io.github.haykam821.cabinfever.game.map.CabinFeverMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;

public class CabinFeverWaitingPhase {
	private final GameSpace gameSpace;
	private final CabinFeverMap map;
	private final CabinFeverConfig config;

	public CabinFeverWaitingPhase(GameSpace gameSpace, CabinFeverMap map, CabinFeverConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<CabinFeverConfig> context) {
		CabinFeverMapBuilder mapBuilder = new CabinFeverMapBuilder(context.getConfig());
		CabinFeverMap map = mapBuilder.create();

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.createGenerator(context.getServer()))
			.setRaining(true)
			.setTimeOfDay(18000)
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			CabinFeverWaitingPhase phase = new CabinFeverWaitingPhase(game.getSpace(), map, context.getConfig());

			GameWaitingLobby.applyTo(game, context.getConfig().getPlayerConfig());
			CabinFeverActivePhase.setRules(game);

			// Listeners
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
			game.on(OfferPlayerListener.EVENT, phase::offerPlayer);
			game.on(RequestStartListener.EVENT, phase::requestStart);
		});
	}

	private boolean isFull() {
		return this.gameSpace.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	private JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	private StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameSpace.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		CabinFeverActivePhase.open(this.gameSpace, this.map, this.config);
		return StartResult.OK;
	}

	private void addPlayer(ServerPlayerEntity player) {
		CabinFeverActivePhase.spawn(this.gameSpace.getWorld(), this.map, player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		CabinFeverActivePhase.spawn(this.gameSpace.getWorld(), this.map, player);
		return ActionResult.FAIL;
	}
}