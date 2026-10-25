package com.lagando.deslize.model

/**
 * Representa uma ação recente de swipe (manter ou descartar) para permitir desfazer (Undo).
 */
data class UndoAction(
    val item: MediaItem,
    val status: String // MediaStatus.TRASHED ou MediaStatus.KEPT
)
