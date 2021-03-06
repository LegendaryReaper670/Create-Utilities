package com.fallenreaper.createutilities.utils;

import com.simibubi.create.content.contraptions.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.repack.registrate.providers.DataGenContext;
import com.simibubi.create.repack.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class BlockStateDataGenUtil {
    public class BlockStateUtils {
//* from create alloyed
        public static <T extends DirectionalAxisKineticBlock> void directionalPoweredAxisBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
            BlockStateGen.directionalAxisBlock(ctx, prov, (blockState, vertical) -> prov.models()
                    .getExistingFile(prov.modLoc("block/" + ctx.getName() + "/" + (vertical ? "vertical" : "horizontal") + (blockState.getValue(BlockStateProperties.POWERED) ? "_powered" : ""))));
        }

        public static <T extends Block> void facingPoweredAxisBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
            prov.directionalBlock(ctx.getEntry(),
                    blockState -> prov.models().getExistingFile(
                            prov.modLoc("block/" + ctx.getName() + "/block" + (blockState.getValue(BlockStateProperties.POWERED) ? "_powered" : ""))
                    )
            );
        }

        public static <T extends Block> void horizontalFacingLitBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
            prov.horizontalBlock(ctx.get(), blockState -> prov.models()
                    .getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block" + (blockState.getValue(AbstractFurnaceBlock.LIT) ? "_lit" : ""))));
        }


        public static <T extends Block> void facingBlockstate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, String modelPath) {
            prov.directionalBlock(ctx.getEntry(),
                    blockState -> prov.models().getExistingFile(
                            prov.modLoc(modelPath)
                    )
            );
        }
    }

}
