package com.kirox.app.security.shield

/** Built-in lists of «Рентген ссылки». Updated with each release; nothing is downloaded. */
object ShieldDomains {
    /** Important sites people get phished for (Telegram, KZ / RU banks and state services, big platforms). */
    val KEY: List<String> = listOf(
        // Telegram
        "telegram.org", "t.me", "telegram.me", "telegram.dog", "telegra.ph", "fragment.com", "ton.org", "wallet.tg", "tonkeeper.com",
        // Kazakhstan: banks, payments, state
        "kaspi.kz", "halykbank.kz", "homebank.kz", "onlinebank.kz", "jusan.kz", "jusanbank.kz", "forte.kz", "fortebank.com", "bcc.kz", "berekebank.kz",
        "eubank.kz", "centercredit.kz", "freedombank.kz", "ffin.kz", "rbk.kz", "homecredit.kz", "atfbank.kz", "vtb.kz", "altynbank.kz", "nurbank.kz",
        "shinhan.kz", "alfabank.kz", "egov.kz", "gov.kz", "elicense.kz", "enbek.kz", "kgd.gov.kz", "nationalbank.kz", "salyk.kz", "kazpost.kz",
        "beeline.kz", "kcell.kz", "tele2.kz", "altel.kz", "kazakhtelecom.kz", "olx.kz", "krisha.kz", "kolesa.kz", "wildberries.kz", "chocofamily.kz",
        "air.kz", "airastana.com", "flyarystan.com", "railways.kz", "glovo.kz", "yandex.kz", "mail.kz", "hh.kz",
        // Russia: banks, payments, state
        "sberbank.ru", "sber.ru", "online.sberbank.ru", "tinkoff.ru", "tbank.ru", "vtb.ru", "alfabank.ru", "gazprombank.ru", "raiffeisen.ru", "rshb.ru",
        "pochtabank.ru", "sovcombank.ru", "otkritie.ru", "mkb.ru", "psbank.ru", "rosbank.ru", "uralsib.ru", "homecredit.ru", "ozon.ru", "ozonbank.ru",
        "qiwi.com", "yoomoney.ru", "nspk.ru", "cbr.ru", "gosuslugi.ru", "nalog.gov.ru", "nalog.ru", "mos.ru", "pfr.gov.ru", "sfr.gov.ru", "fssp.gov.ru",
        "mvd.ru", "pochta.ru", "rzd.ru", "avito.ru", "wildberries.ru", "wb.ru", "yandex.ru", "ya.ru", "mail.ru", "vk.com", "ok.ru", "dzen.ru",
        "mts.ru", "beeline.ru", "megafon.ru", "tele2.ru", "rt.ru", "kinopoisk.ru", "hh.ru", "sbermarket.ru", "dns-shop.ru", "mvideo.ru", "aliexpress.ru",
        // Global
        "google.com", "google.kz", "google.ru", "gmail.com", "youtube.com", "apple.com", "icloud.com", "microsoft.com", "live.com", "outlook.com",
        "office.com", "github.com", "whatsapp.com", "instagram.com", "facebook.com", "messenger.com", "twitter.com", "x.com", "tiktok.com", "linkedin.com",
        "paypal.com", "visa.com", "mastercard.com", "binance.com", "bybit.com", "okx.com", "coinbase.com", "kraken.com", "metamask.io", "trustwallet.com",
        "steampowered.com", "steamcommunity.com", "epicgames.com", "playstation.com", "xbox.com", "netflix.com", "spotify.com", "amazon.com", "aliexpress.com", "booking.com",
        "airbnb.com", "zoom.us", "discord.com", "openai.com", "wikipedia.org", "dropbox.com", "yahoo.com", "ebay.com", "skype.com", "viber.com",
    )

    private val keySet = KEY.toHashSet()
    fun isKey(site: String) = site in keySet

    /** Brand names that must not appear inside someone else's address. Short / common words are left out. */
    val BRANDS: List<String> = listOf(
        "telegram", "kaspi", "halyk", "halykbank", "homebank", "jusan", "fortebank", "egov", "sberbank", "sber", "tinkoff", "gosuslugi", "alfabank",
        "gazprombank", "raiffeisen", "sovcombank", "pochtabank", "wildberries", "paypal", "binance", "metamask", "trustwallet", "tonkeeper", "steamcommunity",
        "whatsapp", "instagram", "icloud", "appleid", "fragment", "yoomoney", "airastana", "kazpost", "nalog",
    )

    val SHORTENERS: Set<String> = setOf(
        "bit.ly", "tinyurl.com", "clck.ru", "goo.gl", "t.co", "cutt.ly", "is.gd", "ow.ly", "rebrand.ly", "shorturl.at", "tiny.cc", "u.to", "vk.cc",
        "qps.ru", "rb.gy", "s.id", "lnkd.in", "buff.ly", "bit.do", "v.gd", "surl.li", "tinyurl.ru", "to.click", "l.wl.co", "clc.am", "shorte.st", "adf.ly",
    )

    /** Cheap zones often used for throw-away phishing sites; they add weight only next to another sign. */
    val RISKY_ZONES: Set<String> = setOf("top", "xyz", "click", "live", "icu", "buzz", "monster", "rest", "cfd", "sbs", "quest", "cyou", "zip", "mov", "tk", "ml", "ga", "cf", "gq", "work", "support", "online", "site", "fun", "space", "website", "shop", "store", "info")

    val TWO_PART_ZONES: Set<String> = setOf(
        "com.kz", "org.kz", "gov.kz", "edu.kz", "net.kz", "co.uk", "org.uk", "gov.uk", "ac.uk", "com.ru", "org.ru", "net.ru", "msk.ru", "spb.ru", "gov.ru",
        "com.ua", "org.ua", "co.jp", "com.au", "com.br", "com.tr", "com.cn", "co.kr", "co.in", "com.uz", "co.uz", "com.kg", "gov.uz", "github.io", "web.app", "pages.dev", "vercel.app", "netlify.app",
    )
}
