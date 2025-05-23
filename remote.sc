// Remote
// Remote control and/or synchronize buttons and levers.
// By Rocka84 (foospils)
// v1.7.1

_print(...msg) -> print(player(), format(msg));
_exit(msg) -> (_print('r ' + msg); exit());
_ucfirst(in) -> upper(slice(in, 0, 1)) + slice(in, 1);
_find_key(lst, value) -> reduce(pairs(lst), if(_:1 == value, _:0, _a), null);

global_permission_names = {
  -2 -> 'adventure',
  -1 -> 'creative',
  0 -> 'everyone',
  1 -> 'moderator',
  2 -> 'gamemaster',
  3 -> 'admin',
  4 -> 'owner',
  5 -> 'disabled',
};

global_check_interval = 1; // Set 0 to disable tick handler

global_permissions = {
  'admin' -> 3,
  'bind' -> 0,
  'give' -> -1,
  'link' -> 2,
  'use_remote' -> 0,
} + (read_file('permissions', 'JSON') || {});

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'global',
  'commands' -> {
    '' -> _() -> (
      base = 'w /' + system_info('app_name') + ' ';
      _print('wi Remote control and/or synchronize buttons and levers.\n');
      _print(base, 'w autobind [<name>]', 'gi  Bind Remote to nearest suitable block.');
      _print(base, 'w bind [<name>]', 'gi  Bind Remote by right clicking.');
      _print(base, 'w link [abort]', 'gi  Start/Abort linking levers/buttons.');
      _print(base, 'w unlink', 'gi  Unlink Remote by right clicking.');
      _print(base, 'w give (button|lever)', 'gi  Captain Obvious was here.');
      _print(base, 'w clean', 'gi  Cleanup linked blocks.');
    ),
    'info'            -> 'info',
    'autobind'        -> _()  -> autobind_mainhand(player(), null),
    'autobind <name>' -> _(n) -> autobind_mainhand(player(), n),
    'bind'            -> _()  -> set_selection_mode('bind', null),
    'bind <name>'     -> _(n) -> set_selection_mode('bind', n),
    'bind abort'      -> _()  -> set_selection_mode(null, null),
    'link'            -> _()  -> set_selection_mode('link', null),
    'link abort'      -> _()  -> set_selection_mode(null, null),
    'unlink'          -> _()  -> set_selection_mode('unlink', null),
    'give lever'      -> _()  -> give_item(player(), global_data_lever_remote),
    'give button'     -> _()  -> give_item(player(), global_data_button_remote),
    'clean'           -> 'clean_links',
    'config permission <permission> <level>' -> _(p,l) -> set_permission(p, l),
    'config show' -> _() -> list_permissions(),
  },
  'arguments' -> {
    'name' -> {
      'type' -> 'text',
      'suggest' -> [],
    },
    'permission' -> {
      'type' -> 'term',
      'suggest' -> keys(global_permissions),
    },
    'level' -> {
      'type' -> 'term',
      'suggest' -> values(global_permission_names),
    },
    'ticks' -> {
      'type' -> 'int',
      'min' -> 1,
    },
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
    'minecraft:custom_name' -> '[{"text":"Lever Remote","italic":false,"color":"#ffff55"}]',
    'minecraft:lore' -> ['{"text":"Target not set","italic":false,"color":"gray"}'],
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
    'minecraft:custom_name' -> '[{"text":"Button Remote","italic":false,"color":"#ffff55"}]',
    'minecraft:lore' -> ['{"text":"Target not set","italic":false,"color":"gray"}'],
    'minecraft:custom_model_data' -> 200
  }
};

run('datapack list');
if (create_datapack('scarpet_' + system_info('app_name'), {'data' -> {'minecraft' -> {
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
}}}), (
  run('datapack disable ' + '"file/scarpet_' + system_info('app_name') + '.zip"');
  run('datapack enable ' + '"file/scarpet_' + system_info('app_name') + '.zip"');
  run('recipe give @a minecraft:lever_remote');
  run('recipe give @a minecraft:button_remote');
));

global_blocks_passive = read_file('blocks_passive', 'JSON') || {};
// if (!global_blocks_passive, global_blocks_passive = {});

global_blocks_active = read_file('blocks_active', 'JSON') || {};
// if (!global_blocks_active, global_blocks_active = {});

_persist() -> (
  write_file('blocks_passive', 'JSON', global_blocks_passive);
  write_file('blocks_active', 'JSON', global_blocks_active);
);


_check_permission(permission) -> (
  if (player() == null, return(global_permissions:permission < 5));
  if (global_permissions:permission < 0 && player()~'gamemode' == global_permissions:permission * -1, return(true));
  player()~'permission_level' >= global_permissions:permission;
);

list_permissions() -> (
  if (!_check_permission('admin'), _exit('Not allowed'));
  _print('wb Required Permissions');
  for(pairs(global_permissions), (
    _print('b ' + _:0, 'w : ' + global_permission_names:(_:1) + ' (' + _:1 + ')');
  ));
);

set_permission(permission, level) -> (
  if (!_check_permission('admin'), _exit('Not allowed'));
  if (global_permissions:permission == null, _exit('Unknown permission'));
  if (number(level) == level, level = number(level), level = _find_key(global_permission_names, level));
  if (level == null, return());
  global_permissions:permission = level;
  write_file('permissions', 'JSON', global_permissions);
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

_get_bound_item(item, block, name) -> (
  item_data = _parse_item_data(item);
  if (block ~ (item_data:'type') == null, (
    _print('r Can\'t bind this item to this block!');
    return();
  ));
  if (item:1 > 1, (
    _print('r Can only bind single items!');
    return();
  ));

  pos = pos(block);

  data = item:2:'components';
  if (!name, name = _ucfirst(item_data:'type') + ' Remote' );
  data:'minecraft:custom_name' = encode_json([{'text' -> name, 'italic' -> false, 'color' -> '#55ffff'}]);
  data:'minecraft:lore' = [encode_json([{'text' -> 'Target: ', 'color' -> 'gray'},{'text' -> join(' ', map(pos, round(_))), 'italic' -> false, 'color' -> 'gray'}])];
  data:'minecraft:custom_data':'remote':'pos' = pos;
  data:'minecraft:max_stack_size' = 1;

  _item_to_string(item:0, data);
);

_bind_inventory(player, slot, block, name) -> (
  item = _get_bound_item(inventory_get(player, slot), block, name);
  if (!item, return(false));
  slot_str = if (slot<9, ' hotbar.' + slot, ' inventory.' + (slot - 8));
  run('/item replace entity ' + player~'name' + slot_str + ' with ' + item);
  _print('e Bound item to ', 'be ' + (name || block), 'e  at [' + join(' ', pos(block)) + '].');
  true;
);

_find_target(player, type) -> (
  scan(player()~'pos', [2,2,2], if(_ ~ type != null, block=_));
  block;
);

info() -> (
  print(player(), _parse_item_data(query(player(), 'holds', 'mainhand')));
);

autobind_mainhand(player, name) -> (
  if (!_check_permission('bind'), _exit('Not allowed'));
  item_data = _parse_item_data(query(player, 'holds', 'mainhand'));
  if (!item_data, return(false));

  block = _find_target(player, item_data:'type');
  if (!block, return(false));

  _bind_inventory(player, player~'selected_slot', block, name);
);

_bind_mainhand(player, block, name) -> (
  item_data = _parse_item_data(query(player, 'holds', 'mainhand'));
  if (!item_data || (block ~ (item_data:'type') == null), return(false));
  _bind_inventory(player, player~'selected_slot', block, name);
);

give_item(player, data) -> (
  if (!_check_permission('give'), _exit('Not allowed'));
  run('/give ' + player~'name' + ' ' + _item_to_string(data:'id', data:'components'));
);

use_remote(player, item) -> (
  if (!_check_permission('use_remote'), _exit('Not allowed'));
  data = _parse_item_data(item);
  if (!data || !data:'pos', return(false));
  block = block(data:'pos');
  if (
    set_lever(block, null), (
      sound('block.stone_button.click_' + if(block_state(block):'powered' == 'true', 'off', 'on'), player~'pos', 1);
      sync_passive(block);
      true;
    ),
    push_button(block), (
      sound('block.stone_button.click_on', player~'pos', 1);
      sync_passive(block);
      true;
    ),
    false
  );
);

global_particles = 'dust{"scale":1,"color":[0.6,0.1,0.1]}';

is_on(block) -> if(block ~ 'lamp|copper_bulb' != null, bool(block_state(block):'lit'), bool(block_state(block):'powered'));

set_lever(block, state) -> (
  if (block != 'lever', return(false));

  data = block_state(block);
  data:'powered' = if (state == null, !bool(data:'powered'), bool(state));
  _set_block_data(block, data);

  sound('block.stone_button.click_' + if(data:'powered', 'on', 'off'), pos(block), 1);
  particle(global_particles, pos(block));

  true;
);

set_lamp(block, state) -> (
  if (block != 'redstone_lamp' && block ~ 'copper_bulb' == null, return(false));

  data = block_state(block);
  data:'lit' = if (state == null, !bool(data:'lit'), bool(state));
  _set_block_data(block, data);
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

set_auto(block, state) -> (
  if (
    block == 'lever', set_lever(block, state),
    block ~ 'button' && state, push_button(block),
    block ~ 'lamp|copper_bulb', set_lamp(block, state),
    false
  );
);

_set_block_data(block, data) -> (
  set(pos(block), block, data);
  update(pos(block));
  for(neighbours(block), update(pos(_)));
);

_pos_id(pos) -> join('_', map(pos, round(_)));

sync_passive(block) -> (
  id = _pos_id(pos(block));
  if (!global_blocks_passive:id, return(false));
  set_auto(block(global_blocks_passive:id:1), !is_on(block)),
);

global_selected_block = null;
global_selection_mode = false;
global_bind_name = null;

set_selection_mode(mode, name) -> (
  if (mode && !_check_permission(mode), _exit('Not allowed'));
  if (
    mode == 'bind', _print('yb Bind mode:', 'w  right click the target now.'),
    mode == 'link', _print('yb Linking mode:', 'w  right click the first block now.'),
    mode == null, _print('yb Linking aborted')
  );
  global_selection_mode = mode;
  global_bind_name = name;
  global_selected_block = null;
);

_match_blocks(a, b, expr) -> ((a ~ expr != null) && (b ~ expr != null));

_link_passive(blockA, blockB) -> (
  posA = pos(blockA);
  posB = pos(blockB);

  global_blocks_passive:(_pos_id(posA)) = [posA, posB];
  global_blocks_passive:(_pos_id(posB)) = [posB, posA];
  _persist();

  _print('e blocks linked (passive mode)');
);

_link_active(blockA, blockB) -> (
  posA = pos(blockA);
  posB = pos(blockB);

  global_blocks_active:(_pos_id(posA)) = [posA, posB, false];
  global_blocks_active:(_pos_id(posB)) = [posB, posA, false];
  _persist();

  _print('e blocks linked (active mode)');
);

_unlink_block(block) -> (
  posA_id = _pos_id(pos(block));
  if (global_blocks_active:posA_id, (
    delete(global_blocks_active, _pos_id(global_blocks_active:posA_id:1));
    delete(global_blocks_active, posA_id);
  ), global_blocks_passive:posA_id, (
    delete(global_blocks_passive, _pos_id(global_blocks_passive:posA_id:1));
    delete(global_blocks_passive, posA_id);
  ), (
    _print('r Block not linked, aborting.');
    return();
  ));
  _persist();
  _print('e blocks unlinked');
);

_is_block_linked(block) -> (
  id = _pos_id(pos(block));
  bool(global_blocks_active:id || global_blocks_passive:id);
);

_select_block_to_link(player, block) -> (
  if (!global_selection_mode, return(false));

  if (
    block ~ 'lever|button|lamp|copper_bulb' == null, (
      _print('r Invalid block, try again');
    ), global_selection_mode == 'bind', (
      _bind_mainhand(player, block, global_bind_name);
      global_selection_mode = null;
    ), global_selection_mode == 'unlink', (
      _unlink_block(block);
      global_selection_mode = null;
    ), _is_block_linked(block), (
      _print('r Block already linked, try again');
    ), global_selected_block == null, (
      _print('y first block selected');
      global_selected_block = block;
    ), pos(block) == pos(global_selected_block), (
      _print('r Can\'t link block to itself');
    ), (block == 'lever' && global_selected_block == 'lever') || _match_blocks(block, global_selected_block, 'button'), (
      _link_passive(block, global_selected_block);
      global_selection_mode = null;
      global_selected_block = null;
    ), _match_blocks(block, global_selected_block, 'lamp|copper_bulb'), (
      _link_active(block, global_selected_block);
      global_selection_mode = null;
      global_selected_block = null;
    ), (
      _print('r blocks don\'t match');
    )
  );
  true;
);

_check_link(posA_id, posA, posB, list) -> (
  posB_id = _pos_id(posB);
  if (!list:posA_id, return());
  if (!list:posB_id, (
    delete(list, posA_id);
    _print('y Unlinking ' + posA_id);
    return();
  ));

  if (!posA, posA = map(split('_', posA_id), number(_)));
  blockA = block(posA);
  blockB = block(posB);

  if (_pos_id(list:posB_id:1) == posA_id && _match_blocks(blockA, blockB, 'lever|button|lamp|copper_bulb'), return());

  _print('y Unlinking ' + posA_id + ' and ' + posB_id);
  delete(list, posA_id);
  delete(list, posB_id);
);

clean_links() -> (
  if (!_check_permission('admin'), _exit('Not allowed'));
  for (pairs(global_blocks_active),  _check_link(_:0, _:1:0, _:1:1, global_blocks_active));
  for (pairs(global_blocks_passive), _check_link(_:0, _:1:0, _:1:1, global_blocks_passive));
  _persist();
);


// passively wait for player interaction
__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec) -> (
  if (
    _select_block_to_link(player, block), return('cancel'),
    sync_passive(block), return(),
  );
);

__on_player_uses_item(player, item_tuple, hand) -> (
  if(use_remote(player, item_tuple), return('cancel'));
);


// actively observe and sync blocks
_observe_blocks() -> (
  for(values(global_blocks_active), (
    state = is_on(block(_:0));
    if (state != _:2, (
      _:2 = state;
      set_auto(block(_:1), state);
    ));
  ));
);

if (global_check_interval > 0, (
  __on_tick() -> (
    if (tick_time() % global_check_interval == 0, _observe_blocks());
  );
));

