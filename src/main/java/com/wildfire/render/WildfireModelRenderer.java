package com.wildfire.render;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Standalone box geometry, ported from the upstream mod.
 *
 * <p>Boxes are built in model pixels and carry their own UVs, so they can be drawn straight into a
 * {@link net.minecraft.client.renderer.Tessellator} without going through {@code ModelRenderer}.</p>
 */
public class WildfireModelRenderer {

    public static class ModelBox {

        public final TexturedQuad[] quads;
        public final float posX1, posY1, posZ1;
        public final float posX2, posY2, posZ2;

        public ModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz,
                float delta, boolean mirror) {
            this(tW, tH, texU, texV, x, y, z, dx, dy, dz, delta, mirror, 5);
        }

        protected ModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz,
                float delta, boolean mirror, int quads) {
            this(tW, tH, texU, texV, x, y, z, dx, dy, dz, delta, mirror, quads, false);
        }

        protected ModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz,
                float delta, boolean mirror, int quads, boolean extra) {
            this(tW, tH, texU, texV, x, y, z, dx, dy, dz, delta, delta, delta, mirror, quads, extra);
        }

        /**
         * As above, but inflating each axis separately.
         *
         * <p>The jacket and armor copies of a breast are the same box drawn again, so every face they
         * share with the skin copy has to be pushed strictly outside it or the two z-fight. Inflating
         * does that without touching the UVs -- the same trick vanilla armor uses over the body -- and
         * keeping X at zero leaves the inner face on the body centre line, where the two breasts meet.</p>
         */
        protected ModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz,
                float deltaX, float deltaY, float deltaZ, boolean mirror, int quads, boolean extra) {
            this.posX1 = x;
            this.posY1 = y;
            this.posZ1 = z;
            this.posX2 = x + (float) dx;
            this.posY2 = y + (float) dy;
            this.posZ2 = z + (float) dz;
            this.quads = new TexturedQuad[quads];
            float f = x + (float) dx;
            float f1 = y + (float) dy;
            float f2 = z + (float) dz;
            x = x - deltaX;
            y = y - deltaY;
            z = z - deltaZ;
            f = f + deltaX;
            f1 = f1 + deltaY;
            f2 = f2 + deltaZ;
            if (mirror) {
                float f3 = f;
                f = x;
                x = f3;
            }
            initQuads(tW, tH, texU, texV, dx, dy, dz, mirror, extra,
                    new PositionTextureVertex(f, y, z, 0.0F, 8.0F),
                    new PositionTextureVertex(f, f1, z, 8.0F, 8.0F),
                    new PositionTextureVertex(x, f1, z, 8.0F, 0.0F),
                    new PositionTextureVertex(x, y, f2, 0.0F, 0.0F),
                    new PositionTextureVertex(f, y, f2, 0.0F, 8.0F),
                    new PositionTextureVertex(f, f1, f2, 8.0F, 8.0F),
                    new PositionTextureVertex(x, f1, f2, 8.0F, 0.0F),
                    new PositionTextureVertex(x, y, z, 0.0F, 0.0F));
        }

        protected void initQuads(int tW, int tH, int texU, int texV, int dx, int dy, int dz, boolean mirror,
                boolean extra, PositionTextureVertex vertex, PositionTextureVertex vertex1,
                PositionTextureVertex vertex2, PositionTextureVertex vertex3, PositionTextureVertex vertex4,
                PositionTextureVertex vertex5, PositionTextureVertex vertex6, PositionTextureVertex vertex7) {
            this.quads[0] = new TexturedQuad(texU + dz + dx, texV + dz, texU + dz + dx + dz, texV + dz + dy, tW, tH,
                    mirror, ForgeDirection.EAST, vertex4, vertex, vertex1, vertex5);
            this.quads[1] = new TexturedQuad(texU, texV + dz, texU + dz, texV + dz + dy, tW, tH,
                    mirror, ForgeDirection.WEST, vertex7, vertex3, vertex6, vertex2);
            this.quads[2] = new TexturedQuad(texU + dz, texV, texU + dz + dx, texV + dz, tW, tH,
                    mirror, ForgeDirection.DOWN, vertex4, vertex3, vertex7, vertex);
            this.quads[3] = new TexturedQuad(texU + dz, texV + dz + 4, texU + dz + dx, texV + 1 + dz + dy, tW, tH - 1,
                    mirror, ForgeDirection.UP, vertex1, vertex2, vertex6, vertex5);
            this.quads[4] = new TexturedQuad(texU + dz, texV + dz, texU + dz + dx, texV + dz + dy, tW, tH,
                    mirror, ForgeDirection.NORTH, vertex, vertex7, vertex2, vertex1);
        }
    }

    /** The jacket layer over one breast; only the outward-facing side is drawn. */
    public static class OverlayModelBox extends ModelBox {

        public OverlayModelBox(boolean isLeft, int tW, int tH, int texU, int texV, float x, float y, float z,
                int dx, int dy, int dz, float delta, boolean mirror) {
            super(tW, tH, texU, texV, x, y, z, dx, dy, dz, delta, mirror, 4, isLeft);
        }

        public OverlayModelBox(boolean isLeft, int tW, int tH, int texU, int texV, float x, float y, float z,
                int dx, int dy, int dz, float deltaX, float deltaY, float deltaZ, boolean mirror) {
            super(tW, tH, texU, texV, x, y, z, dx, dy, dz, deltaX, deltaY, deltaZ, mirror, 4, isLeft);
        }

        @Override
        protected void initQuads(int tW, int tH, int texU, int texV, int dx, int dy, int dz, boolean mirror,
                boolean isLeft, PositionTextureVertex vertex, PositionTextureVertex vertex1,
                PositionTextureVertex vertex2, PositionTextureVertex vertex3, PositionTextureVertex vertex4,
                PositionTextureVertex vertex5, PositionTextureVertex vertex6, PositionTextureVertex vertex7) {
            if (!isLeft) {
                this.quads[0] = new TexturedQuad(texU + dz + dx, texV + dz, texU + dz + dx + dz, texV + dz + dy, tW, tH,
                        mirror, ForgeDirection.EAST, vertex4, vertex, vertex1, vertex5);
            } else {
                this.quads[0] = new TexturedQuad(texU, texV + dz, texU + dz, texV + dz + dy, tW, tH,
                        mirror, ForgeDirection.WEST, vertex7, vertex3, vertex6, vertex2);
            }
            this.quads[1] = new TexturedQuad(texU + dz, texV, texU + dz + dx, texV + dz, tW, tH,
                    mirror, ForgeDirection.DOWN, vertex4, vertex3, vertex7, vertex);
            this.quads[2] = new TexturedQuad(texU + dz, texV + dz + 4, texU + dz + dx, texV + 1 + dz + dy, tW, tH - 1,
                    mirror, ForgeDirection.UP, vertex1, vertex2, vertex6, vertex5);
            this.quads[3] = new TexturedQuad(texU + dz, texV + dz, texU + dz + dx, texV + dz + dy, tW, tH,
                    mirror, ForgeDirection.NORTH, vertex, vertex7, vertex2, vertex1);
        }
    }

    /**
     * Like {@link ModelBox} but with the UVs pinned to a 4x4 side, so changing the box depth does not
     * slide the front face across the skin.
     */
    public static class BreastModelBox extends ModelBox {

        public BreastModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz,
                float delta, boolean mirror) {
            super(tW, tH, texU, texV, x, y, z, dx, dy, dz, delta, mirror);
        }

        public BreastModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz,
                float deltaX, float deltaY, float deltaZ, boolean mirror) {
            super(tW, tH, texU, texV, x, y, z, dx, dy, dz, deltaX, deltaY, deltaZ, mirror, 5, false);
        }

        @Override
        protected void initQuads(int tW, int tH, int texU, int texV, int dx, int dy, int dz, boolean mirror,
                boolean extra, PositionTextureVertex vertex, PositionTextureVertex vertex1,
                PositionTextureVertex vertex2, PositionTextureVertex vertex3, PositionTextureVertex vertex4,
                PositionTextureVertex vertex5, PositionTextureVertex vertex6, PositionTextureVertex vertex7) {
            this.quads[0] = new TexturedQuad(texU + 4 + dx, texV + 4, texU + 4 + dx + 4, texV + 4 + dy, tW, tH,
                    mirror, ForgeDirection.EAST, vertex4, vertex, vertex1, vertex5);
            this.quads[1] = new TexturedQuad(texU, texV + 4, texU + 4, texV + 4 + dy, tW, tH,
                    mirror, ForgeDirection.WEST, vertex7, vertex3, vertex6, vertex2);
            this.quads[2] = new TexturedQuad(texU + 4, texV, texU + 4 + dx, texV + 4, tW, tH,
                    mirror, ForgeDirection.DOWN, vertex4, vertex3, vertex7, vertex);
            this.quads[3] = new TexturedQuad(texU + 4, texV + 4 + 4, texU + 4 + dx, texV + 1 + 4 + dy, tW, tH - 1,
                    mirror, ForgeDirection.UP, vertex1, vertex2, vertex6, vertex5);
            this.quads[4] = new TexturedQuad(texU + 4, texV + 4, texU + 4 + dx, texV + 4 + dy, tW, tH,
                    mirror, ForgeDirection.NORTH, vertex, vertex7, vertex2, vertex1);
        }
    }

    public static class PositionTextureVertex {

        public final float x, y, z;
        public final float texturePositionX, texturePositionY;

        public PositionTextureVertex(float x, float y, float z, float texU, float texV) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.texturePositionX = texU;
            this.texturePositionY = texV;
        }

        public PositionTextureVertex withTexturePosition(float texU, float texV) {
            return new PositionTextureVertex(x, y, z, texU, texV);
        }
    }

    public static class TexturedQuad {

        public final PositionTextureVertex[] vertexPositions;
        public final float normalX, normalY, normalZ;

        public TexturedQuad(float u1, float v1, float u2, float v2, float texWidth, float texHeight, boolean mirrorIn,
                ForgeDirection direction, PositionTextureVertex... positionsIn) {
            if (positionsIn.length != 4) {
                throw new IllegalArgumentException("Wrong number of vertices. Expected: 4, Received: " + positionsIn.length);
            }
            this.vertexPositions = positionsIn;
            positionsIn[0] = positionsIn[0].withTexturePosition(u2 / texWidth, v1 / texHeight);
            positionsIn[1] = positionsIn[1].withTexturePosition(u1 / texWidth, v1 / texHeight);
            positionsIn[2] = positionsIn[2].withTexturePosition(u1 / texWidth, v2 / texHeight);
            positionsIn[3] = positionsIn[3].withTexturePosition(u2 / texWidth, v2 / texHeight);
            if (mirrorIn) {
                int i = positionsIn.length;
                for (int j = 0; j < i / 2; ++j) {
                    PositionTextureVertex vertex = positionsIn[j];
                    positionsIn[j] = positionsIn[i - 1 - j];
                    positionsIn[i - 1 - j] = vertex;
                }
            }

            this.normalX = (mirrorIn ? -1 : 1) * direction.offsetX;
            this.normalY = direction.offsetY;
            this.normalZ = direction.offsetZ;
        }
    }
}
