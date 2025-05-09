// Remote
// By Rocka84 (foospils)
// v1.0

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'global',
  'commands' -> {
    '' -> _() -> print('Remote'),
    'test' -> 'test',
    'remote_toggle_lever' -> _() -> remote_toggle_lever(player()),
    'give' -> _() -> give_remote_lever(player()),
    // 'replace' -> _() -> replace_remote_lever(player(), player()~'pos'),
    'bind' -> _() -> bind_lever(player(), find_lever(player), null),
    'bind <name>' -> _(name) -> bind_lever(player(), find_lever(player), name),
    'toggle_lever' -> _() -> toggle_lever(find_lever(player())),
    'push_button' -> _() -> push_button(find_button(player())),
  }
};

global_data_remote_lever = {
  'id' -> 'minecraft:sugar',
  'components' -> {
    'minecraft:custom_data' -> {
      'remote_lever' -> {}
    },
    'minecraft:item_model' -> 'minecraft:lever',
    'minecraft:enchantments' -> {
      'levels' -> {
        'minecraft:infinity' -> 1
      },
      'show_in_tooltip' -> false
    },
    'minecraft:custom_name' -> '[{"text":"Remote Lever","italic":false}]',
    'minecraft:lore' -> ['{"text":"Target not set","italic":false}']
  }
};

_get_item_data(player) -> (
  item = query(player, 'holds', 'mainhand');
  if (!item, return(null));
  // if (!item || !item:0 == 'sugar', return());
  data = item:2:'components':'minecraft:custom_data';
  if (!data, return(null));
  data = parse_nbt(data);
  if (!data, return(null));

  data:'remote_lever'
};

_create_item(item, data) -> (
  item + '[' + join(',', map(pairs(data), _:0 + '=' + encode_nbt(_:1))) + ']';
);

_create_remote_lever(pos, name) -> (
  components = copy(global_data_remote_lever:'components');

  if (name, components:'minecraft:custom_name' = encode_json([{'text' -> name, 'italic' -> false}]));
  components:'minecraft:custom_data':'remote_lever' = { 'pos' -> pos };
  components:'minecraft:lore' = [encode_json([{'text' -> 'Target: ' + join(' ', map(pos, round(_))), 'italic' -> false}])];

  _create_item(global_data_remote_lever:'id', components);
);

give_remote_lever(player) -> (
  run('/give ' + player~'name' + ' ' + _create_remote_lever(player()~'pos', null));
);

bind_lever(player, pos, name) -> (
  data = _get_item_data(player);
  if (data == null, (
    print('Non suitable item');
    return();
  ));

  if (type(pos) == 'block', pos = pos(pos));
  if (pos == null, pos = player~'pos');
  run('/item replace entity ' + player~'name' + ' hotbar.' + player()~'selected_slot' + ' with ' + _create_remote_lever(pos, name));
);


find_lever(player) -> (
  scan(player()~'pos', [2,2,2], if(_=='lever', block=_));
  block;
);

find_button(player) -> (
  scan(player()~'pos', [2,2,2], if(_ ~ 'button' != null, block=_));
  block;
);


test() -> (
  print(_get_item_data(player()));
);

remote_toggle_lever(player) -> (
  data = _get_item_data(player);
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


print(create_datapack('scarpet_' + system_info('app_name'), {'data' -> {'minecraft' -> {
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
      'result' -> data_lever
    }
  }
}}}));

__on_player_uses_item(player, item_tuple, hand) -> (
  // print([player, item_tuple, hand])
  if(remote_toggle_lever(player), return('cancel'));
);
