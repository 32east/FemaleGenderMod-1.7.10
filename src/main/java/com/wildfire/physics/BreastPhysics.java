package com.wildfire.physics;

import com.wildfire.api.IGenderArmor;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * A single spring-damper driven breast. Ticked once per client tick per player.
 */
public class BreastPhysics {

    private float bounceVel = 0, targetBounce = 0, velocity = 0, wfg_femaleBreast, wfg_preBounce;
    private float bounceRotVel = 0, targetRotVel = 0, rotVelocity = 0, wfg_bounceRotation, wfg_preBounceRotation;
    private float bounceVelX = 0, targetBounceX = 0, velocityX = 0, wfg_femaleBreastX, wfg_preBounceX;

    private boolean justSneaking = false, alreadySleeping = false;

    private float breastSize = 0, preBreastSize = 0;

    private double motionX, motionY, motionZ;
    private boolean hasPrePos = false;
    private double prePosX, prePosY, prePosZ;

    private final GenderPlayer genderPlayer;

    private int randomB = 1;
    private boolean alreadyFalling = false;

    public BreastPhysics(GenderPlayer genderPlayer) {
        this.genderPlayer = genderPlayer;
    }

    public void update(EntityPlayer plr, IGenderArmor armor) {
        this.wfg_preBounce = this.wfg_femaleBreast;
        this.wfg_preBounceX = this.wfg_femaleBreastX;
        this.wfg_preBounceRotation = this.wfg_bounceRotation;
        this.preBreastSize = this.breastSize;

        if (!hasPrePos) {
            this.prePosX = plr.posX;
            this.prePosY = plr.posY;
            this.prePosZ = plr.posZ;
            this.hasPrePos = true;
            return;
        }

        float breastWeight = genderPlayer.getBustSize() * 1.25f;
        float targetBreastSize = genderPlayer.getBustSize();

        if (!genderPlayer.getGender().canHaveBreasts()) {
            targetBreastSize = 0;
        } else {
            float tightness = WildfireHelper.clamp(armor.tightness(), 0, 1);
            // Tighter armor compresses the chest, by at most 15%
            targetBreastSize *= 1 - 0.15F * tightness;
        }

        if (breastSize < targetBreastSize) {
            breastSize += Math.abs(breastSize - targetBreastSize) / 2f;
        } else {
            breastSize -= Math.abs(breastSize - targetBreastSize) / 2f;
        }

        this.motionX = plr.posX - this.prePosX;
        this.motionY = plr.posY - this.prePosY;
        this.motionZ = plr.posZ - this.prePosZ;
        this.prePosX = plr.posX;
        this.prePosY = plr.posY;
        this.prePosZ = plr.posZ;

        float bounceIntensity = (targetBreastSize * 3f) * genderPlayer.getBounceMultiplier();
        float resistance = WildfireHelper.clamp(armor.physicsResistance(), 0, 1);
        bounceIntensity *= 1 - resistance;

        if (!genderPlayer.getBreasts().isUniboob()) {
            bounceIntensity = bounceIntensity * WildfireHelper.randFloat(0.5f, 1.5f);
        }
        if (plr.fallDistance > 0 && !alreadyFalling) {
            randomB = plr.worldObj.rand.nextBoolean() ? -1 : 1;
            alreadyFalling = true;
        }
        if (plr.fallDistance == 0) {
            alreadyFalling = false;
        }

        this.targetBounce = (float) motionY * bounceIntensity;
        this.targetBounce += breastWeight;
        this.targetRotVel = -((plr.renderYawOffset - plr.prevRenderYawOffset) / 15f) * bounceIntensity;

        float f = (float) (motionX * motionX + motionY * motionY + motionZ * motionZ);
        f = f / 0.2F;
        f = f * f * f;
        if (f < 1.0F) {
            f = 1.0F;
        }

        this.targetBounce += MathHelper.cos(plr.limbSwing * 0.6662F + (float) Math.PI) * 0.5F * plr.limbSwingAmount * 0.5F / f;
        this.targetRotVel += (float) motionY * bounceIntensity * randomB;

        if (plr.isSneaking() && !this.justSneaking) {
            this.justSneaking = true;
            this.targetBounce += bounceIntensity;
        }
        if (!plr.isSneaking() && this.justSneaking) {
            this.justSneaking = false;
            this.targetBounce += bounceIntensity;
        }

        Entity vehicle = plr.ridingEntity;
        if (vehicle != null) {
            float movement = (float) (vehicle.motionX * vehicle.motionX + vehicle.motionY * vehicle.motionY
                    + vehicle.motionZ * vehicle.motionZ);
            if (vehicle instanceof EntityBoat) {
                if (movement > 0.01f && vehicle.ticksExisted % clampMovement(movement) == 5) {
                    this.targetBounce = bounceIntensity / 3.25f;
                }
            } else if (vehicle instanceof EntityMinecart) {
                if (Math.random() * movement < 0.5f && movement > 0.2f) {
                    this.targetBounce = (Math.random() > 0.5 ? -1 : 1) * bounceIntensity / 6f;
                }
            } else if (vehicle instanceof EntityHorse) {
                if (vehicle.ticksExisted % clampMovement(movement) == 5 && movement > 0.1f) {
                    this.targetBounce = bounceIntensity / 4f;
                }
            } else if (vehicle instanceof EntityPig) {
                if (vehicle.ticksExisted % clampMovement(movement) == 5 && movement > 0.08f) {
                    this.targetBounce = bounceIntensity / 4f;
                }
            }
        }

        if (plr.isSwingInProgress && plr.ticksExisted % 5 == 0 && !plr.isPlayerSleeping()) {
            this.targetBounce += (Math.random() > 0.5 ? 0.25f : -0.25f) * bounceIntensity;
        }
        if (plr.isPlayerSleeping() && !this.alreadySleeping) {
            this.targetBounce = bounceIntensity;
            this.alreadySleeping = true;
        }
        if (!plr.isPlayerSleeping() && this.alreadySleeping) {
            this.targetBounce = bounceIntensity;
            this.alreadySleeping = false;
        }

        float percent = genderPlayer.getFloppiness();
        float bounceAmount = 0.45f * (1f - percent) + 0.15f;
        bounceAmount = WildfireHelper.clamp(bounceAmount, 0.15f, 0.6f);
        float delta = 2.25f - bounceAmount;

        float distanceFromMin = Math.abs(bounceVel + 0.5f) * 0.5f;
        float distanceFromMax = Math.abs(bounceVel - 2.65f) * 0.5f;

        if (bounceVel < -0.5f) {
            targetBounce += distanceFromMin;
        }
        if (bounceVel > 2.5f) {
            targetBounce -= distanceFromMax;
        }
        if (targetBounce < -1.5f) {
            targetBounce = -1.5f;
        }
        if (targetBounce > 2.5f) {
            targetBounce = 2.5f;
        }
        if (targetRotVel < -25f) {
            targetRotVel = -25f;
        }
        if (targetRotVel > 25f) {
            targetRotVel = 25f;
        }

        this.velocity = WildfireHelper.lerp(bounceAmount, this.velocity, (this.targetBounce - this.bounceVel) * delta);
        this.bounceVel += this.velocity * percent * 1.1625f;

        this.velocityX = WildfireHelper.lerp(bounceAmount, this.velocityX, (this.targetBounceX - this.bounceVelX) * delta);
        this.bounceVelX += this.velocityX * percent;

        this.rotVelocity = WildfireHelper.lerp(bounceAmount, this.rotVelocity, (this.targetRotVel - this.bounceRotVel) * delta);
        this.bounceRotVel += this.rotVelocity * percent;

        this.wfg_bounceRotation = this.bounceRotVel;
        this.wfg_femaleBreastX = this.bounceVelX;
        this.wfg_femaleBreast = this.bounceVel;
    }

    public float getBreastSize(float partialTicks) {
        return WildfireHelper.lerp(partialTicks, preBreastSize, breastSize);
    }

    public float getPreBounceY() {
        return this.wfg_preBounce;
    }

    public float getBounceY() {
        return this.wfg_femaleBreast;
    }

    public float getPreBounceX() {
        return this.wfg_preBounceX;
    }

    public float getBounceX() {
        return this.wfg_femaleBreastX;
    }

    public float getBounceRotation() {
        return this.wfg_bounceRotation;
    }

    public float getPreBounceRotation() {
        return this.wfg_preBounceRotation;
    }

    private int clampMovement(float movement) {
        int val = (int) (10 - movement * 2f);
        if (val < 1) {
            val = 1;
        }
        return val;
    }
}
