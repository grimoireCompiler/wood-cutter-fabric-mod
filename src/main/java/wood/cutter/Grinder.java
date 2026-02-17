package wood.cutter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Grinder extends Item {

    public Grinder(Properties settings) {
        super(settings);
    }

    public static final String MOD_ID = "wood-cutter";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState blockState = world.getBlockState(pos);
        Player player = context.getPlayer();
        Block block = blockState.getBlock();
        if(block instanceof StairBlock blockStair){
            if(player.isShiftKeyDown()) {
                Half half = blockState.getValue(StairBlock.HALF);
                blockState = blockState.setValue(StairBlock.HALF, half == Half.BOTTOM ? Half.TOP : Half.BOTTOM);
            }else
                blockState = blockState.rotate(Rotation.CLOCKWISE_90);

            //Handle connecting stairs
            Direction facing = blockState.getValue(StairBlock.FACING);
            BlockPos nPos = pos.offset(facing.getUnitVec3i());

            //blockState = blockState.updateShape(world, null, pos, Direction.UP, nPos, world.getBlockState(nPos), world.random);

            world.setBlockAndUpdate(pos, blockState);
            world.updateNeighborsAt(pos, block);

            world.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1f, (float) (0.5 + 0.5*Math.random()));
            return InteractionResult.SUCCESS;
        }else if (StairsHelper.hasStairsVariant(block)) {
            if (!world.isClientSide()) {
                Direction facing = context.getHorizontalDirection();
                Block stair = StairsHelper.getStairsVariant(block);
                BlockState stairState = stair.defaultBlockState().setValue(StairBlock.FACING, facing);

                //Project the hit through the block
                Vec3 eye = player.getEyePosition();
                Vec3 hitPos = context.getClickLocation();
                Vec3 line = hitPos.subtract(eye);
                Direction projection = facing.getOpposite();
                Vec3 planeNormal = Vec3.atLowerCornerOf(projection.getUnitVec3i());
                Vec3 planeCenter = pos.getCenter().subtract(planeNormal.scale(0.5));
                double d = planeCenter.subtract(eye).dot(planeNormal)/(line.dot(planeNormal));
                Vec3 newHitPos = eye.add(line.scale(d));

                // Create a new hit result as if hitting the block behind
                BlockHitResult newHitResult = new BlockHitResult(
                        newHitPos,
                        projection,
                        pos,
                        true
                );

                BlockPlaceContext placementContext = new BlockPlaceContext(
                        player,
                        context.getHand(),
                        new ItemStack(stair.asItem()),
                        newHitResult
                );

                world.destroyBlock(pos, false);
                if (stair instanceof StairBlock stairsBlockCast) {
                    stairState = stairsBlockCast.getStateForPlacement(placementContext);
                    if (stairState == null)
                        stairState = stair.defaultBlockState().setValue(StairBlock.FACING, facing);

                }
                world.setBlockAndUpdate(pos, stairState);
                world.updateNeighborsAt(pos, block);
                world.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1f, (float) (0.5 + 0.5*Math.random()));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
