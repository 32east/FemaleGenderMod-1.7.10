# Female Gender Mod — Minecraft 1.7.10 port

A port of [Wildfire's Female Gender Mod](https://github.com/FemaleGenderMod/FemaleGenderMod) to
Minecraft 1.7.10 / Forge, built for the GT New Horizons modpack. It adds breasts with optional
physics to the player model, plus in-game screens to configure them.

Upstream only targets 1.16+, and the 1.7.10 rendering stack is a different world (immediate-mode
OpenGL instead of `MatrixStack` + `VertexConsumer`), so this is a reimplementation rather than a
patch. The geometry, physics and default values are ported line by line from upstream so it looks
and moves the same.

## Features

| | |
|---|---|
| Breast model on the player | size, separation, height, depth, rotation, single/dual physics |
| Physics | bounce and floppiness, reacting to movement, jumping, sneaking, swinging and vehicles |
| Armor | chestplates are drawn conforming to the breasts, including dyed leather |
| Jacket layer | drawn when the skin is 64×64 and the player model has a body overlay (e.g. SkinPort) |
| Configuration | `H` opens the player list → wardrobe → appearance / character settings |
| Persistence | one JSON file per player under `config/WildfireGender/` |
| Multiplayer | settings are synced through a Forge channel when the server also has the mod |
| Hurt sounds | optional female hurt sound |

Not ported: cloud sync (upstream 4.0+, needs their web service) and the breast UV editor.

### How armor affects physics

Each chest item has a *resistance* (how much it damps the bounce) and a *tightness* (how much it
compresses the chest). Physics is switched off entirely at resistance 1, which is why plate armor
looks rigid. These are upstream's values; they live in
[SimpleGenderArmor](src/main/java/com/wildfire/render/armor/SimpleGenderArmor.java).

| Chestplate | Resistance | Tightness | Physics |
|---|---|---|---|
| Leather | 0.30 | 0.50 | yes, damped |
| Chainmail | 0.50 | 0.20 | yes, damped |
| Gold | 0.85 | — | barely visible |
| Iron / Diamond | 1.00 | — | **none** |
| Anything else, including modded | 0.50 | — | yes, damped |
| Not an `ItemArmor` chest piece (elytra, backpacks) | — | — | treated as not covering the chest |

Other mods can register their own values with `WildfireHelper.addGenderArmor(item, resistance,
tightness)` or a full `IGenderArmor`.

## Building

The GTNH Gradle plugin needs a **Java 25** JVM to run, and the RetroFuturaGradle source
transformer needs an **Azul Zulu 21** toolchain. `build.sh` points `JAVA_HOME` at a local JDK 25;
override it with `FGM_JDK25`, and set the Zulu path in `org.gradle.java.installations.paths` in
`gradle.properties` if yours lives elsewhere.

```bash
./build.sh build
```

The distributable jar lands in `build/libs/femalegender-<version>.jar`. Versioning comes from git
tags, so the working tree needs at least one tag.

To drop the build straight into a modpack:

```bash
./build.sh installMod
```

It copies the **reobfuscated** jar (not the `-dev` one, which keeps MCP names and dies at runtime
with `NoSuchMethodError`) to `mods/femalegender.jar`, under a fixed name so a stale build cannot be
picked up by mistake. Point it elsewhere with `FGM_PACK_MODS`.

## Testing

`com.wildfire.debug.AutoTest` scripts the client: it creates a flat world, poses the player through
a fixed set of male/female, armored, side-on and physics frames, opens each GUI screen, writes a
screenshot for every step into `screenshots/`, then quits. Nothing runs unless
`-Dfemalegender.autotest=true` is set.

```bash
./build.sh runClient -Pautotest
```

Screenshots land in `run/client/screenshots/`. The male and female frames are captured with physics
off and a fixed camera, so they can be diffed pixel by pixel.

Besides the screenshots it also logs three things worth asserting: whether the sounds from
`sounds.json` reached the sound registry, what the hurt sound was replaced with on each side, and
that the multi-line tooltips actually split.

Useful properties:

- `-Dfemalegender.autotest.menuWait=300` — ticks to wait at the main menu before creating the
  world; large modpacks replace the main menu and need longer.
- `-Dfemalegender.autotest.world=NAME` — world folder to use.

The same run works inside a full modpack: install the mod, then launch the pack with the same
system properties. `scripts/launch_gtnh.py` does that for a TLauncher GT New Horizons install.

## License

Upstream is GNU LGPLv3; this port keeps that license. See [LICENSE](./LICENSE).
