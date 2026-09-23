The same Meteor-GPT jar was exercised on isolated copies of PandaSpigot 1.8.8 and Paper 26.3, each bound to a random local port. The original server directories and Mineflayer installation were not modified.

The final matrix passed 404 assertions across 28 strikes and interruptions, with 6980 complete server ticks measured. No measured strike tick exceeded 50 ms.

| Server | Measured ticks | Maximum complete tick |
| --- | ---: | ---: |
| 1.8.8 | 3493 | 43.720292 ms |
| 26.3 | 3487 | 28.018583 ms |

The public ServerTickEndEvent supplied complete tick durations, including the completion or interruption tick. Measurements apply to these isolated local servers and the recorded workload.

Each server completed Fire, Ice, and Void at power 5 on a terrain and container fixture, then repeated every element with a completely solid crater sphere containing 1419 blocks. The harness checked the empty sphere at impact, visible debris, the frozen interval, upward player motion, animal and player health, the elemental material fill, progressive reconstruction, exact final block and inventory state, sign text, and the absence of dropped items and visual entities.

The rollback comparison covered 12696 block positions. The fixture included a double chest with named items and lore, a furnace inventory, stairs with orientation, sand, and signs. On the current server it also included waterlogged stairs, a bookshelf block state, and text on the back of the sign.

Forced disable tests covered countdown, approach, frozen impact, resumed debris motion, finale, and reconstruction. Additional Ice and Void interruptions verified that a preexisting fixed personal sky setting returned exactly. Menu checks covered defaults, animation, cancellation, shift clicks, hotbar swaps, clone attempts, item dropping, dragging, and double clicks. Console use and a second concurrent strike were refused.

The game tests used the existing 1.8.8 client and the existing 26.2 client through temporary ViaVersion and ViaBackwards adapters on the 26.3 test copy. A separate temporary probe observed server state and tick duration. Particle payloads were counted at the packet boundary because the installed bot codec cannot read every particle payload; nested velocity packets were normalized in the harness. These tests were headless and did not capture rendered screenshots. The distributed plugin requires none of the test components.

The jar contains 12 plugin classes, all Java 8 bytecode, and no bundled library classes or server internal references. SHA256: `c4d49e265b821093c7c9c91c627c0e6afb6bba056826ab8bb70ef8f9e6e64f6c`.

Raw results, client logs, and server logs are retained in `../working/meteor-gpt/evidence/`. Reproduce with `prepare.py`, `build.py`, `build-probe.py`, `launch.py`, and `verify.py` in `../working/meteor-gpt/`.

The compatibility code uses the public Bukkit [block snapshot contract](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/block/BlockState.html) and [player API](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/Player.html), with capability checks for API additions and removals.

Both servers also booted successfully with Meteor-GPT as the only installed plugin. Their standalone startup and shutdown logs are archived with the evidence. All temporary server processes were stopped, their ports were verified closed, and the cloned server directories were removed.
