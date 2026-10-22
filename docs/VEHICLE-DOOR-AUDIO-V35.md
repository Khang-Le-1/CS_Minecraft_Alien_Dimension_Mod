# Truck and push-bar door recordings — v35

## Mapping

| Supplied file | Use |
| --- | --- |
| in-left.wav / in-right.wav | Successful truck entry from the corresponding side |
| out_left.wav / out_right.wav | Successful truck exit on the selected safe dismount side |
| loop.wav | Occupied truck engine at rest |
| loop_drive.wav | Moving truck engine, including reverse |
| pushbardoor_open.wav | Push-bar door opening |
| pushbardoor_close.wav | Push-bar door closing |

Truck sides follow the vehicle heading. Exit sound follows the safe dismount location, not necessarily the entry side. Canceled boarding/dismounting does not play a door clip. An empty or removed truck does not run an engine loop. These are sound changes only: there is no new truck door mesh animation, second seat or ignition control.

The idle and driving recordings loop client-side, follow the truck position, and smoothly crossfade with synchronized driving speed. They stop on exit, removal, leaving range, dimension change and disconnect. Nearby players hear occupied trucks; there is no server tick loop spam. The truck's old minecart placeholder is disabled; rover sounds are unchanged. Engine/entry/exit volume follows Minecraft's Friendly Creatures (NEUTRAL) slider.

Push-bar doors use a dedicated block-set sound type. Manual paired operation plays one recording, not two stacked clips. Opening/closing retains existing animation, collision, upper-half synchronization and egress behavior. Redstone/scripted transitions use the new type as well. Other door types are not changed. Volume follows the Blocks slider.

All eight supplied WAVs were converted to 44.1 kHz mono Ogg Vorbis for positional playback. Durations and source gain are retained; the stereo push-bar recordings are downmixed. Source hashes, output hashes, format and decode checks are included in the update verification folder.

## Installation

Minecraft 1.21.1 / NeoForge 21.1.252 / Java 21. Close Minecraft/server, back up the save, replace the previous TIMELOCKED JAR with the included JAR on both client and server, and restart fully. Keep only one TIMELOCKED JAR.

Existing trucks and push-bar doors pick up these sounds after restart. No intro replay, world rebuild or chunk deletion is required for audio. Chapter 3 and the v34 SCC circulation fixes are retained.

## Verification limits

The production build runs Spotless and Checkstyle gates. Native server tests cover sound registration, heading-relative entry choice, safe-side exit choice, canceled/duplicate interactions, engine state, rover isolation, paired-door event counts and unchanged story/geometry regressions. Asset checks cover Vorbis decoding, mono format, duration and non-silent output. These checks do not substitute for listening in a graphical client; subjective volume balance and seamlessness have not been auditioned in Minecraft.
