// mark_area - Visualize the area selected in WorldEdit.
// By Rocka84 (foospils)
// v1.1

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'player',
  'commands' ->
  {
    '' -> _() -> print('Visualize the area selected in WorldEdit.'),
    'posOne' -> _() -> pos1(pos(player())),
    'posOne <x> <y> <z>' -> _(x, y, z) -> pos1([x, y, z]),
    'posTwo' -> _() -> pos2(pos(player())),
    'posTwo <x> <y> <z>' -> _(x, y, z) -> pos2([x, y, z]),
    'expand' -> _() -> expand(1, _get_player_direction()),
    'expand <count>' -> _(c) -> expand(c, _get_player_direction()),
    'expand <count> <direction>' -> ['expand'],
    'contract' -> _() -> expand(-1, _get_player_direction()),
    'contract <count>' -> _(c) -> expand(c * -1, _get_player_direction()),
    'contract <count> <direction>' -> _(c, d) -> expand(c * -1, d),
    'hide' -> ['hide'],
    // 'test' -> _() -> (print(['look',_get_player_direction()]); print(['facing', query(player(), 'facing')]);),
  }
};

global_selwand = 'wooden_axe';

global_pos1 = null;
global_pos2 = null;

global_min = null;
global_max = null;

global_direction_vectors = {
  'none'  -> [ 0,  0,  0],
  'up'    -> [ 0,  1,  0],
  'down'  -> [ 0, -1,  0],
  'north' -> [ 0,  0, -1],
  'east'  -> [ 1,  0,  0],
  'south' -> [ 0,  0,  1],
  'west'  -> [-1,  0,  0],
};



_draw_box(from, to, color) -> (
  draw_shape('box', 48000, {
    'from' -> from,
    'to' -> to,
    'color' -> color,
  });
);

_hide_box(from, to, color) -> (
  draw_shape('box', 0, {
    'from' -> from,
    'to' -> to,
    'color' -> color,
  });
);

_calc_minmax() -> (
  if(global_pos1 == null || global_pos2 == null, return());

  global_min = [
    floor(min(global_pos1:0, global_pos2:0)),
    floor(min(global_pos1:1, global_pos2:1)),
    floor(min(global_pos1:2, global_pos2:2))
  ];
  global_max = [
    ceil(max(global_pos1:0, global_pos2:0)),
    ceil(max(global_pos1:1, global_pos2:1)),
    ceil(max(global_pos1:2, global_pos2:2))
  ];
);

_run_worldedit(command) -> (
  // print('/'+'/'+command);
  run('/'+'/'+command);
);

_get_player_direction() -> (
  look = query(player(), 'look');

  if(look:2 < -0.9, (
    return('north');
  ));
  if(look:0 >  0.9, (
    return('east');
  ));
  if(look:2 >  0.9, (
    return('south');
  ));
  if(look:0 < -0.9, (
    return('west');
  ));
  if(look:1 >  0.9, (
    return('up');
  ));
  if(look:1 < -0.9, (
    return('down');
  ));

  return('none');
);

_coord_floor(a) -> (
  [
    floor(a:0),
    floor(a:1),
    floor(a:2)
  ]
);

_coord_ceil(a) -> (
  [
    ceil(a:0),
    ceil(a:1),
    ceil(a:2)
  ]
);

show() -> (
  if(global_pos1 != null, (
    _draw_box(_coord_floor(global_pos1), _coord_ceil(global_pos1), 0xFF0000FF);
  ));
  if(global_min != null && global_max != null, (
    _draw_box(global_min, global_max, 0x00FF00FF);
  ));
  if(global_pos2 != null, (
    _draw_box(_coord_floor(global_pos2), _coord_ceil(global_pos2), 0x0000FFFF);
  ));
);

hide() -> (
  if(global_pos1 != null, (
    _hide_box(_coord_floor(global_pos1), _coord_ceil(global_pos1), 0xFF0000FF);
  ));
  if(global_min != null && global_max != null, (
    _hide_box(global_min, global_max, 0x00FF00FF);
  ));
  if(global_pos2 != null, (
    _hide_box(_coord_floor(global_pos2), _coord_ceil(global_pos2), 0x0000FFFF);
  ));
);

pos1(pos) -> (
  // _run_worldedit('pos1 ' + floor(pos:0) + ',' + floor(pos:1) + ',' + floor(pos:2));

  hide();
  global_pos1 = pos;
  _calc_minmax();
  show();
);

pos2(pos) -> (
  // _run_worldedit('pos2 ' + floor(pos:0) + ',' + floor(pos:1) + ',' + floor(pos:2));

  hide();
  global_pos2 = pos;
  _calc_minmax();
  show();
);

expand(count, direction) -> (
  // _run_worldedit('expand ' + count + ' ' + direction);

  hide();
  vector = get(global_direction_vectors, direction);
  if(global_pos1 * vector > global_pos2 * vector, (
    global_pos1 = global_pos1 + (vector * count);
  ),(
    global_pos2 = global_pos2 + (vector * count);
  ));
  _calc_minmax();
  show();
);

shift(count, direction) -> (
  // _run_worldedit('expand ' + count + ' ' + direction);

  hide();
  vector = get(global_direction_vectors, direction);
  global_pos1 = global_pos1 + (vector * count);
  global_pos2 = global_pos2 + (vector * count);
  _calc_minmax();
  show();
);


__on_player_clicks_block(player, block, face) -> (
  if (query(player, 'holds', 'mainhand'):0 == global_selwand, (
    pos1(pos(block) + [0.5, 0.5, 0.5]);
  ));
);

__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec) -> (
  if (hand == 'mainhand' && item_tuple:0 == global_selwand, (
    pos2(pos(block) + [0.5, 0.5, 0.5]);
  ));
);

__on_player_command(player, command) -> (
  if (command ~ '/desel', (
    hide();
    global_pos1 = null;
    global_pos2 = null;
    global_min = null;
    global_max = null;
    return();
  ));

  if (command ~ '/pos1', (
    args = split(' ', command);
    if (length(args) > 1, (
      pos1(split(',', args:1));
    ),(
      pos1(pos(player()));
    ));
    return();
  ));

  if (command ~ '/pos2', (
    args = split(' ', command);
    if (length(args) > 1, (
      pos2(split(',', args:1));
    ),(
      pos2(pos(player()));
    ));
    return();
  ));

  if (command ~ '/expand', (
    args = split(' ', command);
    if (length(args) < 2, return());

    if (length(args) > 2, (
      direction = args:2;
    ),(
      direction = _get_player_direction();
    ));

    expand(number(args:1), direction);
    return();
  ));

  if (command ~ '/contract', (
    args = split(' ', command);
    if (length(args) < 2, return());

    if (length(args) > 2, (
      direction = args:2;
    ),(
      direction = _get_player_direction();
    ));

    expand(number(args:1) * -1, direction);
    return();
  ));

  if (command ~ '/shift', (
    args = split(' ', command);
    if (length(args) < 2, return());

    if (length(args) > 2, (
      direction = args:2;
    ),(
      direction = _get_player_direction();
    ));

    shift(number(args:1), direction);
    return();
  ));

  if (command ~ '/move' && command ~ '-[abem]*s[abem]*', (
    args = split(' ', command);

    count = 0;
    direction = null;

    if (length(args) == 1, count = 1);

    for (args, (
         if (number(_) > 0, count = number(_));
         if (['north','east','south','west','up','down'] ~ _, direction = _);
    ));

    if (count < 1, return());

    if (direction == null, direction = _get_player_direction());

    shift(count, direction);
    return();
  ));
);

