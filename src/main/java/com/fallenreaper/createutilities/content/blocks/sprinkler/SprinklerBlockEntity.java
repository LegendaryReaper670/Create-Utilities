package com.fallenreaper.createutilities.content.blocks.sprinkler;

import com.fallenreaper.createutilities.CreateUtilities;
import com.fallenreaper.createutilities.index.CUConfig;
import com.fallenreaper.createutilities.core.data.IDevInfo;
import com.fallenreaper.createutilities.core.data.blocks.liquidtank.FluidNode;
import com.jozufozu.flywheel.repack.joml.Math;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.fluids.particle.FluidParticleData;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.tileEntity.ComparatorUtil;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.fallenreaper.createutilities.content.blocks.sprinkler.HorizontalAxisBlock.CEILING;
import static com.fallenreaper.createutilities.content.blocks.sprinkler.SprinklerInteractionHandler.*;
import static com.fallenreaper.createutilities.core.utils.MathUtil.isInsideCircle;
import static com.simibubi.create.content.contraptions.base.HorizontalKineticBlock.HORIZONTAL_FACING;

//todo, redo this some day
//------WIP------//
public class SprinklerBlockEntity extends KineticTileEntity implements IHaveGoggleInformation, IDevInfo {
    public State currentState;
    protected SmartFluidTankBehaviour tankBehaviour;
    protected SprinklerInteractionHandler handler;
    private int radius;
    private boolean canSpawnParticles;
    private boolean hasFluidIn;
    private boolean canHydrate;
    private float generatedSpeed;

    public SprinklerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);

        this.currentState = State.NONE;
        this.handler = new SprinklerInteractionHandler(this);
        setLazyTickRate(20);
    }

    private static int secondsInTicks(int seconds) {
        return seconds * 20;
    }


    public Integer getRadius() {
        this.radius = (int) Math.min(Math.abs((getSpeed() / 128F) * 8), 12);
        return radius;
    }


    public void reset() {
        radius = 0;
    }

    public int getFluidAmount() {
        return tankBehaviour.getPrimaryHandler().getFluid().getAmount();
    }


    public boolean isWater() {
        var fluidStack = new FluidStack(Fluids.WATER, getFluidAmount());
        if (getContainedFluid().isEmpty())
            return false;

        return getContainedFluid().isFluidEqual(fluidStack) && !getContainedFluid().isEmpty();
    }


    public boolean isLava() {
        var fluidStack = new FluidStack(Fluids.LAVA, getFluidAmount());
        if (getContainedFluid().isEmpty())
            return false;

        return getContainedFluid().isFluidEqual(fluidStack) && !getContainedFluid().isEmpty();
    }

    protected boolean hasFluid() {
        return !getContainedFluid().isEmpty();

    }

    protected boolean isValidSpeed() {
        return Math.abs(getSpeed()) != 0;
    }

    void start() {

        this.hasFluidIn = true;
        this.currentState = State.LOADED;
        // sendData();
    }

    @Override
    public void tick() {
        super.tick();
        boolean isServer = !Objects.requireNonNull(getLevel()).isClientSide;

        if (this.getWorld() == null)
            return;


        if (hasFluid()) {
            start();
            if (isValidSpeed() && isLoaded() && getFluidAmount() != 0) {
                canHydrate = true;
                this.currentState = State.HYDRATING;

            }
        } else if (!isHydrating() && !isLoaded() || getContainedFluid().isEmpty()) {
            this.currentState = State.NONE;

            reset();
        }
        if (!isServer) {
            if (isHydrating() && hasFluid())
                initiateParticles();

        } else {
            if (isHydrating() && getFluidAmount() != 0) {
                applyEffects(getAABB((int) aabb().getYsize()), isLava());
                if (isWater())
                    findFarmland();


                refill();
            }


        }

    }

    private void applyEffects(AABB aabb, boolean lava) {
        if (getLevel() == null)
            return;

        List<LivingEntity> trackedEntities = getLevel().getEntitiesOfClass(LivingEntity.class, aabb);

        if (trackedEntities.size() != 0) {
            if (isWater())
                return;
            for (LivingEntity entity : trackedEntities) {

                if (!getContainedFluid().isEmpty()) {
                    List<MobEffectInstance> list = PotionUtils.getPotion(getContainedFluid().getOrCreateTag()).getEffects();
                    if (Math.sqrt(entity.distanceToSqr(new Vec3(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()))) < getRadius()) {
                        if (lava) {
                            entity.hurt(DamageSource.ON_FIRE, 2.0f);
                        }
                        // level.playSound((PlayerEntity) null, getBlockPos(), SoundEvents.PLAYER_HURT_ON_FIRE, SoundCategory.BLOCKS, 1, this.level.random.nextFloat() * 0.1F + 0.9F);

                        if (list.isEmpty()) {
                            return;
                        }
                        int entitiesAmount = trackedEntities.size();
                        float value = (float) getRadius() / entitiesAmount;

                        for (MobEffectInstance effectsInTheList : list) {
                            MobEffect actualEffect = effectsInTheList.getEffect();
                            MobEffectCategory effectCategory = actualEffect.getCategory();
                            MobEffectInstance effectInstance = new MobEffectInstance(actualEffect, effectCategory == MobEffectCategory.HARMFUL ? secondsInTicks((int) (value / 2) + 1) : secondsInTicks((int) (value / 3) + 1), (int) ((int) value * (getSpeed() / 256F)));
                            entity.addEffect(effectInstance);
                        }
                    }
                }
            }
        }
    }

    protected int getTankMaxCapacity() {
        return 1500;
    }


    @Override
    public void write(CompoundTag compound, boolean clientPacket) {

        compound.putInt("Radius", getRadius());
        compound.putInt("FluidAmount", getFluidAmount());
        compound.putBoolean("ContainerFluid", hasFluidIn);
        compound.putBoolean("HasWaterIn", isWater());
        compound.putBoolean("IsReady", isHydrating());
        NBTHelper.writeAABB(aabb());
        var stateString = currentState.name();
        stateString = stateString.substring(0, 1).toUpperCase() + stateString.substring(1).toLowerCase();
        compound.putString("State", stateString);
        NBTHelper.writeEnum(compound, "StateIn", currentState);
        //  compound.putString("State", compound.getInt("StateInteger") == 1 ? "Awaiting" : compound.getInt("StateInteger") == 0 ? "Hydrating" : "Loaded");
        if (isHydrating() && !getContainedFluid().isEmpty() && clientPacket) {
            compound.putBoolean("Particle", true);
            canSpawnParticles = false;
        }
        super.write(compound, clientPacket);
    }

    @Override
    public void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if (getWorld() != null)
            NBTHelper.readAABB(NBTHelper.writeAABB(aabb()));

        this.radius = compound.getInt("Radius");
        this.hasFluidIn = compound.getBoolean("ContainerFluid");
        boolean hasWaterStored = compound.getBoolean("HasWaterIn");
        this.canHydrate = compound.getBoolean("IsReady");
        // currentState = SprinklerTileEntity.State.values()[compound.getInt("StateInteger")];

        this.currentState = NBTHelper.readEnum(compound, "StateIn", State.class);

        if (compound.contains("Particle")) {
            canSpawnParticles = true;
        }
    }

    public boolean isHydrating() {

        return currentState == State.HYDRATING;
    }


    public boolean isLoaded() {

        return currentState == State.LOADED;
    }

    protected AABB getAABB(int height) {
        return new AABB(worldPosition).inflate(getRadius(), height, getRadius());

    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();


    }

    public int getComparatorOutput() {
        return ComparatorUtil.levelOfSmartFluidTank(getLevel(), getBlockPos());
    }

    protected void refill() {
        FluidNode.Flow flow;

        flow = new FluidNode.Flow(true, getContainedFluid());
        flow.progress.tickChaser();
        float multiplier = Math.abs(getSpeed() / getRadius()) + 8/16F;
        float ratio = multiplier / 8;


        float flowSpeed = Mth.clamp(flow.progress.getValue() + multiplier, 0, 1);
        flow.progress.setValue(flowSpeed);


        if (flow.progress.getValue() >= 1)
            getTank().drain(1, IFluidHandler.FluidAction.EXECUTE);
        notifyUpdate();

    }

    private SmartFluidTank getTank() {
        return tankBehaviour.getPrimaryHandler();
    }

    @Override
    public void setLazyTickRate(int slowTickRate) {
        super.setLazyTickRate(slowTickRate);

    }

    protected Random getRandom() {
        if (getLevel() != null) {
            return getLevel().getRandom();
        }

        return new Random();
    }

    private void findFarmland() {
        int CHANCE = CUConfig.FARMLAND_HYDRATE_CHANCE.get();
        AABB aabb = new AABB(worldPosition).inflate(getRadius(), getBlockState().getValue(CEILING) ? aabb().getYsize() : 0, getRadius());

        // MutableBoundingBox boundingBox = new MutableBoundingBox(worldPosition.offset(-getRadius(), -1, -getRadius()), worldPosition.offset(getRadius(), -1, getRadius()));
        // Farmland hydration & plants growth logic
        for (BlockPos blockPositions : BlockPos.randomBetweenClosed(getRandom(), 15, getBlockPos().getX() - getRadius(), (int) (getBlockPos().getY() - aabb().getYsize()), getBlockPos().getZ() - getRadius(), getBlockPos().getX() + getRadius(), getBlockPos().getY() + 1, getBlockPos().getZ() + getRadius())) {
            var blockState = getLevel().getBlockState(blockPositions);
            if (blockState.isAir())
                continue;
            if (hasFarmlandBlock(blockPositions, getLevel())) {
                if (isInsideCircle(getRadius(), getBlockPos(), blockPositions))
                    hydrateFarmland(blockPositions, getLevel(), blockState, aabb);
            }
            //todo: WIP, obviously this doesn't get called so just a placeholder for now
            /*
            if (isLava() && isHydrating()) {
                var dir = Direction.NORTH;
                if (FireBlock.canBePlacedAt(getLevel(), blockPositions, dir)) {
                    getLevel().playSound(null, blockPositions, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, getRandom().nextFloat() * 0.4F + 0.8F);
                    var blockS = FireBlock.getState(getLevel(), blockPositions);
                    getLevel().setBlock(blockPositions, blockS, 11);
                }
            }
             */
            //Crops grow logic
            if (checkForPlants(blockState.getBlock())) {
                if (isInsideCircle(getRadius(), getBlockPos(), blockPositions)) {
                    if (getRandom().nextInt(100) < CHANCE)
                        blockState.getBlock().randomTick(blockState, (ServerLevel) this.getLevel(), blockPositions, getRandom());
                }
            }
        }
    }

    public AABB aabb() {
        var axisAlignedBB2 = new AABB(getBlockPos()).inflate(getRadius(), 0, getRadius());
        var globPosition = new Vec3(0, getBlockPos().getY(), 0);

        if (getWorld() != null) {
            for (BlockPos bos : BlockPos.betweenClosed(
                    getBlockPos().getX() - getRadius(),
                    getBlockPos().getY() - getWorld().getHeight(),
                    getBlockPos().getZ() - getRadius(),
                    getBlockPos().getX() + getRadius(),
                    getBlockPos().getY() - 1,
                    getBlockPos().getZ() + getRadius())) {
                var blockState = getWorld().getBlockState(bos);
                if (blockState.isAir())
                    continue;

                var detectedblockPos = new Vec3(0, bos.getY(), 0);

                int dist = (int) Math.abs(Math.sqrt(globPosition.distanceToSqr(detectedblockPos)));

              /*
                ClientPlayerEntity = Minecraft.getInstance().player;
                clientPlayerEntity.sendMessage(slasha, clientPlayerEntity.getUUID());
              */
                var axisAlignedBB = axisAlignedBB2.expandTowards(0, -dist + 1, 0);
                return axisAlignedBB;

            }
        }
        return axisAlignedBB2;

    }

    private void renderDebugOutline(AABB axisAlignedBB) {

        CreateClient.OUTLINER.showAABB("sprinklerDebugBox" + getBlockPos(), axisAlignedBB).colored(new Color(getRadius() >= 1 ? 170 : 199, getRadius() >= 1 ? 255 : 0, getRadius() > 2 ? 0 : 57));
    }

    public Level getWorld() {
        return this.level;
    }

    protected double getFillState() {
        return (getFluidAmount() / getTankMaxCapacity()) * 100f;
    }

    //todo, redo tooltip because 0.5 broke  literally everything
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        LangBuilder lang = Lang.builder(CreateUtilities.ID);
/*
        if (Mth.equal(stressBase, 0))
            return added;

        Lang.translate("gui.goggles.generator_stats")
                .forGoggles(tooltip);
        Lang.translate("tooltip.capacityProvided")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        float speed = getTheoreticalSpeed();
        if (speed != getGeneratedSpeed() && speed != 0)
            stressBase *= getGeneratedSpeed() / speed;
        speed = java.lang.Math.abs(speed);

        float stressTotal = stressBase * speed;

        Lang.number(stressTotal)
                .translate("generic.unit.stress")
                .style(ChatFormatting.AQUA)
                .space()
                .add(Lang.translate("gui.goggles.at_current_speed")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
        */


        var axisAlignedBB2 = new AABB(getBlockPos()).inflate(getRadius(), 0, getRadius());
        var globPosition = new Vec3(0, getBlockPos().getY(), 0);


        tooltip.add(componentSpacing.plainCopy()
                .append("Sprinkler Info:"));

        var mb = Lang.translate("generic.unit.millibuckets");
        var fluidStackIn = getContainedFluid();
        var fluidName = new TranslatableComponent(fluidStackIn.getTranslationKey()).withStyle(ChatFormatting.GRAY);
        var indent = new TextComponent(spacing + " ");
        var contained = new TextComponent(String.valueOf(fluidStackIn.getAmount())).plainCopy().append(mb.string()).withStyle(ChatFormatting.GOLD);
        var slash = new TextComponent(" / ").withStyle(ChatFormatting.GRAY);
        var capacity = new TextComponent(String.valueOf(getTankMaxCapacity())).plainCopy().append(mb.string()).withStyle(ChatFormatting.DARK_GRAY);

        if (hasFluidIn && !getContainedFluid().isEmpty()) {
            tooltip.add(indent.plainCopy()
                    .append(fluidName));
            tooltip.add(indent.plainCopy()
                    .append(contained)
                    .append(slash)
                    .append(capacity));
   /*
     Percentage tooltip

        tooltip.add(indent.plainCopy()
                .append(percentage)
                .append(slash)
                .append(percentage100));

*/
        } else {
            var maxCapacity = Lang.translate("gui.goggles.fluid_container.capacity").style(ChatFormatting.GRAY);
            var amount = Lang.builder(String.valueOf((tankBehaviour.getPrimaryHandler().getTankCapacity(0)))).add(mb).style(ChatFormatting.GOLD);

            tooltip.add(indent.plainCopy()
                    .append(maxCapacity.string())
                    .append(amount.string()));
        }

        if (isPlayerSneaking) {
            if (Math.abs(getSpeed()) < 8) {

                tooltip.add(componentSpacing.plainCopy().plainCopy()
                        .append(lang.translate("sprinkler.content.range" + ":").string())
                        .withStyle(ChatFormatting.GRAY).append(getRadius() + " " + lang.translate("sprinkler.content.units").string()).withStyle(ChatFormatting.RED));
            } else {
                tooltip.add(componentSpacing.plainCopy()
                        .append(lang.translate("sprinkler.content.range").string() + ":")
                        .append(getRadius() + " " + lang.translate("sprinkler.content.units").string()).withStyle(ChatFormatting.AQUA));
            }


            for (BlockPos bos : BlockPos.betweenClosed(getBlockPos().getX() - getRadius(), getBlockPos().getY() - getWorld().getHeight(), getBlockPos().getZ() - getRadius(), getBlockPos().getX() + getRadius(), getBlockPos().getY() - 1, getBlockPos().getZ() + getRadius())) {
                var blockState = getWorld().getBlockState(bos);
                if (blockState.isAir())
                    continue;

                Vec3 detectedblockPos = new Vec3(0, bos.getY(), 0);

                var dist = (int) Math.abs(Math.sqrt(globPosition.distanceToSqr(detectedblockPos)));


                var axisAlignedBB = axisAlignedBB2.expandTowards(0, -dist + 1, 0);
                renderDebugOutline(axisAlignedBB);
            }
        }

        return true;
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {

        tankBehaviour = SmartFluidTankBehaviour.single(this, getTankMaxCapacity());
        behaviours.add(tankBehaviour);

    }

    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed, getBlockState().getValue(HORIZONTAL_FACING));
    }


    public FluidStack getContainedFluid() {
        return this.tankBehaviour.getPrimaryHandler().getFluid();
    }

    //TODO, redo this completely
    private void initiateParticles() {
        if (this.getWorld() == null) {
            return;
        }
        double x = this.getBlockPos().getX() + 0.5f;
        double y = this.getBlockPos().getY() + 1.0f;
        double z = this.getBlockPos().getZ() + 0.5f;
        Vec3 pos = new Vec3(x, y, z);
        float time = AnimationTickHolder.getRenderTime(getLevel());
        float speed = getSpeed();
        float angle;

        angle = (time * speed / 3F) % 360F;
        float random = getLevel().getRandom().nextFloat();
        for (int i = 0; i < 4; i++) {
            float alpha = (float) (((angle * -3) + 90 * i) * Math.PI) / 180F;
            float cosA = Math.cos(alpha);
            float sinA = Math.sin(alpha);

            Vec3 acceleration = new Vec3(Math.min(getSpeed() / 128F, 1F), Math.min(getSpeed()/128F, 1F),Math.min(getSpeed()/128F, 1F)).add(random*2.5, random*2.5,random*2.5).normalize();
            ParticleOptions particle = new FluidParticleData(AllParticleTypes.FLUID_PARTICLE.get(), getContainedFluid());
            Vec3 speedd = VecHelper.offsetRandomly(Vec3.ZERO, Create.RANDOM, 0.01f).add(acceleration.scale(-0.01f));
            boolean isCeiling = getBlockState().getValue(CEILING);
            float modifier = (float) getRadius() / 12F;
            for (int k = 0; k <= 4; k++) {
                float beta = k * ((float) Math.PI) / (180F);


                this.getWorld().addParticle(particle,
                        pos.x() +  (double) cosA / 2 , pos.y() - (isCeiling ? 15 / 16f : 0), pos.z()  + (double) sinA/2 ,
                        (random * 1.25F * cosA) * modifier, random * 2F * (isCeiling ? Math.sin(-beta)  : Math.cos(beta)),  (random*1.25F * sinA) * modifier);
            }
        }
    }

    public String getStateName() {
        var stateString = currentState.name();
        stateString = stateString.substring(0, 1).toUpperCase() + stateString.substring(1).toLowerCase();
        return stateString;
    }

    public State getState() {
        return currentState;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        var state = getBlockState();
        var ceiling = state.getValue(CEILING);
        if (this.isFluidHandlerCap(cap))
            if (ceiling) {
                if (side == Direction.UP)
                    return tankBehaviour.getCapability()
                            .cast();

            } else {
                if (side == Direction.DOWN)
                    return tankBehaviour.getCapability()
                            .cast();
            }
        return super.getCapability(cap, side);
    }

    @Override
    protected boolean isFluidHandlerCap(Capability<?> cap) {
        return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;

    }

    @Override
    public String getProvidedInfo() {
        return "HydrationState: " + getStateName() + "; Radius: " + getRadius() + "; FluidAmount: " + getFluidAmount();
    }


    public enum State {
        HYDRATING,
        NONE,
        LOADED
    }
}
