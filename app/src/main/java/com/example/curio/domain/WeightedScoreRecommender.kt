package com.example.curio.domain

interface Recommender {
    fun rank(
        candidates: List<Discovery>,
        savedIds: Set<String>,
        viewedIds: Set<String>,
    ): List<Discovery>
}

class WeightedScoreRecommender : Recommender {
    override fun rank(
        candidates: List<Discovery>,
        savedIds: Set<String>,
        viewedIds: Set<String>,
    ): List<Discovery> {
        return candidates.sortedByDescending { candidate ->
            var score = 0
            if (candidate.id in savedIds) score += 5
            if (candidate.id !in viewedIds) score += 3 else score -= 2
            score
        }
    }
}
