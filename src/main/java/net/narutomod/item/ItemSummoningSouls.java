package net.narutomod.item;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.EdoSoulRegistry;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.entity.EntityEdoTensei;

@ElementsNarutomodMod.ModElement.Tag
public class ItemSummoningSouls extends ElementsNarutomodMod.ModElement {
    @GameRegistry.ObjectHolder("narutomod:summoning_souls") public static final Item block = null;
    public static final ItemJutsu.JutsuEnum SUMMON = new ItemJutsu.JutsuEnum(0,
        "jutsu.narutomod.edo_summon", 'B', 100d, new Summon()).withCustomBalance();

    public ItemSummoningSouls(ElementsNarutomodMod instance) { super(instance, 1030); }
    @Override public void initElements() { elements.items.add(SoulsItem::new); }
    @Override @SideOnly(Side.CLIENT) public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(block, 0, new ModelResourceLocation("narutomod:summoning_souls", "inventory"));
    }

    public static void deliver(EntityPlayer player) {
        SoulsItem item = (SoulsItem)block;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() == block && item.isOwner(stack, player)) { item.refresh(stack, player); return; }
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (stack.getItem() == block && item.isOwner(stack, player)) { item.refresh(stack, player); return; }
        }
        ItemStack stack = new ItemStack(block);
        item.setOwner(stack, player);
        stack.clearCustomName();
        item.setIsAffinity(stack, true);
        item.enableJutsu(stack, SUMMON, true);
        item.addJutsuXp(stack, SUMMON, SUMMON.requiredXP);
        item.refresh(stack, player);
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    public static class SoulsItem extends ItemJutsu.Base {
        public SoulsItem() {
            super(ItemJutsu.JutsuEnum.Type.NINJUTSU, SUMMON);
            setRegistryName("summoning_souls");
            setUnlocalizedName("summoning_souls");
            setCreativeTab(TabCustomTabs.jutsus);
        }
        // No automatic rebinding, including creative players and copied stacks.
        @Override protected boolean isOwner(ItemStack stack, EntityLivingBase entity) {
            return entity.getUniqueID().equals(getOwnerUuid(stack));
        }
        @Override public EnumActionResult canActivateJutsu(ItemStack stack, ItemJutsu.JutsuEnum jutsu, EntityPlayer player) {
            if (!isOwner(stack, player)) {
                if (!player.world.isRemote) player.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_owner"), true);
                return EnumActionResult.FAIL;
            }
            return super.canActivateJutsu(stack, jutsu, player);
        }
        @Override protected boolean executeJutsu(ItemStack stack, EntityLivingBase caster, float power) {
            return isOwner(stack, caster) && super.executeJutsu(stack, caster, power);
        }
        @Override public ActionResult<ItemStack> onItemRightClick(World world,EntityPlayer player,EnumHand hand) {
            ItemStack stack=player.getHeldItem(hand);
            if(player.isSneaking() && isOwner(stack,player)) {
                if(!world.isRemote)recallSelected(stack,player);
                return new ActionResult<>(EnumActionResult.SUCCESS,stack);
            }
            return super.onItemRightClick(world,player,hand);
        }
        @Override protected void setNextJutsu(ItemStack stack, EntityLivingBase caster) {
            if (caster.world.isRemote || !(caster instanceof EntityPlayer) || !isOwner(stack, caster)) return;
            EntityPlayer player = (EntityPlayer)caster;
            int count = EdoSoulRegistry.get(player.world).souls(player.getUniqueID()).tagCount();
            if (count == 0) return;
            int delta = player.isSneaking() ? -1 : 1;
            stack.getTagCompound().setInteger("SelectedSoul", Math.floorMod(stack.getTagCompound().getInteger("SelectedSoul") + delta, count));
            refresh(stack, player);
            player.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_selected",
                stack.getTagCompound().getInteger("SelectedSoul") + 1, stack.getTagCompound().getString("SoulName")), true);
        }
        public void refresh(ItemStack stack, EntityPlayer player) {
            if (player.world.isRemote || !isOwner(stack, player)) return;
            NBTTagList list = EdoSoulRegistry.get(player.world).souls(player.getUniqueID());
            NBTTagCompound tag = stack.getTagCompound();
            int count = list.tagCount();
            int selected = count == 0 ? 0 : Math.floorMod(tag.getInteger("SelectedSoul"), count);
            tag.setInteger("SelectedSoul", selected);
            tag.setInteger("SoulCount", count);
            tag.setString("SoulName", count == 0 ? "" : list.getCompoundTagAt(selected).getString("Name"));
            int first = selected / 8 * 8;
            NBTTagList window = new NBTTagList();
            for (int i = first; i < Math.min(count, first + 8); i++) {
                NBTTagCompound label=new NBTTagCompound();
                label.setString("Name",list.getCompoundTagAt(i).getString("Name"));
                window.appendTag(label);
            }
            tag.setInteger("WindowStart", first);
            tag.setTag("SoulWindow", window);
        }
        @Override public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
            if (!world.isRemote && entity instanceof EntityPlayer && entity.ticksExisted % 20 == 0) refresh(stack, (EntityPlayer)entity);
        }
        @Override @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
            lines.add(new TextComponentTranslation("tooltip.narutomod.edo_controls").getUnformattedText());
            lines.add(new TextComponentTranslation("tooltip.narutomod.edo_recall").getUnformattedText());
            if (!stack.hasTagCompound()) return;
            NBTTagCompound tag = stack.getTagCompound();
            NBTTagList window = tag.getTagList("SoulWindow", 10);
            for (int i = 0; i < window.tagCount(); i++) {
                int index = tag.getInteger("WindowStart") + i;
                lines.add((index == tag.getInteger("SelectedSoul") ? "> " : "  ") + (index + 1) + ": " + window.getCompoundTagAt(i).getString("Name"));
            }
            lines.add(new TextComponentTranslation("tooltip.narutomod.edo_count", tag.getInteger("SoulCount")).getUnformattedText());
        }
    }

    private static void recallSelected(ItemStack stack,EntityPlayer caster) {
        if(caster.world.isRemote || !((SoulsItem)block).isOwner(stack,caster))return;
        EdoSoulRegistry archive=EdoSoulRegistry.get(caster.world);
        NBTTagList souls=archive.souls(caster.getUniqueID());
        if(souls.tagCount()==0)return;
        NBTTagCompound soul=souls.getCompoundTagAt(Math.floorMod(stack.getTagCompound().getInteger("SelectedSoul"),souls.tagCount()));
        java.util.UUID soulId=soul.getUniqueId("Soul");
        boolean canceled=EntityEdoTensei.cancelSummon(caster,soulId);
        java.util.UUID active=archive.active(caster.getUniqueID(),soulId);
        if(active!=null && archive.deactivate(caster.getUniqueID(),soulId,active)) {
            canceled=true;
            for(net.minecraft.world.WorldServer world:caster.world.getMinecraftServer().worlds) {
                Entity summoned=world.getEntityFromUuid(active);
                if(summoned instanceof net.narutomod.entity.EntityEdoReanimation)
                    ((net.narutomod.entity.EntityEdoReanimation)summoned).dismiss();
            }
        }
        caster.sendStatusMessage(new TextComponentTranslation(canceled?"message.narutomod.edo_recalled":"message.narutomod.edo_not_active",soul.getString("Name")),true);
    }

    public static class Ritual implements ItemJutsu.IJutsuCallback {
        @Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
            return caster instanceof EntityPlayer && EntityEdoTensei.begin((EntityPlayer)caster, true, null);
        }
        @Override public float getMaxPower() { return 1f; }
    }
    public static class Summon implements ItemJutsu.IJutsuCallback {
        @Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
            if (caster.world.isRemote || !(caster instanceof EntityPlayer) || stack.getItem() != block
                || !((SoulsItem)block).isOwner(stack, caster)) return false;
            NBTTagList souls = EdoSoulRegistry.get(caster.world).souls(caster.getUniqueID());
            if (souls.tagCount() == 0) return false;
            int index = Math.floorMod(stack.getTagCompound().getInteger("SelectedSoul"), souls.tagCount());
            NBTTagCompound soul=souls.getCompoundTagAt(index);
            if(caster.isSneaking()) {
                recallSelected(stack,(EntityPlayer)caster);
                return false; // Recall costs no summon chakra and does not start a new cooldown.
            }
            return EntityEdoTensei.begin((EntityPlayer)caster, false, soul.copy());
        }
        @Override public float getMaxPower() { return 1f; }
    }
}
