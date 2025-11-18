package kr.hhplus.be.server.user.entity

import jakarta.persistence.Entity
import kr.hhplus.be.server.common.BaseEntity

@Entity
class User(
    val userName: String,
    val email: String,
    val password: String,
) : BaseEntity()
