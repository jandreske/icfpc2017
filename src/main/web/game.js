"use strict";

// colors
const MAP_COLOR            = '#ffffe8';
const MAP_BACKGROUND_COLOR = '#ffffff';
const FILLED_COLOR         = '#f0f040';
const UNIT_COLOR           = '#ffb000';
const UNIT_OVERLAP_COLOR   = '#ff6000';
const PIVOT_COLOR          = '#209010';

var PUNTER_COLORS = ['#ff0000','#00ff00','#2020ff','#eeee00','#00eeee','#ee00ee'];


var xoff;
var yoff;
var scale;

function translate_x(coord) {
    return scale * coord - xoff;
}

function translate_y(coord) {
    return scale * coord - yoff;
}

var size = 640;


var game = {};
var map = {};
var problem = {};
var nextMove = 0;


function loadGame(f) {
    var reader = new FileReader();
    reader.onload = function(e) {
        game = JSON.parse(e.target.result);
        $('#lbl_punters').text(game.setup.punters);
        $('#lbl_my_id').text(game.setup.punter);
        $('#lbl_sites').text(game.setup.map.sites.length);
        $('#lbl_rivers').text(game.setup.map.rivers.length);
        $('#lbl_mines').text(game.setup.map.mines.length);
        var min_x = game.setup.map.sites[0].x;
        var max_x = min_x;
        var min_y = game.setup.map.sites[0].y;
        var max_y = min_y;
        game.setup.map.sites.forEach(function (site) {
            if (site.x < min_x) min_x = site.x;
            if (site.x > max_x) max_x = site.x;
            if (site.y < min_y) min_y = site.y;
            if (site.y > max_y) max_y = site.y;
        });
        var dx = max_x - min_x;
        var dy = max_y - min_y;
        scale = size / Math.max(dx, dy);
        xoff = scale * min_x - 25;
        yoff = scale * min_y - 25;
        drawMap();
        nextMove = 0;
    };
    reader.readAsText(f);
}


function get_site(id) {
    var theSite;
    game.setup.map.sites.forEach(function (site) {
        if (site.id == id) {
            theSite = site;
        }
    });
    return theSite;
}

function get_river(s,t) {
    var rivers = game.setup.map.rivers;
    var n = rivers.length;
    for (var i = 0; i < n; i++) {
        var r = rivers[i];
        if ((s === r.source && t === r.target) || (s == r.target && t == r.source)) {
            return r;
        }
    }
    alert("river not found: " + s + "-" + t);
}

function drawRiver(c, river) {
    c.save();
    if (river.owner >= 0) {
        c.fillStyle = PUNTER_COLORS[river.owner];
        c.strokeStyle = PUNTER_COLORS[river.owner];
    }
    c.beginPath();
    var site = get_site(river.source);
    var x = translate_x(site.x);
    var y = translate_y(site.y);
    c.moveTo(x,y);
    site = get_site(river.target);
    x = translate_x(site.x);
    y = translate_y(site.y);
    c.lineTo(x,y);
    c.fill();
    c.stroke();
    c.restore();
}

function drawSite(c, site) {
    var cx = translate_x(site.x);
    var cy = translate_y(site.y);
    c.save();
    c.beginPath();
    if (game.setup.map.mines.indexOf(site.id) >= 0) {
        c.fillStyle = 'red';
    }
    c.arc(cx, cy, 10, 0, 2*Math.PI);
    c.fill();
    c.stroke();
    c.restore();
}

function drawMap() {
    var m = document.getElementById('map');
    m.width = size + 50;
    m.height = size + 50;
    var c = m.getContext('2d');
    c.fillStyle = 'black';
    c.fillRect(0, 0, m.width, m.height);
    c.fillStyle = 'gray';
    c.strokeStyle = 'gray';
    c.save();
    game.setup.map.rivers.forEach(function (river) {
        drawRiver(c, river);
    });
    game.setup.map.sites.forEach(function(site) {
        drawSite(c, site);
    });
    c.restore();
}


function myClone(obj) {
    return JSON.parse(JSON.stringify(obj));
}


function redraw() {
    drawMap();
}


function runNextMove() {
    var move = game.moves[nextMove++];
    var n = game.setup.map.rivers.length;
    var river = get_river(move.source, move.target);
    river.owner = move.punter;
    redraw();
}

var playSpeedFps = 5;
var timer;

function autoPlay() {
    if (nextMove < game.moves.length) {
        runNextMove();
        timer = setTimeout(autoPlay, 1000 / playSpeedFps);
    }
}

$(document).ready(function(){
    $('#problem').live('change', function (e) {
        loadGame(e.target.files[0]);
    });

    $('#btn_move').click(function () {
        runNextMove();
    });
    $('#btn_run').click(function () {
        autoPlay();
    });
    $('#btn_pause').click(function () {
        timer && clearTimeout(timer);
    });

});
