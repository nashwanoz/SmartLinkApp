package com.khamrnet.app.data.model

data class UserEntity(
    val id: Long = 1L,
    val username: String = "admin",
    val userCode: String = "101",
    val displayName: String = "المدير",
    val role: String = "ADMIN",
    val password: String = "1234"
)
