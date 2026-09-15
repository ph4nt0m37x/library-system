package com.borrowingservice.model

import com.borrowingservice.model.entity.BanPeriod
import com.borrowingservice.model.view.BanPeriodView
import jakarta.persistence.Column
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BanReasonMappingTest {
    @Test
    fun `ban reasons use extensible varchar columns instead of native database enums`() {
        listOf(BanPeriod::class.java, BanPeriodView::class.java).forEach { entityType ->
            val reasonField = entityType.getDeclaredField("reason")

            assertEquals(SqlTypes.VARCHAR, reasonField.getAnnotation(JdbcTypeCode::class.java).value)
            assertEquals("varchar(64)", reasonField.getAnnotation(Column::class.java).columnDefinition)
        }
    }
}
