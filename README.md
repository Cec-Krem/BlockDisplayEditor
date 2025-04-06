# BlockDisplayEditor
Edit block displays easily with custom tools !
This plugin uses 1.20 block displays and interactions.

# Features
## Commands
- /bde : get a list of every command
- /bde create {block} {URL/Base64} : create a block display at your location. Can be a player head with skin, but then you MUST give a valid URL : raw/normal or as Base64 (like in the vanilla way of doing). minecraft-heads.com actually gives this data in the 'for developers' section of a head.
- /bde delete <radius> : delete every block display in the given radius (max radius : 12 blocks, not recommended to delete precise block displays).
- /bde tools : get a set of tools to edit block displays (F (or your swap hands key bind if different) to switch between additional tools). Execute this command again to get your inventory back.
- /bde info : get informations about the plugin.

## Tools
Sneaking reverses the process of tools, except for the rotation reset, clone, drag, lock and deletion tool.
- Move (X,Y,Z) : works as if you were teleporting the block display 1 pixel away in the given axis (1/16 of a block).
- Rotation (X,Y,Z) : rotates the block display according to the given axis. The use of quaternions prevents gimbal locks, but can feel unusual to new quaternions users.
- Scale (X,Y,Z,All) : changes the scale of the block display in the given axis. Max size is 5 and minimum is -5 (yes, reversed blocks work). Changes half a pixel by half a pixel (1/32 of a block).
- Reset Scale : self-explanatory, resets the scale of the aimed block display (and interaction height goes back to 1).
- Brightness (Block, Sky) : changes the brightness of the block display, between 0 and 15 for each type of light.
- Move (X,Y,Z) (Double Precision) : same as the Move tool but teleports the block displays 1/32 of a block (0.5 pixel) away in the given axis.
- Reset Rotation : self-explanatory, in case you want to reset the rotation.
- Clone Block Display : clones the aimed block display (including its interaction box) on top of itself.
- Delete : self-explanatory, deletes the aimed block display (and interaction).
- Shrink Interaction : reduce or expand the interaction hitbox (1/32 of a block to 2 blocks wide).
- Ray Drag : moves the clicked block display (with Double Precision, so 1/32 of a block) wherever you look at. Good to move a block display on greater distances.
- Lock/Unlock : locks or unlocks a block display ; when locked, the block display cannot be modified or deleted with tools or commands (from this plugin) and makes different particles when players have the tools on them (scrape particles).

## Permissions
Every command as well as the tools need permissions to be used. Everything is detailed in the plugin.yml file.

# Questions
## Why is the interaction only moving with the move tool ?
Because interactions are basically hitboxes, it is not possible (to my knowledge) to make them rotate or not symmetrical (width is always equal to depth), so it's why I made it teleport with the move tool, and could only change the height with the Scale (Y) tool.

## You should do X or Y
You can pull issues or requests here, but I won't see them immediately, or contact me on discord : ceckrem.

# I'm also on Spigot now !
https://www.spigotmc.org/resources/block-display-editor.119601/
