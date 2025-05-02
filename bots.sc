// bots
//  save and restore sets of bots (carpet players) and auto-restore bots on server restart
// by Rocka84 (foospils)
// based on keepalive.sc by gnembon
// v 1.1

__config() -> {
  'scope' -> 'global',
  'commands' ->
  {
    '' -> _() -> print('Manage your scarpet players a.k.a. bots'),
    'save' -> _() -> save_set('*' + str(player())),
    'save <set>' -> 'save_set',
    'spawn' -> _() -> spawn_set('*' + str(player())),
    'spawn <set>' -> 'spawn_set',
    'delete' -> _() -> delete_set('*' + str(player())),
    'delete <set>' -> 'delete_set',
    'kill' -> _() -> kill_set('*' + str(player())),
    'kill <set>' -> 'kill_set',
    'kill all' -> 'kill_all',
    'test' -> _() -> print('asd'),
  },
  'arguments' -> {
    'set' -> {
      'type' -> 'term',
      'suggester'-> _(args) -> filter(keys(global_bots), !(_ ~ '\\*') && _ != '#autosave'),
    }
  }
};

global_bots = read_file('sets', 'nbt');
global_bots = if (global_bots, parse_nbt(global_bots), {});

_persist() -> (
  write_file('sets', 'nbt', encode_nbt(global_bots));
);

_get_hand(p, hand) -> (
  inv = query(p, 'holds', hand);
  if (inv, [inv:0, str(inv:1)], false);
);

save_set(set) -> (
  global_bots:set = [];
  for (filter(player('all'), _~'player_type' == 'fake'), (
    global_bots:set += {
      'n' -> _~'name',
      'd' -> _~'dimension',
      'x' -> _~'x',
      'y' -> _~'y',
      'z' -> _~'z',
      'a' -> _~'yaw',
      'p' -> _~'pitch',
      'g' -> _~'gamemode',
      'f' -> _~'flying',
      'm' -> _~'mount'~'pos' || false,
      'r' -> _get_hand(_, 'mainhand'),
      'l' -> _get_hand(_, 'offhand'),
      's' -> _~'selected_slot',
    };
  ));
  _persist();
);

global_tries = {};
__configure_bot_when_available(data) -> (
  if ((global_tries:(data:'n') += 1) > 200, (
    print('gave up on ' + data:'n');
    delete(global_tries, data:'n');
    return();
  ));
  p = player(data:'n');
  if (!p, (
    // print('waiting for ' + data:'n');
    schedule(1, '__configure_bot_when_available', data);
    return();
  ));
  // print('found ' + data:'n');

  delete(global_tries, data:n);
  if (p~'player_type' != 'fake', return());

  modify(p, 'flying', data:'f');
  modify(p, 'gamemode', data:'g');

  if (data:'m', (
    mounted = filter(in_dimension(data:'d', entity_area('*', data:'m', [0.5, 0.5, 0.5])), _~'type' != 'player');
    if (mounted, (
      schedule(1, _(p, m) -> modify(p, 'mount', m), p, mounted:0);
    ), (
      schedule(10, _(p, m) -> modify(p, 'pos', m), p, mounted:0);
      schedule(12, _(n) -> run(str('player %s mount', n)), data:'n');
    ));
  ));

  selected_slot = _:'s'||0;
  modify(p, 'selected_slot', selected_slot);
  if (data:'l', inventory_set(p, -1, number(data:'l':1), data:'l':0), inventory_set(p, -1, 0));
  if (data:'r', inventory_set(p, selected_slot, number(data:'r':1), data:'r':0), inventory_set(p, selected_slot, 0));
);

spawn_set(set) -> (
  for (global_bots:set, (
    p = player(_:'n');

    if (p, (
      if (p~'player_type' != 'fake', continue());
      run(str('/execute in %s run teleport %s %f %f %f %f %f', _:'d', _:'n', _:'x', _:'y', _:'z', _:'a', _:'p'));
    ),(
      run(str('player %s spawn at %f %f %f facing %f %f in %s', _:'n', _:'x', _:'y', _:'z', _:'a', _:'p', _:'d'));
    ));

    __configure_bot_when_available(_);
  ));
);

delete_set(set) -> (
  delete(global_bots, set);
  _persist();
);

kill_set(set) -> (
  for (global_bots:set, (
    run('player ' + _:'n' + ' dismount');
    run('player ' + _:'n' + ' kill');
  ));
);

kill_all() -> (
  for (filter(player('all'), _~'player_type' == 'fake'), (
    run('player ' + _~'name' + ' dismount');
    run('player ' + _~'name' + ' kill');
  ));
);

__on_server_shuts_down() -> (
  save_set('#autosave');
  for (filter(player('all'), _~'player_type' == 'fake'), (
    run('player ' + _~'name' + ' dismount');
  ));
);
__on_server_starts() -> schedule(40, 'spawn_set', '#autosave');

