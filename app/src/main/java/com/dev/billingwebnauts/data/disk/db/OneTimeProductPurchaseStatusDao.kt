/*
 * Copyright 2023 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.dev.billingwebnauts.data.disk.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dev.billingwebnauts.data.otps.OneTimeProductPurchaseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OneTimeProductPurchaseStatusDao {
    @Query("SELECT * FROM oneTimeProductPurchases")
    fun getAll(): Flow<List<OneTimeProductPurchaseStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(oneTimeProducts: List<OneTimeProductPurchaseStatus>)

    @Query("DELETE FROM oneTimeProductPurchases")
    suspend fun deleteAll()
}