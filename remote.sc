// Remote
// By Rocka84 (foospils)
// v1.2

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'global',
  'commands' -> {
    '' -> _() -> print('Remote'),
    'info' -> 'info',
    'bind'        -> _()  -> autobind_mainhand(player(), null),
    'bind <name>' -> _(n) -> autobind_mainhand(player(), n),
    'give lever'  -> _()  -> _give_item(player(), global_data_lever_remote),
    'give button' -> _()  -> _give_item(player(), global_data_button_remote),
    // 'use_remote' -> _() -> use_remote(player, query(player(), 'holds', 'mainhand')),
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
    'minecraft:lore' -> ['{"text":"Target not set","italic":false}'],
    'minecraft:custom_model_data' -> 100
  }
};

global_data_button_remote = {
  'id' -> 'minecraft:sugar',
  'components' -> {
    'minecraft:custom_data' -> {
      'remote' -> {
        'type' -> 'button'
      }
    },
    'minecraft:item_model' -> 'minecraft:stone_button',
    'minecraft:enchantments' -> {
      'levels' -> {
        'minecraft:infinity' -> 1
      },
      'show_in_tooltip' -> false
    },
    'minecraft:custom_name' -> '[{"text":"Button Remote","italic":false}]',
    'minecraft:lore' -> ['{"text":"Target not set","italic":false}'],
    'minecraft:custom_model_data' -> 200
  }
};

run('datapack disable ' + '"file/scarpet_' + system_info('app_name') + '.zip"');
run('datapack list');
create_datapack('scarpet_' + system_info('app_name'), {'data' -> {'minecraft' -> {
  'recipe' -> {
    'lever_remote.json' -> {
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
    },
    'button_remote.json' -> {
      'type' -> 'minecraft:crafting_shaped',
      'pattern' -> [
        'g',
        's'
      ],
      'key' -> {
        'g' -> 'minecraft:gold_block',
        's' -> 'minecraft:stick'
      },
      'result' -> global_data_button_remote
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
  if (!name, name = _ucfirst(item_data:'type') + ' Remote' );
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

_give_item(player, data) -> (
  run('/give ' + player~'name' + ' ' + _item_to_string(data:'id', data:'components'));
);

use_remote(player, item) -> (
  data = _parse_item_data(item);
  if (!data || !data:'pos', return(false));
  block = block(data:'pos');
  if (
    data:'type' == 'lever', toggle_lever(player, block),
    data:'type' == 'button', push_button(player, block),
    false
  );
);

toggle_lever(player, block) -> (
  if (block != 'lever', return());

  data = block_state(block);
  data:'powered' = data:'powered' == 'false';
  _set_block_data(block, data);

  snd = 'block.stone_button.click_' + if(data:'powered', 'on', 'off');
  sound(snd, pos(block), 1);
  sound(snd, player~'pos', 1);

  true;
);

push_button(player, block) -> (
  if (block ~ 'button' == null, return());

  data = block_state(block);
  data:'powered' = true;
  _set_block_data(block, data);
  snd = 'block.stone_button.click_on';
  sound(snd, pos(block), 1);
  sound(snd, player~'pos', 1);

  data:'powered' = false;
  delay = if (block ~ 'stone_' == null, 30, 20);
  schedule(delay, _(player,block,data) -> (
    _set_block_data(block, data);
    snd = 'block.stone_button.click_off';
    sound(snd, pos(block), 1);
    sound(snd, player~'pos', 1);
  ), player, block, data);

  true;
);

_set_block_data(block, data) -> (
  set(pos(block), block, data);
  update(pos(block));
  for(neighbours(block), update(pos(_)));
);

__on_player_uses_item(player, item_tuple, hand) -> (
  // print([player, item_tuple, hand]);
  if(use_remote(player, item_tuple), return('cancel'));
);

