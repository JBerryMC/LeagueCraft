scoreboard players operation @s aj.id = .aj.last_id aj.id
tag @s remove aj.new
function #animated_java:lol.structure.nexus/on_summon/as_rig_entities
execute if entity @s[tag=aj.lol.structure.nexus.bone] run function #animated_java:lol.structure.nexus/zzzzzzzz/on_summon/as_bones

