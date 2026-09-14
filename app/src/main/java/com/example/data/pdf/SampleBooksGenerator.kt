package com.example.data.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleBooksGenerator {

    data class SampleBookInfo(
        val title: String,
        val author: String,
        val fileName: String,
        val pages: List<SamplePageContent>
    )

    data class SamplePageContent(
        val header: String,
        val chapterTitle: String?,
        val paragraphs: List<String>,
        val isCover: Boolean = false,
        val quote: String? = null
    )

    suspend fun createSampleBooksIfNotExist(context: Context): List<Pair<SampleBookInfo, File>> = withContext(Dispatchers.IO) {
        val booksDir = File(context.filesDir, "sample_books")
        if (!booksDir.exists()) booksDir.mkdirs()

        val sampleBooks = listOf(
            createGatsbyBook(),
            createAliceBook(),
            createMeditationsBook()
        )

        sampleBooks.map { book ->
            val pdfFile = File(booksDir, book.fileName)
            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                generatePdf(pdfFile, book)
            }
            Pair(book, pdfFile)
        }
    }

    private fun generatePdf(destFile: File, book: SampleBookInfo) {
        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points

        val margin = 50f
        val contentWidth = (pageWidth - margin * 2).toInt()

        book.pages.forEachIndexed { index, pageContent ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Background warm cream tone
            val bgPaint = Paint().apply {
                color = Color.parseColor("#FCFAF6")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            // Inner subtle border
            val borderPaint = Paint().apply {
                color = Color.parseColor("#EAE3D5")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRect(25f, 25f, pageWidth - 25f, pageHeight - 25f, borderPaint)

            if (pageContent.isCover) {
                renderCoverPage(canvas, pageWidth, pageHeight, book)
            } else {
                renderTextPage(canvas, pageWidth, pageHeight, pageContent, index + 1, book.pages.size, margin, contentWidth)
            }

            document.finishPage(page)
        }

        FileOutputStream(destFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun renderCoverPage(canvas: Canvas, width: Int, height: Int, book: SampleBookInfo) {
        // Luxury Crimson Ribbon Header
        val ribbonPaint = Paint().apply {
            color = Color.parseColor("#9E2A2B")
            style = Paint.Style.FILL
        }
        canvas.drawRect(40f, 40f, width - 40f, 60f, ribbonPaint)

        // Ornate Title
        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#1C1A17")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(book.title.uppercase(), width / 2f, height * 0.35f, titlePaint)

        // Decorative line
        val linePaint = Paint().apply {
            color = Color.parseColor("#9E2A2B")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(width * 0.3f, height * 0.40f, width * 0.7f, height * 0.40f, linePaint)

        // Author
        val authorPaint = TextPaint().apply {
            color = Color.parseColor("#69625A")
            textSize = 16f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("by ${book.author}", width / 2f, height * 0.46f, authorPaint)

        // Bottom seal
        val sealPaint = TextPaint().apply {
            color = Color.parseColor("#A8A196")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("LUMINA CLASSICS EDITION", width / 2f, height * 0.88f, sealPaint)
    }

    private fun renderTextPage(
        canvas: Canvas,
        width: Int,
        height: Int,
        pageContent: SamplePageContent,
        pageNum: Int,
        totalPages: Int,
        margin: Float,
        contentWidth: Int
    ) {
        var currentY = 55f

        // Top Header
        val headerPaint = TextPaint().apply {
            color = Color.parseColor("#8E877D")
            textSize = 10f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(pageContent.header.uppercase(), width / 2f, currentY, headerPaint)
        currentY += 25f

        // Chapter Title if present
        if (!pageContent.chapterTitle.isNullOrBlank()) {
            currentY += 20f
            val chapterPaint = TextPaint().apply {
                color = Color.parseColor("#9E2A2B")
                textSize = 16f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(pageContent.chapterTitle, width / 2f, currentY, chapterPaint)
            currentY += 30f
        }

        // Quote Box if present
        if (!pageContent.quote.isNullOrBlank()) {
            val quotePaint = TextPaint().apply {
                color = Color.parseColor("#4A443D")
                textSize = 13f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                isAntiAlias = true
            }
            val quoteLayout = StaticLayout.Builder.obtain(
                "“${pageContent.quote}”",
                0,
                pageContent.quote.length + 2,
                quotePaint,
                contentWidth - 40
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

            val quoteBg = Paint().apply {
                color = Color.parseColor("#F3ECE0")
                style = Paint.Style.FILL
            }
            canvas.drawRect(margin + 10, currentY - 5, width - margin - 10, currentY + quoteLayout.height + 15, quoteBg)

            val quoteBar = Paint().apply {
                color = Color.parseColor("#9E2A2B")
                style = Paint.Style.FILL
            }
            canvas.drawRect(margin + 10, currentY - 5, margin + 14, currentY + quoteLayout.height + 15, quoteBar)

            canvas.save()
            canvas.translate(margin + 25, currentY + 5)
            quoteLayout.draw(canvas)
            canvas.restore()

            currentY += quoteLayout.height + 30f
        }

        // Paragraphs
        val bodyPaint = TextPaint().apply {
            color = Color.parseColor("#26221E")
            textSize = 13.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        pageContent.paragraphs.forEach { paragraph ->
            val layout = StaticLayout.Builder.obtain(
                paragraph,
                0,
                paragraph.length,
                bodyPaint,
                contentWidth
            ).setLineSpacing(4f, 1.15f)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            canvas.save()
            canvas.translate(margin, currentY)
            layout.draw(canvas)
            canvas.restore()

            currentY += layout.height + 16f
        }

        // Bottom Page Number
        val pageNumPaint = TextPaint().apply {
            color = Color.parseColor("#8E877D")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$pageNum", width / 2f, height - 40f, pageNumPaint)
    }

    fun createGatsbyBook(): SampleBookInfo {
        return SampleBookInfo(
            title = "The Great Gatsby",
            author = "F. Scott Fitzgerald",
            fileName = "the_great_gatsby.pdf",
            pages = listOf(
                SamplePageContent("The Great Gatsby", null, emptyList(), isCover = true),
                SamplePageContent(
                    header = "The Great Gatsby",
                    chapterTitle = "Chapter I",
                    paragraphs = listOf(
                        "In my younger and more vulnerable years my father gave me some advice that I’ve been turning over in my mind ever since.",
                        "“Whenever you feel like criticizing any one,” he told me, “just remember that all the people in this world haven’t had the advantages that you’ve had.”",
                        "He didn’t say any more, but we’ve always been unusually communicative in a reserved way, and I understood that he meant a great deal more than that. In consequence, I’m inclined to reserve all judgments, a habit that has opened up many curious natures to me and also made me the victim of not a few veteran bores.",
                        "The abnormal mind is quick to detect and attach itself to this quality when it appears in a normal person, and so it came about that in college I was unjustly accused of being a politician, because I was privy to the secret griefs of wild, unknown men."
                    ),
                    quote = "Reserving judgments is a matter of infinite hope."
                ),
                SamplePageContent(
                    header = "The Great Gatsby",
                    chapterTitle = "The Green Light",
                    paragraphs = listOf(
                        "And as I sat there brooding on the old, unknown world, I thought of Gatsby’s wonder when he first picked out the green light at the end of Daisy’s dock.",
                        "He had come a long way to this blue lawn, and his dream must have seemed so close that he could hardly fail to grasp it. He did not know that it was already behind him, somewhere back in that vast obscurity beyond the city, where the dark fields of the republic rolled on under the night.",
                        "Gatsby believed in the green light, the orgastic future that year by year recedes before us. It eluded us then, but that’s no matter—tomorrow we will run faster, stretch out our arms farther. . . . And one fine morning——",
                        "So we beat on, boats against the current, borne back ceaselessly into the past."
                    ),
                    quote = "So we beat on, boats against the current, borne back ceaselessly into the past."
                ),
                SamplePageContent(
                    header = "The Great Gatsby",
                    chapterTitle = "Summer at West Egg",
                    paragraphs = listOf(
                        "There was music from my neighbor’s house through the summer nights. In his blue gardens men and girls came and went like moths among the whisperings and the champagne and the stars.",
                        "At high tide in the afternoon I watched his guests diving from the tower of his raft, or taking the sun on the hot sand of his beach while his two motor-boats slit the waters of the Sound, drawing aquaplanes over cataracts of foam.",
                        "On week-ends his Rolls-Royce became an omnibus, bearing parties to and from the city between nine in the morning and long past midnight, while his station wagon scampered like a brisk yellow bug to meet all trains.",
                        "And on Mondays eight servants, including an extra gardener, toiled all day with mops and scrubbing-brushes and hammers and shears, repairing the ravages of the night before."
                    )
                ),
                SamplePageContent(
                    header = "The Great Gatsby",
                    chapterTitle = "Daisy's Voice",
                    paragraphs = listOf(
                        "I looked at her again, and she had moved her eyes away from the green light and was looking down toward the harbor. She was lovely, in her white dress with her dark hair falling softly around her pale face.",
                        "“Her voice is full of money,” he said suddenly.",
                        "That was it. I’d never understood before. It was full of money—that was the inexhaustible charm that rose and fell in it, the jingle of it, the cymbals’ song of it. . . . High in a white palace the king’s daughter, the golden girl.",
                        "They were careless people, Tom and Daisy—they smashed up things and creatures and then retreated back into their money or their vast carelessness, or whatever it was that kept them together, and let other people clean up the mess they had made."
                    )
                ),
                SamplePageContent(
                    header = "The Great Gatsby",
                    chapterTitle = "Epilogue & Notes",
                    paragraphs = listOf(
                        "Published in 1925, The Great Gatsby stands as one of the quintessential masterpieces of American literature, exploring the disillusionment of the American Dream amidst the Jazz Age of the 1920s.",
                        "With its lyrical prose, meticulous structural symmetry, and profound emotional depth, Fitzgerald captured the radiant longing and tragic transience of human ambition.",
                        "Thank you for reading this Lumina Edition. Continue to flip pages seamlessly or import your own personal PDF library using the Upload button."
                    ),
                    quote = "There are only the pursued, the pursuing, the busy and the tired."
                )
            )
        )
    }

    fun createAliceBook(): SampleBookInfo {
        return SampleBookInfo(
            title = "Alice's Adventures in Wonderland",
            author = "Lewis Carroll",
            fileName = "alice_in_wonderland.pdf",
            pages = listOf(
                SamplePageContent("Alice's Adventures in Wonderland", null, emptyList(), isCover = true),
                SamplePageContent(
                    header = "Alice in Wonderland",
                    chapterTitle = "Chapter I: Down the Rabbit-Hole",
                    paragraphs = listOf(
                        "Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, “and what is the use of a book,” thought Alice “without pictures or conversations?”",
                        "So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her.",
                        "There was nothing so very remarkable in that; nor did Alice think it so very much out of the way to hear the Rabbit say to itself, “Oh dear! Oh dear! I shall be late!”"
                    ),
                    quote = "What is the use of a book without pictures or conversations?"
                ),
                SamplePageContent(
                    header = "Alice in Wonderland",
                    chapterTitle = "The Rabbit Hole",
                    paragraphs = listOf(
                        "In another moment down went Alice after it, never once considering how in the world she was to get out again.",
                        "The rabbit-hole went straight on like a tunnel for some way, and then dipped suddenly down, so suddenly that Alice had not a moment to think about stopping herself before she found herself falling down a very deep well.",
                        "Either the well was very deep, or she fell very slowly, for she had plenty of time as she went down to look about her and to wonder what was going to happen next. First, she tried to look down and make out what she was coming to, but it was too dark to see anything.",
                        "Down, down, down. Would the fall never come to an end! “I wonder how many miles I’ve fallen by this time?” she said aloud."
                    )
                ),
                SamplePageContent(
                    header = "Alice in Wonderland",
                    chapterTitle = "The Mad Tea-Party",
                    paragraphs = listOf(
                        "There was a table set out under a tree in front of the house, and the March Hare and the Hatter were having tea at it: a Dormouse was sitting between them, fast asleep, and the other two were using it as a cushion, resting their elbows on it, and talking over its head.",
                        "“Very uncomfortable for the Dormouse,” thought Alice; “only, as it’s asleep, I suppose it doesn’t mind.”",
                        "The table was a large one, but the three were all crowded together at one corner of it: “No room! No room!” they cried out when they saw Alice coming.",
                        "“There’s plenty of room!” said Alice indignantly, and she sat down in a large arm-chair at one end of the table."
                    ),
                    quote = "“Take some more tea,” the March Hare said to Alice, very earnestly."
                ),
                SamplePageContent(
                    header = "Alice in Wonderland",
                    chapterTitle = "Curiouser and Curiouser!",
                    paragraphs = listOf(
                        "“Curiouser and curiouser!” cried Alice (she was so much surprised, that for the moment she quite forgot how to speak good English); “now I’m opening out like the largest telescope that ever was! Good-bye, feet!”",
                        "“It’s no use going back to yesterday, because I was a different person then,” Alice remarked with a smile.",
                        "“Why, sometimes I’ve believed as many as six impossible things before breakfast,” said the Queen.",
                        "This concludes the Lumina Edition excerpt of Alice's Adventures in Wonderland."
                    )
                )
            )
        )
    }

    fun createMeditationsBook(): SampleBookInfo {
        return SampleBookInfo(
            title = "Meditations",
            author = "Marcus Aurelius",
            fileName = "meditations_marcus_aurelius.pdf",
            pages = listOf(
                SamplePageContent("Meditations", null, emptyList(), isCover = true),
                SamplePageContent(
                    header = "Meditations",
                    chapterTitle = "Book II: On the River Gran",
                    paragraphs = listOf(
                        "When you wake up in the morning, tell yourself: The people I deal with today will be meddling, ungrateful, arrogant, dishonest, jealous, and surly. They are like this because they cannot distinguish good from evil.",
                        "But I have seen the beauty of good, and the ugliness of evil, and have recognized that the wrongdoer has a nature related to my own—not of the same blood or birth, but the same mind, and possessing a share of the divine.",
                        "None of them can hurt me. No one can implicate me in ugliness. Nor can I feel angry at my kin, or hate him. We were born to work together like feet, hands, and eyes, like the two rows of teeth, upper and lower. To obstruct each other is unnatural."
                    ),
                    quote = "You have power over your mind - not outside events. Realize this, and you will find strength."
                ),
                SamplePageContent(
                    header = "Meditations",
                    chapterTitle = "Book IV: The Citadel of the Soul",
                    paragraphs = listOf(
                        "People look for retreats for themselves, in the country, by the coast, or in the hills. There is nowhere that a person can find a more peaceful and trouble-free retreat than in his own mind.",
                        "So constantly give yourself this retreat, and renew yourself. Let your basic principles be brief and fundamental, the kind that will at once wash away all sorrow and send you back without irritation to the life to which you must return.",
                        "Remember that the soul becomes dyed with the color of its thoughts. Steep it therefore in such a train of thoughts as this: Anywhere a man can live, he can live well.",
                        "Dwell on the beauty of life. Watch the stars, and see yourself running with them."
                    ),
                    quote = "The soul becomes dyed with the color of its thoughts."
                ),
                SamplePageContent(
                    header = "Meditations",
                    chapterTitle = "Book VII: Transient Nature",
                    paragraphs = listOf(
                        "Time is a river, a violent current of events, glimpsed once and already carried past us, and another follows and is gone.",
                        "Do not act as if you were going to live ten thousand years. Death hangs over you. While you live, while it is in your power, be good.",
                        "Accept the things to which fate binds you, and love the people with whom fate brings you together, but do so with all your heart.",
                        "Never let the future disturb you. You will meet it, if you have to, with the same weapons of reason which today arm you against the present."
                    ),
                    quote = "Waste no more time arguing about what a good man should be. Be one."
                ),
                SamplePageContent(
                    header = "Meditations",
                    chapterTitle = "Reflections & Closing",
                    paragraphs = listOf(
                        "Marcus Aurelius Antoninus Augustus reigned as Roman Emperor from 161 to 180 AD and was the last of the rulers known as the Five Good Emperors.",
                        "His personal writings, titled Meditations, were never meant for publication. They represent private exercises in Stoic philosophy, resilience, empathy, and duty.",
                        "Enjoy your focused reading journey with Lumina Reader."
                    )
                )
            )
        )
    }
}
