package draconictransmutation.api.capabilities.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraftforge.common.capabilities.Capability;

/**
 * This interface specifies items that switch between modes when the mode switch keybind is activated (default G)
 *
 * This is exposed through the Capability system.
 *
 * Acquire an instance of this using {@link ItemStack#getCapability(Capability, Direction)}.
 */
public interface IModeChanger {

	/**
	 * Gets the mode from this ItemStack
	 *
	 * @param stack The stack we want the mode of
	 *
	 * @return The mode of this ItemStack
	 */
	byte getMode(@Nonnull ItemStack stack);

	/**
	 * Called serverside when the player presses change mode
	 *
	 * @param player The player pressing the change mode key
	 * @param stack  The stack whose mode we are changing
	 * @param hand   The hand this stack was in, or null if the call was not from the player's hands
	 *
	 * @return Whether the operation succeeded
	 */
	boolean changeMode(@Nonnull PlayerEntity player, @Nonnull ItemStack stack, @Nullable Hand hand);
}