package com.marclw.lolstats.recommendation;

/**
 * GameState -> RecommendedRunePage. See BuildRecommender for the same
 * heuristic-vs-statistical-vs-ML split rationale; runes are kept on a
 * separate interface rather than folded into BuildRecommender since a
 * rune page and an item build are different enough in shape (fixed
 * slots + playstyle notes, vs. an ordered purchase list) that one
 * combined "recommend everything" method would be doing two jobs.
 */
public interface RuneRecommender {

    RecommendedRunePage recommend(GameState gameState);
}
