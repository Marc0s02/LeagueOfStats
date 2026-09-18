package com.marclw.lolstats.ingest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.recommendation.GameState;
import com.marclw.lolstats.recommendation.TeamComposition;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a GameState from Riot's Spectator-V5 active-game response.
 * RiotSpectatorClient only fetches the raw JSON (see that class); the
 * actual field parsing lives here rather than in a separate
 * "SpectatorDataParser" the way Match-V5 has MatchDataParser - Spectator-
 * V5's participant shape is small enough (championId, teamId, puuid) that
 * splitting parsing into its own class isn't earning its keep the way it
 * does for Match-V5's much larger match/timeline payloads.
 *
 * Requires the caller's own puuid to know which participant is "me" vs.
 * an ally - get this once via RiotMatchClient.fetchPuuidByRiotId() and
 * hold onto it, rather than re-fetching per call.
 */
public class LiveGameStateProvider implements GameStateProvider {

    private final RiotSpectatorClient spectatorClient;
    private final ChampionRepository championRepository;
    private String myPuuid;

    public LiveGameStateProvider(RiotSpectatorClient spectatorClient, ChampionRepository championRepository) {
        this.spectatorClient = spectatorClient;
        this.championRepository = championRepository;
    }

    public void setMyPuuid(String myPuuid) {
        this.myPuuid = myPuuid;
    }

    @Override
    public GameState getGameState() {
        if (myPuuid == null) {
            throw new IllegalStateException("setMyPuuid() must be called before getGameState()");
        }

        String json = spectatorClient.fetchActiveGame(myPuuid);
        if (json == null) {
            return null; // not currently in a game
        }

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray participants = root.getAsJsonArray("participants");

        Champion myChampion = null;
        List<Champion> allyChampions = new ArrayList<>();
        List<Champion> enemyChampions = new ArrayList<>();
        Integer myTeamId = null;

        // First pass: find my own participant to establish which teamId is "ally".
        for (var element : participants) {
            JsonObject p = element.getAsJsonObject();
            if (myPuuid.equals(p.get("puuid").getAsString())) {
                myTeamId = p.get("teamId").getAsInt();
                myChampion = findChampionById(p.get("championId").getAsInt());
                break;
            }
        }

        if (myTeamId == null) {
            throw new IllegalStateException(
                    "Active game JSON didn't contain a participant matching puuid " + myPuuid
                            + " - Spectator-V5 response shape may have changed.");
        }

        for (var element : participants) {
            JsonObject p = element.getAsJsonObject();
            if (myPuuid.equals(p.get("puuid").getAsString())) {
                continue; // already captured as myChampion
            }
            Champion champion = findChampionById(p.get("championId").getAsInt());
            if (champion == null) {
                continue; // TODO: decide how to surface an unresolvable championId rather than silently skipping
            }
            if (p.get("teamId").getAsInt() == myTeamId) {
                allyChampions.add(champion);
            } else {
                enemyChampions.add(champion);
            }
        }

        GameState gameState = new GameState(
                myChampion, new TeamComposition(allyChampions), new TeamComposition(enemyChampions), GameState.Source.LIVE);
        if (root.has("gameLength")) {
            gameState.setElapsedGameTimeSeconds(root.get("gameLength").getAsLong());
        }
        return gameState;
    }

    private Champion findChampionById(int championId) {
        return championRepository.getChampions().stream()
                .filter(c -> c.getId() == championId)
                .findFirst()
                .orElse(null);
    }
}
