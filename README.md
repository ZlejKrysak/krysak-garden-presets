# Krysak Garden Presets

A client-side Fabric mod for Minecraft 26.1.2 that saves camera angles (Yaw and Pitch) as named presets
and reloads them with a smooth camera rotation. Opened with `/presets`.

## Features

- Preset manager with a clean dark UI
- Categories per crop with Minecraft item icons
- Create, edit, rename and delete presets
- Search presets by name or crop
- "Use my current view" button
- Smooth rotation with adjustable duration (100 - 5000 ms)
- Presets saved locally as JSON (`krysak-garden-presets.json` in the config folder)

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.0 or newer
- Fabric API
- Java 25

## Installation

1. Install Fabric Loader and Fabric API.
2. Put the mod `.jar` into your `mods` folder.
3. Lunar Client: choose 26.1.2 with the Fabric add-on, open the Mods tab and drag the `.jar` in.

## Building

    ./gradlew build

The jar is created in `build/libs/` (use the one without `-sources`).

## Notice about servers

The mod rotates your camera automatically when you press Load. Some multiplayer servers restrict automation
and client-side modifications. Check the rules of the server before using it there. You are responsible for
how you use the mod.

Not affiliated with Mojang, Microsoft or Lunar Client.

## License

MIT (see LICENSE)