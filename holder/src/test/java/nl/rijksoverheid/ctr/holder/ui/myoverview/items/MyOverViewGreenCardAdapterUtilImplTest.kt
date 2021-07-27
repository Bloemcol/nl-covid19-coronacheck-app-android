package nl.rijksoverheid.ctr.holder.ui.myoverview.items

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import nl.rijksoverheid.ctr.holder.persistence.database.entities.*
import nl.rijksoverheid.ctr.holder.persistence.database.models.GreenCard
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.CredentialUtil
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.GreenCardUtil
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.OriginState
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.OriginUtil
import nl.rijksoverheid.ctr.holder.ui.myoverview.utils.TestResultAdapterItemUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
@RunWith(RobolectricTestRunner::class)
class MyOverViewGreenCardAdapterUtilImplTest: AutoCloseKoinTest() {
        val readEuropeanCredentialVaccination = "{\"credentialVersion\":1,\"issuer\":\"NL\",\"issuedAt\":1627294308,\"expirationTime\":1629717843,\"dcc\":{\"ver\":\"1.3.0\",\"dob\":\"1960-01-01\",\"nam\":{\"fn\":\"Bouwer\",\"fnt\":\"BOUWER\",\"gn\":\"Bob\",\"gnt\":\"BOB\"},\"v\":[{\"tg\":\"840539006\",\"vp\":\"1119349007\",\"mp\":\"EU\\/1\\/20\\/1528\",\"ma\":\"ORG-100030215\",\"dn\":1,\"sd\":1,\"dt\":\"2021-07-18\",\"co\":\"NL\",\"is\":\"Ministry of Health Welfare and Sport\",\"ci\":\"URN:UCI:01:NL:FE6BOX7GLBBZTH6K5OFO42#1\"}],\"t\":null,\"r\":null}}"

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }

    private val credentialUtil = mockk<CredentialUtil>(relaxed = true)
    private val testResultAdapterItemUtil: TestResultAdapterItemUtil = mockk(relaxed = true)
    private val greenCardUtil: GreenCardUtil = mockk(relaxed = true)
    private val originUtil = mockk<OriginUtil>(relaxed = true).apply {
        every { hideSubtitle(any(), any()) } returns false
    }

    private val myOverViewGreenCardAdapterUtil: MyOverViewGreenCardAdapterUtil by lazy {
        MyOverViewGreenCardAdapterUtilImpl(context, credentialUtil, testResultAdapterItemUtil, greenCardUtil, originUtil)
    }

    private val viewBinding = object: ViewBindingWrapper {

        private val p1Title = TextView(context)
        private val p2Title = TextView(context)
        private val p3Title = TextView(context)
        private val p1Subtitle = TextView(context)
        private val p2Subtitle = TextView(context)
        private val p3Subtitle = TextView(context)
        private val lastText = TextView(context)

        override val proof1Title: TextView
            get() = p1Title
        override val proof2Title: TextView
            get() = p2Title
        override val proof3Title: TextView
            get() = p3Title
        override val proof1Subtitle: TextView
            get() = p1Subtitle
        override val proof2Subtitle: TextView
            get() = p2Subtitle
        override val proof3Subtitle: TextView
            get() = p3Subtitle
        override val expiresIn: TextView
            get() = lastText
    }

    @Test
    fun europeanTest() {
        every { credentialUtil.getTestType(any()) } returns "NAAT"
        val greenCard = greenCard(GreenCardType.Eu)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Testbewijs: PCR (NAAT)", viewBinding.proof1Title.text)
        assertEquals("Testdatum: dinsdag 27 juli 11:10", viewBinding.proof1Subtitle.text)
    }

    @Test
    fun domesticTest() {
        val greenCard = greenCard(GreenCardType.Domestic)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Testbewijs:", viewBinding.proof3Title.text)
        assertEquals("geldig t/m dinsdag 27 juli 11:13", viewBinding.proof3Subtitle.text)
    }

    @Test
    fun europeanVaccination() {
        every { credentialUtil.getVaccinationDoses(any(), any()) } returns "dosis 2 van 2"
        val greenCard = greenCard(GreenCardType.Eu, OriginType.Vaccination)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Vaccinatiebewijs: dosis 2 van 2", viewBinding.proof1Title.text)
        assertEquals("Vaccinatiedatum: 27 juli 2021", viewBinding.proof1Subtitle.text)
    }

    @Test
    fun domesticVaccination() {
        val greenCard = greenCard(GreenCardType.Domestic, OriginType.Vaccination)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Vaccinatiebewijs:", viewBinding.proof1Title.text)
        assertEquals("geldig vanaf 27 juli 2021  ", viewBinding.proof1Subtitle.text)
    }

    @Test
    fun europeanRecovery() {
        val greenCard = greenCard(GreenCardType.Eu, OriginType.Recovery)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Herstelbewijs:", viewBinding.proof1Title.text)
        assertEquals("geldig t/m 27 jul 2021", viewBinding.proof1Subtitle.text)
    }

    @Test
    fun domesticRecovery() {
        val greenCard = greenCard(GreenCardType.Domestic, OriginType.Recovery)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Herstelbewijs:", viewBinding.proof2Title.text)
        assertEquals("geldig t/m 27 jul 2021", viewBinding.proof2Subtitle.text)
    }

    private fun greenCard(greenCardType: GreenCardType, originType: OriginType = OriginType.Test): GreenCard {
        // 2021-07-27T09:10Z
        val eventTime = OffsetDateTime.now(Clock.fixed(Instant.ofEpochSecond(1627377000), ZoneId.of("UTC")))
        // 2021-07-27T09:11:40Z
        val validFrom = OffsetDateTime.now(Clock.fixed(Instant.ofEpochSecond(1627377100), ZoneId.of("UTC")))
        // 2021-07-27T09:13:20Z
        val expirationTime = OffsetDateTime.now(Clock.fixed(Instant.ofEpochSecond(1627377200), ZoneId.of("UTC")))
        val credentialEntity = CredentialEntity(
            id = 1,
            greenCardId = 1,
            data = "".toByteArray(),
            credentialVersion = 2,
            validFrom = validFrom,
            expirationTime = expirationTime,
        )

        val originEntity = OriginEntity(
            id = 1,
            greenCardId = 1,
            type = originType,
            eventTime = eventTime,
            expirationTime = expirationTime,
            validFrom = validFrom,
        )

        val greenCard = GreenCard(
            greenCardEntity = GreenCardEntity(
                id = 1,
                walletId = 1,
                type = greenCardType
            ),
            origins = listOf(originEntity),
            credentialEntities = listOf(credentialEntity),
        )

        return greenCard
    }
}