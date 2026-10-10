#pragma once

// Page servie par l'ESP32 pour choisir sa station (etape 2 de l'onboarding).
// Le telephone est sur le Wi-Fi de la maison : la recherche de station interroge
// directement l'open data d'Ile-de-France Mobilites (sans cle). Les directions
// passent par l'ESP32 (/directions), qui seul connait la cle PRIM.
static const char PAGE_STATION[] = R"html(<!doctype html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Prochain Metro</title>
<style>
  :root {
    --fond: #f4f5f7; --carte: #fff; --texte: #1c1f24; --discret: #6b7280;
    --bord: #e2e5ea; --accent: #0a7c6e; --accent-clair: #e3f3f0;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --fond: #15171b; --carte: #1f2228; --texte: #eceef1; --discret: #9aa1ab;
      --bord: #31353d; --accent: #2bb3a3; --accent-clair: #183a36;
    }
  }
  * { box-sizing: border-box; }
  body {
    margin: 0; background: var(--fond); color: var(--texte);
    font: 16px/1.45 system-ui, -apple-system, sans-serif;
  }
  main { max-width: 520px; margin: 0 auto; padding: 20px 16px 120px; }
  h1 { font-size: 22px; margin: 4px 0 2px; }
  .sous-titre { color: var(--discret); margin: 0 0 20px; }
  section {
    background: var(--carte); border: 1px solid var(--bord); border-radius: 14px;
    padding: 16px; margin-bottom: 14px;
  }
  h2 { font-size: 15px; margin: 0 0 10px; display: flex; gap: 8px; align-items: center; }
  .num {
    width: 22px; height: 22px; border-radius: 50%; background: var(--accent); color: #fff;
    font-size: 13px; display: inline-grid; place-items: center; flex: none;
  }
  input[type=search] {
    width: 100%; font: inherit; padding: 12px 14px; border-radius: 10px;
    border: 1px solid var(--bord); background: var(--fond); color: var(--texte);
  }
  input[type=search]:focus { outline: 2px solid var(--accent); border-color: transparent; }
  .station { padding: 12px 0; border-top: 1px solid var(--bord); }
  .station:first-child { border-top: 0; }
  .station-nom { font-weight: 600; }
  .station-commune { color: var(--discret); font-size: 13px; }
  .lignes { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
  .ligne {
    display: inline-flex; align-items: center; gap: 6px; border: 1px solid var(--bord);
    background: var(--carte); color: var(--texte); border-radius: 22px;
    padding: 4px 12px 4px 4px; font: inherit; font-size: 14px; cursor: pointer;
  }
  .ligne.choisie { border-color: var(--accent); background: var(--accent-clair); }
  .pastille {
    min-width: 30px; height: 30px; padding: 0 6px; display: inline-grid; place-items: center;
    font-weight: 700; font-size: 15px; border-radius: 15px; background: #888; color: #fff;
  }
  .pastille.carree { border-radius: 7px; }
  .message { color: var(--discret); font-size: 14px; margin: 10px 0 0; }
  .erreur { color: #c0392b; }
  .direction {
    display: flex; align-items: center; gap: 12px; padding: 12px; margin-top: 8px;
    border: 1px solid var(--bord); border-radius: 10px; cursor: pointer;
  }
  .direction:has(input:checked) { border-color: var(--accent); background: var(--accent-clair); }
  .direction input { width: 20px; height: 20px; accent-color: var(--accent); flex: none; }
  .direction small { display: block; color: var(--discret); }
  .marche { display: flex; align-items: center; justify-content: center; gap: 18px; }
  .marche button {
    width: 48px; height: 48px; border-radius: 50%; border: 1px solid var(--bord);
    background: var(--fond); color: var(--texte); font-size: 24px; cursor: pointer;
  }
  .marche output { font-size: 28px; font-weight: 700; min-width: 90px; text-align: center; }
  .bouton {
    width: 100%; padding: 15px; border: 0; border-radius: 12px; background: var(--accent);
    color: #fff; font: inherit; font-weight: 600; font-size: 17px; cursor: pointer;
  }
  .bouton:disabled { opacity: .35; cursor: default; }
  .lien { background: none; border: 0; color: var(--accent); font: inherit; padding: 0; cursor: pointer; }
  .bas {
    position: fixed; left: 0; right: 0; bottom: 0; padding: 12px 16px 20px;
    background: linear-gradient(transparent, var(--fond) 30%);
  }
  .bas div { max-width: 520px; margin: 0 auto; }
  .fini { text-align: center; padding: 60px 16px; }
  .fini .coche { font-size: 56px; color: var(--accent); }
  [hidden] { display: none !important; }
</style>
</head>
<body>
<main id="formulaire">
  <h1>Prochain Metro</h1>
  <p class="sous-titre">Choisissez votre station : l'ecran vous dira quand partir.</p>

  <section>
    <h2><span class="num">1</span>Votre station</h2>
    <input type="search" id="recherche" placeholder="Ex : Nation, Chatelet, Magenta..." autocomplete="off" autofocus>
    <div id="resultats"></div>
    <p class="message" id="message-recherche"></p>
  </section>

  <section id="etape-direction" hidden>
    <h2><span class="num">2</span>Direction</h2>
    <div id="ligne-choisie"></div>
    <div id="directions"></div>
    <p class="message" id="message-direction"></p>
  </section>

  <section id="etape-marche" hidden>
    <h2><span class="num">3</span>Temps de marche jusqu'a la station</h2>
    <div class="marche">
      <button type="button" id="moins" aria-label="Une minute de moins">&minus;</button>
      <output id="minutes">5 min</output>
      <button type="button" id="plus" aria-label="Une minute de plus">+</button>
    </div>
  </section>
</main>

<div class="bas" id="barre">
  <div><button class="bouton" id="enregistrer" disabled>Choisissez une station</button></div>
</div>

<main id="fini" class="fini" hidden>
  <div class="coche">&#10003;</div>
  <h1>C'est pret !</h1>
  <p class="sous-titre">L'ecran affiche maintenant votre prochain depart.</p>
</main>

<script>
const OPEN_DATA = 'https://data.iledefrance-mobilites.fr/api/explore/v2.1/catalog/datasets/';
const MODES = { Metro: 'Metro', RapidTransit: 'RER', LocalTrain: 'Train', Tramway: 'Tram' };
const ORDRE = Object.keys(MODES);
const $ = id => document.getElementById(id);

let choix = null;  // la ligne touchee
let marche = 5;

// Identifiants de l'open data -> identifiants PRIM
const arretPrim = id => id.includes('monomodalStopPlace')
  ? 'STIF:StopArea:SP:' + id.split(':').pop() + ':'
  : 'STIF:StopPoint:Q:' + id.split(':').pop() + ':';
const lignePrim = id => 'STIF:Line::' + id.split(':').pop() + ':';
// Les polices de l'ecran n'ont pas d'accents
const sansAccents = t => t.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^\x20-\x7e]/g, '');

async function lireJson(url) {
  const r = await fetch(url);
  if (!r.ok) throw new Error('HTTP ' + r.status);
  return r.json();
}

function pastille(l) {
  const p = document.createElement('span');
  p.className = 'pastille' + (l.mode === 'Metro' ? '' : ' carree');
  p.textContent = l.court;
  if (l.couleur) { p.style.background = '#' + l.couleur; p.style.color = '#' + l.texte; }
  return p;
}

// --- 1. Recherche de la station ---
let minuterie;
$('recherche').addEventListener('input', () => {
  clearTimeout(minuterie);
  minuterie = setTimeout(chercher, 250);
});

async function chercher() {
  const q = $('recherche').value.trim();
  $('message-recherche').textContent = '';
  if (q.length < 2) { $('resultats').replaceChildren(); return; }

  const modes = ORDRE.map(m => '"' + m + '"').join(',');
  const where = 'search(stop_name,"' + q.replace(/"/g, '') + '") and mode in (' + modes + ')';
  let donnees;
  try {
    donnees = await lireJson(OPEN_DATA + 'arrets-lignes/records?limit=100'
      + '&select=id,shortname,mode,stop_id,stop_name,nom_commune,stop_lat,stop_lon'
      + '&where=' + encodeURIComponent(where));
  } catch (e) {
    $('message-recherche').innerHTML = '<span class="erreur">Pas d\'acces a internet. Votre telephone est-il bien connecte au Wi-Fi de la maison ?</span>';
    return;
  }
  if (q !== $('recherche').value.trim()) return;  // l'utilisateur a continue a taper

  // Une station = un nom dans une commune, avec ses lignes
  const stations = new Map();
  for (const r of donnees.results) {
    const cle = r.stop_name + '|' + r.nom_commune;
    if (!stations.has(cle)) stations.set(cle, { nom: r.stop_name, commune: r.nom_commune, lignes: new Map() });
    const lignes = stations.get(cle).lignes;
    if (!lignes.has(r.id)) lignes.set(r.id, {
      id: r.id, court: r.shortname, mode: r.mode, arrets: new Set(),
      station: r.stop_name, lat: +r.stop_lat, lon: +r.stop_lon,
    });
    lignes.get(r.id).arrets.add(r.stop_id);
  }

  // Couleurs officielles des lignes
  const ids = [...new Set(donnees.results.map(r => r.id.split(':').pop()))];
  if (ids.length) {
    try {
      const c = await lireJson(OPEN_DATA + 'referentiel-des-lignes/records?limit=100'
        + '&select=id_line,colourweb_hexa,textcolourweb_hexa'
        + '&where=' + encodeURIComponent('id_line in (' + ids.map(i => '"' + i + '"').join(',') + ')'));
      const couleurs = new Map(c.results.map(x => [x.id_line, x]));
      for (const s of stations.values()) for (const l of s.lignes.values()) {
        const x = couleurs.get(l.id.split(':').pop());
        if (x) { l.couleur = x.colourweb_hexa; l.texte = x.textcolourweb_hexa; }
      }
    } catch (e) { /* tant pis pour les couleurs */ }
  }
  afficherStations([...stations.values()]);
}

function afficherStations(stations) {
  const liste = $('resultats');
  liste.replaceChildren();
  if (!stations.length) {
    $('message-recherche').textContent = 'Aucune station de metro, RER, train ou tram ne correspond.';
    return;
  }
  for (const s of stations.slice(0, 8)) {
    const div = document.createElement('div');
    div.className = 'station';
    div.innerHTML = '<div class="station-nom"></div><div class="station-commune"></div><div class="lignes"></div>';
    div.querySelector('.station-nom').textContent = s.nom;
    div.querySelector('.station-commune').textContent = s.commune;
    const lignes = [...s.lignes.values()].sort((a, b) =>
      ORDRE.indexOf(a.mode) - ORDRE.indexOf(b.mode) || a.court.localeCompare(b.court, 'fr', { numeric: true }));
    for (const l of lignes) {
      const b = document.createElement('button');
      b.className = 'ligne';
      b.append(pastille(l), MODES[l.mode]);
      b.onclick = () => {
        document.querySelectorAll('.ligne.choisie').forEach(x => x.classList.remove('choisie'));
        b.classList.add('choisie');
        choisirLigne(l);
      };
      div.querySelector('.lignes').append(b);
    }
    liste.append(div);
  }
}

// --- 2. Directions : demandees a l'ESP32, qui interroge PRIM en direct ---
async function choisirLigne(l) {
  choix = l;
  $('etape-direction').hidden = false;
  $('etape-marche').hidden = false;
  const titre = $('ligne-choisie');
  titre.replaceChildren(pastille(l), ' ' + MODES[l.mode] + ' ' + l.court + ' a ' + l.station);
  titre.style.cssText = 'display:flex;align-items:center;gap:8px;font-weight:600';
  await chargerDirections();
  $('etape-direction').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function chargerDirections() {
  const l = choix;
  $('directions').replaceChildren();
  $('message-direction').textContent = 'Recherche des prochains passages...';
  majBouton();
  let donnees;
  try {
    donnees = await lireJson('/directions?ligne=' + encodeURIComponent(lignePrim(l.id))
      + '&arrets=' + encodeURIComponent([...l.arrets].map(arretPrim).join(',')));
  } catch (e) {
    $('message-direction').innerHTML = '<span class="erreur">L\'ecran ne repond pas. Est-il toujours allume ?</span>';
    return;
  }
  if (choix !== l) return;  // une autre ligne a ete touchee entre temps

  if (!donnees.directions.length) {
    $('message-direction').innerHTML = 'Aucun passage annonce en ce moment (la nuit, par exemple). '
      + '<button class="lien" id="reessayer">Reessayer</button>';
    $('reessayer').onclick = chargerDirections;
    return;
  }
  $('message-direction').textContent = 'Cochez les terminus qui vous emmenent dans le bon sens.';
  donnees.directions.sort((a, b) => a.dans - b.dans);
  for (const d of donnees.directions) {
    const label = document.createElement('label');
    label.className = 'direction';
    label.innerHTML = '<input type="checkbox"><span><span class="nom"></span><small></small></span>';
    label.querySelector('.nom').textContent = d.nom;
    label.querySelector('small').textContent = d.dans <= 0 ? 'A quai' : 'Prochain dans ' + d.dans + ' min';
    const caseACocher = label.querySelector('input');
    caseACocher.direction = d;
    caseACocher.onchange = majBouton;
    $('directions').append(label);
  }
  if (donnees.directions.length === 1) $('directions').querySelector('input').checked = true;
  majBouton();
}

function directionsCochees() {
  return [...document.querySelectorAll('#directions input:checked')].map(c => c.direction);
}

// --- 3. Temps de marche ---
function majMarche(delta) {
  marche = Math.min(60, Math.max(0, marche + delta));
  $('minutes').textContent = marche + ' min';
}
$('moins').onclick = () => majMarche(-1);
$('plus').onclick = () => majMarche(1);

// --- Enregistrer ---
function majBouton() {
  const b = $('enregistrer');
  const n = directionsCochees().length;
  b.disabled = !choix || !n;
  b.textContent = !choix ? 'Choisissez une station' : !n ? 'Cochez au moins une direction' : 'Enregistrer';
}

$('enregistrer').onclick = async () => {
  const directions = directionsCochees();
  const arrets = [...new Set(directions.flatMap(d => d.arrets))];
  const corps = new URLSearchParams({
    ligne: sansAccents(choix.court),
    station: sansAccents(choix.station),
    ligneRef: lignePrim(choix.id),
    arrets: arrets.join(','),
    directions: directions.map(d => d.nom).join('|'),
    marche,
    latitude: choix.lat,
    longitude: choix.lon,
  });
  $('enregistrer').disabled = true;
  $('enregistrer').textContent = 'Enregistrement...';
  try {
    const r = await fetch('/enregistrer', { method: 'POST', body: corps });
    if (!r.ok) throw new Error();
  } catch (e) {
    $('enregistrer').disabled = false;
    $('enregistrer').textContent = 'Echec, reessayer';
    return;
  }
  $('formulaire').hidden = true;
  $('barre').hidden = true;
  $('fini').hidden = false;
};
</script>
</body>
</html>
)html";
