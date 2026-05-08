package com.neon.ascent.core.lore.domain.usecase

import com.neon.ascent.core.lore.data.LoreRepository
import com.neon.ascent.core.lore.data.Megacorp
import javax.inject.Inject

class GetMegacorpUseCase @Inject constructor(
    private val repository: LoreRepository
) {
    operator fun invoke(id: String): Megacorp? = repository.getMegacorp(id)
}
