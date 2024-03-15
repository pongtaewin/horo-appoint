package com.firebaseapp.horoappoint.settings

import kotlin.random.Random

data class UserTab(
    val name: String,
    val subject: String,
    val desc: String,
    val src: String = "https://picsum.photos/64?random=${rnd.nextInt()}",
    val selected: Boolean = false,
    val unread: Boolean = false
) {

    companion object{
        val rnd = Random(0)
        val defaultUserTabs: List<UserTab> = listOf(
            UserTab(
                "Tilo Mitra",
                "Hello from Toronto",
                "Hey, I just wanted to check in with you from Toronto. I got here earlier today."
            ),
            UserTab(
                "Eric Ferraiuolo",
                "Hello from Toronto",
                "Hey, I had some feedback for pull request #51. We should center the menu so it looks better on mobile.\n"
            ),
            UserTab(
                "Reid Burke",
                "Re: Design Language",
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa."
            ),
            UserTab(
                "Yahoo! Finance",
                "How to protect your finances from winter storms",
                "Mauris tempor mi vitae sem aliquet pharetra. Fusce in dui purus, nec malesuada mauris."
            ),
            UserTab(
                "Yahoo! News",
                "Summary for April 3rd, 2021",
                "We found 10 news articles that you may like."
            ),
        )
    }


}