// Remote
// By Rocka84 (foospils)
// v1.4

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'global',
  'commands' -> {
    '' -> _() -> print('Remote'),
    'info'        -> 'info',
    'bind'        -> _()  -> autobind_mainhand(player(), null),
    'bind <name>' -> _(n) -> autobind_mainhand(player(), n),
    'give lever'  -> _()  -> _give_item(player(), global_data_lever_remote),
    'give button' -> _()  -> _give_item(player(), global_data_button_remote),
    'link'        -> _()  -> global_selection_mode = 'link',
    'link abort'  -> _()  -> (global_selection_mode = false; global_selected_block = null),
    'unlink'      -> _()  -> global_selection_mode = 'unlinked',
    // 'use_remote' -> _() -> use_remote(player, query(player(), 'holds', 'mainhand')),
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
        'ggg',
        'gcg',
        'ggg'
      ],
      'key' -> {
        'g' -> 'minecraft:gold_ingot',
        'c' -> 'minecraft:cobblestone'
      },
      'result' -> global_data_button_remote
    }
  }
}}});

global_connected_blocks = read_file('connected_blocks', 'nbt');
global_connected_blocks = if (global_connected_blocks, parse_nbt(global_connected_blocks), {});

_persist() -> (
  write_file('connected_blocks', 'nbt', encode_nbt(global_connected_blocks));
);

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

info() -> (
  print(_parse_item_data(query(player(), 'holds', 'mainhand')));
);

autobind_mainhand(player, name) -> (
  item_data = _parse_item_data(query(player, 'holds', 'mainhand'));
  if (!item_data, return());

  block = _find_target(player, item_data:'type');
  if (!block, return());

  _bind_inventory(player, player~'selected_slot', block, name);
);

_give_item(player, data) -> (
  run('/give ' + player~'name' + ' ' + _item_to_string(data:'id', data:'components'));
);

use_remote(player, item) -> (
  data = _parse_item_data(item);
  if (!data || !data:'pos', return(false));
  block = block(data:'pos');
  if (
    set_lever(block, null), (
      sound('block.stone_button.click_' + if(block_state(block):'powered' == 'true', 'off', 'on'), player~'pos', 1);
      sync_blocks(block);
      true;
    ),
    push_button(block), (
      sound('block.stone_button.click_on', player~'pos', 1);
      sync_blocks(block);
      true;
    ),
    false
  );
);

global_particles = 'dust{"scale":1,"color":[0.6,0.1,0.1]}';

set_lever(block, state) -> (
  if (block != 'lever', return(false));

  data = block_state(block);
  data:'powered' = if (state == null, !bool(data:'powered'), bool(state));
  _set_block_data(block, data);

  sound('block.stone_button.click_' + if(data:'powered', 'on', 'off'), pos(block), 1);
  particle(global_particles, pos(block));

  true;
);

push_button(block) -> (
  if (block ~ 'button' == null, return(false));

  data = block_state(block);
  data:'powered' = true;
  _set_block_data(block, data);
  sound('block.stone_button.click_on', pos(block), 1);
  particle(global_particles, pos(block));

  data:'powered' = false;
  delay = if (block ~ 'stone_' == null, 30, 20);
  schedule(delay, _(block,data) -> (
    _set_block_data(block, data);
    sound('block.stone_button.click_off', pos(block), 1);
  ), block, data);

  true;
);

_set_block_data(block, data) -> (
  set(pos(block), block, data);
  update(pos(block));
  for(neighbours(block), update(pos(_)));
);

__on_player_uses_item(player, item_tuple, hand) -> (
  if(use_remote(player, item_tuple), return('cancel'));
);

_pos_id(pos) -> join('_', map(pos, round(_)));

sync_blocks(block) -> (
  if (block ~ 'button' == null && block != 'lever', return(false));

  id = _pos_id(pos(block));
  if (
    !global_connected_blocks:id, false,
    block == 'lever', set_lever(block(global_connected_blocks:id), block_state(block):'powered' == false),
    block ~ 'button' != null, push_button(block(global_connected_blocks:id)),
    false
  );
);

global_selected_block = null;
global_selection_mode = false;

_select_block_to_link(block) -> (
  if (
    !global_selection_mode, (
      false;
    ), block != 'lever' && block ~ 'button' == null, (
      print(player(), 'invalid block not selected');
      false;
    ), global_selection_mode == 'unlinked', (
      posA = pos(block);
      delete(global_connected_blocks, _pos_id(global_connected_blocks:(_pos_id(posA))));
      delete(global_connected_blocks, _pos_id(posA));
      global_selection_mode = false;
      _persist();

      print(player(), 'blocks unlinked');
      true;
    ), global_selected_block == null, (
      global_selected_block = block;

      print(player(), 'first block selected');
      true;
    ), (block == 'lever' && global_selected_block == 'lever') || (block ~ 'button' != null && global_selected_block ~ 'button'), (
      posA = pos(block);
      posB = pos(global_selected_block);
      global_connected_blocks:(_pos_id(posA)) = posB;
      global_connected_blocks:(_pos_id(posB)) = posA;
      global_selected_block = null;
      global_selection_mode = false;
      _persist();

      print(player(), 'blocks linked');
      true;
    ), (
      print(player(), 'blocks don\'t match');
      false;
    )
  );
);

__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec) -> (
  if (
    _select_block_to_link(block), return('cancel'),
    sync_blocks(block), return(),
  );
);

