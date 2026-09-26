package net.narutomod.client;

import java.io.IOException;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.narutomod.NarutomodMod;
import net.narutomod.keybind.KeyBindingDojutsuControl;
import net.minecraftforge.fml.relauncher.*;

/** Optional selector over the unchanged, server-validated legacy toggle. */
@SideOnly(Side.CLIENT)
public final class GuiDojutsuWheel extends GuiScreen {
    private final List<ItemStack> eyes;private final IntConsumer selection;private final int selected;private int page;
    public GuiDojutsuWheel(List<ItemStack> eyes,int selected,IntConsumer selection){this.eyes=eyes;this.selected=selected;this.selection=selection;page=selected/8;}
    @Override public void initGui(){
        buttonList.clear();int count=Math.min(8,eyes.size()-page*8);int radius=Math.min(94,Math.max(38,(height-100)/2));
        for(int i=0;i<count;i++){double angle=-Math.PI/2+i*Math.PI*2/count;buttonList.add(new GuiButton(page*8+i,width/2+(int)(Math.cos(angle)*Math.min(136,width/2-56))-45,height/2+(int)(Math.sin(angle)*radius)-19,90,38,""));}
        buttonList.add(new GuiButton(10000,width/2-56,height-29,24,20,"<"));buttonList.add(new GuiButton(10001,width/2+32,height-29,24,20,">"));
        buttonList.get(buttonList.size()-2).enabled=page>0;buttonList.get(buttonList.size()-1).enabled=(page+1)*8<eyes.size();
    }
    @Override protected void actionPerformed(GuiButton button)throws IOException{
        if(button.id==10000){page--;initGui();return;}if(button.id==10001){page++;initGui();return;}
        if(button.id<0||button.id>=eyes.size())return;selection.accept(button.id);
        NarutomodMod.PACKET_HANDLER.sendToServer(new KeyBindingDojutsuControl.ToggleMessage(eyes.get(button.id).getItem().getRegistryName().toString()));mc.displayGuiScreen(null);
    }
    @Override public void drawScreen(int x,int y,float partial){
        drawDefaultBackground();drawCenteredString(fontRenderer,I18n.format("gui.narutomod.dojutsu_wheel"),width/2,height/2-10,0xEEE7DA);
        drawCenteredString(fontRenderer,I18n.format("gui.narutomod.dojutsu_wheel_hint"),width/2,height/2+4,0xB6B2AD);
        super.drawScreen(x,y,partial);
        for(GuiButton button:buttonList)if(button.id<10000){
            ItemStack eye=eyes.get(button.id);if(button.id==selected)drawRect(button.x,button.y,button.x+button.width,button.y+2,0xFFCAAF64);
            GlStateManager.color(1,1,1,1);RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(eye,button.x+37,button.y+3);RenderHelper.disableStandardItemLighting();
            String name=eye.getDisplayName();if(fontRenderer.getStringWidth(name)>84)name=fontRenderer.trimStringToWidth(name,75)+"...";
            drawCenteredString(fontRenderer,name,button.x+45,button.y+25,0xFFFFFF);
            if(button.isMouseOver())drawHoveringText(eye.getTooltip(mc.player,net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL),x,y);
        }
        drawCenteredString(fontRenderer,(page+1)+" / "+((eyes.size()+7)/8),width/2,height-23,0xDDD6C8);
    }
    @Override public boolean doesGuiPauseGame(){return false;}
}
