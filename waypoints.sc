// Waypoints - Server wide waypoint system
// Original by Firigion and boyenn
// Fork by Rocka84 (foospils)
// v1.0

global_waypoint_config = {
    // Config option to allow players to tp to the waypoints ( Either via `/waypoint list` or `/waypoint tp` ) 
    // 0 : NEVER
    // 1 : CREATIVE PLAYERS
    // 2 : CREATIVE AND SPECTATOR PLAYERS
    // 3 : OP PLAYERS
    // 4 : ALWAYS
    'allow_tp' -> 2,
    'track_ticks' -> 1
};

_can_player_tp() -> (
    global_waypoint_config:'allow_tp' == 4 ||
    ( global_waypoint_config:'allow_tp' == 3 && player()~'permission_level' > 1) || 
    ( global_waypoint_config:'allow_tp' == 1 && player()~'gamemode'=='creative') ||
    ( global_waypoint_config:'allow_tp' == 2 && player()~'gamemode_id'%2)
);
_is_tp_allowed() -> global_waypoint_config:'allow_tp'; // anything but 0 will give boolean true

waypoints_file = read_file('waypoints','JSON');
saveSystem() -> (
    write_file('waypoints', 'JSON', global_waypoints);
);
global_authors = {};
global_dimensions = {'overworld'}; // so we only show waypoints in dimensions that have any; shoud also support custom ones
if(waypoints_file == null, 
    global_waypoints = {'Origin' ->[[0,100,0], 'Default waypoint', null, 'overworld']}; saveSystem(),
    global_waypoints = waypoints_file; 
    map(values(global_waypoints), 
        if( (auth = _:2) != null, global_authors += auth);
        global_dimensions += _:3
    );
);

global_settings=read_file('settings', 'JSON');
if(global_settings==null, global_settings={});

global_track = {};

_get_list_item(name, data, tp_allowed, player) -> (
    desc = if(data:1, '^g ' + data:1);
    cond_desc = if(!tp_allowed, desc);
    selected = if(global_track:player == name, 'd X', 'd \ \ ');
    if(global_track:player == name, (
        sel_action = str('!/%s track disable', system_info('app_name'));
        sel_hover = str('^g Click to stop tracking')
    ),(
        sel_action = str('!/%s track %s', system_info('app_name'), name);
        sel_hover = str('^g Click to start tracking')
    ));
    item = [
      // 'w [', selected, sel_hover, sel_action, 'w ]  ',
      'by '+name,
    ];

    if(tp_allowed, (
        item += str('!/%s tp %s', system_info('app_name'), name);
        item += '^g Click to teleport!'
    ));

    item += desc;
    item += str('w : %s %s %s ', map(data:0, round(_)));
    item += str('&%s %s %s ', map(data:0, round(_)));
    item += '^g Click to copy coordinates!';

    if(data:2,
        item += 'g by ';
        item += cond_desc;
        item += 'gb '+data:2;
        //if(!_is_tp_allowed(), item += desc)
        item += cond_desc;
    );
    item
);

list(author) -> (
    player = player();
    if(author != null && !has(global_authors, author), _error(author + ' has not set any waypoints'));
    print(player, format('bc === List of current waypoints ==='));
    tp_allowed = _can_player_tp();
    for(global_dimensions,
        current_dim = _;
        dim_already_printed = false;
        for(pairs(global_waypoints),
            [name, data]= _;
            if(current_dim== data:3 && (author == null || author==data:2),
                if(!dim_already_printed, print(player, format('l in '+current_dim)); dim_already_printed=true); // to avoid printing dim header when filtering authors
                print(player, format( _get_list_item(name, data, tp_allowed, player)))
            )
        )
    )
);

del_prompt(name) -> (
	global_to_delete = name;
	print(player(), format(
		'y Are you sure you want to delete ',
		'yb '+name,
		'y ? ',
		'lb [YES] ',
		str('!/%s confirm_del', system_info('app_name')),
		'rb [NO]',
		str('!/%s cancel_del', system_info('app_name')),
	))
);
confirm_del() -> (
	if(global_to_delete,
		del(global_to_delete);
		global_to_delete = null,
		_error('No deletion to confirm')
	)
);
cancel_del() -> (
	if(global_to_delete,
		print(player(), str('Deletion of %s was cancelled', global_to_delete));
		global_to_delete = null,
		_error('No deletion to confirm')
	)
);

del(name) -> (
    if(delete(global_waypoints,name),
    	global_track:player() = null;
    	print(player(), 'Waypoint ' + name + ' deleted.'),
    	//else, failed
    	_error('Waypoint ' + name + ' does not exist'));
    saveSystem();
);

add(name, poi_pos, description) -> (
    if(
        name=='disable',
        _error('That name is not available, it has a special funciton'),

        has(global_waypoints, name), 
        _error('You are trying to overwrite an existing waypoint. Delete it first.'),
        // else, add new one
        player = player();
        if(poi_pos==null, poi_pos=player~'pos');
        global_waypoints:name = [poi_pos, description, str(player), player~'dimension'];
        global_authors += str(player);
        global_dimensions += player~'dimension';
        print(player, format(
            'g Added new waypoint ',
            str('bg %s ', name),
            str('g at %s %s %s', map(poi_pos, round(_))),
        ));
        saveSystem();
    );
);

edit(name, description) -> (
    if(!has(global_waypoints, name), _error('That waypoint does not exist'));
    global_waypoints:name:1 = description;
    print(player(), format('g Edited waypoint\'s description'))
);

tp(name) -> (
    if(!_can_player_tp(), _error(str('%s players are not allowed to teleport', player()~'gamemode')) ); //for modes 1 and 2
    loc = global_waypoints:name:0;
    dim = global_waypoints:name:3;
    if(loc == null, _error('That waypoint does not exist'));
    print('Teleporting ' +player()+ ' to ' + name);
    run(str('execute in %s run tp %s %s %s %s', dim, player(), loc:0, loc:1, loc:2));
);

track(name) -> (
    player = player();
    if(name==null, (
            print(player, format('g Stopped tracking direction'))
    ), has(global_waypoints, name) && global_waypoints:(name):3 == player~'dimension', (
        print(player, format(str('g Started tracking direction to %s', name)));
        print(player, global_waypoints:(name):0);
    ), has(global_waypoints, name), ( // else, not a name nor null
        _error('Can\'t track ' + name + ', is\'s in another dimension')
    ),( // else, not a name nor null
        _error('Waypoint ' + name + ' does not exist')
    ));

    global_track:player = name;
    _track_tick(player);
);


_track_tick(player) -> (
    splayer = str(player);
    if(global_track:player, (
        schedule(global_waypoint_config:'track_ticks', '_track_tick', player);
    ),(
        display_title(player, 'clear');
        exit();
    ));

    if(global_waypoints:(global_track:player):3 != player~'dimension', return());

    ppos = player~'pos';
    look = player~'look';
    eyes = [0, player~'eye_height', 0];
    destination = global_waypoints:(global_track:player):0;

    shape_distance = 2;
    autodisable = global_settings:splayer:'autodisable';

    segment = destination - (ppos + eyes);
    distance = sqrt((segment:0 * segment:0) + (segment:1 * segment:1) + (segment:2 * segment:2));

    if(autodisable > -1 && distance <= autodisable, (
        // print(player, format('g You reached your destination!'));
        display_title(player, 'actionbar', format('g You reached your destination!'));
        global_track:player = null;
        return();
    ));

    direction = segment / distance;

    if (true, (
      if(distance <= shape_distance * 3, (
        shape_pos = destination - ppos;
      ),(
        shape_pos = (shape_distance * direction) + eyes;
      ));

      draw_shape([
        [
          'sphere',
          global_waypoint_config:'track_ticks',
          'player', player,
          'follow', player,
          'center', shape_pos,
          'radius', 0.05,
          'color', 0x000000FF,
          'fill', 0xEE00FF44,
        ],
        [
          'label',
          global_waypoint_config:'track_ticks',
          'player', player,
          'follow', player,
          'text', format('y ' + global_track:player),
          // 'value', format(str('y %s %d', global_track:player, distance)),
          'pos', shape_pos,
          'height', 0.3,
          'size', 3,
        ],
      ]);
    ));

    if (true, (
      dy = look:1 - direction:1;
      char_y = if(dy > 0.05, '↓', dy < -0.05, '↑', ' ');
      distance_str = 'w ' + round(distance) + 'm';

      scalar_xz = (look:0 * direction:2) - (look:2 * direction:0);
      if(scalar_xz < -0.05, (
        display_data = ['yb <'+char_y+' ', distance_str, 'yb    '         ];
      ), scalar_xz >  0.05, (
        display_data = ['yb    ',          distance_str, 'yb  '+char_y+'>'];
      ), (
        display_data = ['yb  '+char_y+' ', distance_str, 'yb  '+char_y+' '];
      ));

      display_title(player, 'actionbar', format(display_data));
    ));
);

help() -> (
    player = player();
    print(player, format('bd ==Help for the Waypoints app=='));
    print(player, format(str('f the following commands are available with /%s', system_info('app_name')) ));
    print(player, format('q \ \ add <name> [<pos>] [<description>]', 'fb \ | ', 'g add a new waypoint at given position with given description'));
    print(player, format('q \ \ del <waypoint>', 'fb \ | ', 'g delete existing waypoint'));
    print(player, format('q \ \ edit <waypoint> <description>', 'fb \ | ', 'g edit the description of an existing waypoint'));
    print(player, format('q \ \ list [<author>]', 'fb \ | ', 'g list all existing waypoints, optionally filtering by author'));
    print(player, format('q \ \ settings track [<what> <value>]', 'fb \ | ', 'g sets strack options'));
    if(_is_tp_allowed(),  print(player, format('q \ \ tp <waypoint>', 'fb \ | ', 'g teleport to given waypoint')));  
);

_error(msg)->(
    print(player(), format(str('r %s', msg)));
    exit()
);

_settings(key, value) -> (
    splayer = str(player());
    if(!has(global_settings, splayer), global_settings:splayer = {});
    global_settings:splayer:key = value;
    write_file('settings', 'JSON', global_settings);
);
global_default_settings = {
    'type' -> {'name'->'particles', 'defaults'->['render', 'particle']},
    'distance' -> {'name'->'distance', 'defaults'->['off', 'on']},
    'autodisable' -> 'off',
};

show_settings() -> (
    splayer = str(player());
    print(splayer, format('b Your current settings are:'));
    for(keys(global_default_settings),
        name = _;
        if(has(global_default_settings:name, 'name'),   //to grab the true name of a display name if they are different
            key = global_default_settings:name:'name';
            default = global_default_settings:name:'defaults',
            
            key = name;
            default = global_default_settings:name
        );
        
        is_default = !has(global_settings:splayer, key);
        value = if(type(default)=='list',
                    default:(global_settings:splayer:key),
                    if(is_default, default, global_settings:splayer:key)
        );
        if(name=='autodisable'&&value==-1, value=default);
        if(name=='length'&&value==-1, value='inf');

        modify_cmd = str('?/%s settings track %s ', system_info('app_name'), name);
        modify_tlt = '^g Click me to modify!';
        print(splayer, format(
            'bd\ \ '+name, modify_tlt, modify_cmd,
            'f \ \ »  ', modify_tlt, modify_cmd,
            'q '+value,  modify_tlt, modify_cmd,
            if(is_default, 'g \ (Unmodified value)')
        ))
    );
);

_get_commands() -> (
    base_commands = {
      '' -> 'help',
      'del <waypoint>' -> 'del_prompt',
      'confirm_del' -> 'confirm_del',
      'cancel_del' -> 'cancel_del',
      'add <name>' -> ['add', null, null],
      'add <name> <pos>' -> ['add', null],
      'add <name> <pos> <description>' -> 'add',
      'edit <waypoint> <description>' -> 'edit',
      'list' -> ['list', null],
      'list <author>' -> 'list',
      'track <waypoint>' -> 'track',
      'track disable' -> ['track', null],
      //'settings track line vector' ->             _()  -> _settings('vector', true),
      //'settings track line direction' ->          _()  -> _settings('vector', false),
      'settings track' -> 'show_settings',
      'settings track type particle' ->           _()  -> _settings('particles',true),
      'settings track type render' ->             _()  -> _settings('particles',false),
      'settings track autodisable off' ->         _()  -> _settings('autodisable', -1),
      'settings track autodisable <distance>' ->  _(d) -> _settings('autodisable', d),
    };
   if(_is_tp_allowed(), put(base_commands, 'tp <waypoint>', 'tp'));
   base_commands;
);

__config() -> {
    'scope'->'global',
    'stay_loaded'-> true,
    'commands' -> _get_commands(),
    'arguments' -> {
      'waypoint' -> {
            'type' -> 'term',
            'suggester'-> _(args) -> keys(global_waypoints),
      },
      'name' -> {
            'type' -> 'term',
            'suggest' -> [], // to make it not suggest anything
      },
      'description' -> {
            'type' -> 'text',
            'suggest' -> [],
      },
      'author' -> {
            'type' -> 'term',
            'suggester'-> _(args) -> keys(global_authors),
      },
      'distance' -> {
            'type' -> 'int',
            'suggest' -> [5, 10],
            'min' -> 0
      },
   }
};
