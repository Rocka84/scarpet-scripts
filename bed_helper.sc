// Bed Helper - Suppress "too far away" message for beds and optionally teleport to clicked bed.
// By Rocka84 (foospils)
// v1.1

__config() -> {
  'stay_loaded' -> true,
  'scope' -> 'player',
  'commands' ->
  {
    '' -> _() -> print(format('b bed_helper: ', 'w Suppress "too far away" message for beds (', strBool(global_enabled), 'w )')),
    'toggle' -> _() -> setEnabled(!global_enabled),
    'enable' -> ['setEnabled', true],
    'disable' -> ['setEnabled', false],
    'doTeleport' -> _() -> print(format('b bed_helper: ', 'w teleportation ', strBool(global_do_teleport))),
    'doTeleport <bool>' -> ['setDoTeleport'],
  },
  'arguments' -> {
    'bool' -> { 'type' -> 'bool' },
  },
};

global_enabled = true;
global_do_teleport = false;

strBool(b) -> (
  if (b, str('be enabled', b), str('br disabled', b));
);

setEnabled(enabled) -> (
  if (global_enabled == enabled, return());
  global_enabled = enabled;
  saveSettings();
  print(format('b bed_helper: ', strBool(global_enabled)));
);

setDoTeleport(do_teleport) -> (
  if (global_do_teleport == do_teleport, return());
  global_do_teleport = do_teleport;
  saveSettings();
  print(format('b bed_helper: ', 'w teleportation ', strBool(global_do_teleport)));
);

saveSettings() -> (
  delete_file(player()~'uuid', 'nbt');
  write_file(player()~'uuid', 'nbt', encode_nbt({
    'enabled' -> global_enabled,
    'do_teleport' -> global_do_teleport
  }));
);

loadSettings() -> (
  settings = parse_nbt(read_file(player()~'uuid', 'nbt'));
  if (settings == 'null', return());
  global_enabled = (settings:'enabled'>0);
  global_do_teleport = (settings:'do_teleport'>0);
);


__on_player_connects(player) -> (
  loadSettings();
);

__on_start() -> (
  loadSettings();
);

distance(a, b) -> (
  v = a - b;
  sqrt((v:0 * v:0) + (v:1 * v:1) + (v:2 * v:2));
);

distanceToBed(player, bed) -> (
  distance(pos(player), pos(bed) + [0.5, 0, 0.5]);
);

isNight() -> (
  return(day_time() % 24000 > 12541);
);

isThunderstorm() -> (
  return(weather() == 'thunder' && weather('rain') > 0);
);

isSpawnBed(player, bed) -> (
  spawn = player~'spawn_point';
  player~'dimension' == spawn:1 && distance(spawn:0, pos(bed)) <= 1;
);

__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec) -> (
  if (!global_enabled || !(block ~ '_bed'), return());

  can_sleep = isNight() || isThunderstorm();

  // when you can't sleep and this bed is already your respawn point, abort the click action
  if (!can_sleep && isSpawnBed(player, block), return('cancel'));

  // if you're near enough to the bed, just let the click event finish
  if (distanceToBed(player, block) <= 3, return());

  // You are too far away

  // When teleportation is enabled and you can sleep, tp to the bed before the click event is finished
  if (global_do_teleport && can_sleep, (
    run(str('tp %d %d %d', pos(block)));
    return();
  ));

  // Abort the click event to suppress messages
  return('cancel');
);

