scoreboard players operation @s aj.id = .aj.last_id aj.id
tag @s remove aj.new
function #animated_java:lol.structure.tower/on_summon/as_rig_entities
execute if entity @s[tag=aj.lol.structure.tower.bone] run function #animated_java:lol.structure.tower/zzzzzzzz/on_summon/as_bones

