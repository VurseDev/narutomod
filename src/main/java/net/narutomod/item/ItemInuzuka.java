package net.narutomod.item;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.PlayerStats;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.procedure.ProcedureOnLeftClickEmpty;
import net.narutomod.procedure.ProcedureUtils;

import java.util.List;
import java.util.UUID;

@ElementsNarutomodMod.ModElement.Tag
public class ItemInuzuka extends ElementsNarutomodMod.ModElement {
	@GameRegistry.ObjectHolder("narutomod:inuzuka")
	public static final Item block = null;
	private static final int NINKEN_ID = 9320;
	public static final ItemJutsu.JutsuEnum NINKEN = new ItemJutsu.JutsuEnum(0, "ninken_companion", 'D', 80d, new NinkenJutsu()).withCustomBalance();

	public ItemInuzuka(ElementsNarutomodMod instance) {
		super(instance, 1013);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new RangedItem());
		elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityNinken.class)
		 .id(new ResourceLocation("narutomod", "ninken"), NINKEN_ID).name("ninken").tracker(64, 3, true).build());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void preInit(FMLPreInitializationEvent event) {
		RenderingRegistry.registerEntityRenderingHandler(EntityNinken.class,
		 renderManager -> new RenderNinken(renderManager));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(block, 0, new ModelResourceLocation("narutomod:inuzuka", "inventory"));
	}

	@Override
	public void init(net.minecraftforge.fml.common.event.FMLInitializationEvent event) {
		ProcedureOnLeftClickEmpty.addQualifiedItem(block, EnumHand.MAIN_HAND);
	}

	public static class RangedItem extends ItemJutsu.Base {
		public RangedItem() {
			super(ItemJutsu.JutsuEnum.Type.INUZUKA, NINKEN);
			this.setUnlocalizedName("inuzuka");
			this.setRegistryName("inuzuka");
			this.setCreativeTab(TabCustomTabs.jutsus);
		}

		@Override
		public net.minecraft.util.EnumActionResult canActivateJutsu(ItemStack stack, ItemJutsu.JutsuEnum jutsuIn, EntityPlayer entity) {
			if (!PlayerStats.getClan(entity).equalsIgnoreCase("Inuzuka") && !entity.isCreative()) {
				return net.minecraft.util.EnumActionResult.FAIL;
			}
			return super.canActivateJutsu(stack, jutsuIn, entity);
		}

		@SideOnly(Side.CLIENT)
		@Override
		public void addInformation(ItemStack itemstack, World world, List<String> list, ITooltipFlag flag) {
			super.addInformation(itemstack, world, list, flag);
			list.add(TextFormatting.GOLD + I18n.format("tooltip.narutomod.inuzuka.clan_only"));
		}
	}

	public static class NinkenJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!(entity instanceof EntityPlayer) || entity.world.isRemote) {
				return false;
			}
			EntityPlayer player = (EntityPlayer)entity;
			float mastery = 0f;
			if (stack.getItem() instanceof ItemJutsu.Base) {
				ItemJutsu.Base item = (ItemJutsu.Base)stack.getItem();
				int required = Math.max(1, item.getRequiredXp(stack, NINKEN));
				mastery = net.minecraft.util.math.MathHelper.clamp(((float)item.getJutsuXp(stack, NINKEN) - required) / (required * 2.0f), 0f, 1f);
			}
			EntityNinken dog = EntityNinken.getForOwner(player);
			if (dog == null) {
				dog = new EntityNinken(player.world, player, mastery);
				dog.setPosition(player.posX + 1.0d, player.posY, player.posZ + 1.0d);
				player.world.spawnEntity(dog);
			} else {
				dog.setMastery(mastery);
				dog.heal(10f + mastery * 20f);
				if (dog.getDistance(player) > 24d) {
					dog.setPositionAndUpdate(player.posX + 1.0d, player.posY, player.posZ + 1.0d);
				}
			}
			ItemJutsu.setCurrentJutsuCooldown(stack, entity, 300);
			player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_WOLF_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return true;
		}
	}

	@SideOnly(Side.CLIENT)
	private static class RenderNinken extends RenderLiving<EntityNinken> {
		private static final ResourceLocation TEXTURE = new ResourceLocation("narutomod:textures/akamaru_ninken.png");

		RenderNinken(RenderManager renderManager) {
			super(renderManager, new ModelAkamaru(), 0.45f);
		}

		@Override
		protected ResourceLocation getEntityTexture(EntityNinken entity) {
			return TEXTURE;
		}

		@Override
		protected void preRenderCallback(EntityNinken entity, float partialTickTime) {
			float scale = 0.76f + entity.getMastery() * 0.24f;
			GlStateManager.scale(scale, scale, scale);
		}
	}

	@SideOnly(Side.CLIENT)
	public static class ModelAkamaru extends ModelBase {
		public final ModelRenderer root;
		public final ModelRenderer body;
		public final ModelRenderer chest;
		public final ModelRenderer neckFluff;
		public final ModelRenderer collar;
		public final ModelRenderer head;
		public final ModelRenderer muzzle;
		public final ModelRenderer jaw;
		public final ModelRenderer nose;
		public final ModelRenderer earLeft;
		public final ModelRenderer earRight;
		public final ModelRenderer earLowerLeft;
		public final ModelRenderer earLowerRight;
		public final ModelRenderer crest;
		public final ModelRenderer cheekLeft;
		public final ModelRenderer cheekRight;
		public final ModelRenderer frontLegLeft;
		public final ModelRenderer frontLegRight;
		public final ModelRenderer rearLegLeft;
		public final ModelRenderer rearLegRight;
		public final ModelRenderer frontLowerLeft;
		public final ModelRenderer frontLowerRight;
		public final ModelRenderer rearLowerLeft;
		public final ModelRenderer rearLowerRight;
		public final ModelRenderer tail;
		public final ModelRenderer tailTip;

		public ModelAkamaru() {
			this.textureWidth = 128;
			this.textureHeight = 128;

			this.root = new ModelRenderer(this);
			this.root.setRotationPoint(0f, 24f, 0f);

			this.body = new ModelRenderer(this);
			this.body.setRotationPoint(0f, -12f, 2f);
			this.body.cubeList.add(new ModelBox(this.body, 33, 17, -5.5f, -3.5f, -8f, 11, 7, 6, 0.18f, false));
			this.body.cubeList.add(new ModelBox(this.body, 35, 0, -6f, -4f, -3f, 12, 8, 7, 0.22f, false));
			this.body.cubeList.add(new ModelBox(this.body, 68, 17, -5.5f, -3.5f, 4f, 11, 7, 5, 0.18f, false));
			this.root.addChild(this.body);

			this.chest = new ModelRenderer(this);
			this.chest.setRotationPoint(0f, -1f, -7f);
			this.chest.cubeList.add(new ModelBox(this.chest, 0, 17, -4.5f, -5f, -3.5f, 9, 7, 7, 0.28f, false));
			this.chest.cubeList.add(new ModelBox(this.chest, 0, 44, -4f, 1f, -3f, 8, 4, 6, 0.15f, false));
			this.body.addChild(this.chest);

			this.neckFluff = new ModelRenderer(this);
			this.neckFluff.setRotationPoint(0f, -2f, -2f);
			this.neckFluff.cubeList.add(new ModelBox(this.neckFluff, 74, 0, -5f, -3.5f, -3.5f, 10, 7, 7, 0.32f, false));
			this.chest.addChild(this.neckFluff);

			this.collar = new ModelRenderer(this);
			this.collar.setRotationPoint(0f, -1.8f, -3.1f);
			this.collar.cubeList.add(new ModelBox(this.collar, 74, 32, -6f, -1f, -4f, 12, 2, 8, 0.48f, false));
			this.chest.addChild(this.collar);

			this.head = new ModelRenderer(this);
			this.head.setRotationPoint(0f, -5f, -4.5f);
			this.head.cubeList.add(new ModelBox(this.head, 0, 0, -4.5f, -5f, -5f, 9, 8, 8, 0.16f, false));
			this.head.cubeList.add(new ModelBox(this.head, 101, 17, -4f, -3.5f, -7f, 8, 7, 4, 0.12f, false));
			this.chest.addChild(this.head);

			this.muzzle = new ModelRenderer(this);
			this.muzzle.setRotationPoint(0f, 0f, -6.5f);
			this.muzzle.cubeList.add(new ModelBox(this.muzzle, 81, 44, -3f, -1.5f, -5f, 6, 3, 5, 0.12f, false));
			this.head.addChild(this.muzzle);

			this.jaw = new ModelRenderer(this);
			this.jaw.setRotationPoint(0f, 1.6f, -1f);
			this.jaw.cubeList.add(new ModelBox(this.jaw, 73, 55, -3.5f, 0f, -4f, 7, 2, 4, 0f, false));
			this.muzzle.addChild(this.jaw);

			this.nose = new ModelRenderer(this);
			this.nose.setRotationPoint(0f, -0.4f, -4.8f);
			this.nose.cubeList.add(new ModelBox(this.nose, 5, 64, -2f, -1.5f, -1.5f, 4, 3, 2, 0.15f, false));
			this.muzzle.addChild(this.nose);

			this.earLeft = this.ear(this.head, -4.4f, -3.8f, -2.3f, false);
			this.earRight = this.ear(this.head, 4.4f, -3.8f, -2.3f, true);
			this.earLowerLeft = this.lowerEar(this.earLeft, -0.2f, -5.5f, -0.15f, false);
			this.earLowerRight = this.lowerEar(this.earRight, 0.2f, -5.5f, -0.15f, true);

			this.crest = new ModelRenderer(this);
			this.crest.setRotationPoint(0f, -5f, -1.2f);
			this.crest.rotateAngleX = -0.12f;
			this.crest.cubeList.add(new ModelBox(this.crest, 0, 32, -1.5f, -4f, -3.5f, 3, 4, 7, 0.25f, false));
			this.head.addChild(this.crest);

			this.cheekLeft = this.cheek(this.head, -4.7f, 0f, -3.5f, false);
			this.cheekRight = this.cheek(this.head, 4.7f, 0f, -3.5f, true);

			this.frontLegLeft = this.upperLeg(this.body, -4.2f, 3f, -5.5f, false);
			this.frontLegRight = this.upperLeg(this.body, 4.2f, 3f, -5.5f, true);
			this.rearLegLeft = this.upperLeg(this.body, -4.2f, 3f, 5.3f, false);
			this.rearLegRight = this.upperLeg(this.body, 4.2f, 3f, 5.3f, true);
			this.frontLowerLeft = this.lowerLeg(this.frontLegLeft, 0f, 4.2f, 0f, false);
			this.frontLowerRight = this.lowerLeg(this.frontLegRight, 0f, 4.2f, 0f, true);
			this.rearLowerLeft = this.lowerLeg(this.rearLegLeft, 0f, 4.2f, 0f, false);
			this.rearLowerRight = this.lowerLeg(this.rearLegRight, 0f, 4.2f, 0f, true);

			// Overlapping fur plates reproduce the layered silhouette of the reference
			// model while every piece remains attached to an animated parent bone.
			this.furLayer(this.chest, 50, 44, 0f, -4.9f, -0.5f, -4f, -0.5f, -3.5f,
			 8, 1, 7, -0.18f, 0f, 0f, false);
			this.furLayer(this.chest, 40, 32, -4.2f, -2.2f, -0.8f, -2f, -1.5f, -3f,
			 2, 5, 6, 0f, -0.10f, 0.28f, false);
			this.furLayer(this.chest, 40, 32, 4.2f, -2.2f, -0.8f, 0f, -1.5f, -3f,
			 2, 5, 6, 0f, 0.10f, -0.28f, true);
			this.furLayer(this.body, 57, 32, -5.0f, -1.0f, -5.3f, -2f, -2.5f, -3f,
			 2, 5, 6, 0.10f, -0.12f, 0.24f, false);
			this.furLayer(this.body, 57, 32, 5.0f, -1.0f, -5.3f, 0f, -2.5f, -3f,
			 2, 5, 6, 0.10f, 0.12f, -0.24f, true);
			this.furLayer(this.body, 96, 55, 0f, -4.1f, -3.5f, -3f, -0.5f, -2.5f,
			 6, 1, 5, -0.22f, 0f, 0f, false);
			this.furLayer(this.body, 96, 55, 0f, -4.3f, 0.2f, -3f, -0.5f, -2.5f,
			 6, 1, 5, -0.05f, 0f, 0f, false);
			this.furLayer(this.body, 96, 55, 0f, -4.0f, 3.8f, -3f, -0.5f, -2.5f,
			 6, 1, 5, 0.16f, 0f, 0f, false);
			this.furLayer(this.body, 21, 32, -5.0f, -0.4f, 4.4f, -2f, -2f, -3.5f,
			 2, 4, 7, -0.08f, -0.12f, 0.22f, false);
			this.furLayer(this.body, 21, 32, 5.0f, -0.4f, 4.4f, 0f, -2f, -3.5f,
			 2, 4, 7, -0.08f, 0.12f, -0.22f, true);
			this.furLayer(this.chest, 0, 64, -1.7f, 3.0f, -3.1f, -0.5f, 0f, -0.5f,
			 1, 5, 1, -0.34f, 0f, 0.26f, false);
			this.furLayer(this.chest, 0, 64, 0f, 3.2f, -3.3f, -0.5f, 0f, -0.5f,
			 1, 5, 1, -0.38f, 0f, 0f, false);
			this.furLayer(this.chest, 0, 64, 1.7f, 3.0f, -3.1f, -0.5f, 0f, -0.5f,
			 1, 5, 1, -0.34f, 0f, -0.26f, true);
			this.furLayer(this.frontLowerLeft, 34, 64, 0f, 1.0f, 0f, -1.5f, -0.5f, -1.5f,
			 3, 1, 3, 0f, 0f, 0f, false);
			this.furLayer(this.frontLowerRight, 34, 64, 0f, 1.0f, 0f, -1.5f, -0.5f, -1.5f,
			 3, 1, 3, 0f, 0f, 0f, true);
			this.furLayer(this.rearLowerLeft, 34, 64, 0f, 1.0f, 0f, -1.5f, -0.5f, -1.5f,
			 3, 1, 3, 0f, 0f, 0f, false);
			this.furLayer(this.rearLowerRight, 34, 64, 0f, 1.0f, 0f, -1.5f, -0.5f, -1.5f,
			 3, 1, 3, 0f, 0f, 0f, true);

			this.tail = new ModelRenderer(this);
			this.tail.setRotationPoint(0f, -2.3f, 7.5f);
			this.tail.rotateAngleX = -0.78f;
			this.tail.cubeList.add(new ModelBox(this.tail, 29, 44, -1.5f, -1.5f, 0f, 3, 3, 7, 0.22f, false));
			this.body.addChild(this.tail);

			this.tailTip = new ModelRenderer(this);
			this.tailTip.setRotationPoint(0f, 0f, 6.4f);
			this.tailTip.rotateAngleX = -0.72f;
			this.tailTip.cubeList.add(new ModelBox(this.tailTip, 0, 55, -1.5f, -1f, 0f, 3, 2, 6, 0.18f, false));
			this.tail.addChild(this.tailTip);
			ModelRenderer tailEnd = new ModelRenderer(this);
			tailEnd.setRotationPoint(0f, 0f, 5.4f);
			tailEnd.rotateAngleX = -0.55f;
			tailEnd.cubeList.add(new ModelBox(tailEnd, 51, 55, -1f, -1f, 0f, 2, 2, 5, 0.12f, false));
			this.tailTip.addChild(tailEnd);
			this.furLayer(this.tail, 104, 44, -0.65f, 0f, 0.4f, -1.5f, -0.5f, -0.5f,
			 3, 1, 7, 0f, -0.18f, 0f, false);
			this.furLayer(this.tail, 104, 44, 0.65f, 0f, 0.4f, -1.5f, -0.5f, -0.5f,
			 3, 1, 7, 0f, 0.18f, 0f, true);
		}

		private ModelRenderer furLayer(ModelRenderer parent, int textureU, int textureV,
		 float pointX, float pointY, float pointZ, float cubeX, float cubeY, float cubeZ,
		 int width, int height, int depth, float rotateX, float rotateY, float rotateZ, boolean mirror) {
			ModelRenderer layer = new ModelRenderer(this);
			layer.setRotationPoint(pointX, pointY, pointZ);
			layer.rotateAngleX = rotateX;
			layer.rotateAngleY = rotateY;
			layer.rotateAngleZ = rotateZ;
			layer.cubeList.add(new ModelBox(layer, textureU, textureV, cubeX, cubeY, cubeZ,
			 width, height, depth, 0.08f, mirror));
			parent.addChild(layer);
			return layer;
		}

		private ModelRenderer ear(ModelRenderer parent, float x, float y, float z, boolean mirror) {
			ModelRenderer part = new ModelRenderer(this);
			part.setRotationPoint(x, y, z);
			part.rotateAngleZ = mirror ? -0.12f : 0.12f;
			part.rotateAngleX = -0.10f;
			part.cubeList.add(new ModelBox(part, 66, 55, mirror ? -1.8f : -0.2f, -6f, -0.5f, 2, 6, 1, 0.12f, mirror));
			parent.addChild(part);
			return part;
		}

		private ModelRenderer lowerEar(ModelRenderer parent, float x, float y, float z, boolean mirror) {
			ModelRenderer part = new ModelRenderer(this);
			part.setRotationPoint(x, y, z);
			part.rotateAngleZ = mirror ? 0.08f : -0.08f;
			part.cubeList.add(new ModelBox(part, 27, 64, mirror ? -1.8f : -0.2f, 0f, -0.5f, 2, 4, 1, 0.08f, mirror));
			parent.addChild(part);
			return part;
		}

		private ModelRenderer cheek(ModelRenderer parent, float x, float y, float z, boolean mirror) {
			ModelRenderer part = new ModelRenderer(this);
			part.setRotationPoint(x, y, z);
			part.rotateAngleZ = mirror ? -0.18f : 0.18f;
			part.cubeList.add(new ModelBox(part, 18, 64, mirror ? -1.8f : -0.2f, -1.5f, -1f, 2, 3, 2, 0.16f, mirror));
			parent.addChild(part);
			return part;
		}

		private ModelRenderer upperLeg(ModelRenderer parent, float x, float y, float z, boolean mirror) {
			ModelRenderer part = new ModelRenderer(this);
			part.setRotationPoint(x, y, z);
			part.cubeList.add(new ModelBox(part, 19, 55, -1.5f, -0.5f, -1.5f, 3, 5, 3, 0.18f, mirror));
			parent.addChild(part);
			return part;
		}

		private ModelRenderer lowerLeg(ModelRenderer parent, float x, float y, float z, boolean mirror) {
			ModelRenderer part = new ModelRenderer(this);
			part.setRotationPoint(x, y, z);
			part.cubeList.add(new ModelBox(part, 119, 55, -1f, 0f, -1f, 2, 4, 2, 0.12f, mirror));
			ModelRenderer paw = new ModelRenderer(this);
			paw.setRotationPoint(0f, 3.4f, -0.8f);
			paw.cubeList.add(new ModelBox(paw, 32, 55, -1.75f, 0f, -2.5f, 4, 2, 5, 0.14f, mirror));
			part.addChild(paw);
			parent.addChild(part);
			return part;
		}

		@Override
		public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
		 float netHeadYaw, float headPitch, float scale) {
			this.root.render(scale);
		}

		@Override
		public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
		 float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
			EntityNinken dog = (EntityNinken)entity;
			this.head.rotateAngleY = netHeadYaw * 0.017453292f;
			this.head.rotateAngleX = headPitch * 0.017453292f;
			this.body.rotateAngleX = 0f;
			this.chest.rotateAngleX = 0f;
			this.jaw.rotateAngleX = this.swingProgress > 0f ? 0.48f : 0f;
			float earSway = MathHelper.sin(ageInTicks * 0.12f) * 0.055f;
			this.earLeft.rotateAngleZ = 0.12f + earSway;
			this.earRight.rotateAngleZ = -0.12f - earSway;
			this.tail.rotateAngleY = MathHelper.cos(ageInTicks * 0.28f) * (dog.isSitting() ? 0.12f : 0.32f);
			this.tail.rotateAngleX = -0.78f;
			this.tailTip.rotateAngleX = -0.72f;

			float gait = MathHelper.cos(limbSwing * 0.6662f) * 1.15f * limbSwingAmount;
			this.frontLegLeft.rotateAngleX = gait;
			this.rearLegRight.rotateAngleX = gait;
			this.frontLegRight.rotateAngleX = -gait;
			this.rearLegLeft.rotateAngleX = -gait;
			this.frontLowerLeft.rotateAngleX = -Math.max(0f, gait) * 0.45f;
			this.rearLowerRight.rotateAngleX = -Math.max(0f, gait) * 0.45f;
			this.frontLowerRight.rotateAngleX = Math.min(0f, gait) * 0.45f;
			this.rearLowerLeft.rotateAngleX = Math.min(0f, gait) * 0.45f;

			if (dog.isSitting()) {
				this.body.rotateAngleX = -0.12f;
				this.chest.rotateAngleX = 0.12f;
				this.frontLegLeft.rotateAngleX = -0.08f;
				this.frontLegRight.rotateAngleX = -0.08f;
				this.rearLegLeft.rotateAngleX = -1.15f;
				this.rearLegRight.rotateAngleX = -1.15f;
				this.rearLowerLeft.rotateAngleX = 1.30f;
				this.rearLowerRight.rotateAngleX = 1.30f;
				this.tail.rotateAngleX = -0.35f;
			} else if (!dog.onGround) {
				this.body.rotateAngleX = -0.12f;
				this.frontLegLeft.rotateAngleX = -0.65f;
				this.frontLegRight.rotateAngleX = -0.65f;
				this.rearLegLeft.rotateAngleX = 0.85f;
				this.rearLegRight.rotateAngleX = 0.85f;
				this.rearLowerLeft.rotateAngleX = -1.0f;
				this.rearLowerRight.rotateAngleX = -1.0f;
			}
		}
	}

	public static class EntityNinken extends EntityWolf implements ItemJutsu.IJutsu {
		private float mastery;

		public EntityNinken(World world) {
			super(world);
			this.enablePersistence();
		}

		public EntityNinken(World world, EntityPlayer owner, float masteryIn) {
			this(world);
			this.setTamedBy(owner);
			this.setCustomNameTag(owner.getName() + "'s Ninken");
			this.setMastery(masteryIn);
		}

		public static EntityNinken getForOwner(EntityPlayer owner) {
			UUID id = owner.getUniqueID();
			for (EntityNinken dog : owner.world.getEntities(EntityNinken.class, e -> e.isEntityAlive() && id.equals(e.getOwnerId()))) {
				return dog;
			}
			return null;
		}

		public void setMastery(float masteryIn) {
			this.mastery = net.minecraft.util.math.MathHelper.clamp(masteryIn, 0f, 1f);
			IAttributeInstance maxHealth = this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
			IAttributeInstance attack = this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
			IAttributeInstance speed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
			maxHealth.setBaseValue(50d + 90d * this.mastery);
			attack.setBaseValue(4d + 10d * this.mastery);
			speed.setBaseValue(0.36d + 0.12d * this.mastery);
			if (this.getHealth() > this.getMaxHealth()) {
				this.setHealth(this.getMaxHealth());
			} else if (this.getHealth() < 1f) {
				this.setHealth(this.getMaxHealth());
			}
			this.setAIMoveSpeed((float)speed.getBaseValue());
		}

		public float getMastery() {
			return this.mastery;
		}

		@Override
		protected void applyEntityAttributes() {
			super.applyEntityAttributes();
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(50d);
			this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4d);
			this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.36d);
			this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32d);
		}

		@Override
		public void onLivingUpdate() {
			super.onLivingUpdate();
			if (!this.world.isRemote) {
				EntityLivingBase owner = this.getOwner();
				if (owner == null || !owner.isEntityAlive()) {
					return;
				}
				if (this.getDistance(owner) > 48d) {
					this.setPositionAndUpdate(owner.posX + 1d, owner.posY, owner.posZ + 1d);
				}
				EntityLivingBase target = owner.getRevengeTarget() != null ? owner.getRevengeTarget() : owner.getLastAttackedEntity();
				if (target != null && target.isEntityAlive() && !this.isOnSameTeam(target) && this.getDistance(target) < 32d) {
					this.setAttackTarget(target);
				}
				if (this.mastery >= 0.65f && this.getAttackTarget() != null && this.ticksExisted % 20 == 0) {
					this.world.spawnParticle(EnumParticleTypes.CRIT, this.posX, this.posY + 0.8d, this.posZ, 0d, 0.05d, 0d);
				}
			}
		}

		@Override
		public boolean attackEntityAsMob(Entity entityIn) {
			boolean hit = super.attackEntityAsMob(entityIn);
			if (hit && entityIn instanceof EntityLivingBase) {
				EntityLivingBase target = (EntityLivingBase)entityIn;
				if (this.mastery >= 0.45f && this.rand.nextFloat() < this.mastery * 0.35f) {
					target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 0, false, false));
					target.attackEntityFrom(DamageSource.causeMobDamage(this), 1.0f + this.mastery * 2.0f);
				}
				if (this.mastery >= 0.75f) {
					EntityLivingBase owner = this.getOwner();
					if (owner != null && owner.getDistance(target) < 5d) {
						Vec3d push = target.getPositionVector().subtract(owner.getPositionVector()).normalize().scale(0.25d);
						ProcedureUtils.addVelocity(target, push.x, 0.18d, push.z);
					}
				}
			}
			return hit;
		}

		@Override
		public boolean shouldAttackEntity(EntityLivingBase target, EntityLivingBase owner) {
			return target != null && owner != null && !target.equals(owner) && !this.isOnSameTeam(target);
		}

		@Override
		public boolean isOnSameTeam(Entity entityIn) {
			return entityIn.equals(this.getOwner()) || super.isOnSameTeam(entityIn);
		}

		@Override
		public void writeEntityToNBT(NBTTagCompound compound) {
			super.writeEntityToNBT(compound);
			compound.setFloat("mastery", this.mastery);
		}

		@Override
		public void readEntityFromNBT(NBTTagCompound compound) {
			super.readEntityFromNBT(compound);
			this.setMastery(compound.getFloat("mastery"));
		}

		@Override
		public ItemJutsu.JutsuEnum.Type getJutsuType() {
			return ItemJutsu.JutsuEnum.Type.INUZUKA;
		}
	}
}
