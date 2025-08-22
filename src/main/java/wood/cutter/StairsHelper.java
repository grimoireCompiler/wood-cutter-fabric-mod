package wood.cutter;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.*;

public class StairsHelper {
    private static Map<Block, Block> blockToStairsCache = null;

    public static Map<Block, Block> initializeCache(MinecraftServer server) {
        if (blockToStairsCache != null) {
            return blockToStairsCache;
        }

        blockToStairsCache = new HashMap<>();

        RecipeManager recipeManager = server.getRecipeManager();
        Collection<StonecuttingRecipe> stonecuttingRecipes = recipeManager
                .listAllOfType(RecipeType.STONECUTTING);

        for (StonecuttingRecipe recipe : stonecuttingRecipes) {
            ItemStack result = recipe.getOutput(server.getRegistryManager());
            Block resultBlock = Block.getBlockFromItem(result.getItem());

            if (resultBlock instanceof StairsBlock) {
                Ingredient ingredient = recipe.getIngredients().get(0);

                for (ItemStack inputStack : ingredient.getMatchingStacks()) {
                    Block inputBlock = Block.getBlockFromItem(inputStack.getItem());
                    if (inputBlock != Blocks.AIR) {
                        blockToStairsCache.compute(inputBlock, (k,old) ->
                                (old == null || Registries.BLOCK.getId(resultBlock).toString().length() < Registries.BLOCK.getId(old).toString().length() ) ? resultBlock : old);
                    }
                }
            }
        }

        return blockToStairsCache;
    }

    public static boolean hasStairsVariant(Block block) {
        Block stairs = blockToStairsCache.get(block);
        return stairs != null;
    }

    public static Block getStairsVariant(Block block) {
        return blockToStairsCache.get(block);
    }
}