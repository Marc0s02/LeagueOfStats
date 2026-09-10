package com.marclw.lolstats.ingest;

/**
 * Manual smoke test: resolves a Riot ID (gameName#tagLine) to a puuid,
 * then lists that account's recent match IDs - so you have a real
 * matchId to hand to SmokeMatch.
 *
 * Usage:
 *   RIOT_API_KEY=your-key-here java SmokeMatchIds <gameName> <tagLine> [count] [regionalBaseUrl]
 *
 * gameName/tagLine come from your Riot ID as shown in the League client
 * (e.g. "Faker#KR1" -> gameName=Faker, tagLine=KR1). count defaults to 5.
 */
public class SmokeMatchIds {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: SmokeMatchIds <gameName> <tagLine> [count] [regionalBaseUrl]");
            System.exit(1);
        }
        String gameName = args[0];
        String tagLine = args[1];
        int count = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        String regionalBaseUrl = args.length > 3 ? args[3] : "https://europe.api.riotgames.com";

        String apiKey = System.getenv("RIOT_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("RIOT_API_KEY environment variable is not set.");
            System.exit(1);
        }

        RiotMatchClient client = new RiotMatchClient(apiKey, regionalBaseUrl);

        System.out.println("Resolving " + gameName + "#" + tagLine + " -> puuid...");
        String puuid = client.fetchPuuidByRiotId(gameName, tagLine);
        System.out.println("puuid: " + puuid);

        System.out.println("Fetching last " + count + " match IDs...");
        String idsJson = client.fetchMatchIdsForPuuid(puuid, count);
        System.out.println(idsJson);
    }
}