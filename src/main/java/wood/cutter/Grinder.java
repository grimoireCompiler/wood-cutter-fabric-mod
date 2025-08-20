package wood.cutter;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Grinder extends Item {

    public Grinder(Settings settings) {
        super(settings);
    }

    public static final String MOD_ID = "wood-cutter";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState blockState = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();
        Block block = blockState.getBlock();

        if(block instanceof StairsBlock blockStair){
            if(player.isSneaking()) {
                BlockHalf half = blockState.get(StairsBlock.HALF);
                blockState = blockState.with(StairsBlock.HALF, half == BlockHalf.BOTTOM ? BlockHalf.TOP : BlockHalf.BOTTOM);
            }else
                blockState = blockStair.rotate(blockState, BlockRotation.CLOCKWISE_90);

            //Handle connecting stiars
            Direction facing = blockState.get(StairsBlock.FACING);
            BlockPos nPos = pos.add(facing.getVector());
            blockState = blockState.getStateForNeighborUpdate(Direction.UP, world.getBlockState(nPos), world, pos, nPos);

            world.setBlockState(pos, blockState);
            world.updateNeighbors(pos, block);

            world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 1f, (float) (0.5 + 0.5*Math.random()));
            return ActionResult.SUCCESS;
        }else if (StairsHelper.hasStairsVariant(block)) {
            if (!world.isClient) {
                Direction facing = context.getHorizontalPlayerFacing();
                Block stair = StairsHelper.getStairsVariant(block);
                BlockState stairState = stair.getDefaultState().with(StairsBlock.FACING, facing);

                //Project the hit through the block
                Vec3d eye = player.getEyePos();
                Vec3d hitPos = context.getHitPos();
                Vec3d line = hitPos.subtract(eye);
                Direction projection = facing.getOpposite();
                Vec3d planeNormal = Vec3d.of(projection.getVector());
                Vec3d planeCenter = pos.toCenterPos().subtract(planeNormal.multiply(0.5));
                double d = planeCenter.subtract(eye).dotProduct(planeNormal)/(line.dotProduct(planeNormal));
                Vec3d newHitPos = eye.add(line.multiply(d));

                // Create a new hit result as if hitting the block behind
                BlockHitResult newHitResult = new BlockHitResult(
                        newHitPos,
                        projection,
                        pos,
                        true
                );

                ItemPlacementContext placementContext = new ItemPlacementContext(
                        player,
                        context.getHand(),
                        new ItemStack(stair.asItem()),
                        newHitResult
                );

                world.breakBlock(pos, false);
                if (stair instanceof StairsBlock stairsBlockCast) {
                    stairState = stairsBlockCast.getPlacementState(placementContext);
                    if (stairState == null)
                        stairState = stair.getDefaultState().with(StairsBlock.FACING, facing);

                }
                world.setBlockState(pos, stairState);
                world.updateNeighbors(pos, block);
                world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 1f, (float) (0.5 + 0.5*Math.random()));
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
