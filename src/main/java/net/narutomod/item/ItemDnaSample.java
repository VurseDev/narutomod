package net.narutomod.item;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.EdoSoulRegistry;
import net.narutomod.ElementsNarutomodMod;

/** Tradeable death specimen. Its identity and captured player data are validated by the server archive. */
@ElementsNarutomodMod.ModElement.Tag
public class ItemDnaSample extends ElementsNarutomodMod.ModElement {
    @ObjectHolder("narutomod:dna_sample") public static final Item block = null;
    public ItemDnaSample(ElementsNarutomodMod elements) { super(elements, 1148); }

    @Override public void initElements() {
        elements.items.add(() -> new Item() {
            @Override @SideOnly(Side.CLIENT)
            public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
                if (sampleId(stack) == null) lines.add(I18n.translateToLocal("tooltip.narutomod.dna_invalid"));
                else {
                    lines.add(stack.getTagCompound().getString("Name"));
                    lines.add(I18n.translateToLocal("tooltip.narutomod.dna_sample"));
                }
            }
        }.setRegistryName("dna_sample").setUnlocalizedName("dna_sample").setMaxStackSize(1));
    }

    @Override @SideOnly(Side.CLIENT) public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(block, 0, new ModelResourceLocation("narutomod:dna_sample", "inventory"));
    }

    @Override public void init(FMLInitializationEvent event) { MinecraftForge.EVENT_BUS.register(new Deaths()); }

    public static ItemStack create(UUID sample, NBTTagCompound captured) {
        if (block == null || sample == null || captured == null || !captured.hasUniqueId("Player")) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(block);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId("Sample", sample);
        tag.setUniqueId("Donor", captured.getUniqueId("Player"));
        String name = captured.getString("Name");
        tag.setString("Name", name.length() > 64 ? name.substring(0, 64) : name);
        stack.setTagCompound(tag);
        return stack;
    }

    public static UUID sampleId(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == block && stack.getCount() == 1
            && stack.hasTagCompound() && stack.getTagCompound().hasUniqueId("Sample")
            && stack.getTagCompound().hasUniqueId("Donor") ? stack.getTagCompound().getUniqueId("Sample") : null;
    }

    public static final class Deaths {
        private final Set<EntityPlayerMP> issued = Collections.newSetFromMap(new WeakHashMap<EntityPlayerMP, Boolean>());

        @SubscribeEvent public void onRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
            // Player entity IDs can be reused on respawn; that must not suppress a later real death.
            issued.remove(event.player);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onDeath(LivingDeathEvent event) {
            if (event.isCanceled() || !(event.getEntityLiving() instanceof EntityPlayerMP)) return;
            EntityPlayerMP player = (EntityPlayerMP)event.getEntityLiving();
            if (player.world.isRemote || player.isCreative() || player.isSpectator() || issued.contains(player)) return;
            EdoSoulRegistry registry = EdoSoulRegistry.get(player.world);
            UUID sample = registry.issue(player);
            ItemStack stack = create(sample, registry.getSample(sample));
            if (stack.isEmpty()) return;
            EntityItem drop = new EntityItem(player.world, player.posX, player.posY + .1, player.posZ, stack);
            drop.setDefaultPickupDelay();
            if (player.world.spawnEntity(drop)) issued.add(player);
        }
    }
}
