package net.narutomod;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Works on the actual stack references from main inventory and offhand; armor/containers are excluded. */
public final class PaperBombAmmo {
    private PaperBombAmmo(){}
    public static int count(Iterable<ItemStack> stacks,Item bomb){
        int count=0;for(ItemStack stack:stacks)if(!stack.isEmpty()&&stack.getItem()==bomb)count+=stack.getCount();return count;
    }
    public static boolean consume(Iterable<ItemStack> stacks,Item bomb,int amount){
        if(amount<=0||bomb==null||count(stacks,bomb)<amount)return false;
        for(ItemStack stack:stacks)if(!stack.isEmpty()&&stack.getItem()==bomb&&amount>0){int n=Math.min(amount,stack.getCount());stack.shrink(n);amount-=n;}
        return true;
    }
}
