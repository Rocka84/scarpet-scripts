// Mob Switch Status Monitor
// By Rocka84 (foospils)
// v1.2

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'global',
  'commands' ->
  {
    '' -> _() -> print('Mob Switch Monitor'),
    'setPosition' -> _() -> set_switch_pos(player()~'dimension', pos(player())),
    'setPosition <dimension> <pos>' -> _(d,p) -> set_switch_pos(d, p),
    'tp' -> _() -> _tp(player(), null),
    'tp <dimension>' -> _(d) -> _tp(player(), d),
    'tp <dimension> <player>' -> _(d,p) -> _tp(p, d),
    'test' -> _() -> test(player()),
    'test <player>' -> _(p) -> player()~'permission_level' > 1 && test(p),
  },
  'arguments' -> {
    'pos' -> { 'type' -> 'pos' },
    'dimension' -> { 'type' -> 'dimension' },
    'player' -> { 'type' -> 'players', 'single' -> true },
  }
};

global_switch_pos = parse_nbt(read_file('switch_positions', 'nbt'));
if (!global_switch_pos, global_switch_pos = {});
global_switch_active = {};
global_check_interval = 100; // 20 tps -> 5s
global_dimensions = keys(global_switch_pos);


set_switch_pos(dimension, pos) -> (
  // if (system_info('world_dimensions') ~ dimension == null, return());
  if (player()~'permission_level' < 2, (
    print(player(), 'Only allowed for operators');
    return();
  ));

  global_switch_pos:dimension = pos;
  print(global_switch_pos);
  write_file('switch_position', 'nbt', encode_nbt(global_switch_pos));
);

check_mob_switch(dimension) -> (
  if (global_switch_pos:dimension == null, return());

  switch_active = in_dimension(dimension, loaded(global_switch_pos:dimension));
  if (global_switch_active:dimension == switch_active, return()); //no change
  global_switch_active:dimension = switch_active;

  display_dimension(dimension, true);
);

_tp(player, dimension) -> (
  if (dimension == null, dimension = player~'dimension');
  run('execute in ' + dimension + ' run tp ' + player + ' ' + str('%d %d %d', global_switch_pos:dimension));
);

_get_message(dimension) -> (
  if (global_switch_active:dimension, (
    [
      format('g Mob Switch ', 'e active'),
      format('e No Mobs will spawn in ' + dimension + '!'),
      'entity.experience_orb.pickup',
    ]
  ),(
    [
      format('g Mob Switch ', 'rb NOT ', 'r active'),
      format('r Mobs will spawn in ' + dimension + '!'),
      'block.bell.use',
    ]
  ));
);

display_dimension(dimension, announce) -> (
  message = _get_message(dimension);
  for (filter(player('all'), _~'dimension' == dimension), _display(_, message, announce));
);

display_player(player, announce) -> (
  _display(player, _get_message(player~'dimension'), announce);
);

_display(player, msg, announce) -> (
  display_title(player, 'player_list_header', msg:0);

  if (!announce, return());

  display_title(player, 'title', msg:0);
  display_title(player, 'subtitle', msg:1);

  if (msg:2, sound(msg:2, pos(player), 0.8));
);

test(player) -> (
  dimension = player~'dimension';
  print(player, str('Dimension: %s  Position: %s  Active: %b', dimension, global_switch_pos:dimension, global_switch_active:dimension));
  display_player(player, true)
);

__on_tick() -> (
  if (tick_time() % global_check_interval == 0, for(global_dimensions, check_mob_switch(_)));
);

__on_player_connects(player) -> (
  display_player(player, false);
);

__on_player_changes_dimension(player, from_pos, from_dimension, to_pos, to_dimension) -> (
  if (global_switch_active:to_dimension != global_switch_active:from_dimension, display_player(player, true));
);

