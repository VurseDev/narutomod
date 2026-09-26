package net.narutomod.item;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.creativetab.TabCustomTabs;

/** Eternal eyes preserve Madara's techniques without inheriting Amaterasu or Kamui. */
@ElementsNarutomodMod.ModElement.Tag
public class ItemMangekyoSharinganMadaraEternal extends ElementsNarutomodMod.ModElement {
    @ObjectHolder("narutomod:mangekyosharinganmadaraeternalhelmet")
    public static final Item helmet = null;

    public ItemMangekyoSharinganMadaraEternal(ElementsNarutomodMod instance) {
        super(instance, 1041);
    }

    @Override
    public void initElements() {
        ItemArmor.ArmorMaterial material = EnumHelper.addArmorMaterial("MANGEKYOSHARINGAN_MADARA_ETERNAL",
            "narutomod:madara_eternal_", 1024, new int[]{2, 5, 6, 10}, 0, null, 2.0F);
        elements.items.add(() -> new ItemMangekyoSharinganMadara.Eye(material, true)
            .setUnlocalizedName("mangekyosharinganmadaraeternalhelmet")
            .setRegistryName("mangekyosharinganmadaraeternalhelmet").setCreativeTab(TabCustomTabs.eyes));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(helmet, 0,
            new ModelResourceLocation("narutomod:mangekyosharinganmadaraeternalhelmet", "inventory"));
    }
}
