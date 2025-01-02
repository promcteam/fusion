## Recipe-Features
The following examples provides you parameters that are used in the recipes-section of a professions recipes'.
<details>
    <summary>Show results example</summary>

```yaml
- name: <name> # How the recipe is internally called
  craftingTime: 5 # How long this item takes in seconds
  craftingLimit: 1 # How many times this recipe can be crafted by the player
  category: stone_weapons # Which category this item belongs to (only of category usage is active)
  hiding: # Hiding the recipe from the player by specific occurences
    noPermission: true # If the player has no permission to see this recipe
    noRank: true # If the player has not the rank of the conditions to see this recipe
    recipeLimitReached: true # If the player has reached the crafting limit of this recipe
  results: {...} # Shown below
  costs: {...} # Shown below
  conditions: {...} # Shown below
```

The hiding optionals are fully optional and can (and should!) be left blank if you dont wont them to override the global configuration. When using those values and global value of noPermission is set to `false`, all recipes will be hidden where a player has no permission. However, when you but noPermission on `true` in this recipe it means only this recipe is overriding this option.
By left blank we mean to really totally remove this entry from the recipes section.
</details>

## Specific Sections
### Results-Section (Recipes only)
The results section of recipes provides native support for:
- One specific item that is given
- Vanilla experience
- Profession experience
- Commands that will be executed [see more](#delayed-commands)

<details>
    <summary>Show results example</summary>

```yaml
results:
  vanillaExp: 0 # You get no vanilla experience
  item: CROSSBOW:1 # You get one crossbow
  professionExp: 25 # You get 25 profession experience
  commands: # You get a stick through a command
  - delay: 0
    as: PLAYER
    cmd: give {player} minecraft:stick 1
```
</details>

### Costs-Section (Recipes & Professions)
Costs are things that will be consumed on crafting. Without having the proper resources of the costs-section, players cant craft the desired item.
The costs section of recipes provides native support for:
- Money (if Vault is installed)
- Vanilla Experience
- Items that will be taken [see more](#working-with-ingredient--result-items)

<details>
    <summary>Show costs example (easy)</summary>

```yaml
costs:
    money: 15.0 # Crafting costs 15.0 money (using decimals is necessary!! 0 is also -> 0.0)
    exp: 3 # Crafting costs 3 vanilla experience
    items: # Crafting costs the following items
    - STICK:3
    - STRING:2
    - FLINT:2
```
</details>

<details>
    <summary>Show costs example (complex)</summary>

```yaml
costs:
    money: 15.0 # Crafting costs 15.0 money (using decimals is necessary!! 0 is also -> 0.0)
    exp: 3 # Crafting costs 3 vanilla experience
    items: # Crafting costs the following items
    - DIVINITY_cool_item:1 # 1 custom item from Divinity
    - STRING:2 # 2 Vanilla string
    - material: ENCHANTED_BOOK # An enchanted book with unbreaking 3
      amount: 1
      enchants:
        unbreaking: 3
    - material: STICK # An stick with sharpness 5 called 'Mighty Stick'
      amount: 1
      name: 'Mighty Stick'
      enchants:
        sharpness: 5
```
</details>

### Conditions-Section (Recipes & Professions)
Conditions are like costs with the difference of no consumption. 
Those are things like levels in professions, a mastery or even [supported plugins](#key-value-conditions--supported-plugins).

<details>
    <summary>Show conditions example (easy) </summary>

```yaml
    professionLevel: 15 # The profession level that needs to be achieved first (similar like under the professions-section below where you could modify the same, just for the current profession only)
    mastery: false # Rather this profession has to be mastered first
    rank: null # The rank that is used for the condition (Achievable through `craft.rank.<rank>` permissions)
    professions: {} # There are no profession conditions. (This has to be a '{}' if not used!)
```
</details>

<details>
    <summary>Show conditions example (professions included) </summary>

```yaml
    professionLevel: 0 # The profession level that needs to be achieved first (similar like under the professions-section below where you could modify the same, just for the current profession only)
    mastery: false # Rather this profession has to be mastered first
    rank: null # The rank that is used for the condition (Achievable through `craft.rank.<rank>` permissions)
    professions:
      armor_smithing: 5 # You additionally need to be armor_smithing level 5 or greater
      weapon_smithing: 15 # Here we replaced 'professionLevel' now
```
</details>

<details>
    <summary>Show conditions example (supported plugins included) </summary>

```yaml
    professionLevel: 0 # The profession level that needs to be achieved first (similar like under the professions-section below where you could modify the same, just for the current profession only)
    mastery: false # Rather this profession has to be mastered first
    rank: null # The rank that is used for the condition (Achievable through `craft.rank.<rank>` permissions)
    professions:
      armor_smithing: 5 # You additionally need to be armor_smithing level 5 or greater
      weapon_smithing: 15 # Here we replaced 'professionLevel' now
    mcmmo:
      archery: 50 # Need to have archery lvl 50 from mcMMO
    jobs:
      miner: 25 # Need to have miner lvl 25 from JobsReborn
    fabled:
      warrior: 5 # Need to have warrior lvl 5 from Fabled
    aura-ability:
        fireball: 3 # Need to have fireball lvl 3 from Aura-Ability
```
</details>

Further details on those complex condition possibilities can be seen [here](#key-value-conditions--supported-plugins).
## Others
### Delayed Commands
Delayed Commands is a configurable command created by the parameters `delay`, `as` and `cmd`:

<details>
    <summary>Show command example</summary>

```yaml
commands:
  - delay: 5 # 5 ticks delay
    as: console # The caster. Possible ones [PLAYER, CONSOLE]
    cmd: say Hello World! # The command that will be executed
```
</details>

Commands are usually provided as a list, meaning they always start 
with a `- delay: [...]` and the other two parameters will be put below on the same level as `delay`.

<details>
    <summary>Show command list example</summary>

```yaml
commands:
  - delay: 5
    as: console
    cmd: say Hello World!
  - delay: 0
    as: console
    cmd: give {player} minecraft:stick 1
```
</details>

### Working with ingredient- & result-items
#### Vanilla
Vanilla Items are simply used by making an item of the material type like `STONE`, `STICK` etc. Case-sensitivity doesnt bother here.
You can also use `VANILLA_<material>` for visual purposes which does the same.

#### Native ItemBuilder
ItemBuilder functions allow for fully customized items used in the plugin. You can use them nearly everywhere (ingredients, results, pattern-items, etc.).

<details>
    <summary>Show a full overview</summary>

```yaml
<item>:
   material: OAK_SIGN # This is an oak sign
   amount: 1 # 1 items will be shown
   durability: 0 # It has 0 durability (can be changed on damagable items)
   unbreakable: false # It is breakable (or unbreakable if 'true')
   name: '§6Armor Smithing: §cLevel $<level>' # The name of the item
   lore: # The lore of the item
     - ' §7The recipes above can be crafted'
     - ' §7with the armor smithing profession.'
     - §8--------------------
     - ' §7You need level §a40 armor smithing'
     - ' §a& $500 to unlock its §7mastery.'
     - ' §7A mastery allows to craft new items.'
   flags: [] # Flags that are applied to the item
   enchants: {} # Enchants that are applied to the item
   data: null # (Probably some meta-data that is not defined yet)
```
</details>

<details>
    <summary>Show a filled item</summary>

```yaml
<item>:
   material: DIAMOND_BOOTS # This is an oak sign
   amount: 1 # 1 items will be shown
   durability: 40 # It has 49 durability
   unbreakable: true # It is unbrealable
   name: '&6Magic Booooots' # The name of the item (colorcodes work with & too.)
   lore: # The lore of the item
     - ' §7The recipes above can be crafted'
     - ' §7with the armor smithing profession.'
     - §8--------------------
     - ' §7You need level §a40 armor smithing'
     - ' §a& $500 to unlock its §7mastery.'
     - ' §7A mastery allows to craft new items.'
   flags: [] # Flags that are applied to the item
   enchants: # Enchants that are applied to the item
     sharpness: 5
     unbreaking: 3
```
</details>

- Here is a full overview of existing [enchantments](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html)
- Here is a full overview of existing [flags](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/ItemFlag.html)

Lastly, we look into the special type of enchanted books. 
You can use enchanted books exactly as those item in the ItemBuilder. 
We have implemented a custom logic to handle the stored enchants of enchanted books and make them available for ingredient purposes for example. 
You have seen this above before:

<details>
    <summary>Show an enchanted book</summary>

```yaml
# This is a list-entry from the ingredients in a recipe
- material: ENCHANTED_BOOK # This is the enchanted book
  amount: 3 # You need this item 3 times
  enchants: # Enchants of the book
    efficiency: 5
    unbreaking: 3
```
</details>

#### Divinity
Divinity is a soft-dependend plugin of us that provides you with a lot of features regarding to items and other modules.
We encourage you to use [Divinity](https://www.spigotmc.org/resources/divinity-custom-item-generator-by-magemonkey-studio-formerly-prorpgitems.99713/) for your custom items, but you can also use others through the ItemBuilder-Function.

The current possible way of using Divinity items is by using the `DIVINITY_<module>_<item>` type.
The following modules exist:

```
DIVINITY_arrows:[item][~level:<level>][:<amount]
DIVINITY_consumables:[item][~level:<level>][:<amount] 
DIVINITY_custom_items:[item][:amount] 
DIVINITY_dismantle:[item][~level:<level>][:<amount] 
DIVINITY_essences:[item][~level:<level>][:<amount] 
DIVINITY_extractor:[item][~level:<level>][:<amount] 
DIVINITY_fortify:[item][~level:<level>][:<amount] 
DIVINITY_gems:[item][~level:<level>][:<amount] 
DIVINITY_identify:[item][~level:<level>][:<amount] 
DIVINITY_item_generator:[item][~level:<level>][~material:<type>][:<amount] 
DIVINITY_magic_dust:[item][~level:<level>][:<amount] 
DIVINITY_runes:[item][~level:<level>][:<amount]
```


### Key-Value Conditions + Supported Plugins
| Condition Key       | Possible Condition Values      | Belong to (Hooks)                                                                                                  |
|---------------------|--------------------------------|--------------------------------------------------------------------------------------------------------------------|
| professions         | Any profession of this plugin  | Natively, Fusion                                                                                                   |
| fabled              | Class-IDs of Fabled            | [Fabled](https://www.spigotmc.org/resources/fabled-an-rpg-engine-by-magemonkey-studio-formerly-proskillapi.91913/) |
| mcmmo               | PrimarySkillTypes of mcMMO     | [mcMMO](https://www.spigotmc.org/resources/official-mcmmo-original-author-returns.64348/)                          |
| jobs                | Levels of Jobs                 | [JobsReborn](https://www.spigotmc.org/resources/jobs-reborn.4216/)                                                 |
| aura_abilities      | Abilities from AuraSkills      | [AuraSkills](https://www.spigotmc.org/resources/auraskills.81069/)                                                 |
| aura_mana_abilities | Mana Abilities from AuraSkills | [AuraSkills](https://www.spigotmc.org/resources/auraskills.81069/)                                                 |
| aura_skills         | Skills from AuraSkills         | [AuraSkills](https://www.spigotmc.org/resources/auraskills.81069/)                                                 |
| aura_stats          | Stats from AuraSkills          | [AuraSkills](https://www.spigotmc.org/resources/auraskills.81069/)                                                 |