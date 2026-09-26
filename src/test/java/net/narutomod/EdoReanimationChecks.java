package net.narutomod;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import net.narutomod.entity.EntityEdoReanimation;

/** Runs real NPC constructors, damage handling and NBT; no client/GL or server archive needed. */
public final class EdoReanimationChecks {
    private static int checks;
    private static void check(boolean value,String why) {checks++;if(!value)throw new AssertionError(why);}
    private static final class EmptyWorld extends World {
        EmptyWorld() {
            super(null,new WorldInfo(new WorldSettings(0,GameType.SURVIVAL,false,false,WorldType.DEFAULT),"Edo fixture"),
                new WorldProviderSurface(),new Profiler(),false);
            provider.setWorld(this);
        }
        @Override protected IChunkProvider createChunkProvider() {return null;}
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty) {return true;}
        @Override public net.minecraft.block.state.IBlockState getBlockState(BlockPos pos) {return Blocks.AIR.getDefaultState();}
    }
    private static final class Player extends EntityPlayer {
        Entity ally;
        Player(World world,String name) {super(world,new GameProfile(UUID.randomUUID(),name));}
        @Override public boolean isSpectator() {return false;}
        @Override public boolean isCreative() {return false;}
        @Override public boolean isOnSameTeam(Entity entity) {return entity==this || entity==ally;}
        @Override public boolean canAttackPlayer(EntityPlayer other) {return false;}
    }
    private static final class Edo extends EntityEdoReanimation {
        Player caster;
        Edo(World world,Player player) {super(world);caster=player;}
        @Override public EntityPlayer getSummoner() {return caster;}
    }
    public static void main(String[] args) {
        Bootstrap.register();
        EmptyWorld world=new EmptyWorld();
        Player caster=new Player(world,"Caster"),other=new Player(world,"Other");
        NBTTagCompound soul=new NBTTagCompound();
        soul.setUniqueId("Player",other.getUniqueID());soul.setUniqueId("Soul",UUID.randomUUID());
        soul.setString("Name","Other");soul.setDouble("NinjaXp",100000);
        Edo edo=new Edo(world,caster);edo.configure(caster.getUniqueID(),soul);
        check(edo.getMaxHealth()==80,"high mastery remains capped at 80 health");
        check(edo.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue()==10,"high mastery damage remains capped at ten");
        check(edo.isOnSameTeam(caster),"owner is always protected");
        check(!edo.attackEntityFrom(DamageSource.causePlayerDamage(caster),100),"owner damage cannot break own summon");
        check(edo.getHealth()==80 && edo.reformTicks()==0,"friendly hit cannot force reform");
        caster.ally=other;
        check(edo.isOnSameTeam(other),"caster teammate is protected");
        check(!edo.attackEntityFrom(DamageSource.causePlayerDamage(other),100),"allied damage cannot break summon");
        caster.ally=null;
        edo.setAttackTarget(other);
        check(edo.getAttackTarget()==null,"AI respects disabled player PvP");
        check(edo.attackEntityFrom(DamageSource.GENERIC,100),"lethal hit starts reform");
        check(edo.reformTicks()==100 && edo.getHealth()==1 && edo.isAIDisabled(),"reforming lasts five seconds and disables combat");
        check(!edo.attackEntityFrom(DamageSource.GENERIC,100),"ordinary hits cannot kill the reforming body");
        NBTTagCompound saved=new NBTTagCompound();edo.writeEntityToNBT(saved);
        Edo restored=new Edo(world,caster);restored.readEntityFromNBT(saved);
        check(restored.reformTicks()==100 && restored.isAIDisabled(),"reload does not skip reform lockout");
        check(restored.ownerId().equals(caster.getUniqueID()) && restored.soulId().equals(soul.getUniqueId("Soul")),"owner and soul identity survive NPC save");
        check(restored.soul().getUniqueId("Player").equals(other.getUniqueID()),"donor identity stays separate from caster");
        soul.setDouble("NinjaXp",Double.NaN);Edo invalid=new Edo(world,caster);invalid.configure(caster.getUniqueID(),soul);
        check(invalid.getMaxHealth()==20 && invalid.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue()==3,"bad XP cannot create invulnerable/infinite-stat NPC");
        System.out.println("Edo reanimation runtime: "+checks+" checks passed.");
    }
}
