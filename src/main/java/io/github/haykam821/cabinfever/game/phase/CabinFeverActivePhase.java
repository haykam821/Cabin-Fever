package io.github.haykam821.cabinfever.game.phase;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import eu.pb4.holograms.api.holograms.AbstractHologram;
import io.github.haykam821.cabinfever.game.CabinFeverConfig;
import io.github.haykam821.cabinfever.game.map.CabinFeverMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class CabinFeverActivePhase {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final CabinFeverMap map;
	private final CabinFeverConfig config;
	private final AbstractHologram guideText;
	private final Set<PlayerRef> players;
	private final Object2IntOpenHashMap<PlayerRef> coalAmounts = new Object2IntOpenHashMap<>();
	private boolean singleplayer;
	private int ticks = 0;

	public CabinFeverActivePhase(GameSpace gameSpace, ServerWorld world, CabinFeverMap map, CabinFeverConfig config, AbstractHologram guideText, Set<PlayerRef> players) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.guideText = guideText;

		this.players = players;
		this.coalAmounts.defaultReturnValue(this.config.getMaxCoal());
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.PORTALS);
		activity.allow(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, CabinFeverMap map, CabinFeverConfig config, AbstractHologram guide) {
		Set<PlayerRef> players = gameSpace.getPlayers().stream().map(PlayerRef::of).collect(Collectors.toSet());
		CabinFeverActivePhase phase = new CabinFeverActivePhase(gameSpace, world, map, config, guide, players);

		gameSpace.setActivity(activity -> {
			CabinFeverActivePhase.setRules(activity);

			// Listeners
			activity.listen(BlockBreakEvent.EVENT, phase::onBreakBlock);
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(BlockUseEvent.EVENT, phase::onUseBlock);
		});
	}

	private void enable() {
		this.singleplayer = this.players.size() == 1;

		ItemStack sword = ItemStackBuilder.of(Items.WOODEN_SWORD)
			.setUnbreakable()
			.build();

		ItemStack tool = ItemStackBuilder.of(Items.WOODEN_PICKAXE)
			.setUnbreakable()
			.addCanDestroy(Blocks.COAL_ORE)
			.addCanDestroy(Blocks.COAL_BLOCK)
			.build();

 		for (PlayerRef playerRef : this.players) {
			playerRef.ifOnline(this.world, player -> {
				player.changeGameMode(GameMode.ADVENTURE);
				CabinFeverActivePhase.spawn(this.world, this.map, player);

				player.giveItemStack(sword.copy());
				player.giveItemStack(tool.copy());
			});
		}
	}

	private boolean updatePlayer(PlayerRef playerRef, ServerPlayerEntity player) {
		int coalAmount = this.coalAmounts.getInt(playerRef);

		player.experienceProgress = MathHelper.clamp(coalAmount / (float) this.config.getMaxCoal(), 0, 1);
		player.setExperienceLevel(coalAmount);

		if (coalAmount <= 0) {
			this.eliminate(player, false);
			return true;
		}
		return false;
	}

	private void tick() {
		this.ticks += 1;
		if (this.guideText != null && ticks == this.config.getGuideTicks()) {
			this.guideText.hide();
		}

		// Eliminate players that do not have enough coal
		boolean decrementCoal = this.ticks % 60 == 0;
		Iterator<PlayerRef> playerIterator = this.players.iterator();
		while (playerIterator.hasNext()) {
			PlayerRef playerRef = playerIterator.next();
			if (decrementCoal) {
				this.coalAmounts.addTo(playerRef, -1);
			}

			playerRef.ifOnline(this.world, player -> {
				if (this.updatePlayer(playerRef, player)) {
					playerIterator.remove();
				}
			});
		}

		// Attempt to determine a winner
		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage());
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	private Text getEndingMessage() {
		if (this.players.size() == 1) {
			PlayerRef winnerRef = this.players.iterator().next();
			if (winnerRef.isOnline(this.world)) {
				PlayerEntity winner = winnerRef.getEntity(this.world);
				return new TranslatableText("text.cabinfever.win", winner.getDisplayName()).formatted(Formatting.GOLD);
			}
		}
		return new TranslatableText("text.cabinfever.no_winners").formatted(Formatting.GOLD);
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getSpawn()).and(() -> {
			offer.player().changeGameMode(GameMode.SPECTATOR);
		});
	}

	private void eliminate(ServerPlayerEntity eliminatedPlayer, boolean remove) {
		this.gameSpace.getPlayers().sendMessage(new TranslatableText("text.cabinfever.eliminated", eliminatedPlayer.getDisplayName()).formatted(Formatting.RED));

		if (remove) {
			this.players.remove(PlayerRef.of(eliminatedPlayer));
		}
		this.setSpectator(eliminatedPlayer);
	}

	private void removePlayer(ServerPlayerEntity player) {
		this.eliminate(player, true);
	}

	private void clearCoal(ServerPlayerEntity player) {
		player.getInventory().remove(CabinFeverActivePhase::isCoal, -1, player.playerScreenHandler.getCraftingInput());
		player.currentScreenHandler.sendContentUpdates();
		player.playerScreenHandler.onContentChanged(player.getInventory());
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		PlayerRef playerRef = PlayerRef.of(player);
		if (this.players.contains(playerRef)) {
			this.coalAmounts.addTo(playerRef, -this.config.getDeathPrice());
			this.clearCoal(player);
			
			Text deathMessage = source.getDeathMessage(player).shallowCopy().formatted(Formatting.RED);
			this.gameSpace.getPlayers().sendMessage(deathMessage);
		}

		CabinFeverActivePhase.spawn(this.world, this.map, player);
		return ActionResult.FAIL;
	}

	private ActionResult onBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);

		int coalAmount = CabinFeverActivePhase.getBlockCoalAmount(state);
		if (coalAmount > 0) {
			PlayerRef playerRef = PlayerRef.of(player);
			if (this.players.contains(playerRef)) {
				int coalUntilMax = this.config.getMaxHeldCoal() - player.getInventory().count(Items.COAL);
				player.giveItemStack(new ItemStack(Items.COAL, Math.min(coalAmount, coalUntilMax)));
			}
		}

		return ActionResult.FAIL;
	}

	private ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		PlayerRef playerRef = PlayerRef.of(player);
		if (this.players.contains(playerRef)) {
			BlockState state = this.world.getBlockState(hitResult.getBlockPos());
			if (state.isIn(BlockTags.CAMPFIRES)) {
				int heldCoal = player.getInventory().count(Items.COAL);
				int coalUntilMax = this.config.getMaxCoal() - this.coalAmounts.getInt(playerRef);

				this.coalAmounts.addTo(playerRef, Math.min(heldCoal, coalUntilMax));
				this.clearCoal(player);

				player.playSound(SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1, 1);
			}
		}

		return ActionResult.FAIL;
	}

	private static boolean isCoal(ItemStack stack) {
		return stack.isIn(ItemTags.COALS);
	}

	private static int getBlockCoalAmount(BlockState state) {
		if (state.isOf(Blocks.COAL_ORE)) return 1;
		if (state.isOf(Blocks.COAL_BLOCK)) return 3;
		return 0;
	}

	public static void spawn(ServerWorld world, CabinFeverMap map, ServerPlayerEntity player) {
		Vec3d spawn = map.getSpawn();
		player.teleport(world, spawn.getX(), spawn.getY(), spawn.getZ(), 0, 0);
	}
}