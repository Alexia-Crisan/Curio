package com.example.curio.feature.tree

import androidx.annotation.StringRes
import com.example.curio.R
import com.example.curio.domain.GrowthStage
import com.example.curio.domain.NodeState

object TreeVisuals {
    @StringRes fun stage(s: GrowthStage): Int = when (s) {
        GrowthStage.SEED -> R.string.tree_stage_seed
        GrowthStage.SPROUT -> R.string.tree_stage_sprout
        GrowthStage.SAPLING -> R.string.tree_stage_sapling
        GrowthStage.TREE -> R.string.tree_stage_tree
        GrowthStage.MATURE -> R.string.tree_stage_mature
        GrowthStage.BLOOM -> R.string.tree_stage_bloom
    }

    @StringRes fun state(s: NodeState): Int = when (s) {
        NodeState.LOCKED -> R.string.tree_state_locked
        NodeState.SEEDED -> R.string.tree_state_seeded
        NodeState.GROWING -> R.string.tree_state_growing
        NodeState.COMPLETE -> R.string.tree_state_complete
    }
}
