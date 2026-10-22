# ATLAS HES HUD startup — v0.8.9.37

## Audio analysis

Source: `TIMELOCKED Vol 1. OST - Siren Sound.mp3`, 210.887 seconds, stereo 44.1 kHz. Analysis used local speech recognition, word timestamps, and cross-checks with small/base/medium English models. This is a machine-assisted transcription, not a human-confirmed verbatim transcript. Music and processed speech obscure several words.

| Source time (approx.) | Spoken content / interpretation | HUD beat |
| --- | --- | --- |
| 0:00–0:11.8 | “Welcome to the H.E.[B/V] Mark 6 primary protective system for use in hazardous environments.” The suit acronym is uncertain; models favor H.E.B., while the exact spoken letter is not confirmed. | Visor / system identification |
| 0:13.06–0:15.48 | “Primary systems online.” | Primary systems |
| 0:19.00–0:21.38 | “Weapon systems check.” Final inflection may be “checked.” | Weapon interface |
| 0:23.00–0:27.52 | Interpreted as ammunition/munitions monitoring systems coming online. Recognition repeatedly renders “And musicians…”; this is not a confirmed exact quotation. | Munitions monitor |
| 0:30.00–0:33.52 | “Vital … monitoring … system … check.” Words between these phrases are uncertain under the music. | Vital monitor |
| 0:36.00–0:39.54 | “Trauma response system[s] online.” | Trauma response |
| 0:40.86–0:44.22 | “Have a very safe day.” | Startup complete |

The full-track scan found no additional corroborated startup speech after this section. An isolated end-of-track “you” recognition had high no-speech probability and was discarded rather than turned into a cue.

The supplied recording has not been re-voiced or relabeled audibly as ATLAS. Its original spoken suit name is preserved. The game overlay is titled ATLAS / HES. A future ATLAS-specific recording can replace it without inventing a transcription here.

## Implementation

The first 44.8 seconds are packaged as a mono, streamed Vorbis clip. It retains the recording's backing music; it is not an isolated voice stem. A 0.6-second fade follows the last spoken line. The remaining OST is not played at every suit-up.

- Begins when all four actual Everleigh ATLAS HES items become equipped, in any order, after initial equipment synchronization.
- Other titanium/space armor, or a partial ATLAS set, does not trigger the full boot.
- A single local, non-positional voice instance plays through the Voice/Speech volume category. It is not broadcast to nearby players.
- Seven diagnostic rows follow the source timestamps. A progress meter and subtle edge scan replace the ordinary ATLAS instruments during boot; the normal HUD returns at completion.
- Live HP and oxygen are read from the player when the vitals phase starts. No fake ammunition count, medical treatment, or extra protection is granted by a diagnostic label.
- The objective panel temporarily yields to the startup and returns afterward. Its server-driven quest state is unchanged.
- Removing any required armor piece cancels startup and its audio. Re-equipping starts a new sequence, not overlapping sound instances.
- Joining already suited does not replay. Dimension transfers, death, spectators, and disconnection stop/reset the presentation rather than stacking/replaying it.
- A suit-up during a locked intro/portal/jump cinematic waits until that scene ends if the suit remains equipped. A cinematic interrupting an already-running boot cancels it rather than replaying speech.
- F1 hides the visuals but does not mute the recording. Single-player pause suspends progression. The startup never locks movement or the camera and does not delay actual survival protection.

## Geometry issues remain open

This is an ATLAS presentation update, not a geometry repair. The narrow SCC hallway, bypassable SCC vestibule, and the additional SARC entrance/glass-partition bypass shown in chat are not claimed fixed. Existing generated blocks and saved story milestones are not rewritten by this update.

## Verification limits

Production compilation, native dedicated-server regressions, deterministic presentation-state tests, audio-container/duration checks, installer tests, and layout mockup inspection are separate evidence. None substitutes for a rendered Minecraft client playthrough or an in-game listening test. Exact voice wording at the marked uncertain phrases still warrants user confirmation.
