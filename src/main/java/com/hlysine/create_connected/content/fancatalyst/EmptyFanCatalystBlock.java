package com.hlysine.create_connected.content.fancatalyst;

import com.hlysine.create_connected.content.WrenchableBlock;
import com.hlysine.create_connected.registries.CCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EmptyFanCatalystBlock extends WrenchableBlock {
    public EmptyFanCatalystBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Block result = catalystFor(itemId);
        if (result == null)
            return super.useItemOn(stack, state, level, pos, player, hand, hit);

        if (!level.isClientSide()) {
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
            level.setBlockAndUpdate(pos, result.defaultBlockState());
            if (!player.isCreative()) {
                if (isBucketInput(itemId))
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                else
                    stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.75f, 1.1f);
        }
        return InteractionResult.SUCCESS;
    }

    private static Block catalystFor(String itemId) {
        return switch (itemId) {
            case "minecraft:lava_bucket" -> CCBlocks.FAN_BLASTING_CATALYST;
            case "minecraft:netherrack" -> CCBlocks.FAN_SMOKING_CATALYST;
            case "minecraft:water_bucket" -> CCBlocks.FAN_SPLASHING_CATALYST;
            case "minecraft:soul_sand" -> CCBlocks.FAN_HAUNTING_CATALYST;
            case "minecraft:powder_snow_bucket" -> CCBlocks.FAN_FREEZING_CATALYST;
            case "create:blaze_cake" -> CCBlocks.FAN_SEETHING_CATALYST;
            case "minecraft:sand" -> CCBlocks.FAN_SANDING_CATALYST;
            case "createnuclear:enriched_soul_soil" -> CCBlocks.FAN_ENRICHED_CATALYST;
            case "create_dragons_plus:dragon_breath_bucket" -> CCBlocks.FAN_ENDING_CATALYST_DRAGONS_BREATH;
            case "minecraft:dragon_head" -> CCBlocks.FAN_ENDING_CATALYST_DRAGON_HEAD;
            case "minecraft:wither_rose" -> CCBlocks.FAN_WITHERING_CATALYST;
            case "create:chocolate_bucket" -> CCBlocks.FAN_CHOCOLATE_COATING_CATALYST;
            case "create:honey_bucket" -> CCBlocks.FAN_HONEY_COATING_CATALYST;
            case "minecraft:creeper_head" -> CCBlocks.FAN_EXPLODING_CATALYST;
            case "minecraft:amethyst_cluster" -> CCBlocks.FAN_RESONANCE_CATALYST;
            case "minecraft:sculk_catalyst" -> CCBlocks.FAN_SCULKING_CATALYST;
            case "minecraft:beacon" -> CCBlocks.FAN_PURIFYING_CATALYST;
            case "create_shimmer:shimmer_bucket" -> CCBlocks.FAN_TRANSMUTATION_CATALYST;
            case "twilightforest:exanimate_essence" -> CCBlocks.FAN_GLOOMING_CATALYST;
            case "createnetherindustry:mysterious_ancient_mechanism" -> CCBlocks.FAN_SOUL_STRIPPING_CATALYST;
            default -> dyeCatalystFor(itemId);
        };
    }

    private static Block dyeCatalystFor(String itemId) {
        String path = itemId.substring(itemId.indexOf(':') + 1);
        String suffix = path.endsWith("_mastic_resin_bucket") ? "_mastic_resin_bucket" :
                path.endsWith("_dye_bucket") ? "_dye_bucket" : null;
        if (suffix == null)
            return null;
        String colorName = path.substring(0, path.length() - suffix.length());
        if (colorName.startsWith("dye_depot_"))
            colorName = colorName.substring("dye_depot_".length());
        DyeColor color = DyeColor.byName(colorName, null);
        return color == null ? null : CCBlocks.FAN_DYEING_CATALYSTS.get(color);
    }

    private static boolean isBucketInput(String itemId) {
        return itemId.endsWith("_bucket");
    }
}
