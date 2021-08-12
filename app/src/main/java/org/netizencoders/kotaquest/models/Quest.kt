package org.netizencoders.kotaquest.models

data class Quest(
    var ID: String? = "",
    var Title: String? = "",
    var Location: String? = "",
    var Description: String? = "",
    var ImageURL: String? = "",
    var Status: String? = "",
    var Poster: String? = "",
    var Quester: String? = "",
    var DatePosted: String? = "",
    var DateCompleted: String? = "",
    var ReportDescription: String? = "",
    var ReportImageURL: String? = ""
)