package net.narutomod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client presentation only; casting costs, timing and acceptance remain server owned. */
@ElementsNarutomodMod.ModElement.Tag
public final class JutsuCastingPresentation extends ElementsNarutomodMod.ModElement {
    public JutsuCastingPresentation(ElementsNarutomodMod elements) { super(elements, 1147); }

    @Override
    @SideOnly(Side.CLIENT)
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new net.narutomod.client.ClientElementalCasting());
    }
}
