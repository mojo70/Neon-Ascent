package com.neon.ascent.core.lore.domain.usecase

import com.neon.ascent.core.lore.data.LoreRepository
import com.neon.ascent.core.lore.data.Megacorp
import javax.inject.Inject

class GetAllMegacorpsUseCase @Inject constructor(
    private val repository: LoreRepository
) {
    operator fun invoke(): List<Megacorp> = repository.getAllMegacorps()
}
