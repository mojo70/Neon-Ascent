package com.neon.ascent.core.data.local.seed

import com.neon.ascent.core.data.local.entity.SanctuaryPassageEntity

object SanctuarySeeds {
    val SANCTUARY_SEED: List<SanctuaryPassageEntity> = listOf(
        SanctuaryPassageEntity(
            id = "psalm_23",
            book = "Psalm",
            chapter = 23,
            verses = "1-6",
            reference = "Psalm 23",
            translation = "ESV",
            title = "The Lord Is My Shepherd",
            text = """The LORD is my shepherd; I shall not want.
He makes me lie down in green pastures. He leads me beside still waters.
He restores my soul. He leads me in paths of righteousness for his name’s sake.

Even though I walk through the valley of the shadow of death, I will fear no evil, for you are with me; your rod and your staff, they comfort me.

You prepare a table before me in the presence of my enemies; you anoint my head with oil; my cup overflows.

Surely goodness and mercy shall follow me all the days of my life, and I shall dwell in the house of the LORD forever.""",
            themes = listOf("shepherd", "provision", "protection", "rest"),
            tags = listOf("biggs-priority", "protection-core"),
            sourceNote = null,
            sortOrder = 1,
            isFavoriteDefault = true
        ),
        SanctuaryPassageEntity(
            id = "psalm_91",
            book = "Psalm",
            chapter = 91,
            verses = "1-16",
            reference = "Psalm 91",
            translation = "ESV",
            title = "My Refuge and My Fortress",
            text = """He who dwells in the shelter of the Most High will abide in the shadow of the Almighty.
I will say to the LORD, “My refuge and my fortress, my God, in whom I trust.”

For he will deliver you from the snare of the fowler and from the deadly pestilence.
He will cover you with his pinions, and under his wings you will find refuge; his faithfulness is a shield and buckler.

You will not fear the terror of the night, nor the arrow that flies by day, nor the pestilence that stalks in darkness, nor the destruction that wastes at noonday.

A thousand may fall at your side, ten thousand at your right hand, but it will not come near you.
You will only look with your eyes and see the recompense of the wicked.

Because you have made the LORD your dwelling place—the Most High, who is my refuge—
no evil shall be allowed to befall you, no plague come near your tent.

For he will command his angels concerning you to guard you in all your ways.
On their hands they will bear you up, lest you strike your foot against a stone.
You will tread on the lion and the adder; the young lion and the serpent you will trample underfoot.

“Because he holds fast to me in love, I will deliver him; I will protect him, because he knows my name.
When he calls to me, I will answer him; I will be with him in trouble; I will rescue him and honor him.
With long life I will satisfy him and show him my salvation.”""",
            themes = listOf("protection", "refuge", "angels", "pestilence"),
            tags = listOf("biggs-priority", "protection-core"),
            sourceNote = null,
            sortOrder = 2,
            isFavoriteDefault = true
        ),
        SanctuaryPassageEntity(
            id = "eph_6_10_18",
            book = "Ephesians",
            chapter = 6,
            verses = "10-18",
            reference = "Ephesians 6:10-18",
            translation = "ESV",
            title = "The Whole Armor of God",
            text = """Finally, be strong in the Lord and in the strength of his might.
Put on the whole armor of God, that you may be able to stand against the schemes of the devil.
For we do not wrestle against flesh and blood, but against the rulers, against the authorities, against the cosmic powers over this present darkness, against the spiritual forces of evil in the heavenly places.
Therefore take up the whole armor of God, that you may be able to withstand in the evil day, and having done all, to stand firm.
Stand therefore, having fastened on the belt of truth, and having put on the breastplate of righteousness, and, as shoes for your feet, having put on the readiness given by the gospel of peace.
In all circumstances take up the shield of faith, with which you can extinguish all the flaming darts of the evil one; and take the helmet of salvation, and the sword of the Spirit, which is the word of God, praying at all times in the Spirit, with all prayer and supplication.
To that end, keep alert with all perseverance, making supplication for all the saints.""",
            themes = listOf("warfare", "armor", "authority"),
            tags = listOf("biggs-priority", "warfare-core"),
            sourceNote = null,
            sortOrder = 3,
            isFavoriteDefault = true
        ),
        SanctuaryPassageEntity(
            id = "isa_53_5",
            book = "Isaiah",
            chapter = 53,
            verses = "5",
            reference = "Isaiah 53:5",
            translation = "ESV",
            title = null,
            text = "But he was pierced for our transgressions; he was crushed for our iniquities; upon him was the chastisement that brought us peace, and with his wounds we are healed.",
            themes = listOf("healing", "atonement"),
            tags = listOf("healing-core"),
            sourceNote = null,
            sortOrder = 4,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "matt_6_33",
            book = "Matthew",
            chapter = 6,
            verses = "33",
            reference = "Matthew 6:33",
            translation = "ESV",
            title = null,
            text = "But seek first the kingdom of God and his righteousness, and all these things will be added to you.",
            themes = listOf("priority", "kingdom", "provision"),
            tags = listOf("priority"),
            sourceNote = null,
            sortOrder = 5,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "1john_4_4",
            book = "1 John",
            chapter = 4,
            verses = "4",
            reference = "1 John 4:4",
            translation = "ESV",
            title = null,
            text = "Little children, you are from God and have overcome them, for he who is in you is greater than he who is in the world.",
            themes = listOf("authority", "greater-is-he"),
            tags = listOf("authority-core"),
            sourceNote = null,
            sortOrder = 6,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "psalm_46",
            book = "Psalm",
            chapter = 46,
            verses = "1-11",
            reference = "Psalm 46",
            translation = "ESV",
            title = "God Is Our Fortress",
            text = """God is our refuge and strength, a very present help in trouble.
Therefore we will not fear though the earth gives way, though the mountains be moved into the heart of the sea, though its waters roar and foam, though the mountains tremble at its swelling. Selah

There is a river whose streams make glad the city of God, the holy habitation of the Most High.
God is in the midst of her; she shall not be moved; God will help her when morning dawns.
The nations rage, the kingdoms totter; he utters his voice, the earth melts.
The LORD of hosts is with us; the God of Jacob is our fortress. Selah

Come, behold the works of the LORD, how he has brought desolations on the earth.
He makes wars cease to the end of the earth; he breaks the bow and shatters the spear; he burns the chariots with fire.
“Be still, and know that I am God. I will be exalted among the nations, I will be exalted in the earth!”
The LORD of hosts is with us; the God of Jacob is our fortress. Selah""",
            themes = listOf("refuge", "strength", "be-still", "end-times"),
            tags = listOf("protection-core"),
            sourceNote = null,
            sortOrder = 7,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "psalm_27",
            book = "Psalm",
            chapter = 27,
            verses = "1-6",
            reference = "Psalm 27:1-6",
            translation = "ESV",
            title = null,
            text = """The LORD is my light and my salvation; whom shall I fear? The LORD is the stronghold of my life; of whom shall I be afraid?

When evildoers assail me to eat up my flesh, my adversaries and foes, it is they who stumble and fall.

Though an army encamp against me, my heart shall not fear; though war arise against me, yet I will be confident.

One thing have I asked of the LORD, that will I seek after: that I may dwell in the house of the LORD all the days of my life, to gaze upon the beauty of the LORD and to inquire in his temple.

For he will hide me in his shelter in the day of trouble; he will conceal me under the cover of his tent; he will lift me high upon a rock.

And now my head shall be lifted up above my enemies all around me, and I will offer in his tent sacrifices with shouts of joy; I will sing and make melody to the LORD.""",
            themes = listOf("light", "salvation", "fear", "dwelling"),
            tags = listOf("protection-core"),
            sourceNote = null,
            sortOrder = 8,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "psalm_121",
            book = "Psalm",
            chapter = 121,
            verses = "1-8",
            reference = "Psalm 121",
            translation = "ESV",
            title = "My Help Comes from the Lord",
            text = """I lift up my eyes to the hills. From where does my help come?
My help comes from the LORD, who made heaven and earth.

He will not let your foot be moved; he who keeps you will not slumber.
Behold, he who keeps Israel will neither slumber nor sleep.

The LORD is your keeper; the LORD is your shade on your right hand.
The sun shall not strike you by day, nor the moon by night.

The LORD will keep you from all evil; he will keep your life.
The LORD will keep your going out and your coming in from this time forth and forevermore.""",
            themes = listOf("help", "keeper", "protection"),
            tags = listOf("protection-core"),
            sourceNote = null,
            sortOrder = 9,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "isa_54_17",
            book = "Isaiah",
            chapter = 54,
            verses = "17",
            reference = "Isaiah 54:17",
            translation = "ESV",
            title = null,
            text = "no weapon that is fashioned against you shall succeed, and you shall refute every tongue that rises against you in judgment. This is the heritage of the servants of the LORD and their vindication from me, declares the LORD.",
            themes = listOf("no-weapon", "vindication"),
            tags = listOf("protection-core"),
            sourceNote = null,
            sortOrder = 10,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "rom_8_31_39",
            book = "Romans",
            chapter = 8,
            verses = "31-39",
            reference = "Romans 8:31-39",
            translation = "ESV",
            title = null,
            text = """What then shall we say to these things? If God is for us, who can be against us?
He who did not spare his own Son but gave him up for us all, how will he not also with him graciously give us all things?
Who shall bring any charge against God’s elect? It is God who justifies.
Who is to condemn? Christ Jesus is the one who died—more than that, who was raised—who is at the right hand of God, who indeed is interceding for us.
Who shall separate us from the love of Christ? Shall tribulation, or distress, or persecution, or famine, or nakedness, or danger, or sword?
As it is written, “For your sake we are being killed all the day long; we are regarded as sheep to be slaughtered.”
No, in all these things we are more than conquerors through him who loved us.
For I am sure that neither death nor life, nor angels nor rulers, nor things present nor things to come, nor powers, nor height nor depth, nor anything else in all creation, will be able to separate us from the love of God in Christ Jesus our Lord.""",
            themes = listOf("more-than-conquerors", "love", "authority"),
            tags = listOf("authority-core"),
            sourceNote = null,
            sortOrder = 11,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "2tim_1_7",
            book = "2 Timothy",
            chapter = 1,
            verses = "7",
            reference = "2 Timothy 1:7",
            translation = "ESV",
            title = null,
            text = "for God gave us a spirit not of fear but of power and love and self-control.",
            themes = listOf("no-fear", "power", "love"),
            tags = listOf("authority-core"),
            sourceNote = null,
            sortOrder = 12,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "james_4_7",
            book = "James",
            chapter = 4,
            verses = "7",
            reference = "James 4:7",
            translation = "ESV",
            title = null,
            text = "Submit yourselves therefore to God. Resist the devil, and he will flee from you.",
            themes = listOf("submit", "resist", "authority"),
            tags = listOf("warfare-core"),
            sourceNote = null,
            sortOrder = 13,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "luke_10_19",
            book = "Luke",
            chapter = 10,
            verses = "19",
            reference = "Luke 10:19",
            translation = "ESV",
            title = null,
            text = "Behold, I have given you authority to tread on serpents and scorpions, and over all the power of the enemy, and nothing shall hurt you.",
            themes = listOf("authority", "enemy"),
            tags = listOf("authority-core"),
            sourceNote = null,
            sortOrder = 14,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "psalm_34_7",
            book = "Psalm",
            chapter = 34,
            verses = "7",
            reference = "Psalm 34:7",
            translation = "ESV",
            title = null,
            text = "The angel of the LORD encamps around those who fear him, and delivers them.",
            themes = listOf("angels", "encamp"),
            tags = listOf("protection-core"),
            sourceNote = null,
            sortOrder = 15,
            isFavoriteDefault = false
        ),
        SanctuaryPassageEntity(
            id = "phil_4_6_7",
            book = "Philippians",
            chapter = 4,
            verses = "6-7",
            reference = "Philippians 4:6-7",
            translation = "ESV",
            title = null,
            text = "do not be anxious about anything, but in everything by prayer and supplication with thanksgiving let your requests be made known to God. And the peace of God, which surpasses all understanding, will guard your hearts and your minds in Christ Jesus.",
            themes = listOf("anxiety", "peace", "prayer"),
            tags = listOf("practical"),
            sourceNote = null,
            sortOrder = 16,
            isFavoriteDefault = false
        )
    )
}
