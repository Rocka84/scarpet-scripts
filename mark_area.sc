__config() -> {
   'scope' -> 'global',
   'commands' ->
   {
      '' -> _() -> print('Hello World!'),
      'posOne' -> _() -> pos1(pos(player())),
      'posOne <x> <y> <z>' -> _(x, y, z) -> pos1([x, y, z]),
      'posTwo' -> _() -> pos2(pos(player())),
      'posTwo <x> <y> <z>' -> _(x, y, z) -> pos2([x, y, z]),
      'expand' -> _() -> expand(1, _get_player_direction()),
      'expand <count>' -> _(c) -> expand(c, _get_player_direction()),
      'expand <count> <direction>' -> ['expand'],
      'hide' -> ['hide'],
   }
};

global_pos1 = null;
global_pos2 = null;

global_min = null;
global_max = null;

_draw_box(from, to, timeout) -> (
    attributes = {
      'from' -> from,
      'to' -> to,
      'color' -> 0xFF0000FF,
      'line' -> 10.0,
    };
    print(attributes);
    draw_shape('box', timeout, attributes);
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

_update() -> (
  hide();
  _calc_minmax();
  show();
);

_run_worldedit(command) -> (
  print('/'+'/'+command);
  run('/'+'/'+command);
);

show() -> (
  if(global_min != null && global_max != null,
    (
      _draw_box(global_min, global_max, 48000);
    )
  );
);

hide() -> (
  if(global_min != null && global_max != null,
    (
      _draw_box(global_min, global_max, 0);
    )
  );
);

pos1(pos) -> (
  _run_worldedit('pos1 ' + round(pos:0) + ',' + round(pos:1) + ',' + round(pos:2));

  global_pos1 = pos;
  _update();
);

pos2(pos) -> (
  _run_worldedit('pos2 ' + round(pos:0) + ',' + round(pos:1) + ',' + round(pos:2));

  global_pos2 = pos;
  _update();
);

global_direction_vectors = {
  'none'  -> [ 0,  0,  0],
  'up'    -> [ 0,  1,  0],
  'down'  -> [ 0, -1,  0],
  'north' -> [ 0,  0, -1],
  'east'  -> [ 1,  0,  0],
  'south' -> [ 0,  0,  1],
  'west'  -> [-1,  0,  0],
};

_get_player_direction() -> (
  look = query(player(), 'look');

  if(look:2 < -0.8, (
    return('north');
  ));
  if(look:0 >  0.8, (
    return('east');
  ));
  if(look:2 >  0.8, (
    return('south');
  ));
  if(look:0 < -0.8, (
    return('west');
  ));
  if(look:1 >  0.8, (
    return('up');
  ));
  if(look:1 < -0.8, (
    return('down');
  ));

  return('none');
);

expand(count, direction) -> (
  _run_worldedit('expand ' + count + ' ' + direction);

  vector = get(global_direction_vectors, direction);
  hide();
  if(min(0, vector:0, vector:1, vector:2) < 0, (
    global_min = global_min + (vector * count);
  ),(
    global_max = global_max + (vector * count);
  ));
  show();

);

