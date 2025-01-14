package draconictransmutation.integration.jei.world_transmute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.matrix.MatrixStack;

import draconictransmutation.DTCore;
import draconictransmutation.api.imc.WorldTransmutationEntry;
import draconictransmutation.gameObjs.registries.DTItems;
import draconictransmutation.utils.WorldTransmutations;
import draconictransmutation.utils.text.DTLang;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;

public class WorldTransmuteRecipeCategory implements IRecipeCategory<WorldTransmuteEntry> {

	public static final ResourceLocation UID = DTCore.rl("world_transmutation");
	private final IDrawable background;
	private final IDrawable arrow;
	private final IDrawable icon;
	private final String localizedName;

	public WorldTransmuteRecipeCategory(IGuiHelper guiHelper) {
		background = guiHelper.createBlankDrawable(135, 48);
		arrow = guiHelper.drawableBuilder(DTCore.rl("textures/gui/arrow.png"), 0, 0, 22, 15).setTextureSize(32, 32).build();
		icon = guiHelper.createDrawableIngredient(new ItemStack(DTItems.PHILOSOPHERS_STONE));
		localizedName = DTLang.WORLD_TRANSMUTE.translate().getString();
	}

	@Nonnull
	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Nonnull
	@Override
	public Class<WorldTransmuteEntry> getRecipeClass() {
		return WorldTransmuteEntry.class;
	}

	@Nonnull
	@Override
	public String getTitle() {
		return localizedName;
	}

	@Nonnull
	@Override
	public IDrawable getBackground() {
		return background;
	}

	@Nonnull
	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void draw(@Nonnull WorldTransmuteEntry recipe, @Nonnull MatrixStack matrix, double mouseX, double mouseY) {
		arrow.draw(matrix, 55, 18);
	}

	@Override
	public void setIngredients(WorldTransmuteEntry recipe, @Nonnull IIngredients ingredients) {
		recipe.setIngredients(ingredients);
	}

	@Override
	public void setRecipe(@Nonnull IRecipeLayout recipeLayout, @Nonnull WorldTransmuteEntry recipeWrapper, @Nonnull IIngredients ingredients) {
		int itemSlots = 0;
		int fluidSlots = 0;

		int xPos = 16;
		for (List<FluidStack> s : ingredients.getInputs(VanillaTypes.FLUID)) {
			recipeLayout.getFluidStacks().init(fluidSlots, true, xPos, 16, 16, 16, FluidAttributes.BUCKET_VOLUME, false, null);
			recipeLayout.getFluidStacks().set(fluidSlots, s);
			fluidSlots++;
			xPos += 16;
		}

		xPos = 16;
		for (List<ItemStack> s : ingredients.getInputs(VanillaTypes.ITEM)) {
			recipeLayout.getItemStacks().init(itemSlots, true, xPos, 16);
			recipeLayout.getItemStacks().set(itemSlots, s);
			itemSlots++;
			xPos += 16;
		}

		xPos = 96;
		for (List<ItemStack> stacks : ingredients.getOutputs(VanillaTypes.ITEM)) {
			recipeLayout.getItemStacks().init(itemSlots, false, xPos, 16);
			recipeLayout.getItemStacks().set(itemSlots, stacks);
			itemSlots++;
			xPos += 16;
		}

		xPos = 96;
		for (List<FluidStack> stacks : ingredients.getOutputs(VanillaTypes.FLUID)) {
			recipeLayout.getFluidStacks().init(fluidSlots, false, xPos, 16, 16, 16, FluidAttributes.BUCKET_VOLUME, false, null);
			recipeLayout.getFluidStacks().set(fluidSlots, stacks);
			fluidSlots++;
			xPos += 16;
		}
	}

	@Nonnull
	@Override
	public List<ITextComponent> getTooltipStrings(@Nonnull WorldTransmuteEntry recipe, double mouseX, double mouseY) {
		if (mouseX > 67 && mouseX < 107 && mouseY > 18 && mouseY < 38) {
			return Collections.singletonList(DTLang.WORLD_TRANSMUTE_DESCRIPTION.translate());
		}
		return Collections.emptyList();
	}

	public static List<WorldTransmuteEntry> getAllTransmutations() {
		List<WorldTransmutationEntry> allWorldTransmutations = WorldTransmutations.getWorldTransmutations();
		//All the ones that have a block state that can be rendered in JEI.
		//For example only render one pumpkin to melon transmutation
		List<WorldTransmuteEntry> visible = new ArrayList<>();
		allWorldTransmutations.forEach(entry -> {
			WorldTransmuteEntry e = new WorldTransmuteEntry(entry);
			if (e.isRenderable()) {
				boolean alreadyHas;
				FluidStack inputFluid = e.getInputFluid();
				if (inputFluid != null) {
					Fluid fluid = inputFluid.getFluid();
					alreadyHas = visible.stream().map(WorldTransmuteEntry::getInputFluid).anyMatch(otherInputFluid -> otherInputFluid != null && fluid == otherInputFluid.getFluid());
				} else {
					ItemStack inputItem = e.getInputItem();
					alreadyHas = visible.stream().anyMatch(otherEntry -> inputItem.isItemEqual(otherEntry.getInputItem()));
				}
				if (!alreadyHas) {
					//Only add items that we haven't already had.
					visible.add(e);
				}
			}
		});
		return visible;
	}
}