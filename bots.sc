// bots
//  save and restore sets of bots (carpet players) and auto-restore bots on server restart
// by Rocka84 (foospils)
// based on keepalive.sc by gnembon
// v 1.0

__config() -> {
  'scope' -> 'global',
  'commands' ->
  {
    '' -> _() -> print('Manage your scarpet players a.k.a. bots'),
    'save' -> ['save_set', '#unnamed'],
    'save <set>' -> 'save_set',
    'spawn' -> ['spawn_set', '#unnamed'],
    'spawn <set>' -> 'spawn_set',
    'kill_all' -> 'kill_all',
    'test' -> _() -> print('asd'),
  },
  'arguments' -> {
    'set' -> {
      'type' -> 'term',
      'suggester'-> _(args) -> filter(keys(global_bots), _ != '#unnamed' && _ != '#autosave'),
    }
  }
};

global_bots = read_file('bots', 'nbt');
global_bots = if (global_bots, parse_nbt(global_bots), {});

_persist() -> (
  write_file('bots', 'nbt', encode_nbt(global_bots));
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
    };
  ));
  _persist();
);

global_tries = {};
__configure_bot_when_ready(data) -> (
  if ((global_tries:(data:'n') += 1) > 200, (
    print('gave up on ' + data:'n');
    delete(global_tries, data:'n');
    return();
  ));
  p = player(data:'n');
  if (!p, (
    // print('waiting for ' + data:'n');
    schedule(1, '__configure_bot_when_ready', data);
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
      // print(mounted:0);
      schedule(1, _(p, m) -> modify(p, 'mount', m), p, mounted:0);
    ), (
      schedule(10, _(n) -> run(str('player %s mount', n)), data:'n');
    //   run(str('player %s mount', data:n));
    ));
  ));
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

    __configure_bot_when_ready(_);
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

