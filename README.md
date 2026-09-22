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

Useful properties:

- `-Dfemalegender.autotest.menuWait=300` — ticks to wait at the main menu before creating the
  world; large modpacks replace the main menu and need longer.
- `-Dfemalegender.autotest.world=NAME` — world folder to use.

## License

Upstream is GNU LGPLv3; this port keeps that license. See [LICENSE](./LICENSE).
