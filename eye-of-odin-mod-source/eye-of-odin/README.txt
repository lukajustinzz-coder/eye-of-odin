EYE OF ODIN - Fabric mod for Minecraft 26.2

WHAT IT DOES
- Equip it in the FACE slot (hover over your head slot in the inventory; needs "Trinkets Updated").
  Without Trinkets it works from the OFFHAND instead.
- Equipping gives 30 Precognition stacks.
- Mobs, players and projectiles hitting you are dodged: no damage, you teleport away, your dodge sound plays, -1 stack.
- At 0 stacks it overheats for 30 seconds (overheated sound, no dodging, no crits), then resets to 30.
- While NOT overheated, your melee hits are critical hits (x1.5 damage).
- Get it by crafting: 8 gold ingots around 1 ender eye, or: /give @s eyeofodin:eye_of_odin

BUILD (no coding)
1. Make a free account on github.com, click "New repository", name it eye-of-odin.
2. Upload ALL files from this folder (including the hidden ".github" folder) to the repository.
3. Open the "Actions" tab. The "build" job runs by itself (a few minutes).
4. Click the finished run, download "eye-of-odin-mod" at the bottom, unzip it.
5. Put the .jar in your mods folder with Fabric API and Trinkets Updated (both for 26.2).

If the build turns red, open the failed run, copy the red error text, and send it to Claude.

SETTINGS: edit the numbers at the top of src/main/java/com/eyeofodin/EyeLogic.java
