package com.app.testik.domain.usecase

import com.app.testik.domain.mapper.toDomain
import com.app.testik.domain.model.CategoryType
import com.app.testik.domain.model.Result
import com.app.testik.domain.model.TestsModel
import com.app.testik.domain.repository.TestRepository
import com.app.testik.domain.util.ResultWrapper
import com.app.testik.domain.util.ResultWrapperImpl
import com.app.testik.util.Constants.QUERY_LIMIT
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import javax.inject.Inject

class GetCategoryTestsUseCase @Inject constructor(
    private val testRepository: TestRepository
) : ResultWrapper by ResultWrapperImpl() {

    suspend operator fun invoke(
        category: CategoryType,
        fromCache: Boolean = false,
        snapshot: QuerySnapshot? = null,
        author: String? = null
    ): Result<TestsModel> =
        wrap(
            block = {
                testRepository.getTestsByCategory(
                    category = category.title,
                    limit = QUERY_LIMIT,
                    source = if (fromCache) Source.CACHE else Source.DEFAULT,
                    snapshot = snapshot,
                    author = author
                )
            },
            mapper = { it!!.toDomain() }
        )
}