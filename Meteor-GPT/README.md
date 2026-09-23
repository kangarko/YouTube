Meteor-GPT is a cinematic, reversible meteor strike for Bukkit and Spigot servers from 1.8.8 through 26.3. The same jar uses public Bukkit APIs throughout, with capability checks for API changes. It has no runtime dependencies and contains no server internals.

Copy `target/Meteor-GPT.jar` into the server's `plugins` folder and restart the server. A player can look at a solid block within 64 blocks and run `/meteor`. Choose Fire, Ice, or Void, select the power, and click Confirm strike. Fire and power 3 are selected initially. Closing the menu cancels the selection.

The strike runs for 18 seconds: a countdown and personal night sky, a descending meteor, suspended debris, an expanding shockwave, an elemental finale, and progressive reconstruction. The largest crater has a radius of 7 blocks. Debris uses invisible armor stands carrying block models, so it cannot settle into blocks or drop items.

Block snapshots preserve the original materials and states. Container inventories are copied independently, including item metadata; legacy signs receive their saved text after the sign block is recreated. Neighboring block physics, fluid flow, inventory transfers, and interactions are held while the strike runs. Living entities receive temporary damage protection and safe positioning when the terrain returns. Previous personal time settings are restored. Disabling the plugin restores the terrain synchronously, removes the visual entities, and releases its chunk tickets.

Build with `mvn clean package`. The compiler targets Java 8 bytecode against `spigot-api:1.8.8-R0.1-SNAPSHOT`; the server's own Java requirement still applies. The resulting jar is `target/Meteor-GPT.jar`.

The recorded test results and their scope are in `TESTING.md`. The reproducible test harness is in the workspace's `working/meteor-gpt` directory. Its temporary server instrument and protocol adapters are separate from the distributed jar.
