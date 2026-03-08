package net.qbismx.hptag.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class ModTagLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public ModTagLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       T entity,
                       float v, float v1,
                       float partialTick,
                       float v3, float v4, float v5) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if(entity.distanceToSqr(mc.player) > 400) return;

        renderTag(mc, entity, poseStack, buffer, packedLight, partialTick);

    }

    private void renderTag(Minecraft mc,
                           T entity,
                           PoseStack poseStack,
                           MultiBufferSource buffer,
                           int packedLight,
                           float partialTick) {

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        Font font = mc.font;

        double d0 = dispatcher.distanceToSqr(entity);

        if (!net.neoforged.neoforge.client.ClientHooks.isNameplateInRenderDistance(entity, d0)) return;

        Vec3 vec3 = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));

        if(vec3 == null) return;

        poseStack.pushPose(); // 座標変換のスタックを書き加える

        // 頭の上に移動
        // poseStack.translate(0, -entity.getBbHeight() + 0.5, 0); やや上すぎる
        // poseStack.translate(0, -entity.getBbHeight(), 0); やや上すぎる。上と変化なし？
        // poseStack.translate(0.0D, entity.getBbHeight() + 0.5D, 0.0D); 足元の下の下に表示される
        // poseStack.translate(0, entity.getEyeHeight(), 0); 足元のちょっと下に表示される
        // poseStack.translate(0, -entity.getEyeHeight() , 0); やや上すぎる
        // poseStack.translate(0, -entity.getBbHeight(), 0); // ハスクに対してはやや上すぎるが、馬に対してはちょうどいいかも
        // poseStack.translate(0, entity.getBbHeight() - entity.getEyeHeight() + 0.5, 0); // ズボン辺りに表示されている
        // poseStack.translate(0, 0.5, 0); // ズボン辺りに表示されている
        // poseStack.translate(0, 1, 0); // 0.5の時より下がっている
        // poseStack.translate(0, -(entity.getBbHeight()*0.5), 0); // ちょうど頭に乗っている感じの高さ
        // poseStack.translate(0, -(entity.getBbHeight()*0.5) + 0.5, 0); // 頭の位置
        // poseStack.translate(0, -(entity.getBbHeight()*0.5) - 0.5, 0); // ハスクや馬にはちょうど良いが、ウォーデンの頭に埋まる
        poseStack.translate(0, -(entity.getBbHeight()*0.5) - (entity.getEyeHeight() * 0.4), 0); //　ハスクやウォーデンの高さに適している

        // カメラ方向に回転したい
        Matrix4f matrix = poseStack.last().pose();
        Quaternionf rot = matrix.getNormalizedRotation(new Quaternionf());
        poseStack.mulPose(new Quaternionf(rot).conjugate()); // 逆回転を加えることで、ネームタグの向きの固定化に成功

        // カメラ回転を適用
        poseStack.mulPose(dispatcher.cameraOrientation());

        // 文字サイズ (正常)
        poseStack.scale(0.025F, -0.025F, 0.025F);

        Component name = entity.getDisplayName();
        Component hp = Component.literal("HP: " + (int) entity.getHealth());

        float x1 = -font.width(name) / 2f;
        float x2 = -font.width(hp) / 2f;

        int width = Math.max(font.width(name), font.width(hp));
        int height = 20; // 2行分

        // 後ろの背景(黒い四角)の設定
        int padding = 2;

        float left = -width / 2f - padding;
        float right = width / 2f + padding;
        float top = -padding;
        float bottom = height + padding;

        VertexConsumer vc = buffer.getBuffer(RenderType.gui());

        // z:-0.01fにして、後ろに置かないと文字の描画がびりびりする。
        vc.addVertex(matrix, left, bottom, -0.01f).setColor(0,0,0,140);
        vc.addVertex(matrix, right, bottom, -0.01f).setColor(0,0,0,140);
        vc.addVertex(matrix, right, top, -0.01f).setColor(0,0,0,140);
        vc.addVertex(matrix, left, top, -0.01f).setColor(0,0,0,140);

        // 文字の表示
        font.drawInBatch(name, x1, 0, 0xFFFFFFFF, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        font.drawInBatch(hp, x2, 10, 0xFF55FF55, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();

    }

}
