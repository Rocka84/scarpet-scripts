// bots
//  save and restore sets of bots (carpet players) and auto-restore bots on server restart
// by Rocka84 (foospils)
// based on keepalive.sc by gnembon
// v1.3

_print(msg) -> if(player(), print(player(), msg), print(msg));
_error(msg) -> (
  _print(format('br Bots: ', 'r ' + msg));
  exit();
);

global_required_permission = 5;
schedule(0, _() -> (
  setting = run('carpet commandPlayer'):1:4;
  if (
    setting ~ ' true'  != null, global_required_permission = 0,
    setting ~ ' false' != null, global_required_permission = 5,
    setting ~ ' ops'   != null, global_required_permission = 2,
    global_required_permission = number(setting ~ '\\d')
  );
));

_check_permission() -> (
  if (player() == null, return(global_required_permission < 5));
  player()~'permission_level' >= global_required_permission;
);

__config() -> {
  'scope' -> 'global',
  'commands' ->
  {
    '' -> _() -> _print('Manage sets of scarpet players a.k.a. bots'),
    'list' -> 'list_sets',
    'save' -> _() -> save_set('*' + str(player())),
    'save <set>' -> 'save_set',
    'apply' -> _() -> apply_set('*' + str(player())),
    'apply <set>' -> 'apply_set',
    'delete' -> _() -> delete_set('*' + str(player())),
    'delete <set>' -> 'delete_set',
    'kill_set' -> _() -> kill_set('*' + str(player())),
    'kill_set <set>' -> 'kill_set',
    'show' -> _() -> show_set('*' + str(player())),
    'show <set>' -> 'show_set',
    'kill <bot>' -> _(b) -> if(_check_permission(), kill(b), _error('Not allowed')),
    'kill all' -> 'kill_all',
    'info <bot>' -> 'info',
    'info all' -> 'info_all',
  },
  'arguments' -> {
    'set' -> {
      'type' -> 'term',
      'suggester'-> _(args) -> filter(keys(global_bots), !(_ ~ '^[\\*#]')),
    },
    'bot' -> {
      'type' -> 'term',
      'suggester'-> _(args) -> filter(player('all'), _~'player_type' == 'fake'),
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

_bot_data(p) -> (
  {
    'n' -> p~'name',
    'd' -> p~'dimension',
    'x' -> p~'x',
    'y' -> p~'y',
    'z' -> p~'z',
    'a' -> p~'yaw',
    'p' -> p~'pitch',
    'g' -> p~'gamemode',
    'f' -> p~'flying',
    'm' -> p~'mount'~'pos' || false,
    'v' -> p~'mount'~'name' || false,
    'r' -> _get_hand(p, 'mainhand'),
    'l' -> _get_hand(p, 'offhand'),
    's' -> p~'selected_slot',
    'c' -> p~'sneaking',
  };
);

save_set(set) -> (
  if (!_check_permission(), _error('Not allowed'));

  global_bots:set = [];
  for (filter(player('all'), _~'player_type' == 'fake'), global_bots:set += _bot_data(_));
  _persist();

  _print(format('b 📂 ', 'yb ' + set, 'w  saved.'));
);

global_tries = {};
__configure_bot_when_available(data) -> (
  if ((global_tries:(data:'n') += 1) > 200, (
    _print('gave up on ' + data:'n');
    delete(global_tries, data:'n');
    return();
  ));
  p = player(data:'n');
  if (!p, (
    // _print('waiting for ' + data:'n');
    schedule(1, '__configure_bot_when_available', data);
    return();
  ));
  // _print('found ' + data:'n');

  delete(global_tries, data:n);
  if (p~'player_type' != 'fake', return());

  modify(p, 'flying', data:'f');
  modify(p, 'gamemode', data:'g');

  if (data:'m', (
    mounted = filter(in_dimension(data:'d', entity_area('*', data:'m', [0.5, 0.5, 0.5])), _~'type' != 'player');
    if (mounted, (
      schedule(1, _(p, m) -> modify(p, 'mount', m), p, mounted:0);
    ), (
      schedule(10, _(p, m) -> modify(p, 'pos', m), p, data:'m');
      schedule(12, _(n) -> run(str('player %s mount', n)), data:'n');
    ));
  ));

  run(str('player %s %s', data:'n', if(data:'c', 'sneak', 'unsneak')));
  // schedule(1, _(c) -> run(c), str('player %s %s', data:'n', if(data:'c', 'sneak', 'unsneak')));

  selected_slot = data:'s'||0;
  modify(p, 'selected_slot', selected_slot);
  if (data:'l', inventory_set(p, -1, number(data:'l':1), data:'l':0), inventory_set(p, -1, 0));
  if (data:'r', inventory_set(p, selected_slot, number(data:'r':1), data:'r':0), inventory_set(p, selected_slot, 0));
);

apply_set(set) -> (
  if (!_check_permission(), _error('Not allowed'));

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

  _print(format('b 📂 ', 'yb ' + set, 'w  applied.'));
);

delete_set(set) -> (
  if (!_check_permission(), _error('Not allowed'));

  delete(global_bots, set);
  _persist();

  _print(format('b 📂 ', 'yb ' + set, 'w  deleted.'));
);

kill_set(set) -> (
  if (!_check_permission(), _error('Not allowed'));

  for (global_bots:set, kill(_:'n'));
  _print(format('b 📂 ', 'yb ' + set, 'w  bots killed.'));
);

kill_all() -> (
  if (!_check_permission(), _error('Not allowed'));

  for (filter(player('all'), _~'player_type' == 'fake'), kill(_~'name'));
  _print(format('b 🤖 ', 'ybi All', 'w  bots killed.'));
);

kill(bot) -> (
  run('player ' + bot + ' dismount');
  run('player ' + bot + ' kill');
);

__on_server_shuts_down() -> (
  save_set('#autosave');
  for (filter(player('all'), _~'player_type' == 'fake'), (
    run('player ' + _~'name' + ' dismount');
  ));
);
__on_server_starts() -> schedule(40, 'apply_set', '#autosave');


list_sets() -> (
  if (global_bots:('*'+player()~'name'), _print(format('w 📂 ', 'iyb Your set', 'w  (' + length(global_bots:('*'+player()~'name')) + ' Bots)')));
  for(pairs(global_bots), (
    if (_:0 ~ '^[\\*#]', continue());
    _print(format('w 📂 ', 'by ' + _:0, 'w  (' + length(_:1) + ' Bots)'))
  ));
  _print('');
);

show_set(set) -> (
  _print(format([
    'b 📂 ',
    if (set == '*' + player()~'name', 'yb Your set', 'yb ' + set)
  ]));
  for(global_bots:set, _show_bot(_));
  _print('');
);

info(name) -> (
  _show_bot(_bot_data(player(name)));
);

info_all() -> (
  for (filter(player('all'), _~'player_type' == 'fake'), _show_bot(_bot_data(player(_))));
);

_round2(in) -> round(in*100)/100;

_show_bot(bot) -> (
  data = [
    'w 🤖 ',  'bl ' + bot:'n',
    'w  at ', 'be ' + _round2(bot:'x') + ' ' + _round2(bot:'y') + ' ' + _round2(bot:'z'),
    'w  in ', 'bv ' + bot:'d',
    'w  in ', 'bm ' + bot:'g', 'w  mode',
  ];
  if (bot:'f',  data += 'bc  flying');
  if (bot:'c',  data += 'bc  sneaking');
  if (bot:'m', (
    put(data, null, [
      'w  riding ', 'bc ' + (bot:'v'||'a vehicle'),
    ], 'extend');
  ));
  if (bot:'r', (
    put(data, null, [
      'w  holding ', 'bq ' + bot:'r':0,
      'w  in ', 'bq mainhand',
    ], 'extend');
  ));
  if (bot:'l', (
    put(data, null, [
      'w  holding ', 'bt ' + bot:'l':0,
      'w  in ', 'bt offhand',
    ], 'extend');
  ));

  _print(format(data));
);

