package com.myreviews.app.data.database.mapper

import com.myreviews.app.data.database.entity.ReviewEntity
import com.myreviews.app.domain.model.Review

object ReviewMapper {
    fun entityToModel(entity: ReviewEntity): Review {
        return Review(
            id = entity.id,
            restaurantId = entity.restaurantId,
            restaurantName = entity.restaurantName,
            restaurantLat = entity.restaurantLat,
            restaurantLon = entity.restaurantLon,
            restaurantAddress = entity.restaurantAddress,
            rating = entity.rating,
            comment = entity.comment,
            visitDate = entity.visitDate,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            userId = entity.userId,
            userName = entity.userName
        )
    }
    
    fun modelToEntity(model: Review): ReviewEntity {
        return ReviewEntity(
            id = model.id,
            restaurantId = model.restaurantId,
            restaurantName = model.restaurantName,
            restaurantLat = model.restaurantLat,
            restaurantLon = model.restaurantLon,
            restaurantAddress = model.restaurantAddress,
            rating = model.rating,
            comment = model.comment,
            visitDate = model.visitDate,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            userId = model.userId,
            userName = model.userName
        )
    }
}