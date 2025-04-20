// Mob Switch Status Monitor

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'global',
  'commands' ->
  {
    '' -> _() -> print('Mob Switch Monitor'),
    'setPosition <pos>' -> _(p) -> set_switch_pos(p),
    // 'isActive' -> _() -> if(global_switch_active, print('Mob Switch active'), print('Mob Switch NOT active')),
    // 'check' -> _() -> check_mob_switch(),
    // 'tp' -> _() -> run(str('tp @p %d %d %d', global_switch_pos + [0, 0, 1])),
    'bot spawn' -> _() -> run(str('player Alex spawn at %d %d %d', global_switch_pos + [0, 2, -1])),
    'bot kill' -> _() -> run('player Alex kill'),
    'test' -> _() -> (print(str('Position: %s  Active: %b', global_switch_pos, global_switch_active)); display_mob_switch(player(), true)),
    'test <player>' -> _(player) -> (print(player, str('Position: %s  Active: %b', global_switch_pos, global_switch_active)); display_mob_switch(player, true)),
    'dbg' -> _() -> print(sound()),
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

  if(global_switch_active, (
    text = format('g Mob Switch ', 'e active');
    sub = format('e No Mobs will spawn!');
  ),(
    text = format('g Mob Switch ', 'rb NOT ', 'r active');
    sub = format('r Mobs will spawn!');
  ));

  display_title(players, 'player_list_header', text);

  if (announce, (
    display_title(players, 'title', text);
    display_title(players, 'subtitle', sub);
  ));
);


__on_tick() -> (
  if (tick_time() % global_check_interval == 0, check_mob_switch());
);

__on_player_connects(player) -> (
  display_mob_switch(player, false);
);

