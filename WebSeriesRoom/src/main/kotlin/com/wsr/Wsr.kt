package com.wsr

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Wsr : MainAPI() {
    override var mainUrl              = "https://webseriesroom.fun"
    override var name                 = "WebSeriesRoom"
    override val hasMainPage          = true
    override var lang                 = "hi"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
            "trending" to "Trending",
            "latest-webseries" to "Latest"
    )

   override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/webseries/$page/${request.data}").document
    
        val results = document
            .select("div.row div.col")
            .mapNotNull { it.toSearchResult() }
    
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = results
            ),
            hasNext = true
        )
    }
    
    private fun Element.toSearchResult(): SearchResponse? {
        val anchor    = this.selectFirst("a") ?: return null
        val title     = fixTitle(anchor.attr("title"))
        val href      = fixUrl(anchor.attr("href"))
        val posterUrl = fixUrlNull(anchor.selectFirst("img")?.attr("data-src"))
    
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }
    var starLink = ""

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val searchResponse = mutableListOf<SearchResponse>()
    
        if (query.startsWith("star:", ignoreCase = true)) {
            if (page == 1) {
                starLink = ""
                val starName = query.removePrefix("star:").trim()
                val document = app.get("$mainUrl/search/list?q=$starName&search=star").document
    
                starLink = document.selectFirst("div.row.row-cols-3 a.card.movie")?.attr("href").orEmpty()
            }
    
            if (starLink.isNotEmpty()) {
                val pagedStarUrl = starLink.replace("star/", "star/$page/")
                val starDoc = app.get(pagedStarUrl).document
                val results = starDoc.select("div.row div.col").mapNotNull { it.toSearchResult() }
                searchResponse.addAll(results)
            }
        } else {
            val document = app.get("$mainUrl/search/list?q=$query&search=webseries&page=$page").document
            val results = document.select("div.row div.col").mapNotNull { it.toSearchResult() }
            searchResponse.addAll(results)
        }
    
        return searchResponse.toNewSearchResponseList()
    }
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
    
        val title  = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
    
        val details = document.select("div.col-12.col-md-8 .card.movie-show-i p")
            .joinToString("\n") { it.text().trim() }
    
        val episodes = document.select("#playlist-tab button")
            .mapNotNull { btn ->
                val targetId = btn.attr("data-bs-target").removePrefix("#")
                val iframeSrc = document.selectFirst("div#$targetId iframe")?.attr("data-src")
                iframeSrc?.let { src ->
                    newEpisode(src) {
                        this.name = btn.text().trim()
                    }
                }
            }
    
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot= details
        }
    }
    
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        loadExtractor(
            data,
            "$mainUrl/",
            subtitleCallback,
            callback
        )
        return true
    }
}
