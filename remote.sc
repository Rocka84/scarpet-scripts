// Remote
// By Rocka84 (foospils)
// v1.1

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'global',
  'commands' -> {
    '' -> _() -> print('Remote'),
    'info' -> 'info',
    'bind'        -> _()  -> autobind_mainhand(player(), null),
    'bind <name>' -> _(n) -> autobind_mainhand(player(), n),
    'give'        -> _()  -> give_lever_remote(player()),
    // 'use_lever_remote' -> _() -> use_lever_remote(query(player(), 'holds', 'mainhand')),
    // 'toggle_lever' -> _() -> toggle_lever(find_lever(player())),
    // 'push_button' -> _() -> push_button(find_button(player())),
  }
};

global_data_lever_remote = {
  'id' -> 'minecraft:sugar',
  'components' -> {
    'minecraft:custom_data' -> {
      'remote' -> {
        'type' -> 'lever'
      }
    },
    'minecraft:item_model' -> 'minecraft:lever',
    'minecraft:enchantments' -> {
      'levels' -> {
        'minecraft:infinity' -> 1
      },
      'show_in_tooltip' -> false
    },
    'minecraft:custom_name' -> '[{"text":"Lever Remote","italic":false}]',
    'minecraft:lore' -> ['{"text":"Target not set","italic":false}']
  }
};

run('datapack disable ' + '"file/scarpet_' + system_info('app_name') + '.zip"');
run('datapack list');
create_datapack('scarpet_' + system_info('app_name'), {'data' -> {'minecraft' -> {
  'recipe' -> {
    'remotelever.json' -> {
      'type' -> 'minecraft:crafting_shaped',
      'pattern' -> [
        's',
        'g'
      ],
      'key' -> {
        'g' -> 'minecraft:gold_block',
        's' -> 'minecraft:stick'
      },
      'result' -> global_data_lever_remote
    }
  }
}}});

_parse_item_data(item) -> (
  if (!item, return(null));
  if (type(item:2)=='nbt', item:2 = parse_nbt(item:2));
  data = item:2:'components':'minecraft:custom_data';
  if (!data, return(null));
  data = parse_nbt(data);
  if (!data, return(null));

  data:'remote';
);

_item_to_string(item, data) -> (
  item + '[' + join(',', map(pairs(data), _:0 + '=' + encode_nbt(_:1))) + ']';
);

_ucfirst(in) -> upper(slice(in, 0, 1)) + slice(in, 1);

_get_bound_item(item, block, name) -> (
  item_data = _parse_item_data(item);
  if (block ~ (item_data:'type') == null, (
    print(player(), 'Can\'t bind this item to this block!');
    return();
  ));

  pos = pos(block);

  data = item:2:'components';
  if (name, name = _ucfirst(item_data:'type') + ' Remote' );
  data:'minecraft:custom_name' = encode_json([{'text' -> name, 'italic' -> false}]);
  data:'minecraft:lore' = [encode_json([{'text' -> 'Target: ' + join(' ', map(pos, round(_))), 'italic' -> false}])];
  data:'minecraft:custom_data':'remote':'pos' = pos;

  _item_to_string(item:0, data);
);

_bind_inventory(player, slot, block, name) -> (
  item = _get_bound_item(inventory_get(player, slot), block, name);
  if (!item, return());
  slot_str = if (slot<9, ' hotbar.' + slot, ' inventory.' + (slot - 8));
  run('/item replace entity ' + player~'name' + slot_str + ' with ' + item);
);

_find_target(player, type) -> (
  scan(player()~'pos', [2,2,2], if(_ ~ type != null, block=_));
  block;
);

bind_mainhand(player, block, name) -> (
  _bind_inventory(player, player~'selected_slot', block, name);
);

info() -> (
  print(_parse_item_data(query(player(), 'holds', 'mainhand')));
);

autobind_mainhand(player, name) -> (
  item_data = _parse_item_data(query(player, 'holds', 'mainhand'));
  if (!item_data, return());

  block = _find_target(player, item_data:'type');
  if (!block, return());

  bind_mainhand(player, block, name);
);

give_lever_remote(player) -> (
  run('/give ' + player~'name' + ' ' + _item_to_string(global_data_lever_remote:'id', global_data_lever_remote:'components'));
);

use_lever_remote(item) -> (
  data = _parse_item_data(item);
  if (!data || !data:'pos', return(false));
  toggle_lever(block(data:'pos'));
  true;
);

toggle_lever(block) -> (
  if (block != 'lever', return());

  data = block_state(block);
  data:'powered' = data:'powered' == 'false';
  _set_block_data(block, data);
);

push_button(block) -> (
  if (block ~ 'button' == null, return());

  data = block_state(block);
  data:'powered' = true;
  _set_block_data(block, data);

  data:'powered' = false;
  delay = if (block ~ 'stone_' == null, 30, 20);
  schedule(delay, _(block,data) -> _set_block_data(block, data), block, data);
);

_set_block_data(block, data) -> (
  set(pos(block), block, data);
  update(pos(block));
  for(neighbours(block), update(pos(_)));
);

__on_player_uses_item(player, item_tuple, hand) -> (
  // print([player, item_tuple, hand]);
  if(use_lever_remote(item_tuple), return('cancel'));
);

