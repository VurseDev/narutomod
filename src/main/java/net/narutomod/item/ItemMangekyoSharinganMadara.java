package net.narutomod.item;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.MadaraTemporalController;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.entity.EntitySusanooBase;
import net.narutomod.procedure.ProcedureSusanoo;
import net.narutomod.procedure.ProcedureUtils;

/** Madara's own ability family; deliberately does not extend the legacy mixed EMS item. */
@ElementsNarutomodMod.ModElement.Tag
public class ItemMangekyoSharinganMadara extends ElementsNarutomodMod.ModElement {
    @ObjectHolder("narutomod:mangekyosharinganmadarahelmet")
    public static final Item helmet = null;

    public ItemMangekyoSharinganMadara(ElementsNarutomodMod instance) {
        super(instance, 1040);
    }

    @Override
    public void initElements() {
        ItemArmor.ArmorMaterial material = EnumHelper.addArmorMaterial("MANGEKYOSHARINGAN_MADARA",
            "narutomod:madara_", 1024, new int[]{2, 5, 6, 10}, 0, null, 1.0F);
        elements.items.add(() -> new Eye(material, false)
            .setUnlocalizedName("mangekyosharinganmadarahelmet")
            .setRegistryName("mangekyosharinganmadarahelmet").setCreativeTab(TabCustomTabs.eyes));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(helmet, 0,
            new ModelResourceLocation("narutomod:mangekyosharinganmadarahelmet", "inventory"));
    }

    public static class Eye extends ItemSharingan.Base {
        public static final int SUSANOO_COLOR = 0x202869BE;
        private final boolean eternal;

        public Eye(ItemArmor.ArmorMaterial material, boolean eternal) {
            super(material);
            this.eternal = eternal;
        }

        @Override public ItemSharingan.Type getSubType() { return ItemSharingan.Type.MADARA; }
        @Override public boolean isMangekyo() { return true; }
        @Override public boolean isEternal() { return eternal; }
        @Override public int getMaxDamage() { return eternal ? 0 : super.getMaxDamage(); }
        @Override public boolean isDamageable() { return !eternal && super.isDamageable(); }
        @Override public int getColor(ItemStack stack) { return SUSANOO_COLOR; }
        @Override public void setColor(ItemStack stack, int color) { super.setColor(stack, SUSANOO_COLOR); }

        @Override
        public void copyOwner(ItemStack toStack, ItemStack fromStack) {
            super.copyOwner(toStack, fromStack);
            // Advancing from Sharingan must also advance the displayed technique name.
            toStack.clearCustomName();
            UUID ownerId = ProcedureUtils.getOwnerId(toStack);
            EntityLivingBase owner = ownerId == null ? null : ProcedureUtils.searchLivingMatchingId(ownerId);
            if (owner != null) toStack.setStackDisplayName(owner.getName() + "'s " + toStack.getDisplayName());
        }

        @Override
        public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
            return "narutomod:textures/mangekyosharinganhelmet_madara" + (eternal ? "_eternal" : "") + ".png";
        }

        @Override
        public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
            super.onArmorTick(world, player, stack);
            if (!world.isRemote && player.getItemStackFromSlot(EntityEquipmentSlot.HEAD) == stack
                && !ItemSharingan.isBlinded(stack)) {
                player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 2, 2, false, false));
            }
        }

        @Override
        public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
            super.onUpdate(stack, world, entity, slot, selected);
            if (!world.isRemote && eternal && entity instanceof EntityPlayer && entity.ticksExisted % 20 == 0) {
                UUID owner = ProcedureUtils.getOwnerId(stack);
                if (owner != null) {
                    for (ItemStack other : ProcedureUtils.getAllItemsOfSubType((EntityPlayer)entity, ItemSharingan.Base.class)) {
                        if (other.getItem() == helmet && owner.equals(ProcedureUtils.getOwnerId(other))) {
                            if (ItemOcularGear.eye(other)==null) other.shrink(1);
                        }
                    }
                }
            }
        }

        @Override
        public boolean onJutsuKey1(boolean pressed, ItemStack stack, EntityPlayer player) {
            if (!pressed && !player.world.isRemote) MadaraTemporalController.toggle(player);
            return true;
        }

        @Override
        public boolean onJutsuKey2(boolean pressed, ItemStack stack, EntityPlayer player) {
            if (!pressed && !player.world.isRemote) ProcedureSusanoo.execute(player);
            return true;
        }

        @Override
        public boolean onSwitchJutsuKey(boolean pressed, ItemStack stack, EntityPlayer player) {
            if (player.getRidingEntity() instanceof EntitySusanooBase) {
                if (!pressed && !player.world.isRemote) ProcedureSusanoo.upgrade(player);
                return true;
            }
            return false;
        }

        @SideOnly(Side.CLIENT)
        @Override
        public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
            super.addInformation(stack, world, tooltip, flag);
            tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu1") + ": "
                + TextFormatting.GRAY + I18n.translateToLocal("tooltip.madara.temporal"));
            tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu2") + ": "
                + TextFormatting.GRAY + I18n.translateToLocal("entity.susanoomadara.name"));
            tooltip.add(TextFormatting.DARK_AQUA + I18n.translateToLocal("tooltip.madara.susanoo_upgrade"));
            tooltip.add(TextFormatting.DARK_GRAY + I18n.translateToLocal(eternal
                ? "tooltip.madara.eternal" : "tooltip.madara.strain"));
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return TextFormatting.RED + super.getItemStackDisplayName(stack) + TextFormatting.RESET;
        }
    }
}
