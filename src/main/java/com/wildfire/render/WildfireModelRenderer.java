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

        /** Constructor for meshes that build their vertices directly rather than from a cuboid. */
        protected ModelBox(float x1, float y1, float z1, float x2, float y2, float z2, TexturedQuad[] quads) {
            this.posX1 = x1;
            this.posY1 = y1;
            this.posZ1 = z1;
            this.posX2 = x2;
            this.posY2 = y2;
            this.posZ2 = z2;
            this.quads = quads;
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

    /**
     * One breast as a closed, rounded volume that intersects the chest instead of sitting on it.
     *
     * <p>Shape, size and position are measured off a reference model rather than guessed: fitting a
     * drooped ellipsoid to Dexio's Basic MC Female reproduces its surface to 0.08 px RMS, so the
     * constants below <em>are</em> that model, expressed in model pixels. The centre sits just in front
     * of the chest plane and the back half stays buried in the torso, which is what keeps the silhouette
     * round from every angle -- an open cap would show its rim as soon as the bust cleared the body
     * edge.</p>
     *
     * <p>The visible half is a square grid bent onto the ellipsoid rather than the usual rings and
     * poles. A pole puts every quad of the tip into one fan, and a fan of long thin triangles is exactly
     * where flat-looking creases show up; an even grid of near-square cells shades cleanly instead.</p>
     */
    public static class RoundBreastModelBox extends ModelBox {

        /** Reference bust at full size, in model pixels, relative to the breast box origin. */
        private static final float CENTER_X = 1.87F;
        private static final float CENTER_Y = 3.09F;
        private static final float CENTER_Z = -0.46F;
        private static final float RADIUS_X = 2.45F;
        private static final float RADIUS_Y = 2.81F;
        private static final float RADIUS_Z = 2.63F;

        /** How far the mass hangs below the centre at its most forward point. */
        private static final float DROOP = 0.46F;

        /** Cells per side of the front grid. */
        private static final int GRID = 10;

        /** How much of the true curvature reaches the shading; see {@link #normal}. */
        private static final float SHADING_ROUNDNESS = 1F;

        /** How far round the back the hidden collar reaches, and where its closing hub sits. */
        private static final float BACK_ANGLE = 40F;
        private static final float MAX_BACK_Z = 1.2F;

        public RoundBreastModelBox(int tW, int tH, float texU, float texV, boolean left, float scale, float inflate) {
            super(centerX(left) - radius(RADIUS_X, scale, inflate), CENTER_Y - radius(RADIUS_Y, scale, inflate),
                    CENTER_Z * scale - radius(RADIUS_Z, scale, inflate),
                    centerX(left) + radius(RADIUS_X, scale, inflate),
                    CENTER_Y + radius(RADIUS_Y, scale, inflate) + DROOP * scale,
                    MAX_BACK_Z,
                    buildQuads(tW, tH, texU, texV, centerX(left), CENTER_Y, CENTER_Z * scale,
                            radius(RADIUS_X, scale, inflate), radius(RADIUS_Y, scale, inflate),
                            radius(RADIUS_Z, scale, inflate), DROOP * scale));
        }

        private static float centerX(boolean left) {
            return left ? -CENTER_X : CENTER_X;
        }

        /** Layers inflate along the surface normal, so the radius grows the same amount in every direction. */
        private static float radius(float base, float scale, float inflate) {
            return base * scale + inflate;
        }

        private static TexturedQuad[] buildQuads(int tW, int tH, float texU, float texV,
                float cx, float cy, float cz, float a, float b, float c, float droop) {
            PositionTextureVertex[][] front = new PositionTextureVertex[GRID + 1][GRID + 1];
            for (int row = 0; row <= GRID; row++) {
                float v = 2F * row / GRID - 1F;
                for (int column = 0; column <= GRID; column++) {
                    float u = 2F * column / GRID - 1F;
                    // Square to disc: the grid keeps its rows and columns, but its outer ring lands on
                    // the rim of the bust instead of on a square's corners.
                    float dx = u * (float) Math.sqrt(Math.max(0D, 1D - v * v / 2D));
                    float dy = v * (float) Math.sqrt(Math.max(0D, 1D - u * u / 2D));
                    front[row][column] = surface(tW, tH, texU, texV, cx, cy, cz, a, b, c, droop, dx, dy);
                }
            }

            PositionTextureVertex[] rim = borderLoop(front);
            float backCos = (float) Math.cos(Math.toRadians(BACK_ANGLE));
            float backSin = (float) Math.sin(Math.toRadians(BACK_ANGLE));
            PositionTextureVertex[] collar = new PositionTextureVertex[rim.length];
            for (int i = 0; i < rim.length; i++) {
                // The rim already sits on the unit circle, so the same direction scaled by the cosine
                // carries it round the back of the ellipsoid.
                float dx = (rim[i].x - cx) / a;
                float dy = (rim[i].y - cy) / b;
                collar[i] = surfaceBack(tW, tH, texU, texV, cx, cy, cz, a, b, c,
                        dx * backCos, dy * backCos, backSin);
            }

            TexturedQuad[] quads = new TexturedQuad[GRID * GRID + 2 * rim.length];
            int index = 0;
            for (int row = 0; row < GRID; row++) {
                for (int column = 0; column < GRID; column++) {
                    // Down a column first, then across: that order faces the quads at the camera
                    quads[index++] = new TexturedQuad(new PositionTextureVertex[] {
                            front[row][column], front[row + 1][column],
                            front[row + 1][column + 1], front[row][column + 1] });
                }
            }
            for (int i = 0; i < rim.length; i++) {
                int next = (i + 1) % rim.length;
                quads[index++] = new TexturedQuad(new PositionTextureVertex[] {
                        rim[i], rim[next], collar[next], collar[i] });
            }

            // Closing fan, buried in the torso: only there so nothing shows through when the bust
            // clears the body edge under cleavage, armor or a body rotation.
            PositionTextureVertex hub = new PositionTextureVertex(cx, cy, Math.min(cz + c, MAX_BACK_Z),
                    (texU + 2F) / tW, (texV + 2.5F) / tH).withNormal(0F, 0F, 1F);
            for (int i = 0; i < collar.length; i++) {
                quads[index++] = new TexturedQuad(new PositionTextureVertex[] {
                        collar[i], collar[(i + 1) % collar.length], hub, hub });
            }
            return quads;
        }

        /**
         * A point on the front of the bust from its position across the chest plane.
         *
         * @param dx horizontal position within the bust, -1 to 1
         * @param dy vertical position within the bust, -1 to 1
         */
        private static PositionTextureVertex surface(int tW, int tH, float texU, float texV,
                float cx, float cy, float cz, float a, float b, float c, float droop, float dx, float dy) {
            float dz = (float) Math.sqrt(Math.max(0D, 1D - dx * dx - dy * dy));
            // Straight-on projection: a skin's chest reads exactly as it does on the flat body, and the
            // droop moves the mass without dragging the texture down with it.
            float u = texU + 4F * (0.5F + 0.5F * dx);
            float v = texV + 5F * (0.5F + 0.5F * dy);
            float[] n = normal(a, b, c, droop, dx, dy, dz);
            return new PositionTextureVertex(cx + a * dx, cy + b * dy + droop * dz, cz - c * dz, u / tW, v / tH)
                    .withNormal(n[0], n[1], n[2]);
        }

        /** As {@link #surface}, for the hidden half behind the chest plane, which never droops. */
        private static PositionTextureVertex surfaceBack(int tW, int tH, float texU, float texV,
                float cx, float cy, float cz, float a, float b, float c, float dx, float dy, float back) {
            float u = texU + 4F * (0.5F + 0.5F * dx);
            float v = texV + 5F * (0.5F + 0.5F * dy);
            float[] n = normal(a, b, c, 0F, dx, dy, -back);
            return new PositionTextureVertex(cx + a * dx, cy + b * dy, Math.min(cz + c * back, MAX_BACK_Z),
                    u / tW, v / tH)
                    .withNormal(n[0], n[1], n[2]);
        }

        /**
         * Outward normal of the drooped ellipsoid: the gradient of its implicit form, leant back towards
         * the chest's own normal by {@link #SHADING_ROUNDNESS}.
         *
         * <p>The body around the bust is flat-shaded boxes lit as one value per face. A fully round
         * normal spans that whole range on its own, which is what makes a sphere read as pasted onto the
         * chest rather than part of it; damping it keeps the silhouette round while the shading stays in
         * the same register as the torso beside it.</p>
         */
        private static float[] normal(float a, float b, float c, float droop, float dx, float dy, float dz) {
            float nx = dx / a;
            float ny = dy / b;
            float nz = -(dz - dy * droop / b) / c;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length == 0F) {
                return new float[] { 0F, 0F, -1F };
            }
            nx = SHADING_ROUNDNESS * nx / length;
            ny = SHADING_ROUNDNESS * ny / length;
            nz = SHADING_ROUNDNESS * nz / length - (1F - SHADING_ROUNDNESS);
            float blended = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (blended == 0F) {
                return new float[] { 0F, 0F, -1F };
            }
            return new float[] { nx / blended, ny / blended, nz / blended };
        }

        /** The grid's outer ring, walked as one closed loop. */
        private static PositionTextureVertex[] borderLoop(PositionTextureVertex[][] grid) {
            PositionTextureVertex[] loop = new PositionTextureVertex[4 * GRID];
            int index = 0;
            for (int column = 0; column < GRID; column++) {
                loop[index++] = grid[0][column];
            }
            for (int row = 0; row < GRID; row++) {
                loop[index++] = grid[row][GRID];
            }
            for (int column = GRID; column > 0; column--) {
                loop[index++] = grid[GRID][column];
            }
            for (int row = GRID; row > 0; row--) {
                loop[index++] = grid[row][0];
            }
            return loop;
        }
    }


    public static class PositionTextureVertex {

        public final float x, y, z;
        public final float texturePositionX, texturePositionY;
        public final float normalX, normalY, normalZ;
        public final boolean hasNormal;

        public PositionTextureVertex(float x, float y, float z, float texU, float texV) {
            this(x, y, z, texU, texV, 0F, 0F, 0F, false);
        }

        private PositionTextureVertex(float x, float y, float z, float texU, float texV,
                float normalX, float normalY, float normalZ, boolean hasNormal) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.texturePositionX = texU;
            this.texturePositionY = texV;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
            this.hasNormal = hasNormal;
        }

        public PositionTextureVertex withTexturePosition(float texU, float texV) {
            return new PositionTextureVertex(x, y, z, texU, texV, normalX, normalY, normalZ, hasNormal);
        }

        public PositionTextureVertex withNormal(float x, float y, float z) {
            return new PositionTextureVertex(this.x, this.y, this.z, texturePositionX, texturePositionY,
                    x, y, z, true);
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

        /**
         * A quad whose vertices already carry their own UVs and normals, used by curved meshes.
         *
         * <p>The face normal is derived from the vertices themselves and only serves as the fallback for
         * a degenerate corner; the smooth per-vertex normals are what actually shade the surface.</p>
         */
        public TexturedQuad(PositionTextureVertex[] positionsIn) {
            if (positionsIn.length != 4) {
                throw new IllegalArgumentException("Wrong number of vertices. Expected: 4, Received: " + positionsIn.length);
            }
            this.vertexPositions = positionsIn;

            PositionTextureVertex p0 = positionsIn[0];
            PositionTextureVertex p1 = positionsIn[1];
            PositionTextureVertex p2 = positionsIn[2];
            float ax = p1.x - p0.x;
            float ay = p1.y - p0.y;
            float az = p1.z - p0.z;
            float bx = p2.x - p0.x;
            float by = p2.y - p0.y;
            float bz = p2.z - p0.z;
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length == 0F) {
                this.normalX = 0F;
                this.normalY = 0F;
                this.normalZ = -1F;
            } else {
                this.normalX = nx / length;
                this.normalY = ny / length;
                this.normalZ = nz / length;
            }
        }
    }
}
