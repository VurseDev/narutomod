package net.narutomod.item;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.creativetab.TabModTab;

import java.util.List;
import javax.annotation.Nullable;

@ElementsNarutomodMod.ModElement.Tag
public class ItemBaryonMode extends ElementsNarutomodMod.ModElement {
	@ObjectHolder("narutomod:baryon_modehelmet")
	public static final Item helmet = null;
	@ObjectHolder("narutomod:baryon_modebody")
	public static final Item body = null;
	@ObjectHolder("narutomod:baryon_modelegs")
	public static final Item legs = null;

	@SideOnly(Side.CLIENT)
	private ModelBaryonMode model;

	public ItemBaryonMode(ElementsNarutomodMod instance) {
		super(instance, 1008);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void init(FMLInitializationEvent event) {
		this.model = new ModelBaryonMode();
	}

	@Override
	public void initElements() {
		ItemArmor.ArmorMaterial material = EnumHelper.addArmorMaterial("BARYON_MODE", "narutomod:baryon_mode_",
		 1024, new int[]{0, 0, 0, 0}, 0, null, 0.0F);
		this.elements.items.add(() -> new BaryonArmor(material, EntityEquipmentSlot.HEAD)
		 .setUnlocalizedName("baryon_modehelmet").setRegistryName("baryon_modehelmet").setCreativeTab(TabModTab.tab));
		this.elements.items.add(() -> new BaryonArmor(material, EntityEquipmentSlot.CHEST)
		 .setUnlocalizedName("baryon_modebody").setRegistryName("baryon_modebody").setCreativeTab(TabModTab.tab));
		this.elements.items.add(() -> new BaryonArmor(material, EntityEquipmentSlot.LEGS)
		 .setUnlocalizedName("baryon_modelegs").setRegistryName("baryon_modelegs").setCreativeTab(TabModTab.tab));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(helmet, 0, new ModelResourceLocation("narutomod:baryon_modehelmet", "inventory"));
		ModelLoader.setCustomModelResourceLocation(body, 0, new ModelResourceLocation("narutomod:baryon_modebody", "inventory"));
		ModelLoader.setCustomModelResourceLocation(legs, 0, new ModelResourceLocation("narutomod:baryon_modelegs", "inventory"));
	}

	public class BaryonArmor extends ItemArmor {
		public BaryonArmor(ItemArmor.ArmorMaterial material, EntityEquipmentSlot slot) {
			super(material, 0, slot);
		}

		@Override
		public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
			super.onArmorTick(world, player, stack);
			if (!world.isRemote && !canUseBaryonMode(player)) {
				player.setItemStackToSlot(this.armorType, ItemStack.EMPTY);
				if (!player.inventory.addItemStackToInventory(stack.copy())) {
					player.dropItem(stack.copy(), false);
				}
				stack.setCount(0);
				player.sendMessage(new TextComponentString(TextFormatting.RED + "Only OPs can use Baryon Mode."));
			}
		}

		private boolean canUseBaryonMode(EntityPlayer player) {
			return player.canUseCommand(4, "narutomod.baryon_mode");
		}

		@Override
		@SideOnly(Side.CLIENT)
		public ModelBiped getArmorModel(EntityLivingBase living, ItemStack stack, EntityEquipmentSlot slot, ModelBiped defaultModel) {
			ModelBaryonMode armorModel = ItemBaryonMode.this.model;
			armorModel.isSneak = living.isSneaking();
			armorModel.isRiding = living.isRiding();
			armorModel.isChild = living.isChild();
			return armorModel;
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
			return "narutomod:textures/barion_kurama.png";
		}

		@Override
		public int getMaxDamage() {
			return 0;
		}

		@Override
		public boolean isDamageable() {
			return false;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
			super.addInformation(stack, worldIn, tooltip, flagIn);
			tooltip.add(TextFormatting.DARK_RED + "Baryon-inspired RP visual armor");
			tooltip.add(TextFormatting.GRAY + "Animated ears, chakra mantle, and tails.");
		}
	}

	@SideOnly(Side.CLIENT)
	public static class ModelBaryonMode extends ModelBiped {
		private final ModelRenderer earRight;
		private final ModelRenderer earLeft;
		private final ModelRenderer browRight;
		private final ModelRenderer browLeft;
		private final ModelRenderer chestAura;
		private final ModelRenderer backFlame;
		private final ModelRenderer[] tails = new ModelRenderer[9];
		private final ModelRenderer[] tailTips = new ModelRenderer[9];

		public ModelBaryonMode() {
			this.textureWidth = 100;
			this.textureHeight = 100;

			this.bipedHead = new ModelRenderer(this);
			this.bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);
			this.bipedHead.cubeList.add(new ModelBox(this.bipedHead, 0, 0, -4.0F, -8.0F, -4.0F, 8, 8, 8, 0.32F, false));

			this.bipedHeadwear = new ModelRenderer(this);
			this.bipedHeadwear.setRotationPoint(0.0F, 0.0F, 0.0F);

			this.earRight = new ModelRenderer(this);
			this.earRight.setRotationPoint(-3.2F, -7.4F, -0.6F);
			this.bipedHead.addChild(this.earRight);
			setRotationAngle(this.earRight, -0.1745F, 0.0F, -0.5585F);
			this.earRight.cubeList.add(new ModelBox(this.earRight, 32, 0, -1.0F, -5.6F, -1.0F, 2, 6, 2, 0.0F, false));

			this.earLeft = new ModelRenderer(this);
			this.earLeft.setRotationPoint(3.2F, -7.4F, -0.6F);
			this.bipedHead.addChild(this.earLeft);
			setRotationAngle(this.earLeft, -0.1745F, 0.0F, 0.5585F);
			this.earLeft.cubeList.add(new ModelBox(this.earLeft, 32, 0, -1.0F, -5.6F, -1.0F, 2, 6, 2, 0.0F, true));

			this.browRight = new ModelRenderer(this);
			this.browRight.setRotationPoint(-2.3F, -4.3F, -4.35F);
			this.bipedHead.addChild(this.browRight);
			setRotationAngle(this.browRight, 0.0F, 0.0F, 0.3142F);
			this.browRight.cubeList.add(new ModelBox(this.browRight, 40, 0, -2.0F, -0.4F, -0.3F, 4, 1, 1, 0.0F, false));

			this.browLeft = new ModelRenderer(this);
			this.browLeft.setRotationPoint(2.3F, -4.3F, -4.35F);
			this.bipedHead.addChild(this.browLeft);
			setRotationAngle(this.browLeft, 0.0F, 0.0F, -0.3142F);
			this.browLeft.cubeList.add(new ModelBox(this.browLeft, 40, 0, -2.0F, -0.4F, -0.3F, 4, 1, 1, 0.0F, true));

			this.bipedBody = new ModelRenderer(this);
			this.bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);
			this.bipedBody.cubeList.add(new ModelBox(this.bipedBody, 0, 16, -4.0F, 0.0F, -2.0F, 8, 12, 4, 0.35F, false));

			this.chestAura = new ModelRenderer(this);
			this.chestAura.setRotationPoint(0.0F, 2.0F, -2.35F);
			this.bipedBody.addChild(this.chestAura);
			this.chestAura.cubeList.add(new ModelBox(this.chestAura, 52, 20, -5.0F, -1.0F, -0.1F, 10, 11, 1, 0.0F, false));

			this.backFlame = new ModelRenderer(this);
			this.backFlame.setRotationPoint(0.0F, 1.0F, 2.7F);
			this.bipedBody.addChild(this.backFlame);
			setRotationAngle(this.backFlame, 0.2094F, 0.0F, 0.0F);
			this.backFlame.cubeList.add(new ModelBox(this.backFlame, 53, 55, -7.0F, -3.0F, 0.0F, 14, 16, 1, 0.0F, false));

			this.bipedRightArm = new ModelRenderer(this);
			this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
			this.bipedRightArm.cubeList.add(new ModelBox(this.bipedRightArm, 24, 16, -3.0F, -2.0F, -2.0F, 4, 12, 4, 0.35F, false));

			this.bipedLeftArm = new ModelRenderer(this);
			this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
			this.bipedLeftArm.cubeList.add(new ModelBox(this.bipedLeftArm, 24, 16, -1.0F, -2.0F, -2.0F, 4, 12, 4, 0.35F, true));

			this.bipedRightLeg = new ModelRenderer(this);
			this.bipedRightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);
			this.bipedRightLeg.cubeList.add(new ModelBox(this.bipedRightLeg, 0, 32, -2.0F, 0.0F, -2.0F, 4, 12, 4, 0.3F, false));

			this.bipedLeftLeg = new ModelRenderer(this);
			this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
			this.bipedLeftLeg.cubeList.add(new ModelBox(this.bipedLeftLeg, 0, 32, -2.0F, 0.0F, -2.0F, 4, 12, 4, 0.3F, true));

			for (int i = 0; i < this.tails.length; i++) {
				float yaw = (i - 4) * 0.24F;
				this.tails[i] = new ModelRenderer(this);
				this.tails[i].setRotationPoint(0.0F, 9.5F, 2.2F);
				this.bipedBody.addChild(this.tails[i]);
				setRotationAngle(this.tails[i], 0.95F, yaw, 0.0F);
				this.tails[i].cubeList.add(new ModelBox(this.tails[i], 72, 0, -0.9F, -0.7F, 0.0F, 2, 2, 12, 0.25F, false));

				this.tailTips[i] = new ModelRenderer(this);
				this.tailTips[i].setRotationPoint(0.0F, 0.0F, 11.4F);
				this.tails[i].addChild(this.tailTips[i]);
				this.tailTips[i].cubeList.add(new ModelBox(this.tailTips[i], 74, 16, -1.25F, -1.05F, -0.2F, 3, 3, 7, 0.0F, false));
			}
		}

		@Override
		public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor,
				Entity entityIn) {
			super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
			float pulse = MathHelper.sin(ageInTicks * 0.14F) * 0.08F;
			this.earRight.rotateAngleZ = -0.5585F - pulse;
			this.earLeft.rotateAngleZ = 0.5585F + pulse;
			this.backFlame.rotateAngleX = 0.2094F + MathHelper.sin(ageInTicks * 0.1F) * 0.05F;
			this.chestAura.rotateAngleX = MathHelper.sin(ageInTicks * 0.12F) * 0.025F;

			for (int i = 0; i < this.tails.length; i++) {
				float side = (i - 4) * 0.24F;
				float wave = MathHelper.sin(ageInTicks * 0.17F + i * 0.65F);
				this.tails[i].rotateAngleX = 0.9F + wave * 0.12F;
				this.tails[i].rotateAngleY = side + wave * 0.08F;
				this.tails[i].rotateAngleZ = wave * 0.05F;
				this.tailTips[i].rotateAngleX = wave * 0.16F;
			}
		}

		private static void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
			modelRenderer.rotateAngleX = x;
			modelRenderer.rotateAngleY = y;
			modelRenderer.rotateAngleZ = z;
		}
	}
}
