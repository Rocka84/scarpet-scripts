// Mob Switch Status Monitor
// By Rocka84 (foospils)
// v1.1.1

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'global',
  'commands' ->
  {
    '' -> _() -> print('Mob Switch Monitor'),
    'setPosition' -> _() -> set_switch_pos(pos(player())),
    'setPosition <pos>' -> _(p) -> set_switch_pos(p),
    // 'isActive' -> _() -> (if(global_switch_active, print('Mob Switch active'), print('Mob Switch NOT active')); return(global_switch_active)),
    // 'tp' -> _() -> run(str('tp @p %d %d %d', global_switch_pos)),
    // 'bot spawn' -> _() -> run(str('player Alex spawn at %d %d %d', global_switch_pos)),
    // 'bot kill' -> _() -> run('player Alex kill'),
    'test' -> _() -> test(player()),
    'test <player>' -> _(p) -> test(p),
  },
  'arguments' -> {
    'pos' -> { 'type' -> 'pos' },
    'player' -> { 'type' -> 'players', 'single' -> true },
  }
};

global_switch_pos = parse_nbt(read_file('switch_pos', 'nbt'));
global_switch_active = false;
global_check_interval = 100; // 20 tps -> 5s


set_switch_pos(pos) -> (
  global_switch_pos = pos;
  write_file('switch_pos', 'nbt', encode_nbt(global_switch_pos));
);

check_mob_switch() -> (
  if (global_switch_pos == 'null', return());

  switch_active = loaded(global_switch_pos);
  if (global_switch_active == switch_active, return()); //no change
  global_switch_active = switch_active;

  display_mob_switch(player('all'), true);
);

display_mob_switch(players, announce) -> (
  if (global_switch_pos == 'null', return());

  if (global_switch_active, (
    text = format('g Mob Switch ', 'e active');
    sub = format('e No Mobs will spawn!');
    snd = 'entity.experience_orb.pickup';
  ),(
    text = format('g Mob Switch ', 'rb NOT ', 'r active');
    sub = format('r Mobs will spawn!');
    snd = 'block.bell.use';
  ));

  display_title(players, 'player_list_header', text);

  if (!announce, return());

  display_title(players, 'title', text);
  display_title(players, 'subtitle', sub);

  if (type(players) == 'list', (
    for (players, sound(snd, pos(_), 0.8));
  ),(
    sound(snd, pos(player()), 0.8);
  ));
);

test(player) -> (
  print(player, str('Position: %s  Active: %b', global_switch_pos, global_switch_active));
  display_mob_switch(player, true)
);

__on_tick() -> (
  if (tick_time() % global_check_interval == 0, check_mob_switch());
);

__on_player_connects(player) -> (
  display_mob_switch(player, false);
);

