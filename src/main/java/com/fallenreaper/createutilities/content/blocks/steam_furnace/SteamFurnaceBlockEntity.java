package com.fallenreaper.createutilities.content.blocks.steam_furnace;

import com.fallenreaper.createutilities.core.data.IDevInfo;
import com.fallenreaper.createutilities.core.data.blocks.InteractableBlockEntity;
import com.fallenreaper.createutilities.core.utils.InteractionHandler;
import com.fallenreaper.createutilities.index.CUFluids;
import com.jozufozu.flywheel.repack.joml.Math;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.components.steam.SteamEngineBlock;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.processing.EmptyingByBasin;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.tileEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.fallenreaper.createutilities.content.blocks.steam_furnace.FurnaceState.*;
import static com.fallenreaper.createutilities.content.blocks.steam_furnace.SteamFurnaceBlock.CREATIVE_LIT;
import static com.fallenreaper.createutilities.core.utils.MathUtil.*;
import static com.google.common.base.Strings.repeat;
import static com.simibubi.create.Create.RANDOM;
import static com.simibubi.create.foundation.fluid.FluidHelper.convertToStill;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
import static net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack;

//------WIP------//
public class SteamFurnaceBlockEntity extends InteractableBlockEntity implements IHaveGoggleInformation, ISteamProvider, IDevInfo {
    public static int MAX_SLOT_SIZE = 4;
    protected final int[] cookingProgress;
    protected final int[] cookingTime;
    private final Couple<SmartFluidTankBehaviour> tanks;
    public SteamFurnaceBoilerData boiler;
    public float indicatorProgress;
    public ItemStackHandler internalInventory;
    public int itemSearchCooldown;
    public List<ItemStackHandler> itemStackHandlers;
    public boolean hasSteamEngine;
    public boolean hasInputFluidIn;
    public FurnaceState furnaceState;
    public SteamFurnaceItemHandler inventory;
    public boolean hasFuel;
    public LazyOptional<IItemHandler> itemCapability;
    public LazyOptional<IFluidHandler> fluidCapability;
    public SmartFluidTankBehaviour inputTank;
    public SmartFluidTankBehaviour outputTank;
    public int litTime;
    public boolean isUnlimited;
    public float fluidConsumptionTick;
    public float producedSteam;
    public boolean hasOutputFluidIn;
    public boolean spawnCookingParticles;
    public RecipeType<?> recipeType;
    public float[] offsetRotation;


    public SteamFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.inventory = createItemHandler();
        this.internalInventory = createInternalItemHandler();
        this.itemCapability = LazyOptional.of(() -> new CombinedInvWrapper(inventory, internalInventory));
        this.furnaceState = FurnaceState.NONE;
        this.tanks = Couple.create(inputTank, outputTank);
        this.cookingProgress = new int[MAX_SLOT_SIZE];
        this.cookingTime = new int[MAX_SLOT_SIZE];
        this.offsetRotation = new float[MAX_SLOT_SIZE];
        this.itemStackHandlers = new ArrayList<>();
        itemStackHandlers.add(getInternalInventory());
        itemStackHandlers.add(getInventory());
        this.recipeType = RecipeType.CAMPFIRE_COOKING;
        this.boiler = new SteamFurnaceBoilerData(this);
    }

    public static boolean isFuel(ItemStack pStack) {
        return ForgeHooks.getBurnTime(pStack, null) > 0;
    }

    protected static float getIndicatorProgress(SteamFurnaceBlockEntity furnace) {
        return Mth.sin((furnace.indicatorProgress / 30) * Mth.PI / 2);
    }

    public static int getFuelBurnTime(ItemStack pFuel) {
        if (pFuel.isEmpty())
            return 0;
        if (!isFuel(pFuel))
            return 0;

        return ForgeHooks.getBurnTime(pFuel, null);
    }

    public static int getFreeSlot(IItemHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public static int getOccupiedSlot(IItemHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (!slotStack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        inputTank = createFluidHandler(SmartFluidTankBehaviour.INPUT);
        outputTank = createFluidHandler(SmartFluidTankBehaviour.OUTPUT);
        behaviours.add(inputTank);
        behaviours.add(outputTank);
        inputTank.getPrimaryHandler().setValidator(p -> FluidHelper.isWater(p.getFluid()));
        outputTank.getPrimaryHandler().setValidator(p -> convertToStill(p.getFluid()) == CUFluids.STEAM.get());

        fluidCapability = LazyOptional.of(() -> {
            LazyOptional<? extends IFluidHandler> inputCap = inputTank.getCapability();
            LazyOptional<? extends IFluidHandler> outputCap = outputTank.getCapability();
            return new CombinedTankWrapper(outputCap.orElse(null), inputCap.orElse(null));
        });
    }

    private SmartFluidTankBehaviour createFluidHandler(BehaviourType<SmartFluidTankBehaviour> type) {
        return new SmartFluidTankBehaviour(type, this, 1, getTankCapacity(), true);
    }

    private SteamFurnaceItemHandler createItemHandler() {
        return new SteamFurnaceItemHandler(this, 1);
    }

    private ItemStackHandler createInternalItemHandler() {

        return new InternalItemStackHandler(
                MAX_SLOT_SIZE, this);
    }

    public int getTankCapacity() {
        return 1024 / 2;
    }
//TODO: ADD LAST SAVED SLOT OPTION
    @Override
    public InteractionResult onInteract(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
        if (level == null)
            return InteractionResult.FAIL;

        ItemStack itemStack = pPlayer.getItemInHand(pHand).copy();
        ItemStack stackInSlot = getFuelStack().copy();
        if (level.getBlockEntity(pPos) instanceof SteamFurnaceBlockEntity) {

            //FLUIDS

            SoundEvent soundevent;
            BlockState fluidState = null;

            boolean present = itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY)
                    .isPresent();
            if (pPlayer.getItemInHand(pHand).is(Items.BUCKET) && !isFuel(itemStack)) {
                if (hasWater()) {
                    Fluid fluid = getInputTank().getFluid().getFluid();

                    FluidAttributes attributes = fluid.getAttributes();
                    soundevent = attributes.getFillSound();
                    if (soundevent == null)
                        soundevent =
                                FluidHelper.isTag(fluid, FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;

                    this.getInputTank().drain(Math.min(getTankCapacity(), getInputTank().getFluidAmount()), IFluidHandler.FluidAction.EXECUTE);
                    if (pLevel.isClientSide())
                        pLevel.playSound(pPlayer, pPos, soundevent, SoundSource.BLOCKS, 1.0f, 1f);

                }
                return InteractionResult.SUCCESS;
            }

            if (present && !isFuel(itemStack)) {

                if (getInputTank() != null) {
                    if (!pPlayer.getItemInHand(pHand).is(Items.BUCKET)) {
                        Pair<FluidStack, ItemStack> emptyItem = EmptyingByBasin.emptyItem(pLevel, itemStack, true);
                        Fluid fluid = getInputTank().getFluid().getFluid();
                        fluidState = fluid.defaultFluidState()
                                .createLegacyBlock();
                        FluidAttributes attributes = fluid.getAttributes();
                        soundevent = attributes.getEmptySound();
                        if (soundevent == null)
                            soundevent =
                                    FluidHelper.isTag(fluid, FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
                        FluidStack fluidFromItem = emptyItem.getFirst();

                        this.getInputTank().fill(fluidFromItem, IFluidHandler.FluidAction.EXECUTE);


                        if (pLevel.isClientSide())
                            pLevel.playSound(pPlayer, pPos, soundevent, SoundSource.BLOCKS, 1.0f, 1f);

                    }
                }
                return InteractionResult.SUCCESS;
            }

            //FUEL ITEMS
            if (pHitResult.getDirection() != Direction.UP) {
                if (!hasFuel() && itemStack.isEmpty())
                    return InteractionResult.FAIL;
                if (!itemStack.isEmpty() && !isFuel(itemStack))
                    return InteractionResult.FAIL;

                if (!hasFuel()) {
                    if (!itemStack.isEmpty() && isFuel(itemStack)) {
                        //  if (!pPlayer.isCreative()) {
                        pPlayer.setItemInHand(pHand, ItemStack.EMPTY);
                        setItemStack(itemStack);
                        if (getBurnTime() > 0)
                            setBurnTime(getFuelBurnTime(getFuelStack()));
                    }
                } else if (!itemStack.isEmpty()) {
                    if (canItemStacksStack(itemStack, stackInSlot)) {
                        int desiredAmount = Math.max(stackInSlot.getMaxStackSize() - stackInSlot.getCount(), 0);
                        stackInSlot.grow(Math.min(desiredAmount, itemStack.getCount()));
                        setItemStack(stackInSlot);
                        itemStack.shrink(desiredAmount);
                        pPlayer.setItemInHand(pHand, itemStack);
                    }
                } else {
                    setItemStack(ItemStack.EMPTY);
                    pPlayer.setItemInHand(pHand, stackInSlot);
                    if (isBurning())
                        setBurnTime(getFuelBurnTime(getFuelStack()));
                    if (pLevel.isClientSide())
                        pLevel.playSound(pPlayer, pPos, SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.BLOCKS, 1.25F - pLevel.getRandom().nextFloat() * 0.28f, 1.0F - pLevel.getRandom().nextFloat() * 0.23f);
                }
                notifyUpdate();
                return InteractionResult.SUCCESS;
            } else {
                if (level.getBlockState(getBlockPos().relative(Direction.UP)).isAir()) {
                    //TODO: MAKE THIS ONLY REMOVE THE AMOUNT DEPENDING ON THE AMOUNT OF FREE SLOTS
                    if (hasFreeSlot() && hasRecipe(itemStack) && !itemStack.isEmpty()) {
                        if (addItem(itemStack, level.getRandom().nextFloat() * 360f)) {
                            itemStack.shrink(1);
                            pPlayer.setItemInHand(pHand, itemStack);

                            if (pLevel.isClientSide())
                                pLevel.playSound(pPlayer, pPos, SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.BLOCKS, 1.25F - pLevel.getRandom().nextFloat() * 0.28f, 1.0F - pLevel.getRandom().nextFloat() * 0.23f);
                            return InteractionResult.SUCCESS;
                        }
                    } else if (itemStack.isEmpty() && hasOccupiedSlot()) {
                        ItemStack stackInSlotInternal = getInternalInventory().getStackInSlot(getOccupiedSlot(getInternalInventory()));
                        pPlayer.setItemInHand(pHand, stackInSlotInternal.copy());
                        stackInSlotInternal.shrink(1);

                        if (pLevel.isClientSide())
                            pLevel.playSound(pPlayer, pPos, SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.BLOCKS, 1.25F - pLevel.getRandom().nextFloat() * 0.28f, 1.0F - pLevel.getRandom().nextFloat() * 0.23f);
                        return InteractionResult.SUCCESS;
                    }
                /*
                else if (!itemStack.isEmpty() && hasOccupiedSlot()) {
                    ItemStack stackInSlotInternal = getInternalInventory().getStackInSlot(getOccupiedSlot(getInternalInventory()));
                    if (canItemStacksStack(itemStack, stackInSlotInternal)) {
                       itemStack.grow(1);
                        stackInSlotInternal.shrink(1);
                        pPlayer.setItemInHand(pHand, itemStack);
                    }
                }

                 */
                }
            }
        }
        return super.onInteract(pState, pLevel, pPos, pPlayer, pHand, pHitResult);
    }

    public boolean isUnlimitedFuel() {
        return isUnlimited;
    }

    public void initFields() {

        this.hasInputFluidIn = !getInputTank().getFluid().isEmpty();
        this.hasOutputFluidIn = !getOutputTank().getFluid().isEmpty() || producedSteam > 0;
        this.hasFuel = !getFuelStack().isEmpty() && !getFuelStack().is(Items.AIR);
        this.isUnlimited = getFuelBurnTime(getFuelStack()) >= 220000;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getLevel() == null)
            return;


        boolean isClientSide = this.getLevel().isClientSide;
        initFields();

        if (litTime > 0 && !isUnlimitedFuel())
            litTime--;

        if (litTime <= 0 && hasFuel()) {
            setBurnTime(getFuelBurnTime(getFuelStack()));
            if (!isUnlimitedFuel())
                getFuelStack().shrink(1);

        }
        tickItemSearch();
        syncBlockState();
        syncFurnaceState();
        if (producedSteam >= getTankCapacity()) {
            producedSteam = getTankCapacity();
        }
        if (producedSteam < 0)
            producedSteam = Math.clamp(producedSteam, 0, getTankCapacity());

        if(getState().isActive() || getState().isProducing()) {
            cookTick();
        }
        else {
            coolDown();
            spawnCookingParticles = false;
        }

        if (getState().isProducing()) {

            updateBoilerTemperature();
            fluidConsumptionTick++;

            if (fluidConsumptionTick % 4 == 0) {
                fluidConsumptionTick -= Math.max(0, fluidConsumptionTick);
                tryDrain();
            }

        }
        if (producedSteam > 0 && !isBurning() || !hasWater() && hasSteamEngineAttached()) {
                float level = (float) boiler.attachedEngines / 6.0f;
            getOutputTank().drain(1, IFluidHandler.FluidAction.EXECUTE);
            producedSteam = Math.clamp(producedSteam - 1, 0, getTankCapacity());
        }


        if (hasFuel() || isBurning()) {
            if (isClientSide) {
                spawnParticles();
            }
        }
        if (boiler != null)
            boiler.tick();

    }

    public void coolDown() {
        for (int i = 0; i < getInternalInventory().getSlots(); ++i) {
            if (getInternalInventory().getStackInSlot(i).isEmpty())
                return;

            if (cookingProgress[i] > 0) {
                cookingProgress[i] = Mth.clamp(cookingProgress[i] - 1, 0, cookingTime[i]);
            }
        }
    }
//rename to produceSteam
    public void tryDrain() {
        boolean needsWater = false;
        if (isUnlimitedFuel())
            return;



        producedSteam++;
        if (producedSteam >= getTankCapacity()) {
            producedSteam = getTankCapacity();
        }
        FluidStack drained = getInputTank().drain(getInputTank().getFluid(), IFluidHandler.FluidAction.SIMULATE);
        getInputTank().drain(1, IFluidHandler.FluidAction.EXECUTE);
        FluidStack steam = new FluidStack(CUFluids.STEAM.get(), getTankCapacity());
        getOutputTank().fill(steam, IFluidHandler.FluidAction.SIMULATE);
    }

    public void syncFurnaceState() {
        if (getLevel() != null) {
            if (isBurning() && hasWater() || isUnlimitedFuel() && !getState().isRunning()) {
                updateState(PRODUCING);
            } else if (isBurning()) {
                updateState(ACTIVE);
            } else if (hasWater() && !isBurning()) {
                updateState(LOADED);
            } else if (getBoiler().isActive() && hasSteam()) {
                updateState(RUNNING);
            } else {
                updateState(FurnaceState.NONE);
            }
        }
    }

    public void spawnParticles() {
        if (getLevel() == null)
            return;
        if (!getLevel().isClientSide())
            return;
        SimpleParticleType particleType;
        Vec3 pos = VecHelper.getCenterOf(this.worldPosition);
        Direction direction = getBlockState()
                .getValue(BlockStateProperties.HORIZONTAL_FACING);
        Vec3i N = direction.getOpposite().getNormal();
        Vec3 N2 = new Vec3(N.getX(), N.getY(), N.getZ());
        pos = pos.add(-N.getX() * 0.53, -0.1, -N.getZ() * 0.53);

        if (getState().isProducing()) {
            if (fluidConsumptionTick % 10 == 0 && !(getOutputTank().getFluidAmount() >= getTankCapacity())) {
                if (getLevel().getRandom().nextFloat() * 100 < 100) {
                    float volume = 8f / 12 - level.random.nextFloat() * .5f;
                    float pitch = 1.18f - level.random.nextFloat() * .25f;
                    level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                            SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, volume, pitch, true);
                    AllSoundEvents.STEAM.playAt(level, worldPosition, volume / 8, .8f, true);
                }
            }

            particleType = isUnlimitedFuel() ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.LAVA;
            if (getLevel().getRandom().nextFloat() < 0.5F / 16F) {
                Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, RANDOM, 0.1f);
                random = random.subtract(N2.scale(random.dot(N2)));
                pos = pos.add(random);
                getLevel().addParticle(particleType, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }

        double d0 = (double) getBlockPos().getX() + 0.5D;
        double d1 = getBlockPos().getY();
        double d2 = (double) getBlockPos().getZ() + 0.5D;
        Direction.Axis direction$axis = direction.getAxis();
        double d4 = getLevel().getRandom().nextDouble() * 0.6D - 0.3D;
        double d5 = direction$axis == Direction.Axis.X ? (double) direction.getStepX() * 0.52D : d4;
        double d6 = getLevel().getRandom().nextDouble() * 10.0D / 16.0D;
        double d7 = direction$axis == Direction.Axis.Z ? (double) direction.getStepZ() * 0.52D : d4;
        if (Objects.requireNonNull(getLevel()).getRandom().nextDouble() < 0.5D) {
            getLevel().addParticle(ParticleTypes.SMOKE, d0 + d5, d1 + d6, d2 + d7, 0.0D, 0.0D, 0.0D);
            getLevel().playLocalSound(d0, d1, d2, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, .75F + getLevel().getRandom().nextFloat() * .5F, 1.0F - getLevel().getRandom().nextFloat() * .5F, false);
        }


        if (getLevel().getRandom().nextFloat() * 100F < 25F) {
            getLevel().addParticle(ParticleTypes.SMOKE, (d0 + d5) / d5 * d0, (d1 + d6) / d1, d2 + d7, 0.0D, 0.0D, 0.0D);
        }

        for (int i = 0; i < getInternalInventory().getSlots(); i++) {
            if (spawnCookingParticles) {

                getLevel().playSound(null, worldPosition, SoundEvents.AZALEA_STEP, SoundSource.BLOCKS,
                        1.5f - getLevel().random.nextFloat() * .425f, 1.0f - level.random.nextFloat() * .25f);
            }
        }
    }

    public ItemStack getFuelStack() {

        return getInventory().getStackInSlot(0);
    }

    public void updateState(FurnaceState furnaceState) {
        this.furnaceState = furnaceState;
    }

    public boolean isBurning() {
        return getBurnTime() > 0;
    }

    // @Credit: Create Aeronautics; Eriksoon
    public void initParticles(boolean extinguish, boolean creative) {
        if (level == null)
            return;

        Vec3 pos = VecHelper.getCenterOf(this.worldPosition);

        Direction direction = getBlockState()
                .getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        Vec3i directionNormal = direction.getNormal();
        Vec3 toVec = new Vec3(directionNormal.getX(), directionNormal.getY(), directionNormal.getZ());
        pos = pos.add(-directionNormal.getX() * 0.5F, -0.1, -directionNormal.getZ() * 0.5F);
        Random levelRandom = getLevel().getRandom();
        Vec3 speed = VecHelper.offsetRandomly(Vec3.ZERO, levelRandom, 0.1f).add(toVec.scale(-0.03));
        SoundEvent soundEvent;
        ParticleOptions particle = creative ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        soundEvent = extinguish ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.FIRECHARGE_USE;

        level.playSound(null, worldPosition, soundEvent, SoundSource.BLOCKS,
                2f + level.random.nextFloat() * .325f, .75f - level.random.nextFloat() * .25f);
        for (int i = 0; i < 4; i++) {
            Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, levelRandom, 0.1f);
            random = random.subtract(toVec.scale(random.dot(toVec)));
            pos = pos.add(random);
            level.addParticle(particle, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);

        }


    }

    public boolean canUpdateState() {
        return !this.getBlockState().getValue(LIT) && isBurning();
    }

    public void syncBlockState() {

        if (getLevel() != null) {
            boolean isBlockStateLit = this.getBlockState().getValue(LIT);
            boolean isBlockStateCreativeLit = this.getBlockState().getValue(CREATIVE_LIT);
            if (!this.getBlockState().getValue(LIT) && isBurning() && !isUnlimitedFuel()) {
                getLevel().setBlock(getBlockPos(), this.getBlockState().setValue(LIT, true), 3);
                initParticles(false, false);
            } else if (!isBurning() && isBlockStateLit) {
                getLevel().setBlock(getBlockPos(), this.getBlockState().setValue(LIT, false), 3);
                initParticles(true, false);
            } else if (isBurning() && !isBlockStateCreativeLit && isUnlimitedFuel() && !isBlockStateLit) {
                getLevel().setBlock(getBlockPos(), this.getBlockState().setValue(CREATIVE_LIT, true), 3);
                initParticles(false, true);
            } else if (!isBurning() && isBlockStateCreativeLit && !isUnlimitedFuel()) {
                getLevel().setBlock(getBlockPos(), this.getBlockState().setValue(CREATIVE_LIT, false), 3);
                initParticles(true, true);
            }
            sendData();
        }
    }


    public int getBurnTime() {
        return this.litTime;
    }

    public void setBurnTime(int litTime) {
        this.litTime = litTime;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ItemStack fuelIn = getFuelStack();
        Component indent = new TextComponent(" ");
        Component indent1 = new TextComponent(spacing + " ");
        Component arrow = new TextComponent("->").withStyle(ChatFormatting.DARK_GRAY);
        Component time = getBurnTime() <= 0 ? new TextComponent("") : new TextComponent(formatTime(getCalculatedBurnTime(fuelIn))).withStyle(ChatFormatting.GOLD);
        String item = !hasFuel() ? "Empty" : fuelIn.getItem().getName(fuelIn).getString();
        Component in = new TextComponent(item + " " + (fuelIn.isEmpty() ? "" : "x") + (fuelIn.getCount() <= 0 ? "" : fuelIn.getCount())).withStyle(fuelIn.isEmpty() ? ChatFormatting.RED : ChatFormatting.GREEN);
        LangBuilder mb = Lang.translate("generic.unit.millibuckets");
        FluidStack fluidStackIn = getInputTank().getFluid();
        MutableComponent firstLine = arrow.plainCopy().append(indent);
        Component fluidName = new TranslatableComponent(fluidStackIn.getTranslationKey()).withStyle(ChatFormatting.AQUA);
        Component contained = new TextComponent(String.valueOf(fluidStackIn.getAmount())).plainCopy().append(mb.string()).withStyle(ChatFormatting.GOLD);
        Component containedSteam = new TextComponent(String.valueOf((int) getProducedSteam())).plainCopy().append(mb.string()).withStyle(ChatFormatting.GOLD);
        Component slash = new TextComponent(" / ").withStyle(ChatFormatting.GRAY);
        Component capacity = new TextComponent(String.valueOf(getTankCapacity())).plainCopy().append(mb.string()).withStyle(ChatFormatting.DARK_GRAY);
        Component unlimited;
        unlimited = new TextComponent("Unlimited").withStyle(ChatFormatting.LIGHT_PURPLE);
        if (!hasSteamEngineAttached() && !boiler.isActive()) {
            tooltip.add(indent1.plainCopy()
                    .append("Steam Furnace:"));
            if (getBurnTime() > 0)
                tooltip.add(firstLine.plainCopy().append(" Burn Time:").withStyle(ChatFormatting.GRAY).append(indent).append(isUnlimitedFuel() ? unlimited : time));

            tooltip.add(firstLine.plainCopy().append(" Fuel:").withStyle(ChatFormatting.GRAY).append(indent).append(in));
//todo: find a way to check if it has atleast one item on the furnace int tank and use degrading progress like the burn time but instead for cooking time
        /*
        if (hasOccupiedSlot()) {
            int highestCookProgress = sortHighest(cookingProgress);
            int indexOf = getIndexOf(cookingProgress, highestCookProgress);
            Component cookTime = getCookProgressBar(12, getProgress(indexOf), cookingTime[indexOf]);
            tooltip.add(firstLine.plainCopy().append(" Cooking Bar:").withStyle(ChatFormatting.GRAY).append(indent).append(cookTime));
        }
         */

            int slot = getOccupiedSlot(getInternalInventory());
            int prevSlot = 0;

            if(getState().isProducing() || getState().isActive()) {
                for (int i = 0; i < getInternalInventory().getSlots(); i++) {
                    ItemStack stackInSlot = getInternalInventory().getStackInSlot(i);
                    if(!stackInSlot.isEmpty()) {
                         prevSlot = i;
                         break;
                    }
                }

             boolean change = false;
                if(!(slot == prevSlot)) {
                    prevSlot = slot;
                    change = true;
                }
                int highestCookProgress = sortLowest(cookingProgress);
                int indexOf = getIndexOf(cookingProgress, highestCookProgress);

                    if(slot != indexOf && change)
                        slot = indexOf;

                        float progress = Mth.lerp(((float) cookingProgress[slot] / (float) cookingTime[slot]), 0, 1);
                        if (progress >= 1)
                            progress = 0;
                        Component cookTime = getCookProgressBar(16, progress, cookingTime[slot]);

                        if(!change)
                        tooltip.add(firstLine.plainCopy().append(" Cooking:").withStyle(ChatFormatting.GRAY).append(indent).append(cookTime));


                //  System.out.println(progress);
            }
            tooltip.add(indent.plainCopy());
            Lang.translate("gui.goggles.fluid_container")
                    .forGoggles(tooltip);
            if (hasWater()) {
            /*
            tooltip.add(arrow.plainCopy()
                    .append(indent)
                    .append("Fluid: ").withStyle(ChatFormatting.GRAY)
                    .append(fluidName)
            );
         */
                FluidStack fluid = getInputTank().getFluid();
                Lang.fluidName(fluid)
                        .style(ChatFormatting.GRAY)
                        .forGoggles(tooltip, 1);

                Lang.builder()
                        .add(Lang.number(fluid.getAmount())
                                .add(mb)
                                .style(ChatFormatting.GOLD))
                        .text(ChatFormatting.GRAY, " / ")
                        .add(Lang.number(getTankCapacity())
                                .add(mb)
                                .style(ChatFormatting.DARK_GRAY))
                        .forGoggles(tooltip, 1);
            /*
            tooltip.add(firstLine
                    .append("Water Amount: ").withStyle(ChatFormatting.GRAY)
                    .append(contained)
                    .append(slash)
                    .append(capacity));
             */
            } else {
                Lang.translate("gui.goggles.fluid_container.capacity")
                        .add(Lang.number(getTankCapacity())
                                .add(mb)
                                .style(ChatFormatting.GOLD))
                        .style(ChatFormatting.GRAY)
                        .forGoggles(tooltip, 1);
            }
            if (hasSteam()) {
                FluidStack fluid = getOutputTank().getFluid();
                Lang.fluidName(new FluidStack(CUFluids.STEAM.get(), 2))
                        .style(ChatFormatting.GRAY)
                        .forGoggles(tooltip, 1);

                Lang.builder()
                        .add(Lang.number(Math.round(producedSteam))
                                .add(mb)
                                .style(ChatFormatting.GOLD))
                        .text(ChatFormatting.GRAY, " / ")
                        .add(Lang.number(getTankCapacity())
                                .add(mb)
                                .style(ChatFormatting.DARK_GRAY))
                        .forGoggles(tooltip, 1);
            }

            return true;
        } else {
            if(boiler.isActive()) {
                return boiler.addToGoggleTooltip(tooltip, isPlayerSneaking);
            }

        }
        return false;
    }

    public boolean hasFuel() {
        if (isUnlimited)
            return true;

        return hasFuel;
    }



    public boolean hasWater() {
        return hasInputFluidIn;
    }

    public boolean hasSteam() {
        return hasOutputFluidIn;
    }

    public void setItemStack(ItemStack itemStack) {
        this.inventory.setStackInSlot(0, itemStack);
    }

    public SteamFurnaceItemHandler getInventory() {
        return this.inventory;
    }

    public ItemStackHandler getInternalInventory() {
        return this.internalInventory;
    }

    @Override
    public float getProducedSteam() {
        return producedSteam;
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        // compound.put("BoilerData", boiler.write());
        compound.putBoolean("HasFuel", !getInventory().getStackInSlot(0).isEmpty());
        compound.put("FurnaceInventory", getInventory().serializeNBT());
        compound.put("Inventory", getInternalInventory().serializeNBT());
        compound.putInt("BurnTime", getBurnTime());
        compound.putBoolean("HasWater", hasWater());
        compound.putBoolean("HasSteam", hasSteam());
        compound.putBoolean("IsCreative", isUnlimitedFuel());
        compound.putFloat("SteamAmount", producedSteam);
        compound.putIntArray("CookingProgress", this.cookingProgress);
        compound.putIntArray("CookingTotalTimes", this.cookingTime);
        compound.put("Boiler", boiler.write());
        NBTHelper.writeEnum(compound, "State", getState());
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        //  boiler.read(compound.getCompound("BoilerData"), 2);
        this.hasFuel = compound.getBoolean("HasFuel");
        this.inventory.deserializeNBT(compound.getCompound("FurnaceInventory"));
        this.internalInventory.deserializeNBT(compound.getCompound("Inventory"));
        this.litTime = compound.getInt("BurnTime");
        this.hasInputFluidIn = compound.getBoolean("HasWater");
        this.hasOutputFluidIn = compound.getBoolean("HasSteam");
        this.furnaceState = NBTHelper.readEnum(compound, "State", FurnaceState.class);
        this.isUnlimited = compound.getBoolean("IsCreative");
        this.producedSteam = compound.getFloat("SteamAmount");
        boiler.read(compound.getCompound("Boiler"), 1);
        if (compound.contains("CookingProgress", 11)) {
            int[] aint = compound.getIntArray("CookingProgress");
            System.arraycopy(aint, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, aint.length));
        }

        if (compound.contains("CookingTotalTimes", 11)) {
            int[] aint1 = compound.getIntArray("CookingTotalTimes");
            System.arraycopy(aint1, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, aint1.length));
        }
    }

    @Override
    public void addInteractionHandler(List<InteractionHandler> interactions) {
        //   SteamFurnaceInteraction furnaceInteraction = new SteamFurnaceInteraction(this, getTank());
        // interactions.add(furnaceInteraction);

    }

    public SmartFluidTank getInputTank() {
        return this.inputTank.getPrimaryHandler();
    }

    public SmartFluidTank getOutputTank() {
        return this.outputTank.getPrimaryHandler();
    }

    @Override
    public String getProvidedInfo() {
        String timer = hasFuel() ? formatTime(getCalculatedBurnTime(getInventory().getStackInSlot(0))) : "None";
        return "HasFuel: " + hasFuel() + "; BurnTime: " + getBurnTime() + "; Timer: " + timer + "; State: " + formatEnumName(getState());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        itemCapability.invalidate();
        fluidCapability.invalidate();
        boiler.clear();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {

        if (isItemHandlerCap(cap))
            return itemCapability.cast();
        if (isFluidHandlerCap(cap))
            return fluidCapability.cast();
        return super.getCapability(cap);
    }

    @Override
    protected boolean isFluidHandlerCap(Capability<?> cap) {
        return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;

    }

    private IFluidHandler handlerForCapability() {
        return boiler.isActive() ? boiler.createHandler() : getInputTank();
    }

    private void refreshCapability() {
        LazyOptional<IFluidHandler> oldCap = this.fluidCapability;
        this.fluidCapability = LazyOptional.of(this::handlerForCapability);
        oldCap.invalidate();
    }

    @Override
    protected boolean isItemHandlerCap(Capability<?> cap) {
        return super.isItemHandlerCap(cap);
    }

    protected int getCalculatedBurnTime(ItemStack itemStack) {


        int seconds = getBurnTime();

        //Convert To Seconds
        int modifier = getFuelBurnTime(itemStack);

        return seconds + (modifier * itemStack.getCount());
    }

    public int getCalculatedCookTime(ItemStack itemStack, int slot) {
        if (hasFreeSlot())
            return 0;

        int seconds = getCookingProgress(slot);

        //Convert To Seconds
        int modifier = getCookingTime(slot);

        return seconds + (modifier * itemStack.getCount());
    }

    public FurnaceState getState() {
        return this.furnaceState;
    }

    @Override
    public int getOutputSignal() {
        float fluidAmountRatio = (float) getInputTank().getFluidAmount() / getTankCapacity();
        float itemAmountRatio = (float) getFuelStack().getCount() / getFuelStack().getMaxStackSize();
        float signal = itemAmountRatio;
        if (hasWater()) {
            signal = signal + fluidAmountRatio;
            signal = signal / 2;
        }

        return Math.round((signal) * 15);

    }

    //TODO: ADD SMOKIG PARTICLES TO THE FOODS WHILE COOKING
    public void finishCooking(ItemStack itemStack, int slot) {
        if (getLevel() == null)
            return;
        boolean finish = false;

        if (!itemStack.isEmpty()) {
            ItemEntity itementity = new ItemEntity(getLevel(), worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, itemStack.split(RANDOM.nextInt(25) + 10));
            float f = 0.05F;
            itementity.setDeltaMovement(RANDOM.nextGaussian() * (double) f, RANDOM.nextGaussian() * (double) 0.05F + (double) 0.2F, RANDOM.nextGaussian() * (double) 0.05F);
            getLevel().addFreshEntity(itementity);
            getInternalInventory().setStackInSlot(slot, ItemStack.EMPTY);
            finish = true;
        }
        if (finish) {
            getLevel().playSound(null, worldPosition, SoundEvents.AZALEA_STEP, SoundSource.BLOCKS,
                    .5f + level.random.nextFloat() * .325f, 1.0f - level.random.nextFloat() * .25f);
        }
        setChanged();
    }

    public void handleInsertion() {

        if (level == null || level.isClientSide)
            return;

        boolean discard = false;

        AABB box = new AABB(worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), worldPosition.getX() + 1, (worldPosition.getY()) + 5 / 16D, worldPosition.getZ() + 1).deflate(0.961f);

        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!itemEntity.isAlive())
                continue;
            ItemStack itemStack = itemEntity.getItem();

            if (hasRecipe(itemStack) || canAcceptItem(getFreeSlot(getInternalInventory())) || hasFreeSlot()) {
                if (addItem(itemStack.copy(), level.getRandom().nextFloat() * 360f)) {
                    itemEntity.discard();
                    break;
                }
            }
        }
    }

    public boolean hasRecipe(ItemStack itemStack) {
        if (getLevel() == null)
            return false;
        Container inventoryWrapper = new SimpleContainer(itemStack);
        return getLevel().getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, inventoryWrapper, getLevel()).isPresent();
    }

    public void tickItemSearch() {
        if (!hasFreeSlot())
            return;


        if (itemSearchCooldown-- <= 0) {
            itemSearchCooldown = 5;
            handleInsertion();
        }

    }

    public boolean hasFreeSlot() {
        return getFreeSlot(getInternalInventory()) != -1;
    }

    public boolean canAcceptItem(int slot) {
        return getInternalInventory().getStackInSlot(slot).isEmpty();
    }

    public boolean addItem(ItemStack itemStack, float rotation) {


        if (hasRecipe(itemStack) && getCookingRecipe(itemStack).isPresent()) {

            for (int i = 0; i < getInternalInventory().getSlots(); i++) {
                ItemStack stack = getInternalInventory().getStackInSlot(i);
                int cookingTime = getCookingRecipe(itemStack).get().getCookingTime();
                if (canAcceptItem(i)) {
                    this.cookingProgress[i] = 0;
                    this.cookingTime[i] = cookingTime;
                    this.offsetRotation[i] = rotation;
                    getInternalInventory().setStackInSlot(i, itemStack.split(1));
                    return true;
                }
            }
        }
        notifyUpdate();

        return false;
    }

    public ItemStack getEmptySlot() {
        ItemStack itemStack = null;
        for (int i = 0; i < getInternalInventory().getSlots(); ++i) {
            ItemStack found = getInternalInventory().getStackInSlot(i);
            if (!found.isEmpty())
                itemStack = found;

        }
        return itemStack;
    }

    public void cookTick() {
        boolean shouldUpdate = false;
        if (getLevel() == null) return;

        for (int i = 0; i < getInternalInventory().getSlots(); ++i) {
            ItemStack itemstack = getInternalInventory().getStackInSlot(i);
            if (!itemstack.isEmpty()) {
                shouldUpdate = true;
                spawnCookingParticles = true;
                cookingProgress[i]++;
                if (hasRecipe(itemstack)) {
                    if (cookingProgress[i] >= cookingTime[i]) {
                        finishCooking(getRecipeResult(itemstack), i);
                    }
                }
            }
        }
        if (shouldUpdate) setChanged();
    }

    public Component getCookProgressBar(int maxWidth, float lerpValue, int maxSize) {

        var base = "";
        float modifier = lerpValue * maxSize;
        float fullValue = (modifier / maxSize) * maxWidth;
        float remaining = ((maxSize * (1 - lerpValue)) / maxSize) * maxWidth;

        base += ChatFormatting.GRAY + repeat("|", Math.round(fullValue));

        if (lerpValue < 1)
            base += ChatFormatting.DARK_GRAY + repeat("|", Math.round(remaining));

        return new TextComponent(base);
    }

    public float getProgress(int index) {
        return Math.min(1, (float) (cookingProgress[index] / cookingTime[index]));
    }

    public ItemStack getRecipeResult(ItemStack itemStack) {
        Container container = new SimpleContainer(itemStack);
        return getCookingRecipe(itemStack).map((p_155305_) -> p_155305_.assemble(container)).orElse(itemStack);
    }

    //TODO; RECODE THIS URGENTLY
    public Vec2 getItemOffset(int slot) {
        final float X_OFFSET = 4F / 16F;
        final float Y_OFFSET = 4F / 16F;
        Vec2[] offset = {new Vec2(-X_OFFSET, Y_OFFSET),
                new Vec2(X_OFFSET, -Y_OFFSET),
                new Vec2(X_OFFSET, Y_OFFSET),
                new Vec2(-X_OFFSET, -Y_OFFSET)};
        return offset[slot];
    }

    public float getRotation(int slot) {

        return offsetRotation[slot];
    }

    public boolean hasOccupiedSlot() {
        return getOccupiedSlot(getInternalInventory()) != -1;
    }

    public Optional<CampfireCookingRecipe> getCookingRecipe(ItemStack itemStack) {
        assert this.level != null;
        return this.level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SimpleContainer(itemStack), this.level);
    }

    public int getCookingTime(int slot) {
        return cookingTime[slot];
    }

    public int getCookingProgress(int slot) {
        return cookingProgress[slot];
    }

    public RecipeType<?> getRecipeType() {
        return recipeType;
    }

    public SteamFurnaceBoilerData getBoiler() {
        return boiler;
    }

    @Override
    public @Nullable SmartTileEntity getBlockEntity() {
        return null;
    }

    public void updateState() {
        boolean wasBoiler = boiler.isActive();
        boolean changed = boiler.evaluate(this);

        if (wasBoiler != boiler.isActive()) {
        // refreshCapability();
        }

        if (changed) {
            notifyUpdate();
        }
    }

    public boolean hasSteamEngineAttached() {
        for (Direction direction : Iterate.directions) {
            BlockPos pos = getBlockPos().relative(direction);
            BlockState attachedState = level.getBlockState(pos);
            if (AllBlocks.STEAM_ENGINE.has(attachedState) && SteamEngineBlock.getFacing(attachedState) == direction)
                return true;
        }
        return false;
    }

    public void updateBoilerTemperature() {

        if (!boiler.isActive())
            return;
        boiler.needsHeatLevelUpdate = true;
    }


    public static class InternalItemStackHandler extends ItemStackHandler {
        SteamFurnaceBlockEntity te;

        public InternalItemStackHandler(int size, SteamFurnaceBlockEntity te) {
            super(size);
            this.te = te;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            Container inventoryWrapper = new SimpleContainer(stack);
            assert te.getLevel() != null;
            return te.hasRecipe(stack);
        }

        @Override
        protected void onLoad() {
            super.onLoad();
        }

        @Override
        protected void onContentsChanged(int slot) {
            te.notifyUpdate();
        }
    }
}


