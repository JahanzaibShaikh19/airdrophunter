package com.airdrophunter.repository

import com.airdrophunter.domain.ProUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProUserRepository : JpaRepository<ProUser, Long> {
    fun findByEmail(email: String): Optional<ProUser>
    fun findByLicenseKey(licenseKey: String): Optional<ProUser>
    fun findByEmailAndIsActiveTrue(email: String): Optional<ProUser>
    fun existsByEmailAndIsActiveTrue(email: String): Boolean
    fun existsByGumroadSaleId(saleId: String): Boolean
}
