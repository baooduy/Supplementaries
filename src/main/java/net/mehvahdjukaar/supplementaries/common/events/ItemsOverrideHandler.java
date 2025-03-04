package net.mehvahdjukaar.supplementaries.common.events;

import net.mehvahdjukaar.selene.blocks.IOwnerProtected;
import net.mehvahdjukaar.selene.builtincompat.MapAtlasPlugin;
import net.mehvahdjukaar.selene.map.MapHelper;
import net.mehvahdjukaar.selene.util.Utils;
import net.mehvahdjukaar.supplementaries.api.IExtendedItem;
import net.mehvahdjukaar.supplementaries.common.block.blocks.*;
import net.mehvahdjukaar.supplementaries.common.block.tiles.CandleSkullBlockTile;
import net.mehvahdjukaar.supplementaries.common.block.tiles.DoubleSkullBlockTile;
import net.mehvahdjukaar.supplementaries.common.block.tiles.JarBlockTile;
import net.mehvahdjukaar.supplementaries.common.block.util.BlockUtils;
import net.mehvahdjukaar.supplementaries.common.capabilities.CapabilityHandler;
import net.mehvahdjukaar.supplementaries.common.entities.ThrowableBrickEntity;
import net.mehvahdjukaar.supplementaries.common.items.JarItem;
import net.mehvahdjukaar.supplementaries.common.items.additional_behaviors.SimplePlacement;
import net.mehvahdjukaar.supplementaries.common.items.additional_behaviors.WallLanternPlacement;
import net.mehvahdjukaar.supplementaries.common.network.ClientBoundSyncAntiqueInk;
import net.mehvahdjukaar.supplementaries.common.network.NetworkHandler;
import net.mehvahdjukaar.supplementaries.common.utils.CommonUtil;
import net.mehvahdjukaar.supplementaries.configs.ClientConfigs;
import net.mehvahdjukaar.supplementaries.configs.RegistryConfigs;
import net.mehvahdjukaar.supplementaries.configs.ServerConfigs;
import net.mehvahdjukaar.supplementaries.integration.CompatHandler;
import net.mehvahdjukaar.supplementaries.setup.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ItemsOverrideHandler {

    //TODO: clean this up

    //equivalent to Item.useOnBlock to the item itself (called before that though)
    private static final Map<Item, ItemUseOnBlockOverride> HP_ON_BLOCK_OVERRIDES = new HashMap<>();
    private static final Map<Item, ItemUseOnBlockOverride> ON_BLOCK_OVERRIDES = new HashMap<>();

    //equivalent to Item.use
    private static final Map<Item, ItemUseOverride> ITEM_USE_OVERRIDES = new HashMap<>();

    //equivalent to Block.use
    private static final Map<Block, BlockInteractedWithOverride> BLOCK_USE_OVERRIDES = new HashMap<>();

    public static boolean hasBlockPlacementAssociated(Item item) {
        ItemUseOnBlockOverride override = ON_BLOCK_OVERRIDES.get(item);
        return override != null;
    }

    public static void registerOverrides() {

        List<ItemUseOnBlockOverride> HPItemActionOnBlock = new ArrayList<>();
        List<ItemUseOnBlockOverride> itemActionOnBlock = new ArrayList<>();
        List<ItemUseOverride> itemAction = new ArrayList<>();
        List<BlockInteractedWithOverride> actionOnBlock = new ArrayList<>();

        actionOnBlock.add(new DirectionalCakeConversionBehavior());
        actionOnBlock.add(new BellChainBehavior());

        itemAction.add(new ThrowableBrickBehavior());
        itemAction.add(new ClockItemBehavior());

        HPItemActionOnBlock.add(new AntiqueInkBehavior());
        HPItemActionOnBlock.add(new WrenchBehavior());
        HPItemActionOnBlock.add(new SkullCandlesBehavior());

        //maybe move in mixin system (cant for cakes as block interaction has priority)
        itemActionOnBlock.add(new SkullPileBehavior());

        itemActionOnBlock.add(new EnhancedCakeBehavior());

        itemActionOnBlock.add(new MapMarkerBehavior());
        itemActionOnBlock.add(new XpBottlingBehavior());

        if (ServerConfigs.cached.WRITTEN_BOOKS) {
            ((IExtendedItem) Items.WRITABLE_BOOK).addAdditionalBehavior(new SimplePlacement(ModRegistry.BOOK_PILE.get()));
            ((IExtendedItem) Items.WRITTEN_BOOK).addAdditionalBehavior(new SimplePlacement(ModRegistry.BOOK_PILE.get()));
        }
        outer:
        for (Item i : ForgeRegistries.ITEMS) {

            if (ServerConfigs.cached.WALL_LANTERN_PLACEMENT) {
                if (i instanceof BlockItem bi && CommonUtil.isLanternBlock(bi.getBlock())) {
                    ((IExtendedItem) i).addAdditionalBehavior(new WallLanternPlacement());
                    continue;
                }
            }
            if (ServerConfigs.cached.PLACEABLE_BOOKS) {
                if (BookPileBlock.isQuarkTome(i)) {
                    ((IExtendedItem) i).addAdditionalBehavior(new SimplePlacement(ModRegistry.BOOK_PILE.get()));
                    continue;
                }
            }
            //block items dont work here
            /*
            if (ServerConfigs.cached.SKULL_CANDLES) {
                if (i.builtInRegistryHolder().is(ItemTags.CANDLES) &&
                        i.getRegistryName().getNamespace().equals("minecraft")) {
                    ((IExtendedItem) i).addAdditionalBehavior(new SkullCandlesPlacement());
                    continue;
                }
            }*/


            for (ItemUseOnBlockOverride b : itemActionOnBlock) {
                if (b.appliesToItem(i)) {
                    ON_BLOCK_OVERRIDES.put(i, b);
                    continue outer;
                }
            }
            for (ItemUseOverride b : itemAction) {
                if (b.appliesToItem(i)) {
                    ITEM_USE_OVERRIDES.put(i, b);
                    continue outer;
                }
            }
            for (ItemUseOnBlockOverride b : HPItemActionOnBlock) {
                if (b.appliesToItem(i)) {
                    HP_ON_BLOCK_OVERRIDES.put(i, b);
                    continue outer;
                }
            }
        }
        for (Block block : ForgeRegistries.BLOCKS) {
            for (BlockInteractedWithOverride b : actionOnBlock) {
                if (b.appliesToBlock(block)) {
                    BLOCK_USE_OVERRIDES.put(block, b);
                    break;
                }
            }
        }
    }

    public static void tryHighPriorityClickedBlockOverride(PlayerInteractEvent.RightClickBlock event, ItemStack stack) {
        Item item = stack.getItem();

        ItemUseOnBlockOverride override = HP_ON_BLOCK_OVERRIDES.get(item);
        if (override != null && override.isEnabled()) {

            InteractionResult result = override.tryPerformingAction(event.getWorld(), event.getPlayer(), event.getHand(), stack, event.getHitVec(), false);
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        }
    }


    //item clicked on block overrides
    public static void tryPerformClickedBlockOverride(PlayerInteractEvent.RightClickBlock event, ItemStack stack, boolean isRanged) {
        Item item = stack.getItem();
        Player player = event.getPlayer();

        ItemUseOnBlockOverride override = ON_BLOCK_OVERRIDES.get(item);
        if (override != null && override.isEnabled()) {

            InteractionResult result = override.tryPerformingAction(event.getWorld(), player, event.getHand(), stack, event.getHitVec(), isRanged);
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
                return;
            }
        }
        //block overrides behaviors (work for any item)
        if (!player.isShiftKeyDown()) {
            Level world = event.getWorld();
            BlockPos pos = event.getPos();
            BlockState state = world.getBlockState(pos);

            BlockInteractedWithOverride o = BLOCK_USE_OVERRIDES.get(state.getBlock());
            if (o != null && o.isEnabled()) {

                InteractionResult result = o.tryPerformingAction(state, pos, world, player, event.getHand(), stack, event.getHitVec());
                if (result != InteractionResult.PASS) {
                    event.setCanceled(true);
                    event.setCancellationResult(result);
                }
            }
        }
        //TODO: add             CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer) player, pos, heldStack);
    }

    //item clicked overrides
    public static void tryPerformClickedItemOverride(PlayerInteractEvent.RightClickItem event, ItemStack stack) {
        Item item = stack.getItem();

        ItemUseOverride override = ITEM_USE_OVERRIDES.get(item);
        if (override != null && override.isEnabled()) {

            InteractionResult result = override.tryPerformingAction(event.getWorld(), event.getPlayer(), event.getHand(), stack, null, false);
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }

        }
    }

    public static void addOverrideTooltips(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();

        ItemUseOnBlockOverride override = ON_BLOCK_OVERRIDES.get(item);
        if (override != null && override.isEnabled()) {
            List<Component> tooltip = event.getToolTip();
            BaseComponent t = override.getTooltip();
            if (t != null) tooltip.add(t.withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
        } else {
            ItemUseOverride o = ITEM_USE_OVERRIDES.get(item);
            if (o != null && o.isEnabled()) {
                List<Component> tooltip = event.getToolTip();
                BaseComponent t = o.getTooltip();
                if (t != null) tooltip.add(t.withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
            }
        }
    }


    private static abstract class BlockInteractedWithOverride {

        public abstract boolean isEnabled();

        public abstract boolean appliesToBlock(Block block);

        public abstract InteractionResult tryPerformingAction(BlockState state, BlockPos pos, Level world, Player player, InteractionHand hand,
                                                              ItemStack stack, BlockHitResult hit);
    }

    private static abstract class ItemUseOverride {

        public abstract boolean isEnabled();

        public abstract boolean appliesToItem(Item item);

        @Nullable
        public BaseComponent getTooltip() {
            return null;
        }

        public abstract InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand,
                                                              ItemStack stack, BlockHitResult hit, boolean isRanged);
    }

    private static abstract class ItemUseOnBlockOverride extends ItemUseOverride {

        public boolean shouldBlockMapToItem(Item item) {
            return appliesToItem(item);
        }

        @Nullable
        public BaseComponent getTooltip() {
            return null;
        }
    }


    private static class ClockItemBehavior extends ItemUseOverride {

        @Override
        public boolean isEnabled() {
            return ClientConfigs.cached.CLOCK_CLICK;
        }

        @Override
        public boolean appliesToItem(Item item) {
            return item == Items.CLOCK;
        }

        @Override
        public InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {
            if (world.isClientSide) {
                ClockBlock.displayCurrentHour(world, player);
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    private static class ThrowableBrickBehavior extends ItemUseOverride {

        @Override
        public boolean isEnabled() {
            return ServerConfigs.cached.THROWABLE_BRICKS_ENABLED;
        }

        @Nullable
        @Override
        public BaseComponent getTooltip() {
            return new TranslatableComponent("message.supplementaries.throwable_brick");
        }

        @Override
        public boolean appliesToItem(Item item) {
            return CommonUtil.isBrick(item);
        }

        @Override
        public InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
            if (!world.isClientSide) {
                ThrowableBrickEntity brickEntity = new ThrowableBrickEntity(world, player);
                brickEntity.setItem(stack);
                float pow = 0.7f;
                brickEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F * pow, 1.0F * pow);
                world.addFreshEntity(brickEntity);
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    private static class DirectionalCakeConversionBehavior extends BlockInteractedWithOverride {

        @Override
        public boolean isEnabled() {
            return ServerConfigs.cached.DIRECTIONAL_CAKE;
        }

        @Override
        public boolean appliesToBlock(Block block) {
            return block == Blocks.CAKE || (block.builtInRegistryHolder().is(BlockTags.CANDLE_CAKES) && block.getRegistryName().getNamespace().equals("minecraft"));
        }

        @Override
        public InteractionResult tryPerformingAction(BlockState state, BlockPos pos, Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
            //lets converting to candle cake
            if (state.is(BlockTags.CANDLE_CAKES) && stack.is(ItemTags.CANDLES)) {
                return InteractionResult.PASS;
            }
            if (state.is(Blocks.CAKE) && (stack.is(ItemTags.CANDLES) || player.getDirection() == Direction.EAST || state.getValue(CakeBlock.BITES) != 0)) {
                return InteractionResult.PASS;
            }
            if (!(ServerConfigs.cached.DOUBLE_CAKE_PLACEMENT && stack.is(Items.CAKE))) {
                //for candles. normal cakes have no drops
                BlockState newState = ModRegistry.DIRECTIONAL_CAKE.get().defaultBlockState();
                if (world.isClientSide) world.setBlock(pos, newState, 3);
                BlockHitResult raytrace = new BlockHitResult(
                        new Vec3(pos.getX(), pos.getY(), pos.getZ()), hit.getDirection(), pos, false);

                var r = newState.use(world, player, hand, raytrace);
                if (world instanceof ServerLevel serverLevel) {
                    if (r.consumesAction()) {
                        //prevents dropping cake
                        Block.getDrops(state, serverLevel, pos, null).forEach((d) -> {
                            if (d.getItem() != Items.CAKE) {
                                Block.popResource(world, pos, d);
                            }
                        });
                        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY);
                    } else world.setBlock(pos, state, 3); //returns to normal
                }
                return r;
            }
            //fallback to default cake interaction
            return InteractionResult.PASS;
        }
    }

    private static class BellChainBehavior extends BlockInteractedWithOverride {

        @Override
        public boolean isEnabled() {
            return ServerConfigs.cached.BELL_CHAIN;
        }

        @Override
        public boolean appliesToBlock(Block block) {
            return block instanceof ChainBlock;
        }

        @Override
        public InteractionResult tryPerformingAction(BlockState state, BlockPos pos, Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
            //bell chains
            if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                if (RopeBlock.findAndRingBell(world, pos, player, 0, s -> s.getBlock() instanceof ChainBlock && s.getValue(ChainBlock.AXIS) == Direction.Axis.Y)) {
                    return InteractionResult.sidedSuccess(world.isClientSide);
                }
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
            return InteractionResult.PASS;
        }
    }

    private static class MapMarkerBehavior extends ItemUseOnBlockOverride {

        @Override
        public boolean isEnabled() {
            return ServerConfigs.cached.MAP_MARKERS;
        }

        @Override
        public boolean appliesToItem(Item item) {
            return item instanceof MapItem || (CompatHandler.mapatlas && MapAtlasPlugin.isAtlas(item));
        }

        @Override
        public InteractionResult tryPerformingAction(Level level, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {
            BlockPos pos = hit.getBlockPos();
            if (MapHelper.toggleMarkersAtPos(level, pos, stack, player)) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }
    }

    private static class XpBottlingBehavior extends ItemUseOnBlockOverride {

        private static final JarBlockTile DUMMY_JAR_TILE = new JarBlockTile(BlockPos.ZERO, ModRegistry.JAR.get().defaultBlockState());

        @Override
        public boolean isEnabled() {
            return ServerConfigs.cached.BOTTLE_XP;
        }

        @Override
        public boolean appliesToItem(Item item) {
            return item == Items.GLASS_BOTTLE || item instanceof JarItem || item == Items.EXPERIENCE_BOTTLE;
        }

        @Override
        public InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {

            BlockPos pos = hit.getBlockPos();
            Item i = stack.getItem();
            if (world.getBlockState(pos).getBlock() instanceof EnchantmentTableBlock) {
                ItemStack returnStack = null;

                //prevent accidentally releasing bottles
                if (i == Items.EXPERIENCE_BOTTLE) {
                    return InteractionResult.FAIL;
                }

                if (player.experienceLevel > 0 || player.isCreative()) {
                    if (i == Items.GLASS_BOTTLE) {
                        returnStack = new ItemStack(Items.EXPERIENCE_BOTTLE);
                    } else if (i instanceof JarItem) {
                        DUMMY_JAR_TILE.resetHolders();
                        CompoundTag tag = stack.getTagElement("BlockEntityTag");
                        if (tag != null) {
                            DUMMY_JAR_TILE.load(tag);
                        }

                        if (DUMMY_JAR_TILE.canInteractWithFluidHolder()) {
                            ItemStack tempStack = new ItemStack(Items.EXPERIENCE_BOTTLE);
                            ItemStack temp = DUMMY_JAR_TILE.fluidHolder.interactWithItem(tempStack, null, null, false);
                            if (temp != null && temp.getItem() == Items.GLASS_BOTTLE) {
                                returnStack = ((JarBlock) ((BlockItem) i).getBlock()).getJarItem(DUMMY_JAR_TILE);
                            }
                        }
                    }

                    if (returnStack != null) {
                        player.hurt(CommonUtil.BOTTLING_DAMAGE, ServerConfigs.cached.BOTTLING_COST);
                        Utils.swapItem(player, hand, returnStack);

                        if (!player.isCreative())
                            player.giveExperiencePoints(-Utils.getXPinaBottle(1, world.random));

                        if (world.isClientSide) {
                            Minecraft.getInstance().particleEngine.createTrackingEmitter(player, ModRegistry.BOTTLING_XP_PARTICLE.get(), 1);
                        }
                        world.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.BLOCKS, 1, 1);

                        return InteractionResult.sidedSuccess(world.isClientSide);
                    }
                }
            }
            return InteractionResult.PASS;
        }
    }

    private static class EnhancedCakeBehavior extends ItemUseOnBlockOverride {

        @Nullable
        @Override
        public BaseComponent getTooltip() {
            return new TranslatableComponent("message.supplementaries.double_cake");
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public boolean appliesToItem(Item item) {
            return CommonUtil.isCake(item);
        }

        private InteractionResult placeDoubleCake(Player player, ItemStack stack, BlockPos pos, Level world, BlockState state, boolean isRanged) {
            boolean isDirectional = state.getBlock() == ModRegistry.DIRECTIONAL_CAKE.get();

            if ((isDirectional && state.getValue(DirectionalCakeBlock.BITES) == 0) || state == Blocks.CAKE.defaultBlockState()) {

                return replaceSimilarBlock(ModRegistry.DOUBLE_CAKE.get(), player, stack, pos, world, state,
                        null, DoubleCakeBlock.FACING);
            }
            return InteractionResult.PASS;
        }

        @Override
        public InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {
            if (player.getAbilities().mayBuild) {
                BlockPos pos = hit.getBlockPos();
                BlockState state = world.getBlockState(pos);
                Block b = state.getBlock();
                if (b == Blocks.CAKE || b == ModRegistry.DIRECTIONAL_CAKE.get()) {
                    InteractionResult result = InteractionResult.FAIL;

                    if (ServerConfigs.cached.DOUBLE_CAKE_PLACEMENT) {
                        result = placeDoubleCake(player, stack, pos, world, state, isRanged);
                    }
                    return result;
                }
            }
            return InteractionResult.PASS;
        }
    }

    private static class SoapClearBehavior extends ItemUseOnBlockOverride {

        boolean enabled = RegistryConfigs.Reg.SOAP_ENABLED.get();

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public boolean appliesToItem(Item item) {
            return item == ModRegistry.SOAP.get();
        }

        @Override
        public InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {
            if (player.getAbilities().mayBuild) {

                boolean newState = !stack.is(Items.INK_SAC);
                BlockPos pos = hit.getBlockPos();
                BlockEntity tile = world.getBlockEntity(pos);
                if (tile != null) {
                    var cap = tile.getCapability(CapabilityHandler.ANTIQUE_TEXT_CAP);
                    AtomicBoolean success = new AtomicBoolean(false);
                    cap.ifPresent(c -> {
                        if (c.hasAntiqueInk() != newState) {
                            c.setAntiqueInk(newState);
                            tile.setChanged();
                            if (world instanceof ServerLevel serverLevel) {
                                NetworkHandler.sendToAllInRangeClients(pos, serverLevel, 256,
                                        new ClientBoundSyncAntiqueInk(pos, newState));
                            }
                            success.set(true);
                        }
                    });
                    if (success.get()) {
                        if (newState) {
                            world.playSound(null, pos, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        } else {
                            world.playSound(null, pos, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                        if (!player.isCreative()) stack.shrink(1);
                        return InteractionResult.sidedSuccess(world.isClientSide);
                    }
                }
            }
            return InteractionResult.PASS;
        }
    }

    private static class AntiqueInkBehavior extends ItemUseOnBlockOverride {

        @Override
        public boolean isEnabled() {
            return CapabilityHandler.ANTIQUE_CAP_ENABLED;
        }

        @Override
        public boolean appliesToItem(Item item) {
            return item == Items.INK_SAC || item == ModRegistry.ANTIQUE_INK.get();
        }

        @Override
        public InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {
            if (player.getAbilities().mayBuild) {
                boolean newState = !stack.is(Items.INK_SAC);
                BlockPos pos = hit.getBlockPos();
                BlockEntity tile = world.getBlockEntity(pos);
                if (tile != null && (!(tile instanceof IOwnerProtected op) || op.isAccessibleBy(player))) {
                    var cap = tile.getCapability(CapabilityHandler.ANTIQUE_TEXT_CAP);
                    AtomicBoolean success = new AtomicBoolean(false);
                    cap.ifPresent(c -> {
                        if (c.hasAntiqueInk() != newState) {
                            c.setAntiqueInk(newState);
                            tile.setChanged();
                            if (world instanceof ServerLevel serverLevel) {
                                NetworkHandler.sendToAllInRangeClients(pos, serverLevel, 256,
                                        new ClientBoundSyncAntiqueInk(pos, newState));
                            }
                            success.set(true);
                        }
                    });
                    if (success.get()) {
                        if (newState) {
                            world.playSound(null, pos, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        } else {
                            world.playSound(null, pos, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                        if (!player.isCreative()) stack.shrink(1);
                        return InteractionResult.sidedSuccess(world.isClientSide);
                    }
                }
            }
            return InteractionResult.PASS;
        }
    }

    //needed to suppress block actions, so we can always rotate a block even if for example it would open an inventory normally
    private static class WrenchBehavior extends ItemUseOnBlockOverride {

        @Override
        public boolean isEnabled() {
            return RegistryConfigs.Reg.WRENCH_ENABLED.get();
        }

        @Override
        public boolean appliesToItem(Item item) {
            return item == ModRegistry.WRENCH.get();
        }

        @Override
        public InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {
            if (player.getAbilities().mayBuild) {
                var h = ServerConfigs.cached.WRENCH_BYPASS;
                if ((h == ServerConfigs.Hands.MAIN_HAND && hand == InteractionHand.MAIN_HAND) ||
                        (h == ServerConfigs.Hands.OFF_HAND && hand == InteractionHand.OFF_HAND) || h == ServerConfigs.Hands.BOTH) {

                    return stack.useOn(new UseOnContext(player, hand, hit));
                }
            }
            return InteractionResult.PASS;
        }
    }

    private static class SkullPileBehavior extends ItemUseOnBlockOverride {

        @Nullable
        @Override
        public BaseComponent getTooltip() {
            return new TranslatableComponent("message.supplementaries.double_cake");
        }

        @Override
        public boolean isEnabled() {
            return ServerConfigs.cached.SKULL_PILES;
        }

        @Override
        public boolean appliesToItem(Item item) {
            return item instanceof BlockItem bi && bi.getBlock() instanceof SkullBlock skull && skull.getType() != SkullBlock.Types.DRAGON;
        }

        @Override
        public InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {
            if (player.getAbilities().mayBuild) {
                BlockPos pos = hit.getBlockPos();

                if (world.getBlockEntity(pos) instanceof SkullBlockEntity oldTile) {
                    BlockState state = oldTile.getBlockState();
                    if ((state.getBlock() instanceof SkullBlock skullBlock && skullBlock.getType() != SkullBlock.Types.DRAGON)) {

                        ItemStack copy = stack.copy();

                        InteractionResult result = replaceSimilarBlock(ModRegistry.SKULL_PILE.get(), player, stack, pos, world,
                                state, null, SkullBlock.ROTATION);

                        if (result.consumesAction()) {
                            if (world.getBlockEntity(pos) instanceof DoubleSkullBlockTile tile) {
                                tile.initialize(oldTile, skullBlock, copy, player, hand);
                            }
                        }
                        return result;
                    }
                }

            }
            return InteractionResult.PASS;
        }
    }

    private static class SkullCandlesBehavior extends ItemUseOnBlockOverride {

        @Override
        public boolean isEnabled() {
            return ServerConfigs.cached.SKULL_CANDLES;
        }

        @Override
        public boolean appliesToItem(Item item) {
            return item.builtInRegistryHolder().is(ItemTags.CANDLES) && item.getRegistryName().getNamespace().equals("minecraft");
        }

        @Override
        public InteractionResult tryPerformingAction(Level world, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, boolean isRanged) {
            if (player.getAbilities().mayBuild) {
                BlockPos pos = hit.getBlockPos();

                BlockEntity te = world.getBlockEntity(pos);
                if (te instanceof SkullBlockEntity oldTile) {
                    BlockState state = oldTile.getBlockState();
                    if ((state.getBlock() instanceof SkullBlock skullBlock && skullBlock.getType() != SkullBlock.Types.DRAGON)) {

                        ItemStack copy = stack.copy();

                        InteractionResult result = replaceSimilarBlock(ModRegistry.SKULL_CANDLE.get(), player, stack, pos, world,
                                state, SoundType.CANDLE, SkullBlock.ROTATION);

                        if (result.consumesAction()) {
                            if (world.getBlockEntity(pos) instanceof CandleSkullBlockTile tile) {
                                tile.initialize(oldTile, skullBlock, copy, player, hand);
                            }
                        }
                        return result;
                    }
                }
            }
            return InteractionResult.PASS;
        }
    }


    public static InteractionResult replaceSimilarBlock(Block blockOverride, Player player, ItemStack stack,
                                                        BlockPos pos, Level world, BlockState replaced,
                                                        @Nullable SoundType sound, Property<?>... properties) {

        BlockState newState = blockOverride.defaultBlockState();
        for (Property<?> p : properties) {
            newState = BlockUtils.replaceProperty(replaced, newState, p);
        }
        if (newState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidstate = world.getFluidState(pos);
            newState = newState.setValue(BlockStateProperties.WATERLOGGED, fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8);
        }
        if (!world.setBlock(pos, newState, 3)) {
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, pos, stack);
        }
        world.gameEvent(player, GameEvent.BLOCK_PLACE, pos);

        if (sound == null) sound = newState.getSoundType(world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        // if (player instanceof ServerPlayer serverPlayer && !isRanged) {
        //     CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
        // }
        return InteractionResult.sidedSuccess(world.isClientSide);

    }

}
