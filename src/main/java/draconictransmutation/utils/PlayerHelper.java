package draconictransmutation.utils;

import java.math.BigInteger;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import draconictransmutation.DTCore;
import draconictransmutation.integration.IntegrationHelper;
import draconictransmutation.integration.curios.CuriosIntegration;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.items.IItemHandler;

/**
 * Helper class for player-related methods. Notice: Please try to keep methods tidy and alphabetically ordered. Thanks!
 */
public final class PlayerHelper {

	public final static ScoreCriteria SCOREBOARD_EMC = new ReadOnlyScoreCriteria(DTCore.MODID + ":emc_score");

	/**
	 * Tries placing a block and fires an event for it.
	 *
	 * @return Whether the block was successfully placed
	 */
	public static boolean checkedPlaceBlock(ServerPlayerEntity player, BlockPos pos, BlockState state) {
		if (!hasEditPermission(player, pos)) {
			return false;
		}
		World world = player.getEntityWorld();
		BlockSnapshot before = BlockSnapshot.create(world.getDimensionKey(), world, pos);
		world.setBlockState(pos, state);
		BlockEvent.EntityPlaceEvent evt = new BlockEvent.EntityPlaceEvent(before, Blocks.AIR.getDefaultState(), player);
		MinecraftForge.EVENT_BUS.post(evt);
		if (evt.isCanceled()) {
			world.restoringBlockSnapshots = true;
			before.restore(true, false);
			world.restoringBlockSnapshots = false;
			//PELogger.logInfo("Checked place block got canceled, restoring snapshot.");
			return false;
		}
		//PELogger.logInfo("Checked place block passed!");
		return true;
	}

	public static boolean checkedReplaceBlock(ServerPlayerEntity player, BlockPos pos, BlockState state) {
		return hasBreakPermission(player, pos) && checkedPlaceBlock(player, pos, state);
	}

	public static ItemStack findFirstItem(PlayerEntity player, Item consumeFrom) {
		return player.inventory.mainInventory.stream().filter(s -> !s.isEmpty() && s.getItem() == consumeFrom).findFirst().orElse(ItemStack.EMPTY);
	}

	@Nullable
	public static IItemHandler getCurios(PlayerEntity player) {
		if (ModList.get().isLoaded(IntegrationHelper.CURIO_MODID)) {
			return CuriosIntegration.getAll(player);
		}
		return null;
	}

	public static BlockRayTraceResult getBlockLookingAt(PlayerEntity player, double maxDistance) {
		Pair<Vector3d, Vector3d> vecs = getLookVec(player, maxDistance);
		RayTraceContext ctx = new RayTraceContext(vecs.getLeft(), vecs.getRight(), RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, player);
		return player.getEntityWorld().rayTraceBlocks(ctx);
	}

	/**
	 * Returns a vec representing where the player is looking, capped at maxDistance away.
	 */
	public static Pair<Vector3d, Vector3d> getLookVec(PlayerEntity player, double maxDistance) {
		// Thank you ForgeEssentials
		Vector3d look = player.getLook(1.0F);
		Vector3d playerPos = new Vector3d(player.getPosX(), player.getPosY() + player.getEyeHeight(), player.getPosZ());
		Vector3d src = playerPos.add(0, player.getEyeHeight(), 0);
		Vector3d dest = src.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);
		return ImmutablePair.of(src, dest);
	}

	public static boolean hasBreakPermission(ServerPlayerEntity player, BlockPos pos) {
		return hasEditPermission(player, pos) && ForgeHooks.onBlockBreakEvent(player.getEntityWorld(), player.interactionManager.getGameType(), player, pos) != -1;
	}

	public static boolean hasEditPermission(ServerPlayerEntity player, BlockPos pos) {
		if (ServerLifecycleHooks.getCurrentServer().isBlockProtected((ServerWorld) player.getEntityWorld(), pos, player)) {
			return false;
		}
		return Arrays.stream(Direction.values()).allMatch(e -> player.canPlayerEdit(pos, e, ItemStack.EMPTY));
	}

	public static void swingItem(PlayerEntity player, Hand hand) {
		if (player.getEntityWorld() instanceof ServerWorld) {
			((ServerWorld) player.getEntityWorld()).getChunkProvider().sendToTrackingAndSelf(player, new SAnimateHandPacket(player, hand == Hand.MAIN_HAND ? 0 : 3));
		}
	}

	public static void updateScore(ServerPlayerEntity player, ScoreCriteria objective, BigInteger value) {
		updateScore(player, objective, value.compareTo(Constants.MAX_INTEGER) > 0 ? Integer.MAX_VALUE : value.intValueExact());
	}

	public static void updateScore(ServerPlayerEntity player, ScoreCriteria objective, int value) {
		// [VanillaCopy] ServerPlayerEntity.updateScorePoints
		player.getWorldScoreboard().forAllObjectives(objective, player.getScoreboardName(), score -> score.setScorePoints(value));
	}
}